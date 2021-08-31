/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

  /** Debug option to highlight interactive areas in mounted components. */
  public static boolean debugHighlightInteractiveBounds = false;

  /**
   * LithoView overlay showing whether its ComponentTree was computed on UI thread (red) or bg
   * thread (green).
   */
  public static boolean enableLithoViewDebugOverlay = false;

  /** Debug option to highlight mount bounds of mounted components. */
  public static boolean debugHighlightMountBounds = false;

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
   * If false, global keys will not be generated (litho level state updates won't work). It's highly
   * discouraged to change this to false, unless you handle all your updates outside of the litho
   * framework
   */
  public static boolean useGlobalKeys = true;

  /**
   * If non-null, a thread pool will be used for async layouts instead of a single layout thread.
   */
  public static @Nullable LayoutThreadPoolConfiguration threadPoolConfiguration = null;

  public static boolean enableThreadTracingStacktrace = false;

  /**
   * Whether the OnShouldCreateLayoutWithNewSizeSpec is used with Layout Spec with size spec. This
   * will also disable the associated layout caching.
   */
  public static boolean enableShouldCreateLayoutWithNewSizeSpec = true;

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

  public static boolean useCancelableLayoutFutures;
  public static boolean canInterruptAndMoveLayoutsBetweenThreads = true;

  /**
   * field.getAnnotation() has bugs which is causing java crashes in the App, in addition to this we
   * suspect this might be a reason for few other native crashes as well. Adding this flag to verify
   * if this is the root cause.
   */
  public static boolean disableGetAnnotationUsage;

  /** Bisect mount pool to find the buggy implementation causing native crashes. */
  public static boolean isPoolBisectEnabled = false;

  public static String disablePoolsStart = "aaaaa";
  public static String disablePoolsEnd = "zzzzz";

  public static boolean isRenderInfoDebuggingEnabled() {
    return isDebugModeEnabled && enableRenderInfoDebugging;
  }

  public static boolean prioritizeRenderingOnParallel = true;

  public static boolean useSharedFutureOnParallel = true;

  /** (Hopefully) temporary measure as we're investigating a major crash in libhwui. */
  public static boolean disableComponentHostPool = true;

  public static @Nullable LithoPerfBoosterFactory perfBoosterFactory = null;

  /**
   * If true, the {@link #perfBoosterFactory} will be used to indicate that LayoutStateFuture thread
   * can use the perf boost
   */
  public static boolean boostPerfLayoutStateFuture;

  /** Enabling this will force all updateStateSync methods to be updateStateAsync. * */
  public static boolean shouldForceAsyncStateUpdate = false;

  public static boolean useExtensionsWithMountDelegate = false;

  public static boolean delegateToRenderCoreMount = false;

  public static boolean ensureParentMountedInRenderCoreMountState = false;

  public static boolean shouldDisableBgFgOutputs = false;

  public static boolean shouldAddHostViewForRootComponent = false;

  /**
   * If {@code false} we won't force Component to update when Device Orientation change, and rely on
   * its size change.
   */
  public static boolean shouldForceComponentUpdateOnOrientationChange = true;

  /** When {@code true}, disables incremental mount globally. */
  public static boolean isIncrementalMountGloballyDisabled = false;

  /** Keeps the internal nodes used for layout. This will increase memory use. */
  public static boolean keepInternalNodes = false;

  public static boolean emitMessageForZeroSizedTexture = false;

  /**
   * When true, Layout-scoped info such as a Component's scoped Context are fetched from a
   * LayoutState based on the Component's global key instead of reading fields from the Component
   * instance.
   */
  public static boolean useStatelessComponent = false;

  public static boolean keepReconciliationEnabledWhenStateless = true;

  /**
   * When true, Litho will use the InputOnlyInternalNode and DefaultLayoutResult for layout
   * calculations.
   */
  public static boolean useInputOnlyInternalNodes = false;

  public static boolean reuseInternalNodes = false;

  public static boolean enableLayoutCaching = false;

  public static boolean shouldSkipShallowCopy = false;

  public static int textureSizeWarningLimit = Integer.MAX_VALUE;

  public static int overlappingRenderingViewSizeLimit = Integer.MAX_VALUE;

  public static int partialAlphaWarningSizeThresold = Integer.MAX_VALUE;

  public static @Nullable Set<String> componentPreallocationBlocklist = null;

  /** When {@code true} ComponentTree records state change snapshots */
  public static boolean isTimelineEnabled = isDebugModeEnabled;

  public static @Nullable String timelineDocsLink = null;

  /**
   * Only used for testing. If true, the framework will not throw an error if a null LayoutState is
   * returned from a layout calculation in illegal cases (such as for a sync layout calculation).
   */
  @Deprecated public static boolean ignoreNullLayoutStateError = false;

  /** When the LithoView visibility changes, rebind */
  public static boolean rebindWhenVisibilityChanges = false;

  public static boolean swallowUnhandledExceptions = false;

  /** Initialize sticky header on scrolled on scrolled */
  public static boolean initStickyHeaderOnScroll = false;
  /** Initialize sticky header on scrolled when its component tree is null */
  public static boolean initStickyHeaderWhenComponentTreeIsNull = false;

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
  private final boolean mUseInputOnlyInternalNodes;

  public boolean getUseCancelableLayoutFutures() {
    return mUseCancelableLayoutFutures;
  }

  @Deprecated
  public boolean getIgnoreNullLayoutStateError() {
    return mIgnoreNullLayoutStateError;
  }

  public boolean getUseInputOnlyInternalNodes() {
    return mUseInputOnlyInternalNodes;
  }

  private ComponentsConfiguration(ComponentsConfiguration.Builder builder) {
    mUseCancelableLayoutFutures = builder.mUseCancelableLayoutFutures;
    mIgnoreNullLayoutStateError = builder.mIgnoreNullLayoutStateError;
    mUseInputOnlyInternalNodes = builder.mUseInputOnlyInternalNodes;
  }

  public static ComponentsConfiguration.Builder create() {
    return defaultBuilder;
  }

  public static ComponentsConfiguration.Builder create(
      ComponentsConfiguration componentsConfiguration) {
    return new Builder()
        .useCancelableLayoutFutures(componentsConfiguration.getUseCancelableLayoutFutures())
        .ignoreNullLayoutStateError(componentsConfiguration.getIgnoreNullLayoutStateError())
        .useInputOnlyInternalNodes(componentsConfiguration.getUseInputOnlyInternalNodes());
  }

  public static class Builder {
    boolean mUseCancelableLayoutFutures = useCancelableLayoutFutures;
    @Deprecated boolean mIgnoreNullLayoutStateError = false;
    boolean mUseInputOnlyInternalNodes = false;

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

    public ComponentsConfiguration.Builder useInputOnlyInternalNodes(
        boolean useInputOnlyInternalNodes) {
      this.mUseInputOnlyInternalNodes = useInputOnlyInternalNodes;
      return this;
    }

    public ComponentsConfiguration build() {
      return new ComponentsConfiguration(this);
    }
  }
}
