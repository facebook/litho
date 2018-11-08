/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.shadows;

import static org.robolectric.shadow.api.Shadow.directlyOn;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadows.ShadowDrawable;

/**
 * Shadows a {@link ColorDrawable} to support of drawing its description on a
 * {@link org.robolectric.shadows.ShadowCanvas}
 */
@Implements(value = ColorDrawable.class)
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

  @Override
  @Implementation
  public void setAlpha(int alpha) {
    mAlpha = alpha;
    directlyOn(mRealColorDrawable, ColorDrawable.class).setAlpha(alpha);
  }

  @Override
  @Implementation
  public int getAlpha() {
    return mAlpha;
  }
}
