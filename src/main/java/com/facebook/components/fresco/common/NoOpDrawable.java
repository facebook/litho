/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.fresco.common;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/**
 * This drawable is used in place of null in DraweeDrawable. DraweeDrawable extends from
 * ForwardingDrawable which does not work with null drawables.
 */
public class NoOpDrawable extends Drawable {

  @Override
  public void draw(Canvas canvas) {
    // no op
  }

  @Override
  public int getOpacity() {
    return 0;
  }

  @Override
  public void setAlpha(int alpha) {
    // no op
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    // no op
  }
}
