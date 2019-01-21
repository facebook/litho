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
 * A comparable implementation for drawables where an explicit int identifier is compared to test
 * for equivalence in {@link #isEquivalentTo(ComparableDrawable)}. Use this implementation for
 * trivial cases where the number of drawables are discrete and finite. An example:
 *
 * <pre><code>
 *  onCreateLayout(ComponentContext c, @State Day day) {
 *
 *    Drawable drawable = getDrawableForDay(day);
 *    int id = day.ordinal(); // Day in an enum
 *
 *    return Text.create(c)
 *      .text(day)
 *      .background(
 *        ComparableIntIdDrawable.create(drawable, id)
 *      )
 *      .build();
 *  }
 * </code></pre>
 */
public class ComparableIntIdDrawable extends ComparableDrawableWrapper {

  public final int mId;

  protected ComparableIntIdDrawable(Drawable drawable, int id) {
    super(drawable);
    this.mId = id;
  }

  @Override
  public int hashCode() {
    return mId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ComparableIntIdDrawable)) {
      return false;
    }

    return mId == ((ComparableIntIdDrawable) o).mId;
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof ComparableIntIdDrawable)) {
      return false;
    }

    return mId == ((ComparableIntIdDrawable) other).mId;
  }

  /**
   * Utility method to create a new comparable id drawable.
   *
   * @param drawable the drawable to wrap
   * @param id the id of the drawable
   * @return a comparable id drawable
   * @throws IllegalArgumentException if the {@param drawable} is a {@link ComparableDrawable}
   */
  public static ComparableIntIdDrawable create(Drawable drawable, int id) {
    if (drawable == null) {
      throw new IllegalArgumentException("drawable must not be null");
    }
    if (drawable instanceof ComparableDrawable) {
      throw new IllegalArgumentException("drawable is already a ComparableDrawable");
    }
    return new ComparableIntIdDrawable(drawable, id);
  }
}
