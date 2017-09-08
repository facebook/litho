/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.view.View;

/**
 * Defines a tooltip that can be passed to the ComponentTree to be anchored to a component. The
 * framework takes care of finding the position where the tooltip needs to anchored.
 */
public interface LithoTooltip {

  /**
   * Display the content view in a popup window anchored to the bottom-left corner of the anchor
   * view offset by the specified x and y coordinates.
   */
  void showBottomLeft(View anchor, int x, int y);
}
