// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowDrawable;

import static org.robolectric.internal.Shadow.directlyOn;

/**
 * Shadows a {@link ColorDrawable} to support of drawing its description on a
 * {@link org.robolectric.shadows.ShadowCanvas}
 */
@Implements(value = ColorDrawable.class, inheritImplementationMethods = true)
public class ColorDrawableShadow extends ShadowDrawable {

  @RealObject private ColorDrawable mRealColorDrawable;

  private int mAlpha;

  @Implementation
  public void draw(Canvas canvas) {
    canvas.drawColor(mRealColorDrawable.getColor());
  }

  @Implementation
  public void setColorFilter(ColorFilter colorFilter) {
    directlyOn(mRealColorDrawable, ColorDrawable.class).setColorFilter(colorFilter);
  }

  @Implementation
  public void setAlpha(int alpha) {
    mAlpha = alpha;
    directlyOn(mRealColorDrawable, ColorDrawable.class).setAlpha(alpha);
  }

  @Implementation
  public int getAlpha() {
    return mAlpha;
  }
}
