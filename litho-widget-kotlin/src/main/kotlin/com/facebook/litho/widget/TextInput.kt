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

package com.facebook.litho.widget

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.ColorInt
import com.facebook.litho.DslScope
import com.facebook.litho.Sp
import com.facebook.litho.eventHandler
import com.facebook.litho.sp

/**
 * Builder function for creating [TextInputSpec] components.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun DslScope.TextInput(
    initialText: CharSequence = "",
    hint: CharSequence = "",
    @ColorInt textColor: Int = Color.BLACK,
    @ColorInt hintTextColor: Int = Color.LTGRAY,
    textSize: Sp = 14.sp,
    typeface: Typeface = Typeface.DEFAULT,
    textAlignment: Int = View.TEXT_ALIGNMENT_GRAVITY,
    gravity: Int = Gravity.CENTER_VERTICAL or Gravity.START,
    editable: Boolean = true,
    multiline: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    inputType: Int = EditorInfo.TYPE_CLASS_TEXT,
    imeOptions: Int = EditorInfo.IME_NULL,
    noinline onTextChanged: ((TextChangedEvent) -> Unit)? = null,
    noinline onSelectionChanged: ((SelectionChangedEvent) -> Unit)? = null
): TextInput =
    TextInput.create(context)
        .initialText(initialText)
        .hint(hint)
        .textColorStateList(ColorStateList.valueOf(textColor))
        .hintColorStateList(ColorStateList.valueOf(hintTextColor))
        .textSizeSp(textSize.value)
        .typeface(typeface)
        .textAlignment(textAlignment)
        .gravity(gravity)
        .editable(editable)
        .multiline(multiline)
        .minLines(minLines)
        .maxLines(maxLines)
        .inputType(inputType)
        .imeOptions(imeOptions)
        .apply {
          onTextChanged?.let { textChangedEventHandler(eventHandler(it)) }
          onSelectionChanged?.let { selectionChangedEventHandler(eventHandler(it)) }
        }
        .build()
