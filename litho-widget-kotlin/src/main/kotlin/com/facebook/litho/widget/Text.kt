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
import android.graphics.Typeface.NORMAL
import androidx.annotation.ColorInt
import com.facebook.litho.DslScope
import com.facebook.litho.Sp
import com.facebook.litho.sp

/**
 * Temporary builder function for creating [TextSpec] components. In the future it will either be
 * auto-generated or modified to have the final set of parameters.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun DslScope.Text(
    text: CharSequence,
    textSize: Sp = 14.sp,
    @ColorInt textColor: Int = Color.BLACK,
    textStyle: Int = NORMAL
): Text =
    Text.create(context)
        .text(text)
        .textSizeSp(textSize.value)
        .textColor(textColor)
        .textStyle(textStyle)
        .build()
