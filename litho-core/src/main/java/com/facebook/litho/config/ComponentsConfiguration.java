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

package com.facebook.litho.config;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.BuildConfig;
import com.facebook.litho.perfboost.LithoPerfBoosterFactory;
import java.util.Set;

/**
 * Hi there, traveller! This configuration class is not meant to be used by end-users of Litho. It
 * contains mainly flags for features that are either under active development and not ready for
 * public consumption, or for use in experiments.
 *
 * <p>These values are safe defaults and should not require manual changes.
 *
 * <p>This class hosts all the config parameters that the ComponentTree configures it self ....
 * enable and disable features ... A Component tree uses the {@link defaultComponentsConfiguration}
 * by default but a {@link Builder} can be used to create new instances of the config to override
 * the default parameters ... The default config values can also be overridden by manually setting
 * their values in {@link defaultBuilder}
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ComponentsConfiguration {

  /**
   * Indicates whether this is an internal build. Note that the implementation of <code>BuildConfig
   * </code> that this class is compiled against may not be the one that is included in the APK.
   * See: <a
   * href="http://facebook.github.io/buck/rule/android_build_config.html">android_build_config</a>.
   */
  public static final boolean IS_INTERNAL_BUILD = BuildConfig.IS_INTERNAL_BUILD;

  /** Indicates that the incremental mount helper is required for this build. */
  public static final boolean USE_INCREMENTAL_MOUNT_HELPER =
      BuildConfig.USE_INCREMENTAL_MOUNT_HELPER;

  /** Whether we can access properties in Settings.Global for animations. */
  public static final boolean CAN_CHECK_GLOBAL_ANIMATOR_SETTINGS = (SDK_INT >= JELLY_BEAN_MR1);

  /** Whether we need to account for lack of synchronization while accessing Themes. */
  public static final boolean NEEDS_THEME_SYNCHRONIZATION = (SDK_INT <= LOLLIPOP_MR1);

  /** The default priority for threads that perform background layout calculations. */
  public static int DEFAULT_BACKGROUND_THREAD_PRIORITY = 5;

  /** The default priority for threads that perform background sections change set calculations. */
  public static final int DEFAULT_CHANGE_SET_THREAD_PRIORITY = 0;

  /**
   * Option to enabled debug mode. This will save extra data asscociated with each node and allow
   * more info about the hierarchy to be retrieved. Used to enable stetho integration. It is highly
   * discouraged to enable this in production builds. Due to how the Litho releases are distributed
   * in open source IS_INTERNAL_BUILD will always be false. It is therefore required to override
   * this value using your own application build configs. Recommended place for this is in a
   * Application subclass onCreate() method.
   */
  public static boolean isDebugModeEnabled = IS_INTERNAL_BUILD;

  /** Lightweight tracking of component class hierarchy of MountItems. */
  public static boolean isDebugHierarchyEnabled = false;

  /**
   * Populates additional metadata to find mounted components at runtime. Defaults to the presence
   * of an
   *
   * <pre>IS_TESTING</pre>
   *
   * system property at startup but can be overridden at runtime.
   */
  public static boolean isEndToEndTestRun = System.getProperty("IS_TESTING") != null;

  public static boolean isAnimationDisabled =
      "true".equals(System.getProperty("litho.animation.disabled"));

  /**
   * By default end-to-end tests will disable transitions and this flag lets to explicitly enable
   * transitions to test animation related behavior.
   */
  public static boolean forceEnableTransitionsForInstrumentationTests = false;

  public static boolean enableErrorBoundaryComponent = false;

  /**
   * If non-null, a thread pool will be used for async layouts instead of a single layout thread.
   */
  public static @Nullable LayoutThreadPoolConfiguration threadPoolConfiguration = null;

  public static boolean enableThreadTracingStacktrace = false;

  /** Sets if is reconciliation is enabled */
  public static boolean isReconciliationEnabled = true;

  public static @Nullable Boolean overrideReconciliation = null;

  public static @Nullable Boolean overrideLayoutDiffing = null;

  /**
   * Sets if layout diffing is enabled. This should be used in conjugation with
   * {@link#isReconciliationEnabled}.
   */
  public static boolean isLayoutDiffingEnabled = true;

  // TODO T39526148 Remove once Flipper plugin is usable.
  /** If true, information about RenderInfos will be passed to Flipper's layout inspector. */
  public static boolean enableRenderInfoDebugging = false;

  public static boolean canInterruptAndMoveLayoutsBetweenThreads = true;

  /**
   * field.getAnnotation() has bugs which is causing java crashes in the App, in addition to this we
   * suspect this might be a reason for few other native crashes as well. Adding this flag to verify
   * if this is the root cause.
   */
  public static boolean disableGetAnnotationUsage;

  public static boolean isRenderInfoDebuggingEnabled() {
    return isDebugModeEnabled && enableRenderInfoDebugging;
  }

  public static @Nullable LithoPerfBoosterFactory perfBoosterFactory = null;

  /**
   * If true, the {@link #perfBoosterFactory} will be used to indicate that LayoutStateFuture thread
   * can use the perf boost
   */
  public static boolean boostPerfLayoutStateFuture;

  public static boolean alwaysWriteDiffNodes = false;

  /**
   * Start parallel layout of visible range just before serial synchronous layouts in RecyclerBinder
   */
  public static boolean computeRangeOnSyncLayout = false;

  public static boolean shouldDisableBgFgOutputs = false;

  public static boolean shouldAddHostViewForRootComponent = false;

  /** When {@code true}, disables incremental mount globally. */
  public static boolean isIncrementalMountGloballyDisabled = false;

  /** Keeps the litho node tree in the LayoutState. This will increase memory use. */
  public static boolean keepLithoNodes = false;

  /** Keeps the layout result tree in the LayoutState. This will increase memory use. */
  public static boolean keepLayoutResults = true;

  /**
   * Used by LithoViews to determine whether or not to self-manage the view-port changes, rather
   * than rely on calls to notifyVisibleBoundsChanged.
   */
  public static boolean lithoViewSelfManageViewPortChanges = false;

  public static boolean emitMessageForZeroSizedTexture = false;

  public static int textureSizeWarningLimit = Integer.MAX_VALUE;

  public static int overlappingRenderingViewSizeLimit = Integer.MAX_VALUE;

  public static int partialAlphaWarningSizeThresold = Integer.MAX_VALUE;

  public static @Nullable Set<String> componentPreallocationBlocklist = null;

  /** When {@code true} ComponentTree records state change snapshots */
  public static boolean isTimelineEnabled = isDebugModeEnabled;

  public static @Nullable String timelineDocsLink = null;

  /** Initialize sticky header during layout when its component tree is null */
  public static boolean initStickyHeaderInLayoutWhenComponentTreeIsNull = false;

  /**
   * If set to true a single thread will always be used for background layout calculation by
   * ThreadPoolLayoutHandler.getNewInstance().
   */
  public static boolean layoutCalculationAlwaysUseSingleThread = false;

  /**
   * If set to true the default layout thread pool will always be used for background layout
   * calculation by ThreadPoolLayoutHandler.getNewInstance().
   */
  public static boolean layoutCalculationAlwaysUseDefaultThreadPool = false;

  /**
   * If set to true a thread pool with a single thread will always be used for background layout
   * calculation by ThreadPoolLayoutHandler.getNewInstance().
   */
  public static boolean layoutCalculationAlwaysUseSingleThreadedThreadPool = false;

  /**
   * If set to a positive value, this multiplier will be used to create and enforce a thread pool of
   * size #CPU_CORES * multiplier for background layout calculation by
   * ThreadPoolLayoutHandler.getNewInstance().
   */
  public static int layoutCalculationThreadPoolCpuCoresMultiplier = 0;

  /**
   * This subtractor can be used to increase/decrease the size of the background layout calculation
   * thread pool created when layoutCalculationThreadPoolCpuCoresMultiplier is positive.
   */
  public static int layoutCalculationThreadPoolCpuCoresSubtractor = 0;

  /**
   * If true, uses the root ComponentTree's mount content preallocation handler to perform
   * preallocation for nested trees.
   */
  public static boolean enableNestedTreePreallocation = false;

  public static boolean enableVisibilityExtension = true;

  public static boolean enableTransitionsExtension = true;

  /**
   * For backtesting new method of keeping mount content more consistent between updates, determines
   * whether render unit id map should be used for the external components in the backtest.
   */
  public static boolean useRenderUnitIdMapExternalComponents = true;

  /**
   * These params are part of an experiment to try to re-enable host recycling while avoiding native
   * RenderThread crashes we've seen when trying to reuse ComponentHosts.
   */
  public static boolean hostComponentRecyclingByWindowIsEnabled = false;

  public static boolean hostComponentRecyclingByMountStateIsEnabled = false;

  public static boolean unsafeHostComponentRecyclingIsEnabled = false;

  public static int hostComponentPoolSize = 30;

  /** This toggles whether fury should instrument the Litho event callback event */
  public static boolean furyEventCallbackInstrumentation = false;

  /**
   * This toggles whether Litho's MountState uses the position of the item in the host view to
   * mount/unmount or uses the position in the LayoutState. The default is to use the position in
   * the LayoutState.
   */
  public static boolean shouldUsePositionInParentForMounting = false;

  public static boolean isBuildAndLayoutSplitEnabled = false;

  public static boolean applyStateUpdateEarly = false;

  public static boolean isSplitStateHandlersEnabled = false;

  public static boolean useResolvedTree = false;

  public static boolean shouldCompareCommonPropsInIsEquivalentTo = false;

  public static boolean shouldCompareRootCommonPropsInSingleComponentSection = false;

  public static boolean shouldCollectLogsInRecyclerBinder = false;

  public static boolean shouldCheckConsistencyOfChildren = false;

  public static boolean shouldDelegateContentDescriptionChangeEvent = false;

  /** This toggles whether {@Link #LayoutThreadPoolExecutor} should timeout core threads or not */
  public static boolean shouldAllowCoreThreadTimeout = false;

  public static long layoutThreadKeepAliveTimeMs = 1000;

  public static boolean crashIfExceedingStateUpdateThreshold = false;

  public static boolean skipEqualValueStateUpdates = false;

  private static ComponentsConfiguration.Builder defaultBuilder = new Builder();

  private static ComponentsConfiguration defaultComponentsConfiguration = defaultBuilder.build();

  public static void setDefaultComponentsConfigurationBuilder(
      ComponentsConfiguration.Builder componentsConfigurationBuilder) {
    defaultBuilder = componentsConfigurationBuilder;
    defaultComponentsConfiguration = defaultBuilder.build();
  }

  public static ComponentsConfiguration getDefaultComponentsConfiguration() {
    return defaultComponentsConfiguration;
  }

  public static ComponentsConfiguration.Builder getDefaultComponentsConfigurationBuilder() {
    return defaultBuilder;
  }

  private final boolean mUseCancelableLayoutFutures;
  private final @Deprecated boolean mIgnoreNullLayoutStateError;

  private final boolean mShouldReuseOutputs;

  public boolean getUseCancelableLayoutFutures() {
    return mUseCancelableLayoutFutures;
  }

  /**
   * Only used for testing. If true, the framework will not throw an error if a null LayoutState is
   * returned from a layout calculation in illegal cases (such as for a sync layout calculation).
   */
  @Deprecated
  public boolean getIgnoreNullLayoutStateError() {
    return mIgnoreNullLayoutStateError;
  }

  private ComponentsConfiguration(ComponentsConfiguration.Builder builder) {
    mUseCancelableLayoutFutures = builder.mUseCancelableLayoutFutures;
    mIgnoreNullLayoutStateError = builder.mIgnoreNullLayoutStateError;
    mShouldReuseOutputs = builder.mShouldReuseOutputs;
  }

  public boolean shouldReuseOutputs() {
    return mShouldReuseOutputs;
  }

  /**
   * Defines whether or not we should use orchestrate the focus requests in the SectionTree, so as
   * to avoid race conditions in the scroll/focus requests.
   */
  public static boolean isPendingFocusEnabled = false;

  /**
   * Defines whether or not we should throw an exception when focus requests which happen on threads
   * different than the Main Thread.
   */
  public static boolean isNonMainThreadFocusRequestThrowingEnabled = false;

  /**
   * This configuration will determine which strategy we will try to employ to batch state updates.
   *
   * <p>When {@code null} then we will have no specific batching strategy.
   */
  @Nullable public static BatchedUpdatesConfiguration sBatchedUpdatesConfiguration = null;

  public static ComponentsConfiguration.Builder create() {
    return create(defaultComponentsConfiguration);
  }

  public static ComponentsConfiguration.Builder create(
      ComponentsConfiguration componentsConfiguration) {
    return new Builder()
        .useCancelableLayoutFutures(componentsConfiguration.getUseCancelableLayoutFutures())
        .ignoreNullLayoutStateError(componentsConfiguration.getIgnoreNullLayoutStateError());
  }

  public static class Builder {
    boolean mUseCancelableLayoutFutures;
    @Deprecated boolean mIgnoreNullLayoutStateError = false;
    boolean mShouldReuseOutputs = false;

    protected Builder() {}

    public ComponentsConfiguration.Builder useCancelableLayoutFutures(
        boolean useCancelableLayoutFutures) {
      this.mUseCancelableLayoutFutures = useCancelableLayoutFutures;
      return this;
    }

    public ComponentsConfiguration.Builder ignoreNullLayoutStateError(
        boolean ignoreNullLayoutStateError) {
      this.mIgnoreNullLayoutStateError = ignoreNullLayoutStateError;
      return this;
    }

    public ComponentsConfiguration build() {
      return new ComponentsConfiguration(this);
    }
  }
}
