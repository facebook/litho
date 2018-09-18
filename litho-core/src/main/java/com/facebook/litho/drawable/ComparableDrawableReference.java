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
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import javax.annotation.Nullable;

/** Use this to wrap DrawableReference */
public class ComparableDrawableReference<D extends Drawable> implements ComparableDrawable<D> {

  private final Reference<D> reference;
  private @Nullable D mDrawable;

  public ComparableDrawableReference(Reference<D> reference) {
    this.reference = reference;
  }

  @Override
  public D acquire(Context context) {
    mDrawable = Reference.acquire(context, reference);
    return mDrawable;
  }

  @Override
  public void release(Context context) {
    if (mDrawable != null) {
      Reference.release(context, mDrawable, reference);
      mDrawable = null;
    }
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableDrawableReference)) {
      return false;
    }

    return !Reference.shouldUpdate(reference, ((ComparableDrawableReference) other).reference);
  }

  public static ComparableDrawableReference<Drawable> create(Drawable drawable) {
    Reference<Drawable> reference = DrawableReference.create().drawable(drawable).build();
    return new ComparableDrawableReference<>(reference);
  }
}
