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

package com.facebook.samples.litho.kotlin.layout

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.StackLayout
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/** Demonstrates StackLayout for overlapping children with zIndex control. */
class StackLayoutExamplesComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    return VerticalScroll {
      Column(style = Style.padding(16.dp)) {
        child(
            Text(
                text = "StackLayout Examples",
                textSize = 24.sp,
                textColor = Color.BLACK,
                style = Style.margin(bottom = 20.dp),
            ),
        )

        // Example 1: Basic overlay - children stack on top of each other
        child(SectionHeader(title = "Example 1: Basic Overlay"))
        child(
            Text(
                text = "First child sets container size, others overlay on top",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            StackLayout(style = Style.width(200.dp).height(120.dp).margin(bottom = 20.dp)) {
              // First child establishes the container's size
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#4ECDC4"))
                                  .padding(8.dp)
                                  .width(200.dp)
                                  .height(120.dp),
                      ) {
                        child(Text(text = "Background", textSize = 12.sp, textColor = Color.WHITE))
                      },
              )
              // Second child overlays on top
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#80000000"))
                                  .padding(4.dp)
                                  .width(80.dp)
                                  .height(30.dp),
                      ) {
                        child(Text(text = "Overlay", textSize = 10.sp, textColor = Color.WHITE))
                      },
              )
            },
        )

        // Example 2: Badge overlay - small element on top of a larger one
        child(SectionHeader(title = "Example 2: Badge Overlay"))
        child(
            Text(
                text = "Badge positioned on top of content using zIndex",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            StackLayout(style = Style.width(100.dp).height(100.dp).margin(bottom = 20.dp)) {
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#45B7D1"))
                                  .padding(8.dp)
                                  .width(100.dp)
                                  .height(100.dp),
                      ) {
                        child(Text(text = "Content", textSize = 14.sp, textColor = Color.WHITE))
                      },
              )
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#FF6B6B"))
                                  .padding(4.dp)
                                  .width(24.dp)
                                  .height(24.dp),
                      ) {
                        child(Text(text = "3", textSize = 10.sp, textColor = Color.WHITE))
                      },
                  zIndex = 10,
              )
            },
        )

        // Example 3: zIndex ordering - higher zIndex renders on top
        child(SectionHeader(title = "Example 3: zIndex Ordering"))
        child(
            Text(
                text = "Higher zIndex = renders on top, regardless of insertion order",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            StackLayout(style = Style.width(200.dp).height(120.dp).margin(bottom = 20.dp)) {
              // Added first but with highest zIndex — will render on top
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#FF6B6B"))
                                  .padding(8.dp)
                                  .width(200.dp)
                                  .height(120.dp),
                      ) {
                        child(
                            Text(
                                text = "zIndex=20 (first added, renders on top)",
                                textSize = 10.sp,
                                textColor = Color.WHITE,
                            ),
                        )
                      },
                  zIndex = 20,
              )
              // Added second with middle zIndex
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#4ECDC4"))
                                  .padding(8.dp)
                                  .width(160.dp)
                                  .height(90.dp),
                      ) {
                        child(
                            Text(
                                text = "zIndex=10 (middle)",
                                textSize = 10.sp,
                                textColor = Color.WHITE,
                            ),
                        )
                      },
                  zIndex = 10,
              )
              // Added last with lowest zIndex — will render at the bottom
              child(
                  component =
                      Column(
                          style =
                              Style.backgroundColor(Color.parseColor("#45B7D1"))
                                  .padding(8.dp)
                                  .width(120.dp)
                                  .height(60.dp),
                      ) {
                        child(
                            Text(
                                text = "zIndex=5 (last added, renders behind)",
                                textSize = 10.sp,
                                textColor = Color.WHITE,
                            ),
                        )
                      },
                  zIndex = 5,
              )
            },
        )
      }
    }
  }
}

private class SectionHeader(private val title: String) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Text(
        text = title,
        textSize = 16.sp,
        textColor = Color.BLACK,
        style =
            Style.margin(top = 8.dp, bottom = 8.dp)
                .backgroundColor(Color.parseColor("#F0F0F0"))
                .padding(8.dp),
    )
  }
}
