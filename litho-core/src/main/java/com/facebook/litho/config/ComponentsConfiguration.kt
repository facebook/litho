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

import android.os.Build
import com.facebook.litho.BuildConfig
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentHost.UnsafeModificationPolicy
import com.facebook.litho.ComponentTreeDebugEventListener
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.DefaultErrorEventHandler
import com.facebook.litho.ErrorEventHandler
import com.facebook.litho.config.ComponentsConfiguration.Builder
import com.facebook.litho.perfboost.LithoPerfBoosterFactory
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionConfigs

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
    /**
     * This determines if the [ComponentTree] attached to this configuration, will attempt to detect
     * and ignore duplicate state updates coming from usages in the Specs API.
     */
    @JvmField val specsApiStateUpdateDuplicateDetectionEnabled: Boolean = false,
    val shouldCacheLayouts: Boolean = true,
    val disableNestedTreeCaching: Boolean = true,
    val shouldAddHostViewForRootComponent: Boolean = false,
    @JvmField
    val useIncrementalMountGapWorker: Boolean = IncrementalMountExtensionConfigs.useGapWorker,
    val useNonRebindingEventHandlers: Boolean = false,
    internal val shouldDisableBgFgOutputs: Boolean = false,
    /**
     * We have detected a scenario where we don't process visibility bounds change if the
     * localVisibleRect goes of the viewport and a LithoView is nested on an Host that is still
     * visible.
     *
     * This option attempts to tackle this issue by attempting to process an extra pass of IM if we
     * detect the Rect became invisible.
     *
     * Check {@code BaseMountingView#isPreviousRectVisibleAndCurrentInvisible} to get more context.
     */
    @JvmField
    val shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible: Boolean = false,
    /**
     * If enabled, then the [com.facebook.litho.LithoView] will attempt to unmount any mounted
     * content of the mount state when it gets detached from window.
     *
     * This is done to tackle and edge case where mount contents that have the same top/bottom
     * boundaries of the host view are not mounted by incremental mount.
     */
    @JvmField val unmountOnDetachedFromWindow: Boolean = false,
    /** Whether the [ComponentTree] should be using State Reconciliation. */
    @JvmField val isReconciliationEnabled: Boolean = true,
    /** The handler [ComponentTree] will be used to run the pre-allocation process */
    @JvmField val preAllocationHandler: PreAllocationHandler? = null,
    @JvmField val avoidRedundantPreAllocations: Boolean = false,
    /** Whether the [com.facebook.rendercore.MountState] can be mounted using incremental mount. */
    @JvmField val incrementalMountEnabled: Boolean = true,
    /**
     * If enabled, this will enable the recycling of [com.facebook.litho.ComponentHost] using
     * [MountItemsPool]
     */
    @JvmField val componentHostRecyclingEnabled: Boolean = false,
    /**
     * Controls whether we attempt to batch state updates to avoid running the render process for
     * every state update in a small time window. This has been proven to be an overall improvement
     * for performance so it's highly advised to not disable it.
     */
    @JvmField val enableStateUpdatesBatching: Boolean = true,
    /**
     * Whether the [com.facebook.LithoView] associated with the [com.facebook.litho.ComponentTree]
     * will process visibility events.
     */
    @JvmField val visibilityProcessingEnabled: Boolean = true,
    /** Whether we use a Recycler based on a Primitive implementation. */
    @JvmField val primitiveRecyclerEnabled: Boolean = false,
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
    @JvmField val shouldBuildRenderTreeInBg: Boolean = false,
    @JvmField val shouldReuseIdToPositionMap: Boolean = shouldBuildRenderTreeInBg,
    @JvmField var enablePreAllocationSameThreadCheck: Boolean = false,
    @JvmField val enableRecyclerThreadPoolConfig: Boolean = true,
    @JvmField var enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner: Boolean = false
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

    /** Whether we can access properties in Settings.Global for animations. */
    val CAN_CHECK_GLOBAL_ANIMATOR_SETTINGS: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1

    /** Whether we need to account for lack of synchronization while accessing Themes. */
    @JvmField
    val NEEDS_THEME_SYNCHRONIZATION: Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1

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

    /** This toggles whether {@Link #LayoutThreadPoolExecutor} should timeout core threads or not */
    @JvmField var shouldAllowCoreThreadTimeout: Boolean = false
    @JvmField var layoutThreadKeepAliveTimeMs: Long = 1_000
    @JvmField var defaultRecyclerBinderUseStableId: Boolean = true
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
    @JvmField var skipHostAlphaReset: Boolean = false
    @JvmField var useOneShotPreDrawListener: Boolean = false
    @JvmField var useNewCacheValueLogic: Boolean = false

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
  class Builder internal constructor(private var baseConfig: ComponentsConfiguration) {

    private var shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible =
        baseConfig.shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible
    private var shouldAddHostViewForRootComponent = baseConfig.shouldAddHostViewForRootComponent
    private var specsApiStateUpdateDuplicateDetectionEnabled =
        baseConfig.specsApiStateUpdateDuplicateDetectionEnabled
    private var shouldCacheLayouts = baseConfig.shouldCacheLayouts
    private var isReconciliationEnabled = baseConfig.isReconciliationEnabled
    private var preAllocationHandler = baseConfig.preAllocationHandler
    private var incrementalMountEnabled = baseConfig.incrementalMountEnabled
    private var componentHostRecyclingEnabled = baseConfig.componentHostRecyclingEnabled
    private var enableStateUpdatesBatching = baseConfig.enableStateUpdatesBatching
    private var errorEventHandler = baseConfig.errorEventHandler
    private var componentHostInvalidModificationPolicy =
        baseConfig.componentHostInvalidModificationPolicy
    private var visibilityProcessingEnabled = baseConfig.visibilityProcessingEnabled
    private var logTag = baseConfig.logTag
    private var componentsLogger = baseConfig.componentsLogger
    private var debugEventListener = baseConfig.debugEventListener
    private var shouldBuildRenderTreeInBg = baseConfig.shouldBuildRenderTreeInBg
    private var enablePreAllocationSameThreadCheck = baseConfig.enablePreAllocationSameThreadCheck
    private var avoidRedundantPreAllocations = baseConfig.avoidRedundantPreAllocations
    private var unmountOnDetachedFromWindow = baseConfig.unmountOnDetachedFromWindow
    private var primitiveRecyclerEnabled = baseConfig.primitiveRecyclerEnabled
    private var enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner =
        baseConfig.enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner

    fun shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible(
        enabled: Boolean
    ): Builder = also {
      shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible = enabled
    }

    fun shouldAddHostViewForRootComponent(enabled: Boolean): Builder = also {
      shouldAddHostViewForRootComponent = enabled
    }

    fun shouldCacheLayouts(enabled: Boolean): Builder = also { shouldCacheLayouts = enabled }

    fun specsApiStateUpdateDetectionEnabled(enabled: Boolean): Builder = also {
      specsApiStateUpdateDuplicateDetectionEnabled = enabled
    }

    fun isReconciliationEnabled(enabled: Boolean): Builder = also {
      isReconciliationEnabled = enabled
    }

    fun withPreAllocationHandler(handler: PreAllocationHandler?): Builder = also {
      preAllocationHandler = handler
    }

    fun incrementalMountEnabled(enabled: Boolean): Builder = also {
      incrementalMountEnabled = enabled
    }

    fun componentHostRecyclingEnabled(enabled: Boolean): Builder = also {
      componentHostRecyclingEnabled = enabled
    }

    fun enableStateUpdatesBatching(enabled: Boolean): Builder = also {
      enableStateUpdatesBatching = enabled
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

    fun shouldBuildRenderTreeInBg(value: Boolean): Builder = also {
      this.shouldBuildRenderTreeInBg = value
    }

    fun enablePreAllocationSameThreadCheck(value: Boolean): Builder = also {
      enablePreAllocationSameThreadCheck = value
    }

    fun avoidRedundantPreAllocations(value: Boolean): Builder = also {
      avoidRedundantPreAllocations = value
    }

    fun unmountOnDetachedFromWindow(unmountOnDetachedFromWindow: Boolean): Builder = also {
      this.unmountOnDetachedFromWindow = unmountOnDetachedFromWindow
    }

    fun primitiveRecyclerEnabled(primitiveRecyclerEnabled: Boolean): Builder = also {
      this.primitiveRecyclerEnabled = primitiveRecyclerEnabled
    }

    fun enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner(
        enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner: Boolean
    ): Builder = also {
      this.enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner =
          enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner
    }

    fun build(): ComponentsConfiguration {
      return baseConfig.copy(
          specsApiStateUpdateDuplicateDetectionEnabled =
              specsApiStateUpdateDuplicateDetectionEnabled,
          shouldCacheLayouts = shouldCacheLayouts,
          shouldAddHostViewForRootComponent = shouldAddHostViewForRootComponent,
          isReconciliationEnabled = isReconciliationEnabled,
          preAllocationHandler = preAllocationHandler,
          incrementalMountEnabled = incrementalMountEnabled,
          componentHostRecyclingEnabled = componentHostRecyclingEnabled,
          enableStateUpdatesBatching = enableStateUpdatesBatching,
          componentHostInvalidModificationPolicy = componentHostInvalidModificationPolicy,
          visibilityProcessingEnabled = visibilityProcessingEnabled,
          shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible =
              shouldNotifyVisibleBoundsChangeWhenNestedLithoViewBecomesInvisible,
          errorEventHandler = errorEventHandler,
          logTag =
              if (logTag == null && componentsLogger != null) {
                "null"
              } else {
                logTag
              },
          componentsLogger = componentsLogger,
          debugEventListener = debugEventListener,
          shouldBuildRenderTreeInBg = shouldBuildRenderTreeInBg,
          shouldReuseIdToPositionMap = shouldBuildRenderTreeInBg,
          enablePreAllocationSameThreadCheck = enablePreAllocationSameThreadCheck,
          avoidRedundantPreAllocations = avoidRedundantPreAllocations,
          unmountOnDetachedFromWindow = unmountOnDetachedFromWindow,
          primitiveRecyclerEnabled = primitiveRecyclerEnabled,
          enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner =
              enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner)
    }
  }
}
