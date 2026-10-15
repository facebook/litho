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

package com.facebook.samples.litho.kotlin.flexbox

import android.graphics.Color
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

class FlexboxLayoutExamples : KComponent() {
  override fun ComponentScope.render(): Component {
    return VerticalScroll {
      Column(style = Style.padding(16.dp)) {
        // Title
        child(
            Text(
                text = "Flex Properties Examples",
                textSize = 24.sp,
                textColor = Color.BLACK,
                style = Style.margin(bottom = 20.dp),
            ),
        )

        // Example 1: flexGrow in Row - Equal distribution
        // VISUAL: Three boxes of EQUAL width filling the entire container.
        // All boxes have grow=1, so they share the available space equally (1:1:1 ratio).
        child(ExampleSection(title = "Example 1: All children stretch equally along the main axis"))
        child(
            Row(style = Style.height(80.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
            },
        )

        // Example 2: Different flexGrow values in Row
        // VISUAL: Middle box (teal) is TWICE AS WIDE as the red and blue boxes.
        // Starting from 0dp width, flexGrow distributes space proportionally (1:2:1).
        // Middle box gets 2 parts (50%), side boxes get 1 part each (25% each).
        child(
            ExampleSection(
                title = "Example 2: Children grow proportionally based on their flex values",
            ),
        )
        child(
            Text(
                text = "Middle box takes 2x space compared to others",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            Row(style = Style.height(80.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "grow=1",
                      style = Style.width(0.dp).flex(grow = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "grow=2",
                      style = Style.width(0.dp).flex(grow = 2f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "grow=1",
                      style = Style.width(0.dp).flex(grow = 1f),
                  ),
              )
            },
        )

        // Example 3: flexGrow with one fixed width in Row
        // VISUAL: Red box is FIXED at 100dp. Teal and blue boxes SHARE the remaining space equally.
        // The fixed width box doesn't participate in flex growth, only the flexGrow boxes do.
        child(
            ExampleSection(
                title =
                    "Example 3: Flexible children fill remaining space after fixed-size children",
            ),
        )
        child(
            Row(style = Style.height(80.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "100dp",
                      style = Style.width(100.dp),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
            },
        )

        // Example 4: flexShrink in Row (overflow scenario)
        // VISUAL: All three boxes are SHRUNK EQUALLY to fit in the 250dp container.
        // Each box wants 150dp (total 450dp), but container is only 250dp wide.
        // With shrink=1 on all, they share the shrinking burden equally.
        child(
            ExampleSection(title = "Example 4: Children shrink equally when space is constrained"),
        )
        child(
            Text(
                text = "Container width: 250dp, each child wants 150dp",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            Row(style = Style.height(80.dp).width(250.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "shrink=1",
                      style = Style.width(150.dp).flex(shrink = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "shrink=1",
                      style = Style.width(150.dp).flex(shrink = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "shrink=1",
                      style = Style.width(150.dp).flex(shrink = 1f),
                  ),
              )
            },
        )

        // Example 5: Different flexShrink values
        // VISUAL: Red box (shrink=0) stays FULL WIDTH at 150dp and DOES NOT SHRINK.
        // Teal and blue boxes SHRINK to fit remaining space, with blue shrinking MORE (2x).
        // Red box maintains its size, forcing other boxes to compensate.
        child(
            ExampleSection(
                title = "Example 5: Children shrink proportionally based on their shrink values",
            ),
        )
        child(
            Text(
                text = "shrink=0 won't shrink, higher values shrink more",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            Row(style = Style.height(80.dp).width(250.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "shrink=0",
                      style = Style.width(150.dp).flex(shrink = 0f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "shrink=1",
                      style = Style.width(150.dp).flex(shrink = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "shrink=2",
                      style = Style.width(150.dp).flex(shrink = 2f),
                  ),
              )
            },
        )

        // Example 6: flexGrow in Column - Vertical distribution
        // VISUAL: Teal box (grow=2) is TWICE AS TALL as red and blue boxes.
        // In a Column, flexGrow works vertically. Teal gets 50% height, others get 25% each.
        child(ExampleSection(title = "Example 6: Flex properties work vertically in columns"))
        child(
            Column(style = Style.height(300.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "grow=2",
                      style = Style.flex(grow = 2f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "grow=1",
                      style = Style.flex(grow = 1f),
                  ),
              )
            },
        )

        // Example 7: flexShrink in Column
        // VISUAL: Red box (shrink=0) stays FULL HEIGHT at 100dp. Teal and blue boxes
        // SHRINK EQUALLY to fit remaining 100dp (50dp each) in the 200dp container.
        child(
            ExampleSection(
                title = "Example 7: Vertical shrinking when container height is constrained",
            ),
        )
        child(
            Text(
                text = "Container height: 200dp, each child wants 100dp",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            Column(style = Style.height(200.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "shrink=0",
                      style = Style.height(100.dp).flex(shrink = 0f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "shrink=1",
                      style = Style.height(100.dp).flex(shrink = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "shrink=1",
                      style = Style.height(100.dp).flex(shrink = 1f),
                  ),
              )
            },
        )

        // Example 8: Combining grow and shrink
        // VISUAL: Middle box (teal) is TWICE AS WIDE as red and blue boxes (due to grow=2).
        // If space becomes constrained, teal WON'T SHRINK (shrink=0), while red shrinks
        // normally and blue shrinks TWICE AS MUCH (shrink=2).
        child(ExampleSection(title = "Example 8: Children can grow and shrink independently"))
        child(
            Text(
                text = "Middle box grows 2x but won't shrink",
                textSize = 12.sp,
                textColor = Color.GRAY,
            ),
        )
        child(
            Row(style = Style.height(80.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "grow=1 shrink=1",
                      style = Style.width(0.dp).flex(grow = 1f, shrink = 1f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "grow=2 shrink=0",
                      style = Style.width(0.dp).flex(grow = 2f, shrink = 0f),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "grow=1 shrink=2",
                      style = Style.width(0.dp).flex(grow = 1f, shrink = 2f),
                  ),
              )
            },
        )

        // Example 9: No flex properties (default sizing)
        // VISUAL: Each box has its EXACT specified width (100dp, 150dp, 80dp).
        // Without flex properties, boxes don't grow or shrink - they maintain their set dimensions.
        child(
            ExampleSection(
                title = "Example 9: Children maintain their specified sizes without flex",
            ),
        )
        child(
            Row(style = Style.height(80.dp).margin(bottom = 20.dp)) {
              child(
                  ColorBox(
                      color = Color.parseColor("#FF6B6B"),
                      label = "100dp",
                      style = Style.width(100.dp),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#4ECDC4"),
                      label = "150dp",
                      style = Style.width(150.dp),
                  ),
              )
              child(
                  ColorBox(
                      color = Color.parseColor("#45B7D1"),
                      label = "80dp",
                      style = Style.width(80.dp),
                  ),
              )
            },
        )
      }
    }
  }
}

class ExampleSection(private val title: String) : KComponent() {
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

class ColorBox(
    private val color: Int,
    private val label: String,
    private val style: Style = Style,
) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Column(style = style.backgroundColor(color).padding(8.dp).margin(horizontal = 2.dp)) {
      child(Text(text = label, textSize = 12.sp, textColor = Color.WHITE))
    }
  }
}
