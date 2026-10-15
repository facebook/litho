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
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.HorizontalDivider
import com.facebook.litho.kotlin.widget.HorizontalScroll
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.TextInput
import com.facebook.litho.kotlin.widget.VerticalDivider
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.FlexboxScope
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.background
import com.facebook.litho.widget.SolidColor
import com.facebook.litho.widget.Spinner
import com.facebook.litho.widget.TextAlignment
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp
import com.facebook.rendercore.drawableRes
import com.facebook.rendercore.sp
import com.facebook.samples.litho.R
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

class WidgetsExamplesComponent : KComponent() {

  override fun ComponentScope.render(): Component {

    val myOptions = listOf("option 1", "option 2", "option 3")

    return LazyList(style = Style.padding(all = 12.dp)) {
      child(
          id = "text_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(120.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "Text Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_text_widget_example
                      Text(
                          text = "This is my example text",
                          textSize = 12.sp,
                          textColor = Color.RED,
                          alignment = TextAlignment.CENTER,
                      ),
                      // end_text_widget_example
                  )
                }
              },
      )

      // TextInput Widget
      child(
          id = "textinput_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(150.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "TextInput Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_textinput_widget_example
                      TextInput(
                          initialText = "Initial text",
                          multiline = true,
                          inputType = InputType.TYPE_CLASS_TEXT,
                          inputFilter = InputFilter.LengthFilter(/* maxLength= */ 10),
                          style = Style.width(200.dp),
                      ),
                      // end_textinput_widget_example
                  )
                }
              },
      )

      // Image Widget
      child(
          id = "image_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(180.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "Image Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_image_example
                      Image(
                          drawable = drawableRes(R.drawable.ic_launcher),
                          scaleType = ImageView.ScaleType.CENTER_CROP,
                          style = Style.height(100.dp).width(100.dp),
                      ),
                      // end_image_example
                  )
                }
              },
      )

      // Card Widget
      child(
          id = "card_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(150.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "Card Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_card_example
                      Card(
                          cardBackgroundColor = Color.YELLOW,
                          cornerRadius = 5.dp,
                          clippingColor = Color.RED,
                          style = Style.width(120.dp).height(60.dp),
                          child = {
                            Column(
                                style = Style.height(60.dp),
                                alignItems = YogaAlign.CENTER,
                                justifyContent = YogaJustify.CENTER,
                            ) {
                              child(Text("my content"))
                            }
                          },
                      ),
                      // end_card_example
                  )
                }
              },
      )

      // SolidColor Widget
      child(
          id = "solidcolor_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(120.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "SolidColor Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_solidcolor_example
                      SolidColor(
                          color = Color.RED,
                          style = Style.height(40.dp).width(120.dp),
                          // end_solidcolor_example
                      ),
                  )
                }
              },
      )

      // Progress Widget
      child(
          id = "progress_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(120.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "Progress Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_progress_example
                      Progress(style = Style.height(40.dp).width(40.dp)),
                      // end_progress_example
                  )
                }
              },
      )

      // Spinner Widget
      child(
          id = "spinner_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(120.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "Spinner Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_spinner_example
                      Spinner(
                          options = myOptions,
                          selectedOption = myOptions.get(0),
                          onItemSelected = {},
                          style = Style.width(200.dp),
                      ),
                      // end_spinner_example
                  )
                }
              },
      )

      // HorizontalDivider Widget
      child(
          id = "divider_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(180.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "HorizontalDivider Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      Column(style = Style.width(250.dp)) {
                        child(Text("Section 1", textSize = 14.sp))
                        child(HorizontalDivider(style = Style.margin(vertical = 8.dp)))
                        child(Text("Section 2", textSize = 14.sp))
                        child(
                            // start_divider_example
                            HorizontalDivider(
                                color = 0xFF0000FF.toInt(),
                                thickness = 2.dp,
                                style = Style.margin(vertical = 8.dp),
                            ),
                            // end_divider_example
                        )
                        child(Text("Section 3", textSize = 14.sp))
                      },
                  )
                }
              },
      )

      // VerticalDivider Widget
      child(
          id = "vertical_divider_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(180.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "VerticalDivider Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      Row(style = Style.height(100.dp)) {
                        child(Text("Column 1", textSize = 14.sp))
                        child(VerticalDivider(style = Style.margin(horizontal = 8.dp)))

                        child(Text("Column 2", textSize = 14.sp))
                        child(
                            // start_vertical_divider_example
                            VerticalDivider(
                                color = 0xFF00FF00.toInt(),
                                thickness = 2.dp,
                                style = Style.margin(horizontal = 8.dp),
                            ),
                            // end_vertical_divider_example
                        )
                        child(Text("Column 3", textSize = 14.sp))
                      },
                  )
                }
              },
      )

      // HorizontalScroll Widget
      child(
          id = "horizontalscroll_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(140.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "HorizontalScroll Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_horizontalscroll_example
                      HorizontalScroll(style = Style.width(250.dp)) { Row { getComponents(this) } },
                      // end_horizontalscroll_example
                  )
                }
              },
      )

      // VerticalScroll Widget
      child(
          id = "verticalscroll_card",
          component =
              Card(
                  cornerRadius = 12.dp,
                  cardBackgroundColor = Color.parseColor("#F8F9FA"),
                  elevation = 4.dp,
                  style = Style.margin(all = 8.dp),
              ) {
                Column(
                    style = Style.padding(all = 20.dp).height(200.dp),
                    alignItems = YogaAlign.CENTER,
                    justifyContent = YogaJustify.SPACE_BETWEEN,
                ) {
                  child(
                      Text(
                          "VerticalScroll Example",
                          textSize = 18.sp,
                          textColor = Color.parseColor("#1F2937"),
                      ),
                  )
                  child(
                      // start_verticalscroll_example
                      VerticalScroll(style = Style.height(120.dp).width(250.dp)) {
                        Column { getComponents(this) }
                      },
                      // end_verticalscroll_example
                  )
                }
              },
      )
    }
  }

  private fun ResourcesScope.getComponents(flexboxContainerScope: FlexboxScope) {
    for (i in 1..10) {
      flexboxContainerScope.child(
          Column(
              style =
                  Style.width(50.dp)
                      .height(50.dp)
                      .margin(all = 5.dp)
                      .background(RoundedRect(if (i % 2 == 0) 0xff666699 else 0xffd9d9d9, 8.dp)),
          ),
      )
    }
  }
}
