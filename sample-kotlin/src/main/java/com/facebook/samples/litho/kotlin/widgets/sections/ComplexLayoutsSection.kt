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
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.useCached
import com.facebook.litho.view.Shape
import com.facebook.litho.view.background
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect
import com.facebook.yoga.YogaAlign

class ComplexLayoutsSection : KComponent() {

  override fun ComponentScope.render(): Component {
    val whiteBg = useCached { RoundedRect(Color.WHITE, 8.dp) }
    val blueBg = useCached { RoundedRect("#EFF6FF".toColorInt(), 8.dp) }
    val greenBg = useCached { RoundedRect("#F0FDF4".toColorInt(), 8.dp) }
    return Card(
        cornerRadius = 12.dp,
        cardBackgroundColor = Color.WHITE,
        elevation = 4.dp,
        style = Style.margin(all = 8.dp),
    ) {
      Column(style = Style.padding(all = 20.dp)) {
        child(
            Text(
                "Complex Text Layouts",
                textSize = 20.sp,
                textColor = Color.parseColor("#1F2937"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 16.dp),
            ),
        )

        child(
            Text(
                "Example: Social Media Post (with flex)",
                textSize = 14.sp,
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 12.dp),
            ),
        )

        child(
            Column(
                style =
                    Style.width(320.dp)
                        .padding(all = 16.dp)
                        .background(whiteBg)
                        .margin(bottom = 16.dp),
            ) {
              // Header with avatar and name
              child(
                  Row(
                      style = Style.margin(bottom = 12.dp),
                      alignItems = YogaAlign.CENTER,
                  ) {
                    // Avatar - using flex(shrink = 0f) to prevent shrinking
                    child(
                        Column(
                            style =
                                Style.width(48.dp)
                                    .height(48.dp)
                                    .backgroundColor("#10B981".toColorInt())
                                    .flex(shrink = 0f)
                                    .margin(end = 12.dp)
                                    .clip(Shape.Circle),
                        ),
                    )

                    // Name and subtitle - using flex to fill remaining space
                    child(
                        Column(style = Style.flex(grow = 1f)) {
                          child(
                              Text(
                                  "Design Guild",
                                  textSize = 16.sp,
                                  textStyle = Typeface.BOLD,
                                  textColor = Color.parseColor("#1F2937"),
                                  style = Style.margin(bottom = 2.dp),
                              ),
                          )
                          child(
                              Text(
                                  "Alex Rivera",
                                  textSize = 14.sp,
                                  textColor = Color.parseColor("#6B7280"),
                              ),
                          )
                        },
                    )
                  },
              )

              // Post content - NO fixed height to prevent clipping
              child(
                  Text(
                      "Check out the new mockups I've uploaded. Would love to get your feedback on the color scheme and overall layout.",
                      textSize = 14.sp,
                      textColor = Color.parseColor("#374151"),
                  ),
              )
            },
        )

        child(
            Text(
                "Notice: Avatar uses flex(shrink=0f) to stay fixed, name uses flex(grow=1f) to fill space!",
                textSize = 12.sp,
                textColor = Color.parseColor("#059669"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 24.dp),
            ),
        )

        // Two posts side by side with flex
        child(
            Text(
                "Two Posts Side-by-Side (using flex)",
                textSize = 14.sp,
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 12.dp),
            ),
        )

        child(
            Row(style = Style.width(320.dp).margin(bottom = 16.dp)) {
              // First post - using flex to take half the space
              child(
                  Column(
                      style =
                          Style.flex(grow = 1f)
                              .padding(all = 12.dp)
                              .background(blueBg)
                              .margin(end = 8.dp),
                  ) {
                    child(
                        Row(
                            style = Style.margin(bottom = 8.dp),
                            alignItems = YogaAlign.CENTER,
                        ) {
                          child(
                              Column(
                                  style =
                                      Style.width(32.dp)
                                          .height(32.dp)
                                          .backgroundColor("#3B82F6".toColorInt())
                                          .flex(shrink = 0f)
                                          .margin(end = 8.dp)
                                          .clip(Shape.Circle),
                              ),
                          )
                          child(
                              Column(style = Style.flex(grow = 1f)) {
                                child(
                                    Text(
                                        "Team A",
                                        textSize = 14.sp,
                                        textStyle = Typeface.BOLD,
                                        textColor = Color.parseColor("#1F2937"),
                                    ),
                                )
                              },
                          )
                        },
                    )
                    child(
                        Text(
                            "Working on the new feature implementation. Making good progress!",
                            textSize = 12.sp,
                            textColor = Color.parseColor("#374151"),
                        ),
                    )
                  },
              )

              // Second post - using flex to take half the space
              child(
                  Column(
                      style = Style.flex(grow = 1f).padding(all = 12.dp).background(greenBg),
                  ) {
                    child(
                        Row(
                            style = Style.margin(bottom = 8.dp),
                            alignItems = YogaAlign.CENTER,
                        ) {
                          child(
                              Column(
                                  style =
                                      Style.width(32.dp)
                                          .height(32.dp)
                                          .backgroundColor("#10B981".toColorInt())
                                          .flex(shrink = 0f)
                                          .margin(end = 8.dp)
                                          .clip(Shape.Circle),
                              ),
                          )
                          child(
                              Column(style = Style.flex(grow = 1f)) {
                                child(
                                    Text(
                                        "Team B",
                                        textSize = 14.sp,
                                        textStyle = Typeface.BOLD,
                                        textColor = Color.parseColor("#1F2937"),
                                    ),
                                )
                              },
                          )
                        },
                    )
                    child(
                        Text(
                            "Design review completed. Ready for the next phase of development.",
                            textSize = 12.sp,
                            textColor = Color.parseColor("#374151"),
                        ),
                    )
                  },
              )
            },
        )

        child(
            Text(
                "Notice: Both posts use flex(grow=1f) to share space equally, and avatars use flex(shrink=0f) + clip(Shape.Circle) to stay perfect circles!",
                textSize = 12.sp,
                textColor = Color.parseColor("#059669"),
                textStyle = Typeface.BOLD,
            ),
        )
      }
    }
  }
}
