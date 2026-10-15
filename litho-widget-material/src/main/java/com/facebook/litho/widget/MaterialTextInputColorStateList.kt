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

package com.facebook.litho.widget

import android.content.res.ColorStateList
import androidx.annotation.ColorInt
import java.util.Arrays

/**
 * A [ColorStateList] for [EditText] properties that provides an `equals(..)` method so that it can
 * be used `MaterialTextInput.textColorStateList(..)`.
 */
class MaterialTextInputColorStateList
private constructor(@param:ColorInt internal val colors: IntArray) :
    ColorStateList(STATES, colors) {

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as MaterialTextInputColorStateList
    return Arrays.equals(colors, that.colors)
  }

  override fun hashCode(): Int {
    return Arrays.hashCode(colors)
  }

  companion object {
    private val STATES: Array<IntArray> = arrayOf(
        intArrayOf(android.R.attr.state_enabled),
        intArrayOf(android.R.attr.state_pressed),
        intArrayOf(-android.R.attr.state_enabled),
    )

    @JvmStatic
    fun create(
        @ColorInt enabled: Int,
        @ColorInt pressed: Int,
        @ColorInt disabled: Int,
    ): MaterialTextInputColorStateList {
      val colors = intArrayOf(enabled, pressed, disabled)
      return MaterialTextInputColorStateList(colors)
    }
  }
}
