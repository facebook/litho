/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.drawable;

import android.graphics.drawable.ColorDrawable;
import androidx.annotation.ColorInt;

/** A comparable color drawable. */
public class ComparableColorDrawable extends ComparableDrawableWrapper {

  @ColorInt private final int mColor;

  private ComparableColorDrawable(@ColorInt int color) {
    super(new ColorDrawable(color));
    this.mColor = color;
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableColorDrawable)) {
      return false;
    }
    return mColor == ((ComparableColorDrawable) other).mColor;
  }

  public @ColorInt int getColor() {
    return mColor;
  }

  public static ComparableColorDrawable create(@ColorInt int color) {
    return new ComparableColorDrawable(color);
  }
}
