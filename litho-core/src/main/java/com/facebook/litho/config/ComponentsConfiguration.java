/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.config;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

import android.support.annotation.Nullable;
import com.facebook.litho.BuildConfig;
import com.facebook.litho.boost.LithoAffinityBoosterFactory;
import com.facebook.yoga.YogaLogger;

/**
 * Hi there, traveller! This configuration class is not meant to be used by end-users of Litho. It
 * contains mainly flags for features that are either under active development and not ready for
 * public consumption, or for use in experiments.
 *
 * These values are safe defaults and should not require manual changes.
 */
public class ComponentsConfiguration {

  public static YogaLogger YOGA_LOGGER;

  /**
   * Indicates whether this is an internal build. Note that the implementation
   * of <code>BuildConfig</code> that this class is compiled against may not be
   * the one that is included in the
   * APK. See: <a
   * href="http://facebook.github.io/buck/rule/android_build_config.html">android_build_config</a>.
   */
  public static final boolean IS_INTERNAL_BUILD = BuildConfig.IS_INTERNAL_BUILD;

  /** Indicates that the incremental mount helper is required for this build. */
  public static final boolean USE_INCREMENTAL_MOUNT_HELPER =
      BuildConfig.USE_INCREMENTAL_MOUNT_HELPER;

  /** Whether transitions are supported for this API version. */
  public static final boolean ARE_TRANSITIONS_SUPPORTED = (SDK_INT >= ICE_CREAM_SANDWICH);

  /** Whether we can access properties in Settings.Global for animations. */
  public static final boolean CAN_CHECK_GLOBAL_ANIMATOR_SETTINGS = (SDK_INT >= JELLY_BEAN_MR1);

  /** Whether we need to account for lack of synchronization while accessing Themes. */
  public static final boolean NEEDS_THEME_SYNCHRONIZATION = (SDK_INT <= LOLLIPOP_MR1);

  /**
   * Option to enabled debug mode. This will save extra data asscociated with each node and allow
   * more info about the hierarchy to be retrieved. Used to enable stetho integration. It is highly
   * discouraged to enable this in production builds. Due to how the Litho releases are distributed
   * in open source IS_INTERNAL_BUILD will always be false. It is therefore required to override
   * this value using your own application build configs. Recommended place for this is in a
   * Application subclass onCreate() method.
   */
  public static boolean isDebugModeEnabled = IS_INTERNAL_BUILD;

  /**
   * Debug option to highlight interactive areas in mounted components.
   */
  public static boolean debugHighlightInteractiveBounds = false;

  /**
   * LithoView overlay showing whether its ComponentTree was computed on UI thread (red) or bg
   * thread (green).
   */
  public static boolean enableLithoViewDebugOverlay = false;

  /**
   * Debug option to highlight mount bounds of mounted components.
   */
  public static boolean debugHighlightMountBounds = false;

  /**
   * Populates additional metadata to find mounted components at runtime. Defaults to the presence
   * of an <pre>IS_TESTING</pre> system property at startup but can be overridden at runtime.
   */
  public static boolean isEndToEndTestRun = System.getProperty("IS_TESTING") != null;

  /** Enable exception delegation to {@link com.facebook.litho.annotations.OnError}. */
  public static boolean enableOnErrorHandling = false;

  /** Whether the RecyclerCollectionComponent can asynchronously set the root of a SectionTree. */
  public static boolean setRootAsyncRecyclerCollectionComponent = false;

  /**
   * If true, insert operations with the {@link com.facebook.litho.widget.RecyclerBinder} will not
   * start async layout calculations for the items in range, instead these layout calculations will
   * be posted to the next frame.
   */
  public static boolean insertPostAsyncLayout = false;

  /**
   * If false, global keys will not be generated (litho level state updates won't work). It's highly
   * discouraged to to change this to false, unless you handle all your updates outside of the litho
   * framework
   */
  public static boolean useGlobalKeys = true;

  /** The default priority for threads that perform background layout calculations. */
  public static final int DEFAULT_BACKGROUND_THREAD_PRIORITY = 5;

  public static int defaultChangeSetThreadPriority = 0;

  /** If true then we'll allocate YogaEdgesWithIntsImplOptimized arrays in batches */
  public static boolean useBatchArrayAllocator = false;

  /** If true then we'll lazily initialize the LayoutStateOutputIdCalculator */
  public static boolean lazilyInitializeLayoutStateOutputIdCalculator = false;

  /** Whether to unmount all contents of LithoView when its ComponentTree is set to null. */
  public static boolean unmountAllWhenComponentTreeSetToNull = false;

  /**
   * By default end-to-end tests will disable transitions and this flag lets to explicitly enable
   * transitions to test animation related behavior.
   */
  public static boolean forceEnableTransitionsForInstrumentationTests = false;

  /**
   * Configuration for creating a thread pool of threads used for background layout. If null, a
   * single default thread will be used for background layout.
   */
  public static LayoutThreadPoolConfiguration threadPoolForBackgroundThreadsConfig = null;

  /** If true, a single thread pool will be used instead of creating one per RecyclerBinder. */
  public static boolean useSingleThreadPool = false;

  /** Configuration for asynchronous state update */
  public static boolean updateStateAsync = false;

