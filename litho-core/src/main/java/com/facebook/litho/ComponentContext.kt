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
import android.content.res.Resources
import android.content.res.TypedArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.litho.NoOpEventHandler.Companion.getNoOpEventHandler
import com.facebook.litho.annotations.EventHandlerRebindMode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.ComponentsConfiguration.LogicViolationPolicy
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.litho.state.ComponentState
import com.facebook.litho.state.StateProvider
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.ResourceCache
import com.facebook.rendercore.ResourceResolver
import java.util.LinkedList
import java.util.Queue

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
open class ComponentContext {

  val androidContext: Context

  var lithoConfiguration: LithoConfiguration

  private var noStateUpdatesMethod: String? = null

  // Hold a reference to the component which scope we are currently within.
  @ThreadConfined(ThreadConfined.ANY)
  var componentScope: Component? = null
    private set

  @ThreadConfined(ThreadConfined.ANY) private var _globalKey: GlobalKey? = null

  @ThreadConfined(ThreadConfined.ANY) val resourceResolver: ResourceResolver

  @set:VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  @ThreadConfined(ThreadConfined.ANY)
  open var treePropContainer: TreePropContainer? = null

  @ThreadConfined(ThreadConfined.ANY) var parentTreePropContainer: TreePropContainer? = null

  /**
   * @return true if parent's TreeProps are cloned and assigned to mTreeProps. Notice this method
   *   should be accessed by Kotlin API only.
   */
  @ThreadConfined(ThreadConfined.ANY) internal var isParentTreePropContainerCloned: Boolean = false

  @ThreadConfined(ThreadConfined.ANY) val lithoTree: LithoTree?

  val lithoVisibilityEventsController: LithoVisibilityEventsController?

  // Used to hold styling information applied to components
  @StyleRes @ThreadConfined(ThreadConfined.ANY) private var defStyleRes = 0

  @AttrRes @ThreadConfined(ThreadConfined.ANY) private var defStyleAttr = 0

  private var _scopedComponentInfo: ScopedComponentInfo? = null

  var isContextForLayout: Boolean = false
    private set

  @ThreadConfined(ThreadConfined.ANY)
  var isIgnoreMeasureInResolve: Boolean = false
    private set

  /**
   * Temporary workaround to suppress the "measure in resolve" violation policy.
   *
   * This method disables the enforcement of the measure-in-resolve policy for this
   * ComponentContext. Normally, measuring a component during the resolve phase is forbidden as it
   * can lead to undefined behaviors and performance issues. When the policy is set to CRASH, Litho
   * will throw an IllegalStateException if a component attempts to measure during resolve.
   *
   * This workaround should only be used as a temporary solution for legacy components that cannot
   * be immediately migrated to the proper APIs. New code should NOT use this method.
   *
   * **Preferred alternatives:**
   * * For KComponents: Use the RenderWithConstraints API
   * * For the old Layout Specs: Use @OnCreateLayoutWithSizeSpec
   * * For other components: Refactor accordingly to avoid measuring during the resolve phase
   */
  @Deprecated(
      """This is a temporary workaround and should not be used in new code. Components
        should be refactored to avoid measuring during resolve.""",
  )
  fun lithoWorkAroundIgnoreMeasureInResolve() {
    this.isIgnoreMeasureInResolve = true
  }

  private val calculationStateContextThreadLocal: ThreadLocal<CalculationContext?>

  /** This constructors takes a [Context]. This should only be used to create [ComponentTree] */
  @JvmOverloads
  constructor(
      context: Context,
      lithoConfiguration: LithoConfiguration? = null,
      treePropContainer: TreePropContainer? = null,
  ) : this(context, treePropContainer, lithoConfiguration, null, null, null, null, null)

