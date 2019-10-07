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

import android.graphics.drawable.Drawable;

/**
 * Comparable Drawable allows drawables to be compared by explicitly implementing the {@link
 * #isEquivalentTo(ComparableDrawable)}. This allows drawables to be compared in a more meaningful
 * way, instead of using {@link Drawable#equals(Object)} which only checks if the references are
 * equal.
 *
 * @see ComparableDrawableWrapper
 * @see ComparableColorDrawable
 * @see ComparableResDrawable
 */
public abstract class ComparableDrawable extends Drawable {

  /**
   * @param other The other drawable
   * @return {@code true} iff this drawable is equivalent to the {@param other}.
   */
  public abstract boolean isEquivalentTo(ComparableDrawable other);

  /** null safe utility method to check equality of 2 comparable drawables */
  public static boolean isEquivalentTo(ComparableDrawable x, ComparableDrawable y) {
    if (x == null) {
      return y == null;
    } else if (y == null) {
      return true;
    }

    return x.isEquivalentTo(y);
  }
}
