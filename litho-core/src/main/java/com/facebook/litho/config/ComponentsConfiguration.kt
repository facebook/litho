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

package com.facebook.litho.config

import com.facebook.litho.BuildConfig
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentHost.UnsafeModificationPolicy
import com.facebook.litho.ComponentTreeDebugEventListener
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.DefaultErrorEventHandler
import com.facebook.litho.ErrorEventHandler
import com.facebook.litho.config.ComponentsConfiguration.Builder
import com.facebook.litho.perfboost.LithoPerfBoosterFactory
import com.facebook.rendercore.PoolingPolicy
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionConfigs
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer

/**
 * These values are safe defaults and should not require manual changes.
 *
 * This class hosts all the config parameters that the ComponentTree configures it self .... enable
 * and disable features ... A Component tree uses the [.defaultComponentsConfiguration] by default
 * but a [Builder] can be used to create new instances of the config to override the default
 * parameters ... The default config values can also be overridden by manually setting their values
 * in [.defaultBuilder]
 */
data class ComponentsConfiguration
internal constructor(
    val disableNestedTreeCaching: Boolean = true,
    val shouldAddHostViewForRootComponent: Boolean = false,
    @JvmField
    val useIncrementalMountGapWorker: Boolean = IncrementalMountExtensionConfigs.useGapWorker,
    val useNonRebindingEventHandlers: Boolean = false,
    internal val shouldDisableBgFgOutputs: Boolean = false,
    /** The handler [ComponentTree] will be used to run the pre-allocation process */
    @JvmField val preAllocationHandler: PreAllocationHandler? = null,
    @JvmField val avoidRedundantPreAllocations: Boolean = false,
    /** Whether the [com.facebook.rendercore.MountState] can be mounted using incremental mount. */
    @JvmField val incrementalMountEnabled: Boolean = true,
    /** Determines the pooling behavior for component hosts */
    @JvmField val componentHostPoolingPolicy: PoolingPolicy = PoolingPolicy.Disabled,
    /**
     * Whether the [com.facebook.LithoView] associated with the [com.facebook.litho.ComponentTree]
     * will process visibility events.
     */
    @JvmField val visibilityProcessingEnabled: Boolean = true,
    /**
     * This class is an error event handler that clients can optionally set on a [ComponentTree] to
     * gracefully handle uncaught/unhandled exceptions thrown from the framework while resolving a
     * layout.
     */
    @JvmField val errorEventHandler: ErrorEventHandler = DefaultErrorEventHandler,
    @JvmField val componentsLogger: ComponentsLogger? = null,
    @JvmField val logTag: String? = if (componentsLogger == null) null else "null",
    /**
     * Determines whether we log, crash, or do nothing if an invalid
     * [com.facebook.litho.ComponentHost] view modification is detected.
     *
     * @see [ComponentHost.UnsafeModificationPolicy]
     */
    @JvmField
    val componentHostInvalidModificationPolicy: ComponentHost.UnsafeModificationPolicy? = null,
    /**
     * You can define a [ComponentTreeDebugEventListener] to listen on specific litho lifecycle
     * related events.
     *
     * @see [com.facebook.litho.debug.LithoDebugEvent]
     * @see [com.facebook.rendercore.debug.DebugEvent]
     */
    @JvmField val debugEventListener: ComponentTreeDebugEventListener? = null,
    @JvmField var enablePreAllocationSameThreadCheck: Boolean = false,
    @JvmField val enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner: Boolean = false,
    @JvmField val enableDefaultLifecycleOwnerAsFragmentOrActivity: Boolean = false,
    /**
     * This is a temporary config param for debugging state list animator crashes during layout of a
     * [ComponentHost]
     */
    @JvmField val cloneStateListAnimators: Boolean = false,
    @JvmField val enableVisibilityFixForNestedLithoView: Boolean = false,
    /**
     * This flag is used to enable the use of default item animators in lazy collections, so that
     * the behavior is compatible to what exists nowadays in the
     * [com.facebook.litho.sections.widget.RecyclerCollectionComponent].
     */
    @JvmField val useDefaultItemAnimatorInLazyCollections: Boolean = false,
    /**
     * This defines which strategy we will use to bind the
     * [com.facebook.litho.sections.widget.ExperimentalRecycler].
     *
     * @see [PrimitiveRecyclerBinderStrategy] for more details.
     */
    @JvmField
    val primitiveRecyclerBinderStrategy: PrimitiveRecyclerBinderStrategy =
        PrimitiveRecyclerBinderStrategy.DEFAULT,
    /**
     * This flag is used to enable a fix for the issue where components that match the host view
     * size do not get unmounted when they go out of the viewport.
     */
    @JvmField val enableFixForIM: Boolean = false,
    @JvmField val visibilityBoundsTransformer: VisibilityBoundsTransformer? = null,
    @JvmField val sectionsRecyclerViewOnCreateHandler: ((Object) -> Unit)? = null,
    /**
     * Determines whether we should enable stable ids by default in the
     * [com.facebook.litho.widget.RecyclerBinder]
     */
    @JvmField val useStableIdsInRecyclerBinder: Boolean = true,
    /**
     * This will perform an optimization that will verify if the same size specs were used. However,
     * this creates a bug in a specific scenario where double measure happens.
     */
    @JvmField val performExactSameSpecsCheck: Boolean = true,
    /**
     * This will remove size specs from the resolve phase, which could potentially improve the
     * possibility of reusing resolve result.
     */
    @JvmField val enableResolveWithoutSizeSpec: Boolean = false,
    /** This will skip calling `onDraw` for ComponentHost. */
    @JvmField val enableHostWillNotDraw: Boolean = false,
    /** This will enable logging for render in-flight */
    @JvmField val enableLoggingForRenderInFlight: Boolean = false,
    @JvmField val componentEqualityMode: ComponentEqualityMode = ComponentEqualityMode.DEFAULT,

    /**
     * When enabled the framework will add an additional binder to Host RenderUnits to clean up view
     * attributes that may have been added by non-litho code.
     */
    @JvmField val isHostViewAttributesCleanUpEnabled: Boolean = false,
    /**
     * This flag is used to enable a fix for the issue where a cached NestedTree getting lost of
     * state containers
     */
    @JvmField val enableFixForCachedNestedTree: Boolean = false,
) {

  val shouldAddRootHostViewOrDisableBgFgOutputs: Boolean =
      shouldAddHostViewForRootComponent || shouldDisableBgFgOutputs

  companion object {

    /**
     * This is just a proxy to [LithoDebugConfigurations.isDebugModeEnabled]. We have to keep it
     * until we release a new oss version and we can refer to [LithoDebugConfigurations] directly on
     * Flipper.
     */
    @Deprecated("Use the LithoDebugConfigurations instead")
    var isDebugModeEnabled: Boolean
      get() = LithoDebugConfigurations.isDebugModeEnabled
      set(value) {
        LithoDebugConfigurations.isDebugModeEnabled = value
      }

    /**
     * This is just a proxy to [LithoDebugConfigurations.isDebugHierarchyEnabled]. We have to keep
     * it until we release a new oss version and we can refer to [LithoDebugConfigurations] directly
     * on Flipper.
     */
    @Deprecated("Use the LithoDebugConfigurations instead")
    var isDebugHierarchyEnabled: Boolean
      get() = LithoDebugConfigurations.isDebugHierarchyEnabled
      set(value) {
        LithoDebugConfigurations.isDebugHierarchyEnabled = value
      }

    @JvmField var defaultInstance: ComponentsConfiguration = ComponentsConfiguration()

    /** Indicates that the incremental mount helper is required for this build. */
    @JvmField val USE_INCREMENTAL_MOUNT_HELPER: Boolean = BuildConfig.USE_INCREMENTAL_MOUNT_HELPER

    /** The default priority for threads that perform background layout calculations. */
    @JvmField var DEFAULT_BACKGROUND_THREAD_PRIORITY: Int = 5

    /**
     * The default priority for threads that perform background sections change set calculations.
     */
    const val DEFAULT_CHANGE_SET_THREAD_PRIORITY: Int = 0

    /**
     * Populates additional metadata to find mounted components at runtime. Defaults to the presence
     * of an
     *
     * ```
     * IS_TESTING
     * ```
     *
     * system property at startup but can be overridden at runtime.
     */
    @JvmField var isEndToEndTestRun = System.getProperty("IS_TESTING") != null
    @JvmField var isAnimationDisabled = "true" == System.getProperty("litho.animation.disabled")

    /**
     * By default end-to-end tests will disable transitions and this flag lets to explicitly enable
     * transitions to test animation related behavior.
     */
    @JvmField var forceEnableTransitionsForInstrumentationTests: Boolean = false

    @JvmField var enableThreadTracingStacktrace: Boolean = false

    @JvmField var runLooperPrepareForLayoutThreadFactory: Boolean = true

    @JvmField var perfBoosterFactory: LithoPerfBoosterFactory? = null

    /**
     * If true, the [.perfBoosterFactory] will be used to indicate that LayoutStateFuture thread can
     * use the perf boost
     */
    @JvmField var boostPerfLayoutStateFuture: Boolean = false

    /**
     * Start parallel layout of visible range just before serial synchronous layouts in
     * RecyclerBinder
     */
    @JvmField var computeRangeOnSyncLayout: Boolean = false

    /** Keeps the litho layout result tree in the LayoutState. This will increase memory use. */
    @JvmField var keepLayoutResults: Boolean = false

    @JvmField var overlappingRenderingViewSizeLimit: Int = Int.MAX_VALUE
    @JvmField var partialAlphaWarningSizeThresold: Int = Int.MAX_VALUE

    /** Initialize sticky header during layout when its component tree is null */
    @JvmField var initStickyHeaderInLayoutWhenComponentTreeIsNull: Boolean = false

    @JvmField var hostComponentPoolSize: Int = 30

    /** Skip checking for root component and tree-props while layout */
    @JvmField var enableComputeLayoutAsyncAfterInsertion: Boolean = true
    @JvmField var shouldCompareCommonPropsInIsEquivalentTo: Boolean = false
    @JvmField var shouldCompareRootCommonPropsInSingleComponentSection: Boolean = false
    @JvmField var isYogaFlexBasisFixEnabled: Boolean = true

    @JvmField var recyclerBinderStrategy: Int = 0
    @JvmField var shouldOverrideHasTransientState: Boolean = false
    @JvmField var enableFixForDisappearTransitionInRecyclerBinder: Boolean = false
    @JvmField var disableReleaseComponentTreeInRecyclerBinder: Boolean = false
    @JvmField var reduceMemorySpikeUserSession: Boolean = false
    @JvmField var reduceMemorySpikeDataDiffSection: Boolean = false
    @JvmField var reduceMemorySpikeGetUri: Boolean = false
    @JvmField var bindOnSameComponentTree: Boolean = true
    @JvmField var isEventHandlerRebindLoggingEnabled: Boolean = false
    @JvmField var useSafeSpanEndInTextInputSpec: Boolean = false
    @JvmField var useOneShotPreDrawListener: Boolean = false
    /**
     * This flag is used to enable logging for the issue where components with an aspect ratio, like
     * NaN or Infinity.
     */
    @JvmField var enableLoggingForInvalidAspectRatio: Boolean = false
    /** This flag is used to enable a fix for the ANR issue with sticky header RecyclerView. */
    @JvmField var enableFixForStickyHeader: Boolean = false

    /** This flag is used to enable a fix for the primitive component measurement issue. */
    @JvmField var enablePrimitiveMeasurementFix: Boolean = false

    /**
     * This flag is used to enable using PrimitiveComponent implementation of an Image component.
     */
    @JvmField var usePrimitiveImage: Boolean = false

    /**
     * This flag is used to enable using PrimitiveComponent implementation of a SolidColor
     * component.
     */
    @JvmField var usePrimitiveSolidColor: Boolean = false

    /** This config will enable logging of interactable components with 0 alpha */
    @JvmField var isZeroAlphaLoggingEnabled: Boolean = false

    /** This flag is used to enable clearing event handlers and triggers */
    @JvmField var clearEventHandlersAndTriggers: Boolean = false

    /** This flag is to enable usage custom pool scopes */
    @JvmField var customPoolScopesEnabled: Boolean = false

    /** This flag is to enable usage of Primitive Vertical Scroll Component */
    @JvmField var usePrimitiveVerticalScroll: Boolean = false

    /** This flag is to enable usage of Primitive Text */
    @JvmField var usePrimitiveText: Boolean = false

    /**
     * This method is only used so that Java clients can have a builder like approach to override a
     * configuration.
     */
    @JvmStatic fun create(): Builder = create(defaultInstance)

    @JvmStatic
    fun create(configuration: ComponentsConfiguration): Builder = Builder(configuration.copy())
  }

  /**
   * This is a builder that only exists so that Java clients can have an easier time creating and
   * overriding specific configurations. For Kotlin one can use directly the named parameters on the
   * [ComponentsConfiguration] constructor.
   */
  class Builder internal constructor(private val baseConfig: ComponentsConfiguration) {

    private var shouldAddHostViewForRootComponent = baseConfig.shouldAddHostViewForRootComponent
    private var preAllocationHandler = baseConfig.preAllocationHandler
    private var incrementalMountEnabled = baseConfig.incrementalMountEnabled
    private var componentHostPoolingPolicy = baseConfig.componentHostPoolingPolicy
    private var errorEventHandler = baseConfig.errorEventHandler
    private var componentHostInvalidModificationPolicy =
        baseConfig.componentHostInvalidModificationPolicy
    private var visibilityProcessingEnabled = baseConfig.visibilityProcessingEnabled
    private var logTag = baseConfig.logTag
    private var componentsLogger = baseConfig.componentsLogger
    private var debugEventListener = baseConfig.debugEventListener
    private var enablePreAllocationSameThreadCheck = baseConfig.enablePreAllocationSameThreadCheck
    private var avoidRedundantPreAllocations = baseConfig.avoidRedundantPreAllocations
    private var enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner =
        baseConfig.enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner
    private var enableDefaultLifecycleOwnerAsFragmentOrActivity =
        baseConfig.enableDefaultLifecycleOwnerAsFragmentOrActivity
    private var cloneStateListAnimators = baseConfig.cloneStateListAnimators
    private var enableVisibilityFixForNestedLithoView =
        baseConfig.enableVisibilityFixForNestedLithoView
    private var useDefaultItemAnimatorInLazyCollections =
        baseConfig.useDefaultItemAnimatorInLazyCollections
    private var primitiveRecyclerBinderStrategy = baseConfig.primitiveRecyclerBinderStrategy
    private var enableFixForIM = baseConfig.enableFixForIM
    private var visibilityBoundsTransformer = baseConfig.visibilityBoundsTransformer
    private var sectionsRecyclerViewOnCreateHandler: ((Object) -> Unit)? =
        baseConfig.sectionsRecyclerViewOnCreateHandler
    private var useStableIdsInRecyclerBinder = baseConfig.useStableIdsInRecyclerBinder
    private var enableResolveWithoutSizeSpec = baseConfig.enableResolveWithoutSizeSpec
    private var enableHostWillNotDraw = baseConfig.enableHostWillNotDraw
    private var enableLoggingForRenderInFlight = baseConfig.enableLoggingForRenderInFlight
    private var enableFixForCachedNestedTree = baseConfig.enableFixForCachedNestedTree
    private var isHostViewAttributesCleanUpEnabled = baseConfig.isHostViewAttributesCleanUpEnabled

    fun shouldAddHostViewForRootComponent(enabled: Boolean): Builder = also {
      shouldAddHostViewForRootComponent = enabled
    }

    fun withPreAllocationHandler(handler: PreAllocationHandler?): Builder = also {
      preAllocationHandler = handler
    }

    fun incrementalMountEnabled(enabled: Boolean): Builder = also {
      incrementalMountEnabled = enabled
    }

    fun componentHostPoolingPolicy(poolingPolicy: PoolingPolicy): Builder = also {
      componentHostPoolingPolicy = poolingPolicy
    }

    fun componentHostInvalidModificationPolicy(
        invalidModificationPolicy: UnsafeModificationPolicy?
    ): Builder = also { componentHostInvalidModificationPolicy = invalidModificationPolicy }

    fun enableVisibilityProcessing(enabled: Boolean): Builder = also {
      visibilityProcessingEnabled = enabled
    }

    fun errorEventHandler(handler: ErrorEventHandler): Builder = also {
      errorEventHandler = handler
    }

    fun logTag(tag: String?): Builder = also { logTag = tag }

    fun componentsLogger(componentsLogger: ComponentsLogger?): Builder = also {
      this.componentsLogger = componentsLogger
    }

    fun debugEventListener(debugEventListener: ComponentTreeDebugEventListener?) {
      this.debugEventListener = debugEventListener
    }

    fun enablePreAllocationSameThreadCheck(value: Boolean): Builder = also {
      enablePreAllocationSameThreadCheck = value
    }

    fun avoidRedundantPreAllocations(value: Boolean): Builder = also {
      avoidRedundantPreAllocations = value
    }

    fun primitiveRecyclerBinderStrategy(
        primitiveRecyclerBinderStrategy: PrimitiveRecyclerBinderStrategy
    ): Builder = also { this.primitiveRecyclerBinderStrategy = primitiveRecyclerBinderStrategy }

    fun enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner(
        enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner: Boolean
    ): Builder = also {
      this.enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner =
          enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner
    }

    fun enableDefaultLifecycleOwnerAsFragmentOrActivity(
        enableDefaultLifecycleOwnerAsFragmentOrActivity: Boolean
    ): Builder = also {
      this.enableDefaultLifecycleOwnerAsFragmentOrActivity =
          enableDefaultLifecycleOwnerAsFragmentOrActivity
    }

    fun cloneStateListAnimators(enabled: Boolean): Builder = also {
      cloneStateListAnimators = enabled
    }

    fun enableVisibilityFixForNestedLithoView(enabled: Boolean): Builder = also {
      enableVisibilityFixForNestedLithoView = enabled
    }

    fun useDefaultItemAnimatorInLazyCollections(enabled: Boolean): Builder = also {
      useDefaultItemAnimatorInLazyCollections = enabled
    }

    fun enableFixForIM(enabled: Boolean): Builder = also { enableFixForIM = enabled }

    fun visibilityBoundsTransformer(transformer: VisibilityBoundsTransformer?): Builder = also {
      visibilityBoundsTransformer = transformer
    }

    fun sectionsRecyclerViewOnCreateHandler(handler: ((Object) -> Unit)?): Builder = also {
      sectionsRecyclerViewOnCreateHandler = handler
    }

    fun useStableIdsInRecyclerBinder(enabled: Boolean): Builder = also {
      useStableIdsInRecyclerBinder = enabled
    }

    fun enableResolveWithoutSizeSpec(enabled: Boolean): Builder = also {
      enableResolveWithoutSizeSpec = enabled
    }

    fun enableLoggingForRenderInFlight(enabled: Boolean): Builder = also {
      enableLoggingForRenderInFlight = enabled
    }

    fun enableFixForCachedNestedTree(enabled: Boolean): Builder = also {
      enableFixForCachedNestedTree = enabled
    }

    fun enableHostViewAttributesCleanUp(enabled: Boolean): Builder = also {
      isHostViewAttributesCleanUpEnabled = enabled
    }

    fun build(): ComponentsConfiguration {
      return baseConfig.copy(
          shouldAddHostViewForRootComponent = shouldAddHostViewForRootComponent,
          preAllocationHandler = preAllocationHandler,
          incrementalMountEnabled = incrementalMountEnabled,
          componentHostPoolingPolicy = componentHostPoolingPolicy,
          componentHostInvalidModificationPolicy = componentHostInvalidModificationPolicy,
          visibilityProcessingEnabled = visibilityProcessingEnabled,
          errorEventHandler = errorEventHandler,
          logTag =
              if (logTag == null && componentsLogger != null) {
                "null"
              } else {
                logTag
              },
          componentsLogger = componentsLogger,
          debugEventListener = debugEventListener,
          enablePreAllocationSameThreadCheck = enablePreAllocationSameThreadCheck,
          avoidRedundantPreAllocations = avoidRedundantPreAllocations,
          primitiveRecyclerBinderStrategy = primitiveRecyclerBinderStrategy,
          enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner =
              enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner,
          enableDefaultLifecycleOwnerAsFragmentOrActivity =
              enableDefaultLifecycleOwnerAsFragmentOrActivity,
          cloneStateListAnimators = cloneStateListAnimators,
          enableVisibilityFixForNestedLithoView = enableVisibilityFixForNestedLithoView,
          useDefaultItemAnimatorInLazyCollections = useDefaultItemAnimatorInLazyCollections,
          enableFixForIM = enableFixForIM,
          visibilityBoundsTransformer = visibilityBoundsTransformer,
          sectionsRecyclerViewOnCreateHandler = sectionsRecyclerViewOnCreateHandler,
          useStableIdsInRecyclerBinder = useStableIdsInRecyclerBinder,
          enableResolveWithoutSizeSpec = enableResolveWithoutSizeSpec,
          enableHostWillNotDraw = enableHostWillNotDraw,
          enableLoggingForRenderInFlight = enableLoggingForRenderInFlight,
          enableFixForCachedNestedTree = enableFixForCachedNestedTree,
          isHostViewAttributesCleanUpEnabled = isHostViewAttributesCleanUpEnabled,
      )
    }
  }
}