  internal constructor(
      androidContext: Context,
      treePropContainer: TreePropContainer?,
      lithoConfiguration: LithoConfiguration?,
      lithoTree: LithoTree?,
      globalKey: GlobalKey?,
      lithoVisibilityEventsController: LithoVisibilityEventsController?,
      componentScope: Component?,
      parentTreePropContainer: TreePropContainer?,
  ) {
    calculationStateContextThreadLocal = ThreadLocal()
    this.androidContext =
        checkNotNull(androidContext) { "ComponentContext requires a non null Android Context" }
    resourceResolver =
        ResourceResolver(
            androidContext,
            ResourceCache.getLatest(androidContext.resources.configuration),
        )
    this.treePropContainer = treePropContainer
    this.lithoConfiguration =
        lithoConfiguration
            ?: LithoConfiguration(
                this.androidContext,
                ComponentsConfiguration.defaultInstance,
            )

    this.lithoTree = lithoTree
    this._globalKey = globalKey
    this.lithoVisibilityEventsController = lithoVisibilityEventsController
    this.componentScope = componentScope
    this.parentTreePropContainer = parentTreePropContainer
  }

  @JvmOverloads
  constructor(
      context: ComponentContext,
      treePropContainer: TreePropContainer? = context.treePropContainer,
  ) {
    this.androidContext = context.androidContext
    resourceResolver = context.resourceResolver
    componentScope = context.componentScope
    lithoVisibilityEventsController = context.lithoVisibilityEventsController
    lithoTree = context.lithoTree
    this.treePropContainer = treePropContainer ?: context.treePropContainer
    parentTreePropContainer = context.parentTreePropContainer
    _globalKey = context._globalKey
    calculationStateContextThreadLocal = context.calculationStateContextThreadLocal
    lithoConfiguration = context.lithoConfiguration
  }

  fun makeNewCopy(): ComponentContext = ComponentContext(this)

