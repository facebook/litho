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

/** ComparableDrawable */
public interface ComparableDrawable<D extends Drawable> {

  /** @return the drawable */
  D acquire(Context context);

  /** Release the drawable. Once released the drawable should not be used in any way. */
  void release(Context context);

  /**
   * @param other The other drawable
   * @return {@code true} iff this drawable is equivalent to the {@param other}.
   */
  boolean isEquivalentTo(ComparableDrawable other);
}
