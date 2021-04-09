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

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Typeface.DEFAULT
import android.graphics.Typeface.NORMAL
import android.text.TextUtils
import androidx.annotation.ColorInt
import com.facebook.litho.ComponentScope
import com.facebook.litho.Sp
import com.facebook.litho.Style
import com.facebook.litho.sp

/**
 * Temporary builder function for creating [TextSpec] components. In the future it will either be
 * auto-generated or modified to have the final set of parameters.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ComponentScope.Text(
    text: CharSequence?,
    style: Style? = null,
    @ColorInt textColor: Int = Color.BLACK,
    textSize: Sp = 14.sp,
    textStyle: Int = NORMAL,
    typeface: Typeface = DEFAULT,
    alignment: TextAlignment = TextAlignment.TEXT_START,
    isSingleLine: Boolean = false,
    ellipsize: TextUtils.TruncateAt? = null,
    minLines: Int = 0,
    maxLines: Int = Int.MAX_VALUE
): Text =
    Text.create(context)
        .text(text)
        .textColor(textColor)
        .textSizeSp(textSize.value)
        .textStyle(textStyle)
        .typeface(typeface)
        .alignment(alignment)
        .isSingleLine(isSingleLine)
        .minLines(minLines)
        .maxLines(maxLines)
        .apply { ellipsize?.let { ellipsize(it) } }
        .build()
        .apply { applyStyle(style) }
