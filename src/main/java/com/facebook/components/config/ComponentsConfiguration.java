// Copyright 2004-present Facebook. All Rights Reserved.

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

  /**
   * Indicates whether InternalNode should use YogaNode or ReNode
   */
  public static boolean shouldUseRelayout = false;

  /*
   * Use the new bootstrap ranges code instead of initializing all the items when the binder view
   * is measured (t12986103).
   */
  public static boolean bootstrapBinderItems = false;

  /**
   * Indicates whether we should clear the ComponentContext of a component before saving it as
   * the current state value info in the StateHandler.
   */
  public static boolean clearContextForStateHandler = false;

  /**
   * Indicates whether Reference objects should be acquired and released on demand, or pre-acquired
   * and kept in memory.
   */
  public static boolean preAcquireReferences = false;
}
