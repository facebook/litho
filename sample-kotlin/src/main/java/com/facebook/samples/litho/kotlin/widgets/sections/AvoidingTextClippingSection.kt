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
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Card
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.background
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class AvoidingTextClippingSection : KComponent() {

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
                "⚠️ Avoiding Text Clipping",
                textSize = 20.sp,
                textColor = Color.parseColor("#DC2626"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 16.dp),
            ),
        )

        child(
            Text(
                "Text clipping happens when the container doesn't have enough space to display all the text content. Here's how to avoid it:",
                textSize = 14.sp,
                textColor = Color.parseColor("#374151"),
                style = Style.margin(bottom = 16.dp),
            ),
        )

        // BAD EXAMPLE 1: Fixed height causing clipping
        child(
            Text(
                "❌ BAD: Fixed height causes clipping",
                textSize = 16.sp,
                textColor = Color.parseColor("#DC2626"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Column(
                style =
                    Style.width(300.dp)
                        .height(50.dp)
                        .padding(all = 12.dp)
                        .background(RoundedRect(Color.parseColor("#FEE2E2"), 8.dp))
                        .margin(bottom = 16.dp),
            ) {
              child(
                  Text(
                      "This is a long text that demonstrates clipping. When you set a fixed height on the container that is too small for the text content, the text will be cut off at the bottom. Notice how the text just disappears mid-sentence without any ellipsis or indication that there's more content below. This creates a poor user experience.",
                      textSize = 14.sp,
                      textColor = Color.parseColor("#991B1B"),
                  ),
              )
            },
        )

        child(
            Text(
                "Problem: Container has height(50.dp) which is too small for the text",
                textSize = 12.sp,
                textColor = Color.parseColor("#6B7280"),
                style = Style.margin(bottom = 24.dp),
            ),
        )

        // BAD EXAMPLE 2: Using flex with fixed basis causing clipping
        child(
            Text(
                "❌ BAD: Fixed flex basis also causes clipping",
                textSize = 16.sp,
                textColor = Color.parseColor("#DC2626"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Row(style = Style.width(300.dp).height(100.dp).margin(bottom = 16.dp)) {
              child(
                  Column(
                      style =
                          Style.flex(basis = 50.dp, shrink = 0f)
                              .padding(all = 12.dp)
                              .background(RoundedRect(Color.parseColor("#FEE2E2"), 8.dp)),
                  ) {
                    child(
                        Text(
                            "When using flex(basis = 50.dp, shrink = 0f), the container is locked to a fixed size and won't grow to fit the content. This causes the same clipping issue as a fixed height.",
                            textSize = 14.sp,
                            textColor = Color.parseColor("#991B1B"),
                        ),
                    )
                  },
              )
            },
        )

        child(
            Text(
                "Problem: flex(basis = 50.dp, shrink = 0f) prevents growing to fit content",
                textSize = 12.sp,
                textColor = Color.parseColor("#6B7280"),
                style = Style.margin(bottom = 24.dp),
            ),
        )

        // GOOD EXAMPLE 1: No fixed height
        child(
            Text(
                "✅ GOOD: Let the container grow with content",
                textSize = 16.sp,
                textColor = Color.parseColor("#059669"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Column(
                style =
                    Style.width(300.dp)
                        .padding(all = 12.dp)
                        .background(RoundedRect(Color.parseColor("#D1FAE5"), 8.dp))
                        .margin(bottom = 16.dp),
            ) {
              child(
                  Text(
                      "This is a long text that demonstrates proper text wrapping. By NOT setting a fixed height on the container, the text can wrap naturally and all content is visible. The container grows to accommodate all the text content without any clipping.",
                      textSize = 14.sp,
                      textColor = Color.parseColor("#065F46"),
                  ),
              )
            },
        )

        child(
            Text(
                "Solution: Remove fixed height, only set width",
                textSize = 12.sp,
                textColor = Color.parseColor("#6B7280"),
                style = Style.margin(bottom = 24.dp),
            ),
        )

        // GOOD EXAMPLE 2: Using flex to adapt to content
        child(
            Text(
                "✅ GOOD: Use flex(grow = 1f) to adapt to available space",
                textSize = 16.sp,
                textColor = Color.parseColor("#059669"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Row(style = Style.width(300.dp).height(150.dp).margin(bottom = 16.dp)) {
              child(
                  Column(
                      style =
                          Style.flex(grow = 1f)
                              .padding(all = 12.dp)
                              .background(RoundedRect(Color.parseColor("#D1FAE5"), 8.dp)),
                  ) {
                    child(
                        Text(
                            "Using flex(grow = 1f) allows the container to expand and fill available space while adapting to its content. The container will grow to fill the parent but also wrap the text properly so nothing is clipped.",
                            textSize = 14.sp,
                            textColor = Color.parseColor("#065F46"),
                        ),
                    )
                  },
              )
            },
        )

        child(
            Text(
                "Solution: Use flex(grow = 1f) to fill space and adapt to content",
                textSize = 12.sp,
                textColor = Color.parseColor("#6B7280"),
                style = Style.margin(bottom = 24.dp),
            ),
        )

        // GOOD EXAMPLE 3: Using maxLines
        child(
            Text(
                "✅ GOOD: Control text with maxLines",
                textSize = 16.sp,
                textColor = Color.parseColor("#059669"),
                textStyle = Typeface.BOLD,
                style = Style.margin(bottom = 8.dp),
            ),
        )

        child(
            Column(
                style =
                    Style.width(300.dp)
                        .padding(all = 12.dp)
                        .background(RoundedRect(Color.parseColor("#D1FAE5"), 8.dp))
                        .margin(bottom = 16.dp),
            ) {
              child(
                  Text(
                      "This is a long text that demonstrates using maxLines. The text will wrap properly and if it exceeds the maximum number of lines, it will show an ellipsis (...) instead of being clipped. This is better than hard clipping because users know there's more content.",
                      textSize = 14.sp,
                      textColor = Color.parseColor("#065F46"),
                      maxLines = 3,
                      ellipsize = android.text.TextUtils.TruncateAt.END,
                  ),
              )
            },
        )

        child(
            Text(
                "Solution: Use maxLines with ellipsize for controlled truncation",
                textSize = 12.sp,
                textColor = Color.parseColor("#6B7280"),
            ),
        )
      }
    }
  }
}
