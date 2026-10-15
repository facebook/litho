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
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

class BasicTextStylingSection : KComponent() {

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
                "Basic Text Styling",
                textSize = 20.sp,
                textColor = Color.parseColor("#1F2937"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 16.dp),
            ),
        )

        child(
            Text(
                text = "Regular text with default styling",
                textSize = 14.sp,
                textColor = Color.parseColor("#374151"),
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Text(
                text = "Bold text for emphasis",
                textSize = 14.sp,
                textColor = Color.parseColor("#374151"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Text(
                text = "Italic text for subtle styling",
                textSize = 14.sp,
                textColor = Color.parseColor("#374151"),
                textStyle = Typeface.ITALIC,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Text(
                text = "Colored text to draw attention",
                textSize = 14.sp,
                textColor = Color.parseColor("#3B82F6"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Text(
                text = "Large text for headings",
                textSize = 18.sp,
                textColor = Color.parseColor("#1F2937"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Text(
                text = "Small text for secondary information",
                textSize = 12.sp,
                textColor = Color.parseColor("#6B7280"),
            ),
        )
      }
    }
  }
}
