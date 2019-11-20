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
import androidx.annotation.Nullable;

public class DrawableUtils {

  /** null safe utility method to check equality of 2 comparable drawables */
  public static boolean isEquivalentTo(@Nullable Drawable x, @Nullable Drawable y) {
    if (x == null) {
      return y == null;
    } else if (y == null) {
      return false;
    }

    if (x instanceof ComparableDrawable && y instanceof ComparableDrawable) {
      return ((ComparableDrawable) x).isEquivalentTo((ComparableDrawable) y);
    }

    return x.equals(y);
  }
}
