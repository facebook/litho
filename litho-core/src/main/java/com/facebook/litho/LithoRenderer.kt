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
import android.view.View
import androidx.annotation.GuardedBy
import androidx.annotation.UiThread
import com.facebook.litho.NestedLithoTree.commit
import com.facebook.litho.NestedLithoTree.enqueue
import com.facebook.litho.NestedLithoTree.runEffects
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.Host
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
          treeLifecycle = NoOpLifecycleProvider())
  private val errorComponentRef = AtomicReference<Component?>(null)

  /** The root component that this renderer is responsible for rendering. */
  @GuardedBy("this") private var root: Component? = null
  /** The size constraints used for layout calculations. */
  @GuardedBy("this") private var sizeConstraints: SizeConstraints? = null

  @Volatile var currentResolveResult: ResolveResult? = null
  @Volatile var currentLayoutState: LayoutState? = null

  var rootHost: Host? = null
  var currentLifecycle: LithoRendererLifecycle = LithoRendererLifecycle.INITIALIZED

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  var uiThreadLayoutState: LayoutState? = null

  // region Public APIs

  /**
   * Initiates an asynchronous resolution process for a component.
   *
   * @param root The root component to resolve.
   */
  fun resolve(root: Component) {
    // todo
  }

  /**
   * Performs an asynchronous layout calculation with the given constraints. Uses the current
   * resolve result if available, otherwise performs an inline resolve.
   *
   * @param constraints The size constraints to use for layout.
   */
  fun layout(constraints: SizeConstraints) {
    // todo
  }

  /**
   * Performs a synchronous layout calculation. If no resolve result is available, it will perform
   * an inline resolve first.
   *
   * @param constraints The size constraints to use for layout.
   * @return The resulting layout state or null if layout couldn't be completed.
   */
  fun layoutSync(constraints: SizeConstraints): LayoutState? {
    // todo
    return null
  }

  /**
   * Combines resolution and layout in one call. Sets the component and constraints, then initiates
   * the resolve and layout process.
   *
   * @param root The root component to render.
   * @param constraints The size constraints to use.
   */
  fun render(root: Component, constraints: SizeConstraints) {
    // todo
  }

  /**
   * Performs a synchronous resolution and layout in one call. This is a blocking operation that
   * completes the entire rendering pipeline.
   *
   * @param root The root component to render.
   * @param constraints The size constraints to use.
   * @return The resulting layout state or null if rendering couldn't be completed.
   */
  fun renderSync(root: Component, constraints: SizeConstraints): LayoutState? {
    // todo
    return null
  }

  /** Creates a new [ComponentContext] for the resolution. */
  private fun createComponentContext(): ComponentContext =
      ComponentContext(
          context,
          treePropContainer,
          lithoConfiguration,
          lithoTree,
          ROOT_KEY,
          visibilityController,
          null,
          parentTreePropContainer)

  /** Applies any pending state updates that have been enqueued. */
  private fun applyPendingUpdates(isAsync: Boolean) {
    // todo: do render
  }

  /** Promotes the committed layout state to the UI thread. */
  @UiThread
  @GuardedBy("this")
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

  // endregion

  /** Performs a resolve future for the given component. */
  private fun resolve(
      root: Component,
      treeState: TreeState,
      version: Int,
      previousResult: ResolveResult?,
      resolveFutureLock: Any,
      resolveTreeFutures: ArrayList<LithoResolveTreeFuture>,
      renderMode: RenderMode,
  ): TreeFuture.TreeFutureResult<ResolveResult> {
    val future =
        LithoResolveTreeFuture(
            componentContext = createComponentContext(),
            treeId = id,
            component = root,
            treeState = treeState,
            previousResult = previousResult,
            version = version,
            useCancellableFutures = renderMode.isAsync())

    return TreeFuture.trackAndRunTreeFuture(
        future, resolveTreeFutures, renderMode.toRenderSource(), resolveFutureLock, null)
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
  private fun layout(
      resolveResult: ResolveResult,
      sizeConstraints: SizeConstraints,
      version: Int,
      previousLayoutState: LayoutState?,
      layoutFutureLock: Any,
      layoutTreeFutures: ArrayList<LithoLayoutTreeFuture>,
      renderMode: RenderMode,
  ): TreeFuture.TreeFutureResult<LayoutState> {
    val future =
        LithoLayoutTreeFuture(
            treeId = id,
            version = version,
            resolveResult = resolveResult,
            sizeConstraints = sizeConstraints,
            previousLayoutState = previousLayoutState)

    return TreeFuture.trackAndRunTreeFuture(
        future, layoutTreeFutures, renderMode.toRenderSource(), layoutFutureLock, null)
  }

  /**
   * Commits the given layout state if it is newer than the committed layout state and if the
   * constraints are still compatible.
   */
  @Synchronized
  private fun commitLayoutState(layoutState: LayoutState) {
    val committedLayoutVersion =
        currentLayoutState?.version ?: com.facebook.rendercore.RenderState.NO_ID
    if (layoutState.version > committedLayoutVersion &&
        sizeConstraints == layoutState.sizeConstraints) {
      layoutState.toRenderTree()
      currentLayoutState = layoutState
      treeState?.commitLayoutState(layoutState.treeState)
    }
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
            key = globalKey,
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
            key = globalKey,
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
            key = globalKey,
            updater = stateUpdate,
            isLayoutState = isLayoutState,
            isAsync = false,
            isLazy = true,
        ))
  }

  override fun updateHookStateAsync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            key = globalKey,
            updater = updateBlock,
            isLayoutState = isLayoutState,
            isAsync = true,
            attribution = attribution,
        ))
  }

  override fun updateHookStateSync(
      globalKey: String,
      updateBlock: HookUpdater,
      attribution: String?,
      isLayoutState: Boolean
  ) {
    request(
        PendingStateUpdate(
            key = globalKey,
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
      globalKey: String,
      hookStateIndex: Int,
      newValue: T?,
      isLayoutState: Boolean
  ): Boolean {
    return treeState?.canSkipStateUpdate(globalKey, hookStateIndex, newValue, isLayoutState) == true
  }

  override fun <T> canSkipStateUpdate(
      newValueFunction: (T) -> T,
      globalKey: String,
      hookStateIndex: Int,
      isLayoutState: Boolean
  ): Boolean {
    return treeState?.canSkipStateUpdate(
        newValueFunction,
        globalKey,
        hookStateIndex,
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
  override fun request(update: PendingStateUpdate) {
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

  companion object {
    private const val ROOT_KEY: String = "TreeRoot"
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
