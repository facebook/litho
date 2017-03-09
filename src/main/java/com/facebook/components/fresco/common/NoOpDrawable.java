// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.fresco.common;

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
