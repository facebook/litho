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

package com.facebook.samples.litho.kotlin.primitives

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import kotlin.math.max
import kotlin.math.min

/**
 * This class is an oversimplified version of a TextInput Component. It was created for the purpose
 * of showcasing layout() function implemented using creating a View and measuring it.
 */
class SampleTextInput(
    private val initialText: CharSequence = "",
    private val hint: CharSequence? = "",
    private val inputBackground: Drawable = ColorDrawable(Color.TRANSPARENT),
    private val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = SampleTextInputLayoutBehavior(initialText, hint, inputBackground),
        mountBehavior =
            MountBehavior(
                // start_preallocation_example
                ViewAllocator(canPreallocate = true, poolSize = 10) { context -> EditText(context) }
                // end_preallocation_example
                ) {
                  hint.bindTo(EditText::setHint, null)
                  initialText.bindTo(EditText::setText, null)
                  bind(inputBackground) { content ->
                    content.background = getBackgroundOrDefault(androidContext, inputBackground)
                    onUnbind { content.background = null }
                  }
                },
        style = style)
  }
}

internal class SampleTextInputLayoutBehavior(
    private val initialText: CharSequence,
    private val hintText: CharSequence?,
    private val inputBackground: Drawable
) : LayoutBehavior {
  // start_layout_with_view_measurement
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    // The height should be the measured height of EditText with relevant params
    val editTextForMeasure: EditText =
        AppCompatEditText(androidContext).apply {
          setHint(hintText)
          setText(initialText)
          background =
              getBackgroundOrDefault(
                  androidContext,
                  if (inputBackground === ColorDrawable(Color.TRANSPARENT)) background
                  else inputBackground)
        }

    editTextForMeasure.measure(sizeConstraints.toWidthSpec(), sizeConstraints.toHeightSpec())
    val measuredWidth = max(sizeConstraints.minWidth, editTextForMeasure.measuredWidth)
    val measuredHeight = max(sizeConstraints.minHeight, editTextForMeasure.measuredHeight)

    // For width we always take all available space, or collapse to 0 if unspecified.
    val width =
        if (!sizeConstraints.hasBoundedWidth) 0 else min(sizeConstraints.maxWidth, measuredWidth)

    return PrimitiveLayoutResult(width, measuredHeight)
  }
  // end_layout_with_view_measurement
}

private fun getBackgroundOrDefault(context: Context, specifiedBackground: Drawable): Drawable? {
  if (specifiedBackground === ColorDrawable(Color.TRANSPARENT)) {
    val attrs = intArrayOf(android.R.attr.background)
    val a = context.obtainStyledAttributes(null, attrs, android.R.attr.editTextStyle, 0)
    val defaultBackground = a.getDrawable(0)
    a.recycle()
    return defaultBackground
  }
  return specifiedBackground
}
