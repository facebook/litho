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

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.core.minHeight
import com.facebook.litho.core.padding
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.drawableAttr
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * A simple spinner (dropdown) component. Derived from the standard Android [android.widget.Spinner]
 *
 * Additionally added logic to flip the caret vertically once menu is shown.
 *
 * If no optional values are provided the component will look like it's material design counterpart.
 *
 * @param options The options available from the dropdown
 * @param selectedOption The initially selected option for the spinner
 * @param onItemSelected The listener for dropdown selections
 * @param style The style for the spinner component
 * @param selectedTextColor The text color of the selected value
 * @param selectedTextSize The text size of the selected value
 * @param caret The spinner caret icon i.e. arrow at the far right. Notice that this drawable will
 *   be flipped vertically when the dropdown menu is shown
 * @param itemLayout The item layout for the drop down list
 *   android.R.layout.simple_dropdown_item_1line is used by default
 */
class Spinner
@JvmOverloads
constructor(
    private val options: List<String>,
    private val selectedOption: String,
    private val onItemSelected: (String) -> Unit,
    private val style: Style = Style,
    @ColorInt private val selectedTextColor: Int = -0x22000000, // 87% Black
    private val selectedTextSize: Float = -1f,
    private val caret: Drawable? = null,
    @LayoutRes private val itemLayout: Int = android.R.layout.simple_dropdown_item_1line,
) : KComponent() {
  override fun ComponentScope.render(): Component {
    val selection = useState { selectedOption }
    val isShowingDropDown = useState { false }

    val caretDrawableToUse =
        useCached(caret) { caret ?: CaretDrawable(context.androidContext, DEFAULT_CARET_COLOR) }
    val selectedTextSize =
        useCached(selectedTextSize) {
          if (selectedTextSize != -1f) {
            selectedTextSize
          } else {
            DEFAULT_TEXT_SIZE_SP.sp.toPixels()
          }
        }

    val backgroundDrawable =
        useCached(android.R.attr.selectableItemBackground) {
          drawableAttr(android.R.attr.selectableItemBackground)
        }

    return Row(
        justifyContent = YogaJustify.SPACE_BETWEEN,
        style =
            style
                .minHeight(SPINNER_HEIGHT.dp)
                .padding(start = MARGIN_SMALL.dp)
                .background(backgroundDrawable)
                .accessibilityRole(AccessibilityRole.DROP_DOWN_LIST)
                .onClick { event ->
                  val popup = ListPopupWindow(context.androidContext)
                  popup.anchorView = event.view
                  popup.isModal = true
                  popup.promptPosition = ListPopupWindow.POSITION_PROMPT_ABOVE
                  popup.setAdapter(ArrayAdapter(context.androidContext, itemLayout, options))
                  popup.setOnItemClickListener { _, _, position, _ ->
                    val newSelection = options[position]
                    onItemSelected(newSelection)
                    popup.dismiss()
                    selection.update(newSelection)
                  }
                  popup.setOnDismissListener { isShowingDropDown.update(false) }
                  popup.show()
                  isShowingDropDown.update(true)
                }) {
          child(
              createSelectedItemText(
                  context, selection.value, selectedTextSize.toInt(), selectedTextColor))
          child(createCaret(context, caretDrawableToUse, isShowingDropDown.value))
        }
  }

  private fun createCaret(
      c: ComponentContext,
      icon: Drawable,
      isShowingDropDown: Boolean
  ): Component {
    return Image.create(c)
        .drawable(icon)
        .widthDip(SPINNER_HEIGHT.toFloat())
        .heightDip(SPINNER_HEIGHT.toFloat())
        .flexShrink(0f)
        .flexGrow(0f)
        .scale((if (isShowingDropDown) -1 else 1).toFloat())
        .build()
  }

  private fun createSelectedItemText(
      c: ComponentContext,
      selection: String,
      textSizePx: Int,
      @ColorInt textColor: Int
  ): Component {
    return Text.create(c)
        .text(selection)
        .alignSelf(YogaAlign.CENTER)
        .textSizePx(textSizePx)
        .textColor(textColor)
        .build()
  }

  companion object {
    private const val MARGIN_SMALL = 8f
    private const val DEFAULT_CARET_COLOR = -0x76000000 // 54% Black
    private const val DEFAULT_TEXT_SIZE_SP = 16
    private const val SPINNER_HEIGHT = 48
  }
}

/** Draws a simple triangle caret depicting if the Spinner is expanded or collapsed. */
private class CaretDrawable(context: Context, @ColorInt caretColor: Int) : Drawable() {
  private val paint = Paint()
  private val width: Int
  private val height: Int

  // Triangle geometry
  private val trianglePath = Path()
  private val p1 = Point()
  private val p2 = Point()
  private val p3 = Point()

  init {
    paint.color = caretColor
    paint.flags = Paint.ANTI_ALIAS_FLAG
    width = dpToPx(context, CARET_WIDTH_DP).toInt()
    height = dpToPx(context, CARET_HEIGHT_DP).toInt()
  }

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    val cx = bounds.centerX()
    val cy = bounds.centerY()

    // Setup points
    p1[cx - width] = cy - height
    p2[cx + width] = cy - height
    p3[cx] = cy + height

    // Setup triangle
    trianglePath.reset()
    trianglePath.fillType = Path.FillType.EVEN_ODD
    trianglePath.moveTo(p1.x.toFloat(), p1.y.toFloat())
    trianglePath.lineTo(p2.x.toFloat(), p2.y.toFloat())
    trianglePath.lineTo(p3.x.toFloat(), p3.y.toFloat())
    trianglePath.close()
  }

  override fun draw(canvas: Canvas) {
    canvas.drawPath(trianglePath, paint)
  }

  override fun setAlpha(alpha: Int) {
    throw RuntimeException("Not supported")
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    throw RuntimeException("Not supported")
  }

  override fun getOpacity(): Int {
    return PixelFormat.OPAQUE
  }

  private fun dpToPx(context: Context, dpValue: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dpValue.toFloat(), context.resources.displayMetrics)
  }

  companion object {
    private const val CARET_WIDTH_DP = 5
    private const val CARET_HEIGHT_DP = 3
  }
}
