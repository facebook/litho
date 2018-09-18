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

/** ComparableDrawableWrapper to wrap instances of a {@link Drawable} */
public class ComparableDrawableWrapper implements ComparableDrawable<Drawable> {

  private final Drawable mDrawable;

  private ComparableDrawableWrapper(Drawable drawable) {
    this.mDrawable = drawable;
  }

  @Override
  public Drawable acquire(Context context) {
    return mDrawable;
  }

  @Override
  public void release(Context context) {
    // do nothing
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableDrawableWrapper)) {
      return false;
    }

    return mDrawable.equals(((ComparableDrawableWrapper) other).mDrawable);
  }

  public static ComparableDrawableWrapper create(Drawable drawable) {
    return new ComparableDrawableWrapper(drawable);
  }
}
