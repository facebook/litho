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

package com.facebook.samples.litho.kotlin.widgets.sections

import android.graphics.Color
import android.graphics.Typeface
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.background
import com.facebook.litho.widget.TextAlignment
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class TextAlignmentSection : KComponent() {

  override fun ComponentScope.render(): Component {
    return Card(
        cornerRadius = 12.dp,
        cardBackgroundColor = Color.WHITE,
        elevation = 4.dp,
        style = Style.margin(all = 8.dp),
    ) {
      Column(style = Style.padding(all = 20.dp)) {
        child(
            Text(
                "Text Alignment",
                textSize = 20.sp,
                textColor = Color.parseColor("#1F2937"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 16.dp),
            ),
        )

        child(
            Column(
                style =
                    Style.width(300.dp)
                        .padding(all = 12.dp)
                        .background(RoundedRect(Color.parseColor("#F3F4F6"), 8.dp))
                        .margin(bottom = 12.dp),
            ) {
              child(
                  Text(
                      text = "Left aligned text (default)",
                      textSize = 14.sp,
                      alignment = TextAlignment.TEXT_START,
                  ),
              )
            },
        )

        child(
            Column(
                style =
                    Style.width(300.dp)
                        .padding(all = 12.dp)
                        .background(RoundedRect(Color.parseColor("#F3F4F6"), 8.dp))
                        .margin(bottom = 12.dp),
            ) {
              child(
                  Text(
                      text = "Center aligned text",
                      textSize = 14.sp,
                      alignment = TextAlignment.CENTER,
                  ),
              )
            },
        )

        child(
            Column(
                style =
                    Style.width(300.dp)
                        .padding(all = 12.dp)
                        .background(RoundedRect(Color.parseColor("#F3F4F6"), 8.dp)),
            ) {
              child(
                  Text(
                      text = "Right aligned text",
                      textSize = 14.sp,
                      alignment = TextAlignment.TEXT_END,
                  ),
              )
            },
        )
      }
    }
  }
}
