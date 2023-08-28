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
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.perfboost.LithoPerfBoosterFactory;
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionConfigs;
import java.util.Set;

/**
 * Hi there, traveller! This configuration class is not meant to be used by end-users of Litho. It
 * contains mainly flags for features that are either under active development and not ready for
 * public consumption, or for use in experiments.
 *
 * <p>These values are safe defaults and should not require manual changes.
 *
 * <p>This class hosts all the config parameters that the ComponentTree configures it self ....
 * enable and disable features ... A Component tree uses the {@link #defaultComponentsConfiguration}
 * by default but a {@link Builder} can be used to create new instances of the config to override
 * the default parameters ... The default config values can also be overridden by manually setting
 * their values in {@link #defaultBuilder}
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

  /**
   * The LazyList is having a default of {@code false} for the reconciliation being enabled.
   * Ideally, it would default to whatever value is used in its ComponentContext. By enabling this
   * setting, the default will be retrieve via the ComponentContext.
   */
  public static boolean isLazyListUsingComponentContextReconciliationConfig = true;

  public static @Nullable Boolean overrideReconciliation = null;

  public static @Nullable Boolean overrideLayoutDiffing = null;

  /**
   * Sets if layout diffing is enabled. This should be used in conjugation with
   * {@link#isReconciliationEnabled}.
   */
  public static boolean isLayoutDiffingEnabled = true;

  public static boolean runLooperPrepareForLayoutThreadFactory = true;

  public static boolean enableDrawablePreAllocation = false;

  /**
   * field.getAnnotation() has bugs which is causing java crashes in the App, in addition to this we
   * suspect this might be a reason for few other native crashes as well. Adding this flag to verify
   * if this is the root cause.
   */
  public static boolean disableGetAnnotationUsage;

  /** When enabled components which render to null will use a NullNode for reconciliation */
  public static boolean isNullNodeEnabled = true;

  public static boolean isRenderInfoDebuggingEnabled() {
    return isDebugModeEnabled;
  }

  public static @Nullable LithoPerfBoosterFactory perfBoosterFactory = null;

  /**
   * If true, the {@link #perfBoosterFactory} will be used to indicate that LayoutStateFuture thread
   * can use the perf boost
   */
  public static boolean boostPerfLayoutStateFuture;

  /**
   * Start parallel layout of visible range just before serial synchronous layouts in RecyclerBinder
   */
  public static boolean computeRangeOnSyncLayout = false;

  /**
   * When true, IM will not stop when the LithoView's visible rect is empty, and will proceed to
   * unmount everything.
   */
  public static boolean shouldContinueIncrementalMountWhenVisibileRectIsEmpty = false;

  /** When {@code true}, disables incremental mount globally. */
  public static boolean isIncrementalMountGloballyDisabled = false;

  /** Keeps the litho layout result tree in the LayoutState. This will increase memory use. */
  public static boolean keepLayoutResults = false;

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

  /** Initialize sticky header during layout when its component tree is null */
  public static boolean initStickyHeaderInLayoutWhenComponentTreeIsNull = false;

  public static boolean unsafeHostComponentRecyclingIsEnabled = false;

  /** Whether a {@link ComponentHost} can be pre-allocated */
  public static boolean isHostComponentPreallocationEnabled = false;

  public static int hostComponentPoolSize = 30;

  /** When {@code true} ComponentTree records state change snapshots */
  public static boolean isTimelineEnabled = isDebugModeEnabled;

  public static @Nullable String timelineDocsLink = null;

  /**
   * When enabled split resolve and layout futures will each use dedicated thread handlers so that
   * they don't queue against each other.
   */
  public static boolean useSeparateThreadHandlersForResolveAndLayout = false;

  /** Return true when resolve and layout futures are split, and each uses its own thread handler */
  public static boolean isSplitResolveAndLayoutWithSplitHandlers() {
    return useSeparateThreadHandlersForResolveAndLayout;
  }

  public static boolean enableIsBoringLayoutCheckTimeout = false;

  /** Skip checking for root component and tree-props while layout */
  public static boolean isSkipRootCheckingEnabled = false;

  public static boolean shouldCompareCommonPropsInIsEquivalentTo = false;

  public static boolean shouldCompareRootCommonPropsInSingleComponentSection = false;

  public static boolean shouldDelegateContentDescriptionChangeEvent = false;

  /** This toggles whether {@Link #LayoutThreadPoolExecutor} should timeout core threads or not */
  public static boolean shouldAllowCoreThreadTimeout = false;

  public static long layoutThreadKeepAliveTimeMs = 1000;

  public static boolean crashIfExceedingStateUpdateThreshold = false;

  public static boolean enableRecyclerBinderStableId = false;

  public static boolean enableLayoutCaching = false;

  public static int recyclerBinderStrategy = 0;

  public static boolean enableMountableRecycler = false;

  public static boolean enableMountableTwoBindersRecycler = false;
  public static boolean enableSeparateAnimatorBinder = false;

  public static boolean enableMountableRecyclerInGroups = false;

  public static boolean enableMountableInIGDS = false;
  public static boolean enableMountableInIG4A = false;
  public static boolean hostComponentAlwaysShouldUpdate = true;

  public static boolean disableFlexDirectionInResolve = false;

  private static boolean sReduceMemorySpikeUserSession = false;
  private static boolean sReduceMemorySpikeDataDiffSection = false;
  private static boolean sReduceMemorySpikeGetUri = false;

  public static void setReduceMemorySpikeUserSession() {
    sReduceMemorySpikeUserSession = true;
  }

  public static boolean reduceMemorySpikeUserSession() {
    return sReduceMemorySpikeUserSession;
  }

  public static void setReduceMemorySpikeDataDiffSection() {
    sReduceMemorySpikeDataDiffSection = true;
  }

  public static boolean reduceMemorySpikeDataDiffSection() {
    return sReduceMemorySpikeDataDiffSection;
  }

  public static void setReduceMemorySpikeGetUri() {
    sReduceMemorySpikeGetUri = true;
  }

  public static boolean reduceMemorySpikeGetUri() {
    return sReduceMemorySpikeGetUri;
  }

  public static boolean enableStateUpdatesBatching = true;

  @Nullable public static ComponentsLogger sComponentsLogger;

  /** Debug option to highlight interactive areas in mounted components. */
  public static boolean debugHighlightInteractiveBounds = false;

  /** Debug option to highlight mount bounds of mounted components. */
  public static boolean debugHighlightMountBounds = false;

  private static ComponentsConfiguration.Builder defaultBuilder = new Builder();

  private static ComponentsConfiguration defaultComponentsConfiguration = defaultBuilder.build();

  public static void setDefaultComponentsConfigurationBuilder(Builder builder) {
    defaultBuilder = builder;
    defaultComponentsConfiguration = defaultBuilder.build();
  }

  public static ComponentsConfiguration getDefaultComponentsConfiguration() {
    return defaultComponentsConfiguration;
  }

  public static ComponentsConfiguration.Builder getDefaultComponentsConfigurationBuilder() {
    return defaultBuilder;
  }

  private final boolean mUseCancelableLayoutFutures;

  private final boolean mUseInterruptibleResolution;

  private boolean mShouldCacheLayouts;

  private final boolean mShouldCacheNestedLayouts;

  private final boolean mShouldReuseOutputs;

  private final boolean mShouldAddHostViewForRootComponent;

  private final boolean mShouldDisableBgFgOutputs;

  private final boolean mUseIncrementalMountGapWorker;

  private final boolean mNestedPreallocationEnabled;

  private final boolean mUseSyncMountPools;

  public boolean isNestedPreallocationEnabled() {
    return mNestedPreallocationEnabled;
  }

  public boolean useSyncMountPools() {
    return mUseSyncMountPools;
  }

  public boolean getUseCancelableLayoutFutures() {
    return mUseCancelableLayoutFutures;
  }

  public boolean getUseInterruptibleResolution() {
    return mUseInterruptibleResolution;
  }

  public boolean isShouldAddHostViewForRootComponent() {
    return mShouldAddHostViewForRootComponent;
  }

  public boolean isShouldDisableBgFgOutputs() {
    return mShouldAddHostViewForRootComponent || mShouldDisableBgFgOutputs;
  }

  public boolean useIncrementalMountGapWorker() {
    return mUseIncrementalMountGapWorker;
  }

  private ComponentsConfiguration(ComponentsConfiguration.Builder builder) {
    mUseCancelableLayoutFutures = builder.mUseCancelableLayoutFutures;
    mUseInterruptibleResolution = builder.mUseInterruptibleResolution;
    mShouldCacheLayouts = builder.mShouldCacheLayouts;
    mShouldCacheNestedLayouts = builder.mShouldCacheNestedLayouts;
    mShouldReuseOutputs = builder.mShouldReuseOutputs;
    mShouldAddHostViewForRootComponent = builder.mShouldAddHostViewForRootComponent;
    mShouldDisableBgFgOutputs = builder.mShouldDisableBgFgOutputs;
    mUseIncrementalMountGapWorker = builder.mUseIncrementalMountGapWorker;
    mNestedPreallocationEnabled = builder.mNestedPreallocationEnabled;
    mUseSyncMountPools = builder.mUseSyncItemPools;
  }

  public boolean shouldReuseOutputs() {
    return mShouldReuseOutputs;
  }

  public static ComponentsConfiguration.Builder create() {
    return create(defaultComponentsConfiguration);
  }

  public static ComponentsConfiguration.Builder create(ComponentsConfiguration config) {
    return new Builder()
        .useCancelableLayoutFutures(config.getUseCancelableLayoutFutures())
        .shouldAddHostViewForRootComponent(config.isShouldAddHostViewForRootComponent())
        .shouldDisableBgFgOutputs(config.isShouldDisableBgFgOutputs());
  }

  public boolean shouldCacheLayouts() {
    return mShouldCacheLayouts;
  }

  public boolean shouldCacheNestedLayouts() {
    return mShouldCacheNestedLayouts;
  }

  public static class Builder {

    boolean mUseCancelableLayoutFutures = true;
    boolean mUseInterruptibleResolution = true;
    boolean mShouldCacheLayouts = ComponentsConfiguration.enableLayoutCaching;
    boolean mShouldCacheNestedLayouts = ComponentsConfiguration.enableLayoutCaching;
    boolean mShouldReuseOutputs = false;
    boolean mIsLayoutCancellationEnabled = false;
    boolean mShouldAddHostViewForRootComponent = false;
    boolean mShouldDisableBgFgOutputs = false;
    boolean mUseIncrementalMountGapWorker = IncrementalMountExtensionConfigs.useGapWorker;
    boolean mNestedPreallocationEnabled = false;

    boolean mUseSyncItemPools = true;

    protected Builder() {}

    public Builder useCancelableLayoutFutures(boolean enable) {
      this.mUseCancelableLayoutFutures = enable;
      return this;
    }

    public Builder useInterruptibleResolution(boolean enable) {
      this.mUseInterruptibleResolution = enable;
      return this;
    }

    public Builder shouldAddHostViewForRootComponent(boolean enabled) {
      mShouldAddHostViewForRootComponent = enabled;
      return this;
    }

    public Builder shouldDisableBgFgOutputs(boolean enabled) {
      mShouldDisableBgFgOutputs = enabled;
      return this;
    }

    public Builder shouldCacheLayouts(boolean enabled) {
      mShouldCacheLayouts = enabled;
      return this;
    }

    public Builder shouldCacheNestedLayouts(boolean enabled) {
      mShouldCacheNestedLayouts = enabled;
      return this;
    }

    public Builder shouldReuseOutputs(boolean enabled) {
      mShouldReuseOutputs = enabled;
      return this;
    }

    public Builder useIncrementalMountGapWorker(boolean enabled) {
      mUseIncrementalMountGapWorker = enabled;
      return this;
    }

    public Builder useSyncItemPools(boolean enabled) {
      mUseSyncItemPools = enabled;
      return this;
    }

    /**
     * If true, uses the root ComponentTree's mount content allows the usage of the preallocation
     * handler to perform preallocation for nested trees.
     */
    public Builder nestedPreallocationEnabled(boolean enabled) {
      mNestedPreallocationEnabled = enabled;
      return this;
    }

    public ComponentsConfiguration build() {
      return new ComponentsConfiguration(this);
    }
  }
}
