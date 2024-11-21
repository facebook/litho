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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.annotation.ColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Wrapper
import com.facebook.litho.useCached

/**
 * A Component that can wrap another component to add touch feedback via a RippleDrawable
 * background.
 */
class TouchableFeedback(
    private val content: Component,
    @ColorInt private val color: Int = Color.WHITE,
    @ColorInt private val highlightColor: Int = Color.LTGRAY,
) : KComponent() {
  override fun ComponentScope.render(): Component {
    val drawable = useCached(color, highlightColor) { getRippleDrawable(color, highlightColor) }
    return Wrapper.create(context).delegate(content).background(drawable).build()
  }

  private fun getRippleDrawable(normalColor: Int, pressedColor: Int): Drawable {
    return RippleDrawable(
        ColorStateList.valueOf(pressedColor),
        ColorDrawable(normalColor),
        getRippleMask(pressedColor))
  }

  private fun getRippleMask(color: Int): Drawable {
    val shapeDrawable = ShapeDrawable(RectShape())
    shapeDrawable.paint.color = color
    return shapeDrawable
  }
}
