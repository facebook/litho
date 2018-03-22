/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

/**
 * Configures what components can put their children's layout calculations on multiple background
 * threads.`
 */
public class SplitBackgroundLayoutConfiguration {

  static boolean canSplitChildrenLayouts(Component component) {
    // todo mihaelao T27032479
    return false;
  }
}
