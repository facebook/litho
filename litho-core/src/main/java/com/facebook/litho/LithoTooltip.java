/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.graphics.Rect;
import android.view.View;

public interface LithoTooltip {

  /**
   * Do not call this method directly! It should only be called by the Litho framework to ensure
   * that the correct parameters are passed. Use the methods exposed by {@link
   * LithoTooltipController} instead.
   *
   * <p>Display the tooltip on an anchor component with the given bounds relative to the view that
   * contains it.
   *
   * @param container View that wraps the Component that is used as anchor
   * @param anchorBounds Rect with the bounds of the component relative to host view
   * @param xOffset move tooltip with this horizontal offset from default position
   * @param yOffset move tooltip with this vertical offset from default position
   */
  void showLithoTooltip(View container, Rect anchorBounds, int xOffset, int yOffset);
}
