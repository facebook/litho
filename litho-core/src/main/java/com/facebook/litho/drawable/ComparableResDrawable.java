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
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

/** A comparable implementation of drawables sourced from android resources. */
public class ComparableResDrawable extends ComparableDrawableWrapper {

  @DrawableRes private final int mResId;
  private final Configuration mConfig;

  private ComparableResDrawable(@DrawableRes int resId, Configuration config, Drawable drawable) {
    super(drawable);
    this.mResId = resId;
    this.mConfig = config;
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableResDrawable)) {
      return false;
    }
    return mResId == ((ComparableResDrawable) other).mResId
        && mConfig.equals(((ComparableResDrawable) other).mConfig);
  }

  public static ComparableResDrawable create(Context context, @DrawableRes int resId) {
    Configuration config = new Configuration(context.getResources().getConfiguration());
    Drawable drawable = ContextCompat.getDrawable(context, resId);
    return new ComparableResDrawable(resId, config, drawable);
  }
}
