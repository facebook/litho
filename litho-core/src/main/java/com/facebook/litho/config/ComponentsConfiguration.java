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
import com.facebook.litho.BuildConfig;
import com.facebook.litho.perfboost.LithoPerfBoosterFactory;
import java.util.Set;

/**
 * Hi there, traveller! This configuration class is not meant to be used by end-users of Litho. It
 * contains mainly flags for features that are either under active development and not ready for
 * public consumption, or for use in experiments.
 *
 * <p>These values are safe defaults and should not require manual changes.
 */
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
   * discouraged to to change this to false, unless you handle all your updates outside of the litho
   * framework
   */
  public static boolean useGlobalKeys = true;

  /** Whether to unmount all contents of LithoView when its ComponentTree is set to null. */
  public static boolean unmountAllWhenComponentTreeSetToNull = false;

  /**
   * Configuration for creating a thread pool of threads used for background layout. If null, a
   * single default thread will be used for background layout.
   */
  public static @Nullable LayoutThreadPoolConfiguration threadPoolForBackgroundThreadsConfig = null;

  /**
   * If non-null, a thread pool will be used for async layouts instead of a single layout thread.
   */
  public static @Nullable LayoutThreadPoolConfiguration threadPoolConfiguration = null;

  public static boolean enableThreadTracingStacktrace = false;

  /**
   * Whether the background thread that's currently running the layout should have its priority
   * raised to the thread priority of the UI thread.
   */
  public static boolean inheritPriorityFromUiThread = false;

  /**
   * Whether the OnShouldCreateLayoutWithNewSizeSpec is used with Layout Spec with size spec. This
   * will also disable the associated layout caching.
   */
  public static boolean enableShouldCreateLayoutWithNewSizeSpec = true;

  /** Ignore duplicate transition keys in the same layout, rather than throwing an exception. */
  public static boolean ignoreDuplicateTransitionKeysInLayout = false;

  /**
   * Enables more smart approach to processing autogenerated transition ids in
   * TransitionManager.setupTransitions(), taking into account whether there are transitions that
   * target all layout, and the fact that appear/disappear animations cannot be defined for
   * autogenerated ids
   */
  public static boolean onlyProcessAutogeneratedTransitionIdsWhenNecessary = false;

  /** Sets if is reconciliation is enabled */
  public static boolean isReconciliationEnabled = true;

  /**
   * Sets if layout diffing is enabled. This should be used in conjugation with
   * {@link#isReconciliationEnabled}.
   */
  public static boolean isLayoutDiffingEnabled = true;

  // TODO T39526148 Remove once Flipper plugin is usable.
  /** If true, information about RenderInfos will be passed to Flipper's layout inspector. */
  public static boolean enableRenderInfoDebugging = false;

  public static boolean useCancelableLayoutFutures;
  public static boolean canInterruptAndMoveLayoutsBetweenThreads = false;

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

  public static boolean ignoreStateUpdatesForScreenshotTest;

  public static boolean isRenderInfoDebuggingEnabled() {
    return isDebugModeEnabled && enableRenderInfoDebugging;
  }

  public static boolean prioritizeRenderingOnParallel = true;

  public static boolean useSharedFutureOnParallel = true;

  /**
   * If true, we also check if the RecyclerBinder needs remeasuring when checking if it's been
   * measured.
   */
  public static boolean checkNeedsRemeasure = false;

  /** (Hopefully) temporary measure as we're investigating a major crash in libhwui. */
  public static boolean disableComponentHostPool = true;

  public static @Nullable LithoPerfBoosterFactory perfBoosterFactory = null;

  /**
   * If true, the {@link #perfBoosterFactory} will be used to indicate that LayoutStateFuture thread
   * can use the perf boost
   */
  public static boolean boostPerfLayoutStateFuture;

  /**
   * When enabled reconciliation will use the deep clone method of the InternalNode with the
   * simplified implementation of shallow copy.
   */
  public static boolean shouldUseDeepCloneDuringReconciliation = false;

  /** When enabled it would use Internal Nodes for layout diffing instead Diff Nodes. */
  public static boolean useInternalNodesForLayoutDiffing = false;

  public static boolean incrementalVisibilityHandling = false;

  /** Enabling this will force all updateStateSync methods to be updateStateAsync. * */
  public static boolean shouldForceAsyncStateUpdate = false;

  public static boolean useExtensionsWithMountDelegate = false;

  public static boolean delegateToRenderCoreMount = false;

  public static boolean shouldDisableDrawableOutputs = false;

  public static boolean useVisibilityExtension = false;

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

  public static boolean hostHasOverlappingRendering = true;

  public static int textureSizeWarningLimit = Integer.MAX_VALUE;

  public static int overlappingRenderingViewSizeLimit = Integer.MAX_VALUE;

  public static int partialAlphaWarningSizeThresold = Integer.MAX_VALUE;

  public static @Nullable Set<String> componentPreallocationBlocklist = null;

  /** When {@code true} ComponentTree records state change snapshots */
  public static boolean isTimelineEnabled = isDebugModeEnabled;

  public static @Nullable String timelineDocsLink = null;

  /**
   * If true, when a LithoView with nested LithoView children gets released, the children will get
   * released too.
   */
  public static boolean releaseNestedLithoViews = false;
}
