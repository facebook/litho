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

import android.os.Process;
import com.facebook.litho.BuildConfig;
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

  /** If true then the new version of the YogaEdgeWithInts will be used. */
  public static boolean useNewYogaEdge = false;

  /** The default priority for threads that perform background layout calculations. */
  public static final int DEFAULT_BACKGROUND_THREAD_PRIORITY = 5;

  public static int defaultChangeSetThreadPriority = Process.THREAD_PRIORITY_BACKGROUND;

  /**
   * Whether components with transitions set on don't necessarily have to be wrapped in views
   *
   * <p>If enabled, we'll decide if a component needs to be wrapped in a view for running
   * Transitions at the layout phase, taking into account among others factors like: number of
   * children, if background is set, etc. This is under development, thus, when enabled, some
   * functionality may not work as expected or not work at all.
   */
  public static boolean doNotForceWrappingInViewForAnimation = false;

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

  /** Configuration for asynchronous state update */
  public static boolean updateStateAsync = false;

  /** Configuration for lazifying state of ComponentHost */
  public static boolean lazyComponentHost = false;

  /** Assign transition keys to all LayoutOutputs, this enables Transition.allLayout() */
  public static boolean assignTransitionKeysToAllOutputs = false;

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

  public static boolean saveMeasuredSizesInMeasure = false;

  public static boolean prewarmImageTexture = false;

  /** By default ComponentHost (extending ViewGroup) delay press state. */
  public static boolean shouldDelayChildPressedState = true;

  public static boolean useNewIsEquivalentTo = false;

  /** Assess the impact of using the Throwable class to track crashes across threads */
  public static boolean enableThrowableTracking = false;

  /** Whether we should do incremental mount on pre draw */
  public static boolean incrementalMountOnPreDraw = false;

  /** Whether we should use the global visible rect for mounting recursively */
  public static boolean useGlobalRectForRecursiveMounting = true;

  /** Whether we should use the PlaceholderComponent instead of Column as MountSpec holder. */
  public static boolean usePlaceholderComponent = false;
}
