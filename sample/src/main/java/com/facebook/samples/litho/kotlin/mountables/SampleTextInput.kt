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

package com.facebook.samples.litho.kotlin.mountables

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.utils.MeasureUtils
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult

/**
 * This class is an oversimplified version of a TextInput MountableComponent. It was created for the
 * purpose of showcasing measure() function implemented using creating a View and measuring it.
 */
class SampleTextInput(
    private val initialText: CharSequence = "",
    private val hint: CharSequence? = "",
    private val inputBackground: Drawable = ColorDrawable(Color.TRANSPARENT),
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(
        SampleTextInputMountable(initialText, hint, inputBackground), style)
  }
}

internal class SampleTextInputMountable(
    private val initialText: CharSequence,
    private val hint: CharSequence?,
    private val inputBackground: Drawable,
) : SimpleMountable<EditText>(RenderType.VIEW) {

  override fun createContent(context: Context): EditText {
    return EditText(context)
  }

  // start_measure_with_view_measurement
  override fun measure(
      context: LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult {
    // The height should be the measured height of EditText with relevant params
    val editTextForMeasure: EditText = AppCompatEditText(context.androidContext)

    editTextForMeasure.hint = hint
    editTextForMeasure.background =
        getBackgroundOrDefault(
            context.androidContext,
            if (inputBackground === ColorDrawable(Color.TRANSPARENT)) editTextForMeasure.background
            else inputBackground)
    editTextForMeasure.setText(initialText)

    editTextForMeasure.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec))

    val size = Size()
    size.height = editTextForMeasure.measuredHeight

    // For width we always take all available space, or collapse to 0 if unspecified.
    if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
      size.width = 0
    } else {
      size.width = Math.min(SizeSpec.getSize(widthSpec), editTextForMeasure.measuredWidth)
    }
    return MeasureResult(size.width, size.height, null)
  }
  // end_measure_with_view_measurement

  override fun mount(c: Context, content: EditText, layoutData: Any?) {
    content.hint = hint
    content.background = getBackgroundOrDefault(c, inputBackground)
    content.setText(initialText)
  }

  override fun unmount(c: Context, content: EditText, layoutData: Any?) {
    content.hint = ""
    content.background = ColorDrawable(Color.TRANSPARENT)
    content.setText("")
  }

  override fun shouldUpdate(
      newMountable: SimpleMountable<EditText>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    newMountable as SampleTextInputMountable
    return newMountable.initialText != initialText ||
        newMountable.hint != hint ||
        newMountable.inputBackground != inputBackground
  }

  // start_preallocation_example
  override fun canPreallocate(): Boolean = true

  override fun poolSize(): Int {
    return 10
  }
  // end_preallocation_example
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
