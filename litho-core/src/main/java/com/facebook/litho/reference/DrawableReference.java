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

import android.graphics.drawable.Drawable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.ComparableDrawable;

/**
 * A very simple Reference for {@link Drawable} used in all the cases where it's not
 * possible/desirable to use a real Reference. This will simply keep a reference to the Drawable in
 * the Props and return it. Please take care when using this. It keeps the drawable in memory all
 * the time and should only be used when the other built in specs are not applicable and it's not
 * possible to write a custom ReferenceSpec
 */
public final class DrawableReference extends Reference<ComparableDrawable> {

  ComparableDrawable mDrawable;

  private DrawableReference(ComparableDrawable drawable) {
    super(
        ComponentsConfiguration.isDrawableReferenceNonSynchronized
            ? DrawableReferenceLifecycle.get()
            : DrawableReferenceLifecycle.syncGet());
    mDrawable = drawable;
  }

  @Override
  public int hashCode() {
    return mDrawable != null ? mDrawable.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof DrawableReference)) {
      return false;
    }

    DrawableReference state = (DrawableReference) o;
    return mDrawable == state.mDrawable;
  }

  @Override
  public String getSimpleName() {
    return "DrawableReference";
  }

  /**
   * Utility method to create Comparable Drawable Reference
   *
   * @param drawable the drawable to wrap
   * @return the no-op drawable reference
   */
  public static Reference<ComparableDrawable> create(ComparableDrawable drawable) {
    return new DrawableReference(drawable);
  }
}
