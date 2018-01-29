/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.config;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;

import com.facebook.litho.BuildConfig;
import com.facebook.yoga.YogaLogger;

/**
 * Configuration for the Components library.
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

  /**
   * Whether to use Object pooling via {@link com.facebook.litho.ComponentsPools}. This is switch
   * because we are experimenting with turning off pooling to get a sense of what its impact is in
   * production.
   */
  public static volatile boolean usePooling = true;

  /** Enable exception delegation to {@link com.facebook.litho.annotations.OnError}. */
  public static boolean enableOnErrorHandling = false;

  /**
   * Whether incremental mount should use the local visible bounds of the {@link
   * com.facebook.litho.LithoView}.
   */
  public static boolean incrementalMountUsesLocalVisibleBounds = false;

  /**
   * Whether incremental mount that begins in {@link com.facebook.litho.LithoView} should use the
   * local visible bounds of the view.
   */
  public static boolean lithoViewIncrementalMountUsesLocalVisibleBounds = false;

  /**
   * Whether to keep a reference to the InternalNode tree in LayoutState instead of immediately
   * releasing it.
   */
  public static boolean persistInternalNodeTree = false;

  /** Whether the RecyclerCollectionComponent can asynchronously set the root of a SectionTree. */
  public static boolean setRootAsyncRecyclerCollectionComponent = false;

  /**
   * If true, insert operations with the {@link com.facebook.litho.widget.RecyclerBinder} will not
   * start async layout calculations for the items in range, instead these layout calculations will
   * be posted to the next frame.
   */
  public static boolean insertPostAsyncLayout = false;

  /**
   * If true, the components mKey and mChildCounters will not be initialized at construction time.
   */
  public static boolean lazyInitializeComponent = false;

  /**
   * If false, global keys will not be generated (litho level state updates won't work). It's highly
   * discouraged to to change this to false, unless you handle all your updates outside of the litho
   * framework
   */
  public static boolean useGlobalKeys = true;

  /**
   * Whether to use special recycling for ComponentHosts or not. True (default) if we should use
   * scrap host recycling (see ComponentHost#mScrapHosts).
   */
  public static boolean scrapHostRecyclingForComponentHosts = true;

  /**
   * If scrapHostRecyclingForComponentHosts is false, determines whether we try to preallocate
   * ComponentHosts.
   */
  public static boolean preallocateComponentHosts = false;

  /** Whether MatrixDrawable draw call can be shortcutted to underlying drawable */
  public static boolean shortcutMatrixDrawable = false;

  /** If scrapHostRecyclingForComponentHosts is false, determines the ComponentHost pool size. */
  public static int componentHostPoolSize = 30;

  /** If true then the new version of the YogaEdgeWithInts will be used. */
  public static boolean useNewYogaEdge = false;
}
