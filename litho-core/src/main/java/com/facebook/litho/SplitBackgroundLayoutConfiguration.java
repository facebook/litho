/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import com.facebook.litho.config.ComponentsConfiguration;

/**
 * Configures what components can put their children's layout calculations on multiple background
 * threads.
 */
public class SplitBackgroundLayoutConfiguration {

  /** Checks if a component is enabled to split its children layouts on multiple threads. */
  static boolean canSplitChildrenLayouts(Component component) {
    return ComponentsConfiguration.enabledForSplitLayout.contains(
        component.getClass().getSimpleName());
  }

  /** If true, the given component's children layouts will be split on multiple threads. */
  static boolean isSplitLayoutEnabled(Component component) {
    return ComponentsConfiguration.splitLayoutMainThreadPoolConfiguration != null
        && ComponentsConfiguration.splitLayoutBackgroundThreadPoolConfiguration != null
        && component.mSplitChildrenLayoutInThreadPool
        && (ThreadUtils.isMainThread()
            ? ComponentsConfiguration.isMainThreadSplitLayoutEnabled
            : ComponentsConfiguration.isSplitLayoutEnabled);
  }
}
