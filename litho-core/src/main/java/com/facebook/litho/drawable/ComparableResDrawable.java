/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.drawable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import javax.annotation.Nullable;

/** ComparableResDrawable */
public class ComparableResDrawable implements ComparableDrawable<Drawable> {

  @DrawableRes private final int mResId;
  private @Nullable Drawable mDrawable;

  private ComparableResDrawable(@DrawableRes int resId) {
    this.mResId = resId;
  }

  @Override
  public Drawable acquire(Context context) {
    if (mDrawable == null) {
      mDrawable = ContextCompat.getDrawable(context, mResId);
    }

    return mDrawable;
  }

  @Override
  public void release(Context context) {}

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableResDrawable)) {
      return false;
    }

    return mResId == ((ComparableResDrawable) other).mResId;
  }

  public static ComparableResDrawable create(@DrawableRes int resId) {
    return new ComparableResDrawable(resId);
  }
}
