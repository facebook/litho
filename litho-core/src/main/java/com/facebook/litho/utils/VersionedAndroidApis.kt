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

package com.facebook.litho.utils

import android.icu.text.BreakIterator
import android.os.Build
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.DoNotInline
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi

object VersionedAndroidApis {

  @RequiresApi(Build.VERSION_CODES.P)
  object P {
    @DoNotInline
    fun resetPivot(view: View) {
      view.resetPivot()
    }

    @DoNotInline
    fun setAmbientShadowColor(view: View, @ColorInt ambientShadowColor: Int) {
      view.outlineAmbientShadowColor = ambientShadowColor
    }

    @DoNotInline
    fun setSpotShadowColor(view: View, @ColorInt spotShadowColor: Int) {
      view.outlineSpotShadowColor = spotShadowColor
    }
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  object Q {
    @DoNotInline
    @JvmStatic
    fun setTextCursorDrawable(editText: EditText, @DrawableRes cursorDrawableRes: Int) {
      editText.setTextCursorDrawable(cursorDrawableRes)
    }

    @DoNotInline
    @JvmStatic
    fun breakIteratorGetPreceding(text: CharSequence?, ellipsisOffset: Int): Int {
      val iterator = BreakIterator.getCharacterInstance()
      iterator.setText(text)
      return iterator.preceding(ellipsisOffset)
    }
  }
}
