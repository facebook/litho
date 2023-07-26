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

package com.facebook.samples.litho.kotlin.widgets

import android.graphics.Color
import android.text.InputFilter
import android.text.InputType
import android.widget.ImageView
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.FlexboxContainerScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.HorizontalScroll
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.SolidColor
import com.facebook.litho.kotlin.widget.Spinner
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.TextInput
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.view.background
import com.facebook.litho.widget.TextAlignment
import com.facebook.rendercore.dp
import com.facebook.rendercore.drawableRes
import com.facebook.rendercore.sp
import com.facebook.samples.litho.R
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class WidgetsExamplesComponent : KComponent() {

  override fun ComponentScope.render(): Component {

    val myOptions = listOf("option 1", "option 2", "option 3")

    return Column {
      child(Text("Text Example"))
      child(
          // start_text_widget_example
          Text(
              text = "This is my example text",
              textSize = 12.sp,
              textColor = Color.RED,
              alignment = TextAlignment.CENTER)
          // end_text_widget_example
          )
      child(Text("TextInput Example"))
      child(
          // start_textinput_widget_example
          TextInput(
              initialText = "Initial text",
              multiline = true,
              inputType = InputType.TYPE_CLASS_TEXT,
              inputFilter = InputFilter.LengthFilter(/* maxLength = */ 10))
          // end_textinput_widget_example
          )
      child(Text("Image Example"))
      child(
          // start_image_example
          Image(
              drawable = drawableRes(R.drawable.ic_launcher),
              scaleType = ImageView.ScaleType.CENTER_CROP)
          // end_image_example
          )
      child(Text("Card Example"))
      child(
          // start_card_example
          Card(cornerRadius = 5.dp, clippingColor = Color.RED, child = { Text("my content") })
          // end_card_example
          )
      child(Text("SolidColor Example"))
      child(
          // start_solidcolor_example
          SolidColor(color = Color.RED)
          // end_solidcolor_example
          )
      child(Text("Progress Example"))
      child(
          // start_progress_example
          Progress(indeterminateDrawable = drawableRes(R.drawable.ic_launcher))
          // end_progress_example
          )
      child(Text("Spinner Example"))
      child(
          // start_spinner_example
          Spinner(options = myOptions, selectedOption = myOptions.get(0), onItemSelected = {})
          // end_spinner_example
          )
      child(Text("HorizontalScroll Example"))
      child(
          // start_horizontalscroll_example
          HorizontalScroll { Row { getComponents(this) } }
          // end_horizontalscroll_example
          )
      child(Text("VerticalScroll Example"))
      child(
          // start_verticalscroll_example
          VerticalScroll(style = Style.height(100.dp)) { Column { getComponents(this) } }
          // end_verticalscroll_example
          )
    }
  }

  private fun ResourcesScope.getComponents(flexboxContainerScope: FlexboxContainerScope) {
    for (i in 1..10) {
      flexboxContainerScope.child(
          Column(
              style =
                  Style.width(50.dp)
                      .height(50.dp)
                      .margin(all = 5.dp)
                      .background(RoundedRect(if (i % 2 == 0) 0xff666699 else 0xffd9d9d9, 8.dp))))
    }
  }
}