  var calculationStateContext: CalculationContext?
    /** Returns the current calculate state context */
    get() = calculationStateContextThreadLocal.get()
    /**
     * Sets the calculation state context (either a RenderStateContext or LayoutStateContext) on a
     * thread local, making it unique per ComponentTree, per Thread.
     */
    set(context) {
      calculationStateContextThreadLocal.set(context)
    }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun setLithoLayoutContext(lithoLayoutContext: LithoLayoutContext) {
    calculationStateContextThreadLocal.set(lithoLayoutContext)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun clearCalculationStateContext() {
    calculationStateContextThreadLocal.set(null)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun setLayoutStateContextForTesting() {
    setLithoLayoutContext(
        LithoLayoutContext(
            -1,
            MeasuredResultCache(),
            this,
            TreeState(),
            0,
            -1,
            true,
            LayoutCache(),
            null,
            null,
        ),
    )
  }

  /**
   * For test usage. This method will ensure this ComponentContext has a RenderStateContex. This is
   * critical for tests that trigger layout calculation functionality outside of a LayoutState
   * calculation (i.e., including willRender, Layout API, caching, etc).
   */
  @VisibleForTesting
  fun setRenderStateContextForTests(): ResolveContext {
    val resolveContext = ResolveContext(
        if (lithoTree != null) lithoTree.id else -1,
        MeasuredResultCache(),
        TreeState(),
        0,
        -1,
        true,
        null,
        null,
        false,
    )
    renderStateContext = resolveContext

    return resolveContext
  }

  val isCreateLayoutInProgress: Boolean
    /** Returns true if this method is called during layout creation. */
    get() = calculationStateContextThreadLocal.get() != null

  val applicationContext: Context
    get() = androidContext.applicationContext

  val resources: Resources
    get() = androidContext.resources

  fun getText(@StringRes resId: Int): CharSequence {
    val text =
        resourceResolver.resolveText(resId)
            ?: throw RuntimeException(
                "String resource not found for ID #0x${Integer.toHexString(resId)}",
            )
    return text
  }

  fun getString(@StringRes resId: Int): String {
    val text =
        resourceResolver.resolveStringRes(resId)
            ?: throw RuntimeException(
                "String resource not found for ID #0x${Integer.toHexString(resId)}",
            )

    return text
  }

  fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
    val text =
        resourceResolver.resolveStringRes(resId, *formatArgs)
            ?: throw RuntimeException(
                "String resource not found for ID #0x${Integer.toHexString(resId)}",
            )

    return text
  }

  fun getColor(@ColorRes id: Int): Int = resourceResolver.resolveColorRes(id)

  open val globalKey: GlobalKey
    get() {
      if (componentScope == null) {
        throw RuntimeException(
            "getGlobalKey cannot be accessed from a ComponentContext without a scope",
        )
      }

      return checkNotNull(_globalKey) { "Component context missing GlobalKey" }
    }

  /**
   * Internal accessor for globalKey that doesn't throw when componentScope is null. This is used by
   * internal Litho code (e.g., Component.java) that needs to access the raw globalKey value without
   * the scope check, preserving the original behavior of direct field access (mGlobalKey) before
   * the Kotlin conversion.
   */
  @get:JvmName("getNullableGlobalKey")
  internal val nullableGlobalKey: GlobalKey?
    get() = _globalKey

  val errorEventHandler: EventHandler<ErrorEvent>
    get() {
      if (componentScope != null) {
        try {
          val errorEventHandler = scopedComponentInfo.errorEventHandler
          if (errorEventHandler != null) {
            return errorEventHandler
          }
        } catch (e: IllegalStateException) {
          // Fallback to default error handler if scopedComponentInfo throws
          return lithoConfiguration.componentsConfig.errorEventHandler
        }
      }

      return lithoConfiguration.componentsConfig.errorEventHandler
    }

  @get:VisibleForTesting
  val layoutStateFuture: TreeFuture<*>?
    get() = calculationStateContextThreadLocal.get()?.treeFuture

  /**
   * Notify the Component Tree that it needs to synchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  open fun updateStateSync(stateUpdate: StateContainer.StateUpdate, attribution: String) {
    checkIfNoStateUpdatesMethod()

    if (lithoTree == null) {
      return
    }

    lithoTree.stateUpdater.updateStateSync(
        checkNotNull(_globalKey) { "Component context missing GlobalKey during state update" },
        stateUpdate,
        attribution,
        isContextForLayout,
    )
  }

  /**
   * Variant of [updateStateSync] that asserts the update is performed with the [ComponentContext]
   * of [reference] — the component that owns the state. Generated by the Spec annotation processor
   * for `@OnUpdateState` methods; see [assertValidStateUpdateContext].
   */
  fun updateStateSync(
      stateUpdate: StateContainer.StateUpdate,
      attribution: String,
      reference: Class<out Component>,
  ) {
    assertValidStateUpdateContext(reference, attribution)
    updateStateSync(stateUpdate, attribution)
  }

  /**
   * Notify the Component Tree that it needs to asynchronously perform a state update.
   *
   * @param stateUpdate state update to perform
   */
  open fun updateStateAsync(stateUpdate: StateContainer.StateUpdate, attribution: String) {
    checkIfNoStateUpdatesMethod()

    if (lithoTree == null) {
      return
    }

    lithoTree.stateUpdater.updateStateAsync(
        checkNotNull(_globalKey) { "Component context missing GlobalKey during state update" },
        stateUpdate,
        attribution,
        isContextForLayout,
    )
  }

  /**
   * Variant of [updateStateAsync] that asserts the update is performed with the [ComponentContext]
   * of [reference] — the component that owns the state. Generated by the Spec annotation processor
   * for `@OnUpdateState` methods; see [assertValidStateUpdateContext].
   */
  fun updateStateAsync(
      stateUpdate: StateContainer.StateUpdate,
      attribution: String,
      reference: Class<out Component>,
  ) {
    assertValidStateUpdateContext(reference, attribution)
    updateStateAsync(stateUpdate, attribution)
  }

  fun updateStateWithTransition(stateUpdate: StateContainer.StateUpdate, attribution: String) {
    updateStateAsync(stateUpdate, attribution)
  }

  /**
   * Variant of [updateStateWithTransition] that asserts the update is performed with the
   * [ComponentContext] of [reference] — the component that owns the state. Generated by the Spec
   * annotation processor for `@OnUpdateStateWithTransition` methods.
   */
  fun updateStateWithTransition(
      stateUpdate: StateContainer.StateUpdate,
      attribution: String,
      reference: Class<out Component>,
  ) {
    updateStateAsync(stateUpdate, attribution, reference)
  }

  open fun updateStateLazy(stateUpdate: StateContainer.StateUpdate) {
    if (lithoTree == null) {
      return
    }

    lithoTree.stateUpdater.updateStateLazy(
        checkNotNull(_globalKey) { "Component context missing GlobalKey during state update" },
        stateUpdate,
        isContextForLayout,
    )
  }

  /**
   * Variant of [updateStateLazy] that asserts the update is performed with the [ComponentContext]
   * of [reference] — the component that owns the state. Generated by the Spec annotation processor
   * for lazy state updates; see [assertValidStateUpdateContext].
   */
  fun updateStateLazy(stateUpdate: StateContainer.StateUpdate, reference: Class<out Component>) {
    assertValidStateUpdateContext(reference, "updateStateLazy")
    updateStateLazy(stateUpdate)
  }

  /**
   * Reports or throws when a state update is performed with the [ComponentContext] of a component
   * other than the one that owns the state. Mirrors the assertion in [createEventHandler]: a state
   * update routed through the wrong scope is keyed on the wrong [GlobalKey], so it resolves against
   * — or silently no-ops against — the wrong component instead of the one declaring the `@State`.
   *
   * @param reference the component class that owns the state being updated
   * @param source attribution describing the originating state update, used for debug reporting
   */
  private fun assertValidStateUpdateContext(reference: Class<out Component>, source: String) {
    val component = componentScope ?: return
    if (reference == component.javaClass) {
      return
    }

    when (lithoConfiguration.componentsConfig.stateUpdateViolationPolicy) {
      LogicViolationPolicy.CRASH ->
          throw IllegalStateException(
              "State update '$source' was performed with the ComponentContext of " +
                  "'${component.simpleName}' but belongs to <cls>${reference.name}</cls>. " +
                  "Its @State will resolve against the wrong component. Perform the state " +
                  "update with the ComponentContext of the component that owns the state.",
          )
      LogicViolationPolicy.LOG ->
          DebugInfoReporter.report("WrongContextForStateUpdate") {
            this["reference"] = "<cls>${reference.name}</cls>"
            this["component"] = component.simpleName
          }
      LogicViolationPolicy.NONE -> {}
    }
  }

  /**
   * @return A StateContainer with lazy state updates applied. This may be the same container passed
   *   in if there were no updates to apply. This method won't mutate the passed container.
   */
  fun applyLazyStateUpdatesForContainer(container: StateContainer): StateContainer {
    if (lithoTree == null) {
      return container
    }

    return lithoTree.stateUpdater.applyLazyStateUpdatesForContainer(
        checkNotNull(_globalKey) { "Component context missing GlobalKey during state update" },
        container,
        isContextForLayout,
    )
  }

  fun enterNoStateUpdatesMethod(noStateUpdatesMethod: String) {
    this.noStateUpdatesMethod = noStateUpdatesMethod
  }

  fun exitNoStateUpdatesMethod() {
    noStateUpdatesMethod = null
  }

  private fun checkIfNoStateUpdatesMethod() {
    check(noStateUpdatesMethod == null) {
      ("Updating the state of a component during ${noStateUpdatesMethod} leads to unexpected behaviour, consider using lazy state updates.")
    }
  }

  fun setDefStyle(@AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int) {
    this.defStyleAttr = defStyleAttr
    this.defStyleRes = defStyleRes
  }

  fun obtainStyledAttributes(attrs: IntArray, @AttrRes defStyleAttr: Int): TypedArray =
      androidContext.obtainStyledAttributes(
          null,
          attrs,
          if (defStyleAttr != 0) defStyleAttr else this.defStyleAttr,
          defStyleRes,
      )

  val logTag: String?
    get() = lithoConfiguration.componentsConfig.logTag

  /**
   * A utility function to find the View with a given tag under the current Component's LithoView.
   * To set a view tag, use the .viewTag() common prop or Style.viewTag. An appropriate time to call
   * this is in your Component's onVisible callback.
   *
   * As with View.findViewWithTag in general, this must be called on the main thread.
   *
   * Note that null may be returned if the associated View doesn't exist or isn't mounted: with
   * incremental mount turned on (which is the default), if the component is off-screen, it won't be
   * mounted.
   *
   * Finally, note that you should never hold a reference to the view returned by this function as
   * Litho may unmount your Component and mount it to a different View.
   */
  fun <T : View?> findViewWithTag(tag: Any): T? {
    ThreadUtils.assertMainThread()

    if (lithoTree == null) {
      throw RuntimeException(
          "Calling findViewWithTag on a ComponentContext which isn't associated with a Tree. Make sure it's one received in `render` or `onCreateLayout`",
      )
    }
    val mountedView = lithoTree.rootHost.mountedView ?: return null
    // The tree isn't mounted

    return mountedView.findViewWithTag(tag)
  }

  /**
   * A utility function to find the View with a given tag under the current Component's LithoView.
   * To set a view tag, use the .viewTags() common prop or Style.viewTags. An appropriate time to
   * call this is in your Component's onVisible callback.
   *
   * As with View.findViewWithTag in general, this must be called on the main thread.
   *
   * Note that null may be returned if the associated View doesn't exist or isn't mounted: with
   * incremental mount turned on (which is the default), if the component is off-screen, it won't be
   * mounted.
   *
   * Finally, note that you should never hold a reference to the view returned by this function as
   * Litho may unmount your Component and mount it to a different View.
   */
  fun findViewWithTagValue(key: Int, tag: Any): View? {
    if (lithoTree == null) {
      return null
    }
    val mountedView = lithoTree.rootHost.mountedView ?: return null
    // The tree isn't mounted

    return findViewWithTagValueUnder(mountedView, key, tag)
  }

  val errorComponentReceiver: ErrorComponentReceiver?
    get() {
      if (lithoTree == null) {
        return null
      }
      return lithoTree.errorComponentReceiver
    }

  /**
   * Calculates a returns a unique ID for a given component key and output type. The IDs will be
   * unique for components in the ComponentTree this ID generator is linked with. If an ID was
   * already generated for a given component, the same ID will be returned. Otherwise, a new unique
   * ID will be generated.
   *
   * ComponentContext must have a ComponentTree instance.
   *
   * @param componentKey The component key
   * @param type The output type @see OutputUnitType
   */
  fun calculateLayoutOutputId(componentKey: GlobalKey, @OutputUnitType type: Int): Long {
    return checkNotNull(lithoConfiguration.renderUnitIdGenerator) {
          "Cannot generate IDs with a null renderUnitIdGenerator"
        }
        .calculateLayoutOutputId(componentKey, type)
  }

  fun <T : Any> getTreeProp(key: Class<T>): T? = treePropContainer?.get(key)

  fun <T> getTreeProp(key: TreeProp<T>): T {
    val container = treePropContainer
    return if (container == null) key.defaultValue else container.get(key)
  }

  fun <T : Any> getParentTreeProp(key: Class<T>): T? = parentTreePropContainer?.get(key)

  val treePropContainerCopy: TreePropContainer?
    /** Obtain a copy of the tree props currently held by this context. */
    get() = TreePropContainer.copy(treePropContainer)

  val layoutVersion: Int
    get() {
      val calculationContext = calculationStateContextThreadLocal.get()
      if (calculationContext != null) {
        return calculationContext.layoutVersion
      }

      throw IllegalStateException(
          "LayoutVersion is only available during layout calculation.Please only invoke getLayoutVersion from OnCreateLayout/OnMeasure/OnPrepare",
      )
    }

  val resourceCache: ResourceCache?
    get() = resourceResolver.resourceCache

  fun getCachedValue(cacheKey: HookGlobalKey, cachedValueInputs: Any): Any? {
    if (lithoTree == null) {
      return null
    }
    return lithoTree.stateUpdater.getCachedValue(
        cacheKey,
        cachedValueInputs,
        isContextForLayout,
    )
  }

  fun putCachedValue(cacheKey: HookGlobalKey, cachedValueInputs: Any, cachedValue: Any?) {
    if (lithoTree == null) {
      return
    }
    lithoTree.stateUpdater.putCachedValue(
        cacheKey,
        cachedValueInputs,
        cachedValue,
        isContextForLayout,
    )
  }

  val stateUpdater: StateUpdater?
    get() = lithoTree?.stateUpdater

  val stateProvider: StateProvider?
    get() = lithoTree?.stateProvider

  /** @return New instance of [EventTrigger] that is created by the current mComponentScope. */
  open fun <E> newEventTrigger(id: Int, childKey: String, handle: Handle?): EventTrigger<E> {
    val parentKey: Any = if (componentScope == null) "" else globalKey
    return EventTrigger(parentKey, id, childKey, handle)
  }

  fun applyStyle(node: LithoNode, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int) {
    if (defStyleAttr != 0 || defStyleRes != 0) {
      setDefStyle(defStyleAttr, defStyleRes)
      node.applyAttributes(androidContext, defStyleAttr, defStyleRes)
      setDefStyle(0, 0)
    }
  }

  val layoutStateContext: LithoLayoutContext?
    get() {
      val stateContext = calculationStateContextThreadLocal.get()
      if (stateContext is LithoLayoutContext) {
        return stateContext
      }

      return null
    }

  @set:VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  var renderStateContext: ResolveContext?
    get() {
      val stateContext = calculationStateContextThreadLocal.get()
      if (stateContext is ResolveContext) {
        return stateContext
      }

      return null
    }
    set(renderStateContext) {
      calculationStateContextThreadLocal.set(renderStateContext)
    }

  val scopedComponentInfo: ScopedComponentInfo
    get() = checkNotNull(_scopedComponentInfo)

  /**
   * This method determine if transitions are enabled for the user. If the experiment is enabled for
   * the user then they will get cached value else it will be determined using the utility method.
   *
   * @return true if transitions are enabled.
   */
  fun areTransitionsEnabled(): Boolean = lithoConfiguration.areTransitionsEnabled

  val renderUnitIdGenerator: RenderUnitIdGenerator?
    get() = lithoConfiguration.renderUnitIdGenerator

  @get:VisibleForTesting
  val mountedView: View?
    get() {
      if (lithoTree == null) {
        return null
      }
      return lithoTree.rootHost.mountedView
    }

  fun removePendingStateUpdate(key: GlobalKey, isLayoutState: Boolean) {
    if (lithoTree == null) {
      return
    }
    lithoTree.stateUpdater.removePendingStateUpdate(key, isLayoutState)
  }

  fun <E> createEventHandler(
      id: Int,
      mode: EventHandlerRebindMode,
      params: Array<Any?>?,
      reference: Class<out Component>,
      source: String,
  ): EventHandler<E> {
    val component = componentScope
    val info = _scopedComponentInfo

    // EventHandler no-ops if ComponentContext is not associated with a Component.
    if (component !is HasEventDispatcher) {
      DebugInfoReporter.report("EventHandlerWithoutContext") { this["source"] = source }
      return getNoOpEventHandler()
    }

    // Log a debug event or throw error if the event handler is created using the correct context
    if (reference != component.javaClass) {
      when (lithoConfiguration.componentsConfig.eventHandlerViolationPolicy) {
        LogicViolationPolicy.CRASH ->
            throw IllegalStateException(
                "Event handler '$source' was created with the ComponentContext of " +
                    "'${component.simpleName}' but belongs to '${reference.simpleName}'. Its " +
                    "@Prop/@State will resolve against the wrong component. Create the handler with " +
                    "the ComponentContext of the component that owns it.",
            )
        LogicViolationPolicy.LOG ->
            DebugInfoReporter.report("WrongContextForEventHandler") {
              this["source"] = source
              this["reference"] = component.simpleName
            }
        LogicViolationPolicy.NONE -> {}
      }
    }

    val context = calculationStateContext
    val dispatchInfo: EventDispatchInfo
    if (canUseStateForEventDispatchInfo(mode, context, info)) {
      val state: ComponentState<*> = checkNotNull(scopedComponentInfo.state)
      dispatchInfo = checkNotNull(state.eventDispatchInfo)
    } else {
      dispatchInfo = EventDispatchInfo(component as HasEventDispatcher, this)
    }

    val eventHandler = EventHandler<E>(id, mode, dispatchInfo, params)

    if (context == null) {
      eventHandler.dispatchInfo.isBound = true
    }

    return eventHandler
  }

  companion object {
    const val NO_SCOPE_EVENT_HANDLER: String = "ComponentContext:NoScopeEventHandler"

    /**
     * Creates a new ComponentContext instance scoped to the given component and sets it on the
     * component.
     *
     * @param parentContext context scoped to the parent component
     * @param scope component associated with the newly created scoped context
     * @return a new ComponentContext instance scoped to the given component
     */
    @JvmStatic
    fun withComponentScope(
        parentContext: ComponentContext,
        scope: Component,
        globalKey: GlobalKey?,
    ): ComponentContext {
      val componentContext = parentContext.makeNewCopy()
      componentContext.componentScope = scope
      componentContext._globalKey = globalKey
      componentContext.parentTreePropContainer = parentContext.treePropContainer
      // This component resolves during layout
      componentContext.isContextForLayout =
          Component.willDeferResolution(scope) || parentContext.isContextForLayout

      val errorEventHandler =
          ComponentUtils.createOrGetErrorEventHandler(scope, parentContext, componentContext)
      componentContext._scopedComponentInfo =
          ScopedComponentInfo(scope, componentContext, errorEventHandler)

      return componentContext
    }

    @JvmStatic
    fun withComponentScope(
        parentContext: ComponentContext,
        scope: Component,
        globalKey: String?,
    ): ComponentContext =
        withComponentScope(parentContext, scope, globalKey?.let { StringGlobalKey(it) })

    @JvmStatic
    @VisibleForTesting
    @Deprecated("introduced for legacy test cases - don't add new callers")
    fun createScopedComponentContextWithStateForTest(
        parent: ComponentContext,
        component: Component,
        key: String,
    ): ComponentContext {
      val context = withComponentScope(parent, component, key)
      // SpecGeneratedComponents expect a StateContainer to be set on all scoped ComponentContexts
      // before any lifecycle methods are invoked.
      if (component is SpecGeneratedComponent && component.hasState()) {
        val state: ComponentState<StateContainer> =
            ComponentState(component.createInitialStateContainer(context), null)
        context.scopedComponentInfo.state = state
      }
      return context
    }

    /**
     * Creates a new ComponentContext based on the given ComponentContext, propagating log tag,
     * logger, and tree props. This should be used when creating a ComponentContext for a nested
     * ComponentTree (e.g. see HorizontalScrollSpec).
     */
    @JvmStatic
    fun makeCopyForNestedTree(
        parentTreeContext: ComponentContext,
    ): ComponentContext = ComponentContext(
        parentTreeContext.androidContext,
        parentTreeContext.treePropContainerCopy,
        parentTreeContext.lithoConfiguration,
        null,
        null,
        null,
        null,
        null,
    )

    /**
     * Checks if incremental mount is enabled given a ComponentContext, so you can throw an error if
     * you require that incremental mount is enabled (e.g. you use visibility callbacks). This is
     * static to avoid polluting the ComponentContext API.
     */
    @JvmStatic
    fun isIncrementalMountEnabled(c: ComponentContext): Boolean =
        c.lithoConfiguration.componentsConfig.incrementalMountEnabled

    @JvmStatic
    fun isVisibilityProcessingEnabled(c: ComponentContext): Boolean =
        c.lithoConfiguration.componentsConfig.visibilityProcessingEnabled

    @JvmStatic
    fun getComponentsConfig(c: ComponentContext): ComponentsConfiguration =
        c.lithoConfiguration.componentsConfig

    private fun canUseStateForEventDispatchInfo(
        mode: EventHandlerRebindMode,
        calculationContext: CalculationContext?,
        info: ScopedComponentInfo?,
    ): Boolean {
      if (mode != EventHandlerRebindMode.REBIND) {
        return false
      }

      if (calculationContext == null) {
        return false
      }

      if (info?.state?.eventDispatchInfo == null) {
        return false
      }

      return true
    }
  }
}

/** Breadth-first search under [root] for the View whose tag for [key] is [tag]. */
internal fun findViewWithTagValueUnder(root: View, key: Int, tag: Any): View? {
  val queue: Queue<View> = LinkedList()
  queue.add(root)
  while (!queue.isEmpty()) {
    val currentView = queue.poll() ?: continue
    if (currentView.getTag(key) === tag) {
      return currentView
    } else if (currentView is ViewGroup) {
      for (i in 0..<currentView.childCount) {
        queue.add(currentView.getChildAt(i))
      }
    }
  }
  return null
}
