/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.GuardedBy
import androidx.annotation.UiThread
import com.facebook.litho.NestedLithoTree.commit
import com.facebook.litho.NestedLithoTree.enqueue
import com.facebook.litho.NestedLithoTree.runEffects
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.state.StateId
import com.facebook.rendercore.Host
import com.facebook.rendercore.RunnableHandler
import com.facebook.rendercore.RunnableHandler.DefaultHandler
import com.facebook.rendercore.SizeConstraints
import java.util.concurrent.atomic.AtomicReference

/** A renderer for Litho components that handles RESOLVE and LAYOUT. */
class LithoRenderer(
    val context: Context,
    val id: Int = LithoTree.generateComponentTreeId(),
    val componentsConfig: ComponentsConfiguration = ComponentsConfiguration.defaultInstance,
    val renderUnitIdGenerator: RenderUnitIdGenerator = RenderUnitIdGenerator(id),
    override val treeState: TreeState? = TreeState(),
    val parentTreePropContainer: TreePropContainer? = TreePropContainer(),
    val treePropContainer: TreePropContainer? = TreePropContainer(),
    val visibilityController: LithoVisibilityEventsController? = null,
    /* this is only set by the containers for child renderers */
    val synchronizer: UpdateSynchronizer? = null,
) : StateProvider, StateUpdater, StateUpdateRequester, RootHostProvider, ErrorComponentReceiver {

  // todo Rename this class to LithoTreeContext
  private val lithoConfiguration: LithoConfiguration =
      ComponentContextUtils.buildDefaultLithoConfiguration(
          context = context,
          componentsConfig = componentsConfig,
          renderUnitIdGenerator = renderUnitIdGenerator)
  private val lithoTree: LithoTree =
      LithoTree(
          id = id,
          stateProvider = this,
          stateUpdater = this,
          rootHost = this,
          errorComponentReceiver = this,
          treeLifecycle = NoOpLifecycleProvider(),
          uiStateReadRecordsProvider = {
            val host =
                rootHost as? BaseMountingView
                    ?: error("Trying to get UI state read records without a set host")
            host.uiStateReadRecords
          })
  private val errorComponentRef = AtomicReference<Component?>(null)

  /** The root component that this renderer is responsible for rendering. */
  @GuardedBy("this") @Volatile private var root: Component = EmptyComponent()
  /** The size constraints used for layout calculations. */
  @GuardedBy("this")
  @Volatile
  var sizeConstraints: SizeConstraints? = null
    private set

  private val resolveFutureLock = Any()
  @GuardedBy("resolveFutureLock")
  private val resolveTreeFutures = arrayListOf<LithoResolveTreeFuture>()

  private val layoutFutureLock = Any()
  @GuardedBy("layoutFutureLock")
  private val layoutTreeFutures = arrayListOf<LithoLayoutTreeFuture>()

  private val runnableLock = Any()
  @GuardedBy("runnableLock") private var currentLayoutRunnable: DoLayoutRunnable? = null
  @GuardedBy("runnableLock") private var currentResolveRunnable: DoResolveRunnable? = null

  @GuardedBy("this") private var nextResolveVersionCounter: Int = 0
  @GuardedBy("this") private var nextLayoutVersionCounter: Int = 0

  @GuardedBy("this") @Volatile private var currentResolveResult: ResolveResult? = null
  @GuardedBy("this") @Volatile var currentLayoutState: LayoutState? = null

  var rootHost: Host? = null
  var currentLifecycle: LithoRendererLifecycle = LithoRendererLifecycle.INITIALIZED

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  var uiThreadLayoutState: LayoutState? = null

  /** Callback that is invoked when a state update is completed */
  var onStateUpdateCompleted: ((LayoutState) -> Unit)? = null

  // region Public APIs

  /**
   * Initiates an asynchronous resolution process for a component.
   *
   * @param root The root component to resolve.
   */
  fun resolve(root: Component) {
    synchronized(this) { this.root = root }

    resolveAsync(RenderMode.SetRoot)
  }

  /**
   * Performs an asynchronous layout calculation with the given constraints. Uses the current
   * resolve result if available, otherwise performs an async resolve first.
   *
   * @param constraints The size constraints to use for layout.
   */
  fun layout(constraints: SizeConstraints) {
    synchronized(this) { this.sizeConstraints = constraints }

    currentResolveResult ?: resolveAsync(RenderMode.SetConstraints)

    layoutAsync(RenderMode.SetConstraints)
  }

  /**
   * Performs a synchronous layout calculation with the given constraints. Uses the current resolve
   * result if available, otherwise performs an inline resolve.
   *
   * @param constraints The size constraints to use for layout.
   * @return The resulting layout state.
   */
  fun layoutSync(constraints: SizeConstraints): LayoutState {

    val resolveInputs =
        captureResolveInputs(RenderMode.SetConstraintsSync, constraints = constraints)

    val resolveResult = resolveInputs.currentResolveResult ?: run { resolveSync(resolveInputs) }

    val layoutInputs = deriveFromResolveInputs(resolveInputs, resolveResult)

    return layoutSync(layoutInputs)
  }

  /**
   * Performs an asynchronous resolution and layout in one call, which is a non-blocking operation.
   *
   * @param root The root component to render.
   * @param constraints The size constraints to use.
   */
  fun render(root: Component, constraints: SizeConstraints) {
    synchronized(this) {
      this.root = root
      this.sizeConstraints = constraints
    }
    resolveAsync(RenderMode.SetRoot)

    layoutAsync(RenderMode.SetRoot)
  }

  /**
   * Performs a synchronous resolution and layout in one call. This is a blocking operation that
   * completes the entire rendering pipeline.
   *
   * @param root The root component to render.
   * @param constraints The size constraints to use.
   * @return The resulting layout state.
   */
  fun renderSync(root: Component, constraints: SizeConstraints): LayoutState {

    val resolveInputs = captureResolveInputs(RenderMode.SetRootSync, root, constraints)

    val resolveResult = resolveSync(resolveInputs)

    val layoutInputs = deriveFromResolveInputs(resolveInputs, resolveResult)

    return layoutSync(layoutInputs)
  }

  /** Promotes the committed layout state to the UI thread. */
  @UiThread
  @Synchronized
  fun maybePromoteCommittedLayoutStateToUI(layoutState: LayoutState?): Boolean {
    layoutState ?: return false
    if (layoutState === uiThreadLayoutState) return false

    val previousUiThreadLayoutState = uiThreadLayoutState
    uiThreadLayoutState = layoutState

    if (LayoutState.isNullOrEmpty(previousUiThreadLayoutState) &&
        LayoutState.isNullOrEmpty(layoutState)) {
      return false
    }
    layoutState.commit()
    layoutState.runEffects()
    return true
  }

  /** Cleans up any pending resolve or layout operations and resets the renderer state. */
  @AnyThread
  fun cleanup() {
    synchronized(runnableLock) {
      currentResolveRunnable?.let { runnable ->
        renderHandler.remove(runnable)
        currentResolveRunnable = null
      }
    }
    synchronized(runnableLock) {
      currentLayoutRunnable?.let { runnable ->
        renderHandler.remove(runnable)
        currentLayoutRunnable = null
      }
    }
    synchronized(this) {
      currentResolveResult = null
      currentLayoutState = null
    }
  }

  @UiThread
  fun bind(host: Host) {
    rootHost = host
  }

  @UiThread
  fun unbind() {
    rootHost = null
  }

  // endregion

  /** Creates a new [ComponentContext] for the resolution. */
  private fun createComponentContext(treePropContainer: TreePropContainer?): ComponentContext =
      ComponentContext(
          context,
          treePropContainer,
          lithoConfiguration,
          lithoTree,
          ROOT_KEY,
          visibilityController,
          null,
          parentTreePropContainer)

  /** Performs a synchronous state update. */
  private fun updateStateSync() {
    val resolveInputs = captureResolveInputs(RenderMode.StateUpdateSync)

    val resolveResult = resolveSync(resolveInputs)

    val layoutInputs = deriveFromResolveInputs(resolveInputs, resolveResult)

    layoutSync(layoutInputs)
  }

  /** Performs an asynchronous state update. */
  private fun updateState() {
    resolveAsync(RenderMode.StateUpdate)

    layoutAsync(RenderMode.StateUpdate)
  }

  /** Applies any pending state updates that have been enqueued. */
  private fun applyPendingUpdates(isAsync: Boolean) {
    if (isAsync) {
      updateState()
    } else {
      updateStateSync()
    }
  }

  /** Initiates a resolve for the given component in an async way. */
  private fun resolveAsync(renderMode: RenderMode) {
    synchronized(runnableLock) {
      // clear pending resolve runnable
      currentResolveRunnable?.let { runnable ->
        renderHandler.remove(runnable)
        currentResolveRunnable = null
      }

      val runnable = DoResolveRunnable(renderMode).also { currentResolveRunnable = it }
      var tag = EMPTY_STRING
      if (renderHandler.isTracing()) {
        tag = "doResolve ${root.simpleName}"
      }
      renderHandler.post(runnable, tag)
    }
  }

  /** Initiates a resolve for the given component in a sync way. */
  private fun resolveSync(resolveInputs: ResolveInputs): ResolveResult {
    val resolveResult = requestResolve(resolveInputs)
    return checkNotNull(resolveResult) { "We should always have a result for sync resolve!" }
  }

  /** Requests a component resolution with a new version number. */
  private fun requestResolve(resolveInputs: ResolveInputs): ResolveResult? {
    val isSameRoot =
        (resolveInputs.currentResolveResult != null) &&
            ComponentUtils.isEquivalent(
                resolveInputs.rootComponent, resolveInputs.currentResolveResult.component, true)
    val noPendingStateUpdate = resolveInputs.localTreeState.keysForPendingStateUpdates.isEmpty()
    val isSameTreePropContainer =
        com.facebook.rendercore.utils.equals(
            resolveInputs.treePropContainer,
            resolveInputs.currentResolveResult?.context?.treePropContainer)

    // The current root and tree-props are the same as the committed resolved result, and there is
    // no pending state update. Therefore, there is no need to calculate the resolved result again,
    // and we can proceed straight to layout.
    if (isSameRoot && noPendingStateUpdate && isSameTreePropContainer) {
      return resolveInputs.currentResolveResult
    }
    return resolve(resolveInputs, MAX_RESOLVE_TIMES)
  }

  /** Initiates an async layout with the latest resolve result and size constraints. */
  private fun layoutAsync(renderMode: RenderMode) {
    synchronized(runnableLock) {
      // clear pending layout runnable
      currentLayoutRunnable?.let { runnable ->
        renderHandler.remove(runnable)
        currentLayoutRunnable = null
      }
      val runnable = DoLayoutRunnable(renderMode).also { currentLayoutRunnable = it }
      var tag = EMPTY_STRING
      if (renderHandler.isTracing()) {
        tag = "doLayout ${root.simpleName}"
      }
      renderHandler.post(runnable, tag)
    }
  }

  /** Initiates a layout for the given resolve result in a sync way. */
  private fun layoutSync(layoutInputs: LayoutInputs): LayoutState {
    val layoutState = requestLayout(layoutInputs)
    return checkNotNull(layoutState) { "We should always have a result for sync layout!" }
  }

  /** Initiates a layout with a new version number. */
  private fun requestLayout(layoutInputs: LayoutInputs): LayoutState? {
    layoutInputs.currentResolveResult ?: return null
    layoutInputs.constraints ?: return null

    val isSameResolveResult =
        (layoutInputs.currentResolveResult == layoutInputs.currentLayoutState?.resolveResult)
    val areSizeConstraintsCompatible =
        (layoutInputs.constraints == layoutInputs.currentLayoutState?.sizeConstraints)
    if (isSameResolveResult && areSizeConstraintsCompatible) {
      // If the resolve result and size constraints are the same as the committed layout state, we
      // can reuse the committed layout state.
      return layoutInputs.currentLayoutState
    }

    return layout(layoutInputs)
  }

  /** Performs a resolve future for the given component. */
  private fun resolve(resolveInputs: ResolveInputs, retryTimes: Int): ResolveResult? {
    val future =
        LithoResolveTreeFuture(
            componentContext = createComponentContext(resolveInputs.treePropContainer),
            treeId = id,
            component = resolveInputs.rootComponent,
            treeState = resolveInputs.localTreeState,
            previousResult = resolveInputs.currentResolveResult,
            version = resolveInputs.resolveVersion,
            useCancellableFutures = resolveInputs.renderMode.isAsync())

    val treeFutureResult =
        TreeFuture.trackAndRunTreeFuture(
            future,
            resolveTreeFutures,
            resolveInputs.renderMode.toRenderSource(),
            resolveFutureLock,
            null)

    return if (treeFutureResult.result != null) {
      commitResolveResult(treeFutureResult.result)
      treeFutureResult.result
    } else {
      // To check if this is the latest resolve request because we don't want to leave render in
      // flight.
      val isLatestRequest: Boolean =
          synchronized(this) { (resolveInputs.resolveVersion == (nextResolveVersionCounter - 1)) }

      val isWaitingButInterrupted = (TreeFuture.FutureState.WAITING == treeFutureResult.state)
      return if (isWaitingButInterrupted && isLatestRequest && retryTimes > 0) {
        resolve(resolveInputs, retryTimes - 1)
      } else {
        null
      }
    }
  }

  /** Commits the given resolve result if it is newer than the committed resolve result. */
  @Synchronized
  private fun commitResolveResult(resolveResult: ResolveResult) {
    val committedResolveResult = currentResolveResult
    if (committedResolveResult == null || committedResolveResult.version < resolveResult.version) {
      currentResolveResult = resolveResult
    }
    treeState?.commitResolveState(resolveResult.treeState)
  }

  /** Performs a layout future for the given resolve result. */
  private fun layout(layoutInputs: LayoutInputs): LayoutState? {
    val future =
        LithoLayoutTreeFuture(
            treeId = id,
            version = layoutInputs.layoutVersion,
            resolveResult = requireNotNull(layoutInputs.currentResolveResult),
            sizeConstraints = requireNotNull(layoutInputs.constraints),
            previousLayoutState = layoutInputs.currentLayoutState)

    val treeFutureResult =
        TreeFuture.trackAndRunTreeFuture(
            future,
            layoutTreeFutures,
            layoutInputs.renderMode.toRenderSource(),
            layoutFutureLock,
            null)

    treeFutureResult.result?.let { layoutState ->
      layoutState.toRenderTree()

      if (commitLayoutState(layoutState)) {
        if (layoutInputs.renderMode == RenderMode.StateUpdate ||
            layoutInputs.renderMode == RenderMode.StateUpdateSync) {
          uiThreadHandler.post({ onStateUpdateCompleted?.invoke(layoutState) }, TAG_STATE_UPDATE)
        }
      }
    }

    return treeFutureResult.result
  }

  /**
   * Commits the given layout state if it is newer than the committed layout state and if the
   * constraints are still compatible.
   */
  @Synchronized
  private fun commitLayoutState(layoutState: LayoutState): Boolean {
    val currentVersion = currentLayoutState?.id ?: com.facebook.rendercore.RenderState.NO_ID
    if ((layoutState.version > currentVersion) &&
        (layoutState.sizeConstraints == sizeConstraints) &&
        (layoutState.resolveResult == currentResolveResult)) {
      currentLayoutState = layoutState
      treeState?.commitLayoutState(layoutState.treeState)
      return true
    }
    return false
  }

  // region StateUpdater
  override var isFirstMount: Boolean
    get() = treeState?.mountInfo?.isFirstMount == true
    set(value) {
      treeState?.mountInfo?.isFirstMount = value
    }

  override fun updateStateSync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            stateId = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = false,
            attribution = attribution,
        ))
  }

  override fun updateStateAsync(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            stateId = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        ))
  }

  override fun updateStateLazy(
      globalKey: String,
      stateUpdate: StateContainer.StateUpdate,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            stateId = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = false,
            isLazy = true,
        ))
  }

  override fun updateHookStateAsync(
      stateId: StateId,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            stateId = stateId,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        ))
  }

  override fun updateHookStateSync(
      stateId: StateId,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            stateId = stateId,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = false,
            attribution = attribution,
        ))
  }

  override fun applyLazyStateUpdatesForContainer(
      globalKey: String,
      container: StateContainer,
      isLayoutState: Boolean
  ): StateContainer {
    return treeState?.applyLazyStateUpdatesForContainer(globalKey, container, isLayoutState)
        ?: container
  }

  override fun getCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      isLayoutState: Boolean
  ): Any? {
    return treeState?.getCachedValue(globalKey, index, cachedValueInputs, isLayoutState)
  }

  override fun putCachedValue(
      globalKey: String,
      index: Int,
      cachedValueInputs: Any,
      cachedValue: Any?,
      isLayoutState: Boolean
  ) {
    treeState?.putCachedValue(globalKey, index, cachedValueInputs, cachedValue, isLayoutState)
  }

  override fun removePendingStateUpdate(key: String, isLayoutState: Boolean) {
    throw UnsupportedOperationException(
        """This API should not be invoked. Nested Litho Tree updates will
          |be cleared when nested layout state is committed."""
            .trimMargin())
  }

  override fun <T> canSkipStateUpdate(
      stateId: StateId,
      newValue: T?,
      isLayoutState: Boolean
  ): Boolean {
    return treeState?.canSkipStateUpdate(stateId, newValue, isLayoutState) == true
  }

  override fun <T> canSkipStateUpdate(
      newValueFunction: (T) -> T,
      stateId: StateId,
      isLayoutState: Boolean
  ): Boolean {
    return treeState?.canSkipStateUpdate(
        newValueFunction,
        stateId,
        isLayoutState,
    ) == true
  }

  override fun getEventTrigger(key: String): EventTrigger<*>? {
    return treeState?.getEventTrigger(triggerKey = key)
  }

  override fun getEventTrigger(handle: Handle, id: Int): EventTrigger<*>? {
    return treeState?.getEventTrigger(handle = handle, methodId = id)
  }

  // endregion

  // region StateUpdateRequester
  override fun request(update: PendingStateUpdate<*>) {
    if (currentLifecycle != LithoRendererLifecycle.STARTED) {
      // When the current view is out of the preparation range, we don't want to apply any state
      // updates.
      return
    }
    treeState?.enqueue(update)

    if (synchronizer != null) {
      synchronizer.request(update.isAsync)
    } else {
      applyPendingUpdates(update.isAsync)
    }
  }

  // endregion

  // region ErrorComponentReceiver
  override fun onErrorComponent(component: Component?) {
    errorComponentRef.compareAndSet(null, component)
  }

  // endregion

  // region RootHostProvider
  override val mountedView: View?
    get() = rootHost

  // endregion

  /**
   * A runnable that executes the resolve operation on a background thread. This class encapsulates
   * the parameters needed for component resolution and delegates to the doResolve method when
   * executed.
   *
   * @param renderMode The mode that determines if the operation is synchronous or asynchronous
   */
  private inner class DoResolveRunnable(
      private val renderMode: RenderMode,
  ) : ThreadTracingRunnable() {

    override fun tracedRun() {
      requestResolve(captureResolveInputs(renderMode))
    }
  }

  /**
   * A runnable that executes the layout operation on a background thread, using the latest resolve
   * result and size constraints.
   *
   * @param renderMode The mode that determines if the operation is synchronous or asynchronous
   */
  private inner class DoLayoutRunnable(
      private val renderMode: RenderMode,
  ) : ThreadTracingRunnable() {

    override fun tracedRun() {
      requestLayout(captureLayoutInputs(renderMode))
    }
  }

  /** Captures all inputs for Resolve. */
  private fun captureResolveInputs(
      renderMode: RenderMode,
      rootComponent: Component? = null,
      constraints: SizeConstraints? = null,
  ): ResolveInputs {
    return synchronized(this@LithoRenderer) {
      rootComponent?.let { this@LithoRenderer.root = it }
      constraints?.let { this@LithoRenderer.sizeConstraints = it }
      ResolveInputs(
          resolveVersion = this@LithoRenderer.nextResolveVersionCounter++,
          layoutVersion = this@LithoRenderer.nextLayoutVersionCounter++,
          rootComponent =
              this@LithoRenderer.errorComponentRef.getAndSet(null) ?: this@LithoRenderer.root,
          constraints = constraints ?: this@LithoRenderer.sizeConstraints,
          localTreeState = TreeState(this@LithoRenderer.treeState),
          currentResolveResult = this@LithoRenderer.currentResolveResult,
          treePropContainer = TreePropContainer.copy(this@LithoRenderer.treePropContainer),
          renderMode = renderMode,
      )
    }
  }

  /** Captures all inputs for Layout. */
  private fun captureLayoutInputs(
      renderMode: RenderMode,
      resolveResult: ResolveResult? = null,
      constraints: SizeConstraints? = null,
  ): LayoutInputs {
    return synchronized(this@LithoRenderer) {
      constraints?.let { this@LithoRenderer.sizeConstraints = it }
      LayoutInputs(
          layoutVersion = this@LithoRenderer.nextLayoutVersionCounter++,
          currentResolveResult = resolveResult ?: this@LithoRenderer.currentResolveResult,
          currentLayoutState = this@LithoRenderer.currentLayoutState,
          constraints = constraints ?: this@LithoRenderer.sizeConstraints,
          renderMode = renderMode,
      )
    }
  }

  /** Derive layout inputs from a resolve inputs. */
  private fun deriveFromResolveInputs(
      resolveInputs: ResolveInputs,
      resolveResult: ResolveResult,
  ): LayoutInputs {
    return synchronized(this@LithoRenderer) {
      LayoutInputs(
          layoutVersion = resolveInputs.layoutVersion,
          currentResolveResult = resolveResult,
          currentLayoutState = this@LithoRenderer.currentLayoutState,
          constraints = resolveInputs.constraints ?: this@LithoRenderer.sizeConstraints,
          renderMode = resolveInputs.renderMode,
      )
    }
  }

  /** A class that captures all inputs for Resolve. */
  private class ResolveInputs(
      val resolveVersion: Int,
      val layoutVersion: Int,
      val rootComponent: Component,
      val constraints: SizeConstraints?,
      val localTreeState: TreeState,
      val currentResolveResult: ResolveResult?,
      val treePropContainer: TreePropContainer?,
      val renderMode: RenderMode,
  )

  /** A class that captures all inputs for Layout. */
  private class LayoutInputs(
      val layoutVersion: Int,
      val currentResolveResult: ResolveResult?,
      val currentLayoutState: LayoutState?,
      val constraints: SizeConstraints?,
      val renderMode: RenderMode,
  )

  companion object {
    private val uiThreadHandler: RunnableHandler = DefaultHandler(Looper.getMainLooper())
    private val renderHandler = DefaultHandler(ComponentTree.getDefaultLayoutThreadLooper())

    // The max number of times we will retry a failed resolve request.
    private const val MAX_RESOLVE_TIMES: Int = 2

    private const val EMPTY_STRING: String = ""
    private const val ROOT_KEY: String = "TreeRoot"
    private const val TAG_STATE_UPDATE: String = "StateUpdate"
  }
}

