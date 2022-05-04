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

@file:Suppress("KtDataClass")

package libraries.components.litho.theming

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.ColorInt
import com.facebook.litho.Dimen
import com.facebook.litho.sp

data class Typography(
    val h1: TextStyle = TextStyle(fontSize = 48.sp, typeface = Typeface.DEFAULT_BOLD),
    val h2: TextStyle = TextStyle(fontSize = 30.sp, typeface = Typeface.DEFAULT_BOLD),
    val subtitle1: TextStyle =
        TextStyle(
            fontColor = Color.GRAY,
            fontSize = 16.sp,
        ),
    val body1: TextStyle =
        TextStyle(
            fontColor = Color.GRAY,
            fontSize = 14.sp,
        ),
)

data class TextStyle(
    @ColorInt val fontColor: Int = Color.BLACK,
    val fontSize: Dimen = 18.sp,
    val typeface: Typeface = Typeface.DEFAULT,
)
