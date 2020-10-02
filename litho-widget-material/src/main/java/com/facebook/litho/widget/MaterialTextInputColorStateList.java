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

package com.facebook.litho.widget;

import android.content.res.ColorStateList;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import java.util.Arrays;

/**
 * A {@link ColorStateList} for {@link EditText} properties that provides an {@code equals(..)}
 * method so that it can be used {@code MaterialTextInput.textColorStateList(..)}.
 */
public class MaterialTextInputColorStateList extends ColorStateList {

  private static final int[][] STATES =
      new int[][] {
        new int[] {android.R.attr.state_enabled},
        new int[] {android.R.attr.state_pressed},
        new int[] {-android.R.attr.state_enabled},
      };

  int[] colors;

  public static MaterialTextInputColorStateList create(
      @ColorInt int enabled, @ColorInt int pressed, @ColorInt int disabled) {
    @ColorInt int[] colors = new int[] {enabled, pressed, disabled};
    return new MaterialTextInputColorStateList(colors);
  }

  private MaterialTextInputColorStateList(@ColorInt int[] colors) {
    super(STATES, colors);
    this.colors = colors;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaterialTextInputColorStateList that = (MaterialTextInputColorStateList) o;
    return Arrays.equals(colors, that.colors);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(colors);
  }
}
