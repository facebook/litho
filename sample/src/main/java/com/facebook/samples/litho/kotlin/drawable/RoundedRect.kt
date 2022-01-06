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

package com.facebook.samples.litho.kotlin.drawable

import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.facebook.litho.Dimen
import com.facebook.litho.ResourcesScope

private fun RoundedRect(@ColorInt fillColor: Int, @Px cornerRadiusPx: Float): Drawable =
    ShapeDrawable(RoundRectShape(FloatArray(8) { cornerRadiusPx }, null, null)).apply {
      paint.color = fillColor
    }

/** Create a Rectangle drawable with rounded corners */
fun ResourcesScope.RoundedRect(@ColorInt fillColor: Int, cornerRadius: Dimen): Drawable =
    RoundedRect(fillColor, cornerRadius.toPixels(resourceResolver).toFloat())

/**
 * Create a Rectangle drawable with rounded corners. Kotlin resolves numbers >= 0x80000000 as a
 * Long. This is a convenience function that accepts [fillColor] as a Long.
 */
fun ResourcesScope.RoundedRect(@ColorInt fillColor: Long, cornerRadius: Dimen): Drawable =
    RoundedRect(fillColor.toInt(), cornerRadius)