/** A no-op implementation of [LithoTreeLifecycleProvider] that does nothing. */
private class NoOpLifecycleProvider : LithoTreeLifecycleProvider {

  override val isReleased: Boolean
    get() = false

  override fun addOnReleaseListener(listener: LithoTreeLifecycleProvider.OnReleaseListener) {
    // no ops
  }
}

@JvmInline
value class LithoRendererLifecycle(val value: Int) {
  companion object {
    val INITIALIZED: LithoRendererLifecycle = LithoRendererLifecycle(0)
    val STARTED: LithoRendererLifecycle = LithoRendererLifecycle(1)
    val PAUSED: LithoRendererLifecycle = LithoRendererLifecycle(2)
  }
}

fun interface UpdateSynchronizer {
  fun request(isAsync: Boolean)
}

/** A class that represents the trigger type of the render call. */
@JvmInline
value class RenderMode private constructor(val value: Int) {

  /** Returns true if the given source is an asynchronous operation. */
  fun isAsync(): Boolean {
    return this == SetRoot || this == SetConstraints || this == StateUpdate || this == Measure
  }

  fun toRenderSource(): Int {
    return when (this) {
      SetRootSync -> RenderSource.SET_ROOT_SYNC
      SetRoot -> RenderSource.SET_ROOT_ASYNC
      SetConstraintsSync -> RenderSource.SET_SIZE_SPEC_SYNC
      SetConstraints -> RenderSource.SET_SIZE_SPEC_ASYNC
      StateUpdateSync -> RenderSource.UPDATE_STATE_SYNC
      StateUpdate -> RenderSource.UPDATE_STATE_ASYNC
      MeasureSync -> RenderSource.MEASURE_SET_SIZE_SPEC
      Measure -> RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC
      else -> throw IllegalArgumentException("Unknown render mode: $this")
    }
  }

  companion object {
    val SetRootSync: RenderMode = RenderMode(0)
    val SetRoot: RenderMode = RenderMode(1)
    val SetConstraintsSync: RenderMode = RenderMode(2)
    val SetConstraints: RenderMode = RenderMode(3)
    val StateUpdateSync: RenderMode = RenderMode(4)
    val StateUpdate: RenderMode = RenderMode(5)
    val MeasureSync: RenderMode = RenderMode(6)
    val Measure: RenderMode = RenderMode(7)
  }
}
