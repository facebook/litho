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

package com.facebook.litho.annotations;

import androidx.annotation.ArrayRes;
import androidx.annotation.AttrRes;
import androidx.annotation.BoolRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.StringRes;

/**
 * This enum's constants are used to mark that {@link Prop} can receive resources of a corresponding
 * Android resource type as values. In addition or instead of usual builder methods this will
 * trigger generation of methods which expect Android resource values or ids as parameter values.
 *
 * <p><b>For example:</b> <br>
 * If a {@link CharSequence} prop named {@code title} is marked as {@link ResType#STRING}, this will
 * generate not only method {@code title(CharSequence title)} in the component builder, but also
 * various methods that enable initializing the prop from a string resource or attribute:
 *
 * <pre><code>
 *   titleRes(@StringRes int resId)
 *   titleRes(@StringRes int resId, Object... formatArgs)
 *   titleAttr(@AttrRes int attrResId)
 *   titleAttr(@AttrRes int attrResId, @StringRes int defResId)
 * </code></pre>
 *
 * And if an {@code int} prop named {@code size} is marked as {@link ResType#DIMEN_SIZE}, this will
 * not generate a {@code size(int size)} method, but will add a bunch of other methods that accept
 * resources:
 *
 * <pre><code>
 *   sizePx(@Px int sizePx)
 *   sizeRes(@DimenRes int resId)
 *   sizeDip(@Dimension(unit = Dimension.DP) float dip)
 *   sizeAttr(@AttrRes int attrResId)
 *   sizeAttr(@AttrRes int attrResId, @DimenRes int defResId)
 * </code></pre>
 *
 * @see Prop
 * @see PropDefault
 * @see ArrayRes
 * @see BoolRes
 * @see IntegerRes
 * @see StringRes
 * @see DimenRes
 * @see Dimension
 * @see DrawableRes
 * @see ColorRes
 * @see ColorInt
 * @see AttrRes
 */
public enum ResType {
  /** Prop is not related to Android resources. */
  NONE,

  /** Prop's value can be set from a {@link StringRes} resource. */
  STRING,

  /**
   * Prop's value can be set from an {@link ArrayRes} resource which contains {@link StringRes}s.
   */
  STRING_ARRAY,

  /** Prop's value can be set from an {@link IntegerRes} resource. */
  INT,

  /**
   * Prop's value can be set from an {@link ArrayRes} resource which contains {@link IntegerRes}s.
   */
  INT_ARRAY,

  /** Prop's value can be set from a {@link BoolRes} resource. */
  BOOL,

  /** Prop's value can be set from a {@link ColorRes} resource or as a {@link ColorInt} value. */
  COLOR,

  /**
   * Prop's value can be set from a {@link DimenRes} resource or as a {@link Dimension#DP} or {@link
   * Dimension#PX} value.
   */
  DIMEN_SIZE,

  /**
   * Prop's value can be set from a {@link DimenRes} resource or as a {@link Dimension#DP}, {@link
   * Dimension#SP} or {@link Dimension#PX} value.
   */
  DIMEN_OFFSET,

  /**
   * Prop's value can be set from a {@link DimenRes} resource or as a {@link Dimension#DP}, {@link
   * Dimension#SP} or {@link Dimension#PX} value.
   */
  DIMEN_TEXT,

  /** Prop's value can be set from a {@link DimenRes} resource. */
  FLOAT,

  /** Prop's value can be set from a {@link DrawableRes} resource. */
  DRAWABLE,
}
