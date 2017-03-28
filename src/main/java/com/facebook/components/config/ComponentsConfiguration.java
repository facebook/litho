/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.config;

import android.os.Build;

import com.facebook.components.BuildConfig;

/**
 * Configuration for the Components library.
 */
public class ComponentsConfiguration {

  /**
   * Indicates whether this is an internal build.
   * Note that the implementation of {@link BuildConfig} that this class is compiled against may not
   * be the one that is included in the APK. See: <a
   * href="http://facebook.github.io/buck/rule/android_build_config.html">android_build_config</a>.
   */
  public static final boolean IS_INTERNAL_BUILD = BuildConfig.IS_INTERNAL_BUILD;

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
   * Indicates whether LayoutState should try to generate DisplayLists for Components that support
   * that.
   */
  public static boolean shouldGenerateDisplayLists =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

  /**
   * Indicates whether InternalNode should use CSSNodeDEPRECATED or YogaNode
   */
  public static boolean shouldUseCSSNodeJNI = true;

  /*
   * Use the new bootstrap ranges code instead of initializing all the items when the binder view
   * is measured (t12986103).
   */
  public static boolean bootstrapBinderItems = false;
}
