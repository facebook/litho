/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

public class BoundsHelper {

  /**
   * Sets the bounds on the given view if the view doesn't already have those bounds (or if 'force'
   * is supplied).
   */
  public static void applyBoundsToView(
      View view, int left, int top, int right, int bottom, boolean force) {
    int width = right - left;
    int height = bottom - top;

    if (force || view.getMeasuredHeight() != height || view.getMeasuredWidth() != width) {
      view.measure(
          makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    if (force
        || view.getLeft() != left
        || view.getTop() != top
        || view.getRight() != right
        || view.getBottom() != bottom) {
      view.layout(left, top, right, bottom);
    }
  }

  public static void applySizeToDrawableForAnimation(Drawable drawable, int width, int height) {
    Rect bounds = drawable.getBounds();
    drawable.setBounds(bounds.left, bounds.top, bounds.left + width, bounds.top + height);

    // TODO(t22432769): Remove this after D5965597 lands
    if (drawable instanceof MatrixDrawable) {
      ((MatrixDrawable) drawable).bind(width, height);
    }
  }
}