  /**
   * If false, we won't create state handlers. It's highly discouraged to to change this to false,
   * unless you handle all your updates outside of the litho framework
   */
  public static boolean useStateHandlers = true;

  /** Default for ComponentHost#hasOverlappingRendering. */
  public static boolean hostHasOverlappingRendering = true;

  /** Enable variable BatchSize for ArrayBatchAllocator */
  public static boolean variableArrayBatchAllocatorEnabled = false;

  /** Startup Size for ArrayBatchAllocator Batch Size */
  public static int arrayBatchAllocatorStartupSize = 200;

  /** Runtime Size for ArrayBatchAllocation Batch Size */
  public static int arrayBatchAllocationRuntimeSize = 200;

  public static boolean prewarmImageTexture = false;

  public static boolean useNewIsEquivalentTo = false;

  public static boolean useNewIsEquivalentToInLayoutSpec = false;

  public static boolean useNewIsEquivalentToInMountSpec = false;

  public static boolean useNewIsEquivalentToInSectionSpec = false;

  public static boolean useNewIsEquivalentToInOtherSpec = false;

  /** Whether we should use the PlaceholderComponent instead of Column as MountSpec holder. */
  public static boolean usePlaceholderComponent = false;

  /**
   * If true, the async range calculation isn't blocked on the first item finishing layout and it
   * will schedule one layout per bg thread during init range.
   */
  public static boolean asyncInitRange = false;

  /**
   * If true, the async range calculation isn't blocked on the first item finishing layout and it
   * will schedule as many bg layouts as it can while init range completes.
   */
  public static boolean bgScheduleAllInitRange;

  /**
   * If true, a layout for the same ComponentTree will be calculated on a single thread at the same
   * time.
   */
  public static boolean useSharedLayoutStateFuture = false;

  /**
   * If non-null, a thread pool will be used for async layouts instead of a single layout thread.
   */
  public static LayoutThreadPoolConfiguration threadPoolConfiguration = null;

  /**
   * Whether we should diff the view info attributes when checking for mount updates. This fixes
   * issues where updates to MountSpecs are not applied when changes in common view properties do
   * not change the measure of view.
   */
  public static boolean enableViewInfoDiffingForMountStateUpdates = false;

  /** Whether Drawable Reference should get it's lifecycle using a non synchronized getter */
  public static boolean isDrawableReferenceNonSynchronized = false;

  /** Whether we should use Comparable Drawable for more efficient diffing of drawables */
  public static boolean enableComparableDrawable = false;

  /**
   * Whether DefaultComparableDrawables are always equivalent during view info diffing. This is to
   * test if the current default implementation of DefaultComparableDrawable#isEquivalentTo() is
   * causing performance dips.
   */
  public static boolean areDefaultComparableDrawablesAlwaysEquivalent = false;

  public static boolean enableThreadTracingStacktrace = false;

  /**
   * Whether incremental mount should also be done when the LithoView is not visible at all (i.e. so
   * that all of the mount items should be unmounted).
   */
  public static boolean incrementalMountWhenNotVisible = false;

  public static boolean unsetThenReleaseDrawableBackground = false;
  public static boolean unmountThenReleaseDrawableCmp = false;

  /** Whether all drawables are eligible for wrapping into DisplayListDrawables */
  public static boolean useDisplayListForAllDrawables = false;

  public static boolean shouldUpdateMountSpecOnly = false;

  /**
   * Whether the background thread that's currently running the layout should have its priority
   * raised to the thread priority of the UI thread.
   */
  public static boolean inheritPriorityFromUiThread = false;

  public static boolean disablePools = false;

  public static @Nullable LithoAffinityBoosterFactory affinityBoosterFactory = null;

  /**
   * If true, the {@link #affinityBoosterFactory} will be used to bump the LayoutStateFuture thread
   * that the main thread is waiting on a powerful core.
   */
  public static boolean boostAffinityLayoutStateFuture;

  /**
   * If true, the {@link #affinityBoosterFactory} will be used to bump all threads that are
   * calculating a layout to a powerful core.
   */
  public static boolean boostAffinityLithoLayouts;

  /**
   * Whether the OnShouldCreateLayoutWithNewSizeSpec is used with Layout Spec with size spec. This
   * will also disable the associated layout caching.
   */
  public static boolean enableShouldCreateLayoutWithNewSizeSpec = false;

  /**
   * Whether State Handler should explicitly clear cached layouts which may be built using old or
   * stale state. See T36730891 for details
   */
  public static boolean enableStateDidUpdate = true;

  /**
   * Whether we allow animating drawables' properties (other than bounds)
   */
  public static boolean allowAnimatingDrawables = false;

  /** Assign transition keys to all LayoutOutputs, this enables Transition.allLayout() */
  public static boolean assignTransitionKeysToAllOutputs = false;

  public static boolean createPhantomLayoutOutputsForTransitions = false;

  /** Whether keys are based on type id. Default is based on getSimpleName(). */
  public static boolean typeIDBasedKeys = false;

  /**
   * Enables more smart approach to processing autogenerated transition ids in
   * TransitionManager.setupTransitions(), taking into account whether there are transitions that
   * target all layout, and the fact that appear/disappear animations cannot be defined for
   * autogenerated ids
   */
  public static boolean onlyProcessAutogeneratedTransitionIdsWhenNecessary = false;
}
