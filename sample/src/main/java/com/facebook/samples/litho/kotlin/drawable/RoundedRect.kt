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

package com.facebook.samples.litho.kotlin.drawable

import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.TypedValue
import com.facebook.litho.ComponentContext
import java.util.Arrays

object RoundedRect {
  /** Helper function creating rounded rectanlge shape */
  fun build(c: ComponentContext, color: Int, cornerRadiusDp: Int): Drawable {
    val cornerRadiusPx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, cornerRadiusDp.toFloat(), c.resources.displayMetrics)

    val radii = FloatArray(8)
    Arrays.fill(radii, cornerRadiusPx)
    val roundedRectShape = RoundRectShape(radii, null, radii)

    return ShapeDrawable(roundedRectShape).also { it.paint.color = color }
  }
}
