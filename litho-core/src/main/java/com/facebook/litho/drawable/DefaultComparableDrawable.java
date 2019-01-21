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

import android.graphics.drawable.Drawable;

/**
 * Default Comparable Drawable uses object equals to check equivalence.
 *
 * <p>Using this is not the recommended as semantically equal drawables will not be considered
 * equivalent because they will have different references, which could make diffing inefficient.
 */
public final class DefaultComparableDrawable extends ComparableDrawableWrapper {

  private DefaultComparableDrawable(Drawable drawable) {
    super(drawable);
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof DefaultComparableDrawable)) {
      return false;
    }

    return getWrappedDrawable().equals(((DefaultComparableDrawable) other).getWrappedDrawable());
  }

  /**
   * Use to create a wrapper for a drawable which does <em>not</em> extend comparable drawable for
   * backwards compatibility. Use this method only when the value of the property is going to be
   * constant (i.e. does not depend on state)
   *
   * @see ComparableIntIdDrawable
   * @see ComparableColorDrawable
   * @see ComparableResDrawable
   * @see ComparableDrawable
   * @deprecated For internal use only. Consider using {@link ComparableIntIdDrawable}. Extend
   *     {@link ComparableDrawable} for custom drawables (see {@link BorderColorDrawable}) or extend
   *     {@link ComparableDrawableWrapper} and implement {@link #isEquivalentTo(ComparableDrawable)}
   *     if drawable cannot extend {@link ComparableDrawable} (see {@link ComparableColorDrawable}
   *     and {@link ComparableResDrawable}).
   */
  @Deprecated
  public static DefaultComparableDrawable create(Drawable drawable) {
    return new DefaultComparableDrawable(drawable);
  }
}
