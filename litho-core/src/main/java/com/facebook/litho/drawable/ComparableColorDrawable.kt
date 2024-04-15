/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.drawable

import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt

/** A comparable color drawable. */
class ComparableColorDrawable private constructor(@ColorInt color: Int) :
    ColorDrawable(color), ComparableDrawable {

  override fun isEquivalentTo(other: ComparableDrawable): Boolean {
    if (this === other) {
      return true
    }
    return if (other !is ComparableColorDrawable) {
      false
    } else color == other.color
  }

  companion object {

    @JvmStatic
    fun create(@ColorInt color: Int): ComparableColorDrawable = ComparableColorDrawable(color)
  }
}
