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

package com.facebook.litho.reference;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.facebook.litho.drawable.ComparableDrawable;

/**
 * A very simple Reference for {@link Drawable} used in all the cases where it's not
 * possible/desirable to use a real Reference. This will simply keep a reference to the Drawable in
 * the Props and return it. Please take care when using this. It keeps the drawable in memory all
 * the time and should only be used when the other built in specs are not applicable and it's not
 * possible to write a custom ReferenceSpec
 */
final class DrawableReferenceLifecycle extends ReferenceLifecycle<ComparableDrawable> {

  private static final DrawableReferenceLifecycle sInstance = new DrawableReferenceLifecycle();

  private DrawableReferenceLifecycle() {}

  public static DrawableReferenceLifecycle get() {
    return sInstance;
  }

  @Override
  protected ComparableDrawable onAcquire(Context context, Reference<ComparableDrawable> reference) {
    return ((DrawableReference) reference).mDrawable;
  }

  @Override
  protected boolean shouldUpdate(
      Reference<ComparableDrawable> previous, Reference<ComparableDrawable> next) {
    ComparableDrawable previousDrawable = ((DrawableReference) previous).mDrawable;
    ComparableDrawable nextDrawable = ((DrawableReference) next).mDrawable;
    return !previousDrawable.isEquivalentTo(nextDrawable);
  }
}
