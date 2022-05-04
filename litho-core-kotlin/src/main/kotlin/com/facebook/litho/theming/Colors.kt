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

package libraries.components.litho.theming

import android.graphics.Color
import androidx.annotation.ColorInt

inline fun lightColors(
    primary: Int = PRIMARY_LIGHT,
    secondary: Int = SECONDARY_LIGHT,
): Colors = Colors(primary = primary, secondary = secondary)

inline fun darkColors(
    primary: Int = PRIMARY_DARK,
    secondary: Int = SECONDARY_DARK,
): Colors = Colors(primary = primary, secondary = secondary)

data class Colors(
    @ColorInt val primary: Int,
    @ColorInt val secondary: Int,
)

val PRIMARY_LIGHT = Color.parseColor("#4267b2")
val PRIMARY_DARK = Color.parseColor("#003d82")
val SECONDARY_LIGHT = Color.parseColor("#ffffff")
val SECONDARY_DARK = Color.parseColor("#cccccc")
