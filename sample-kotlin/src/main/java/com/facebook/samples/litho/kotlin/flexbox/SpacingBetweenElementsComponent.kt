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
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

/**
 * Demonstrates spacing between stacked elements using Yoga's justifyContent.
 *
 * This is the Litho equivalent of Compose's Arrangement.SpaceBetween / SpaceEvenly / SpaceAround.
 * The key insight is that justifyContent only has effect when there is extra space along the main
 * axis — the container must have explicit size or be constrained by its parent.
 *
 * Mapping:
 * - YogaJustify.SPACE_BETWEEN → Arrangement.SpaceBetween
 * - YogaJustify.SPACE_EVENLY → Arrangement.SpaceEvenly
 * - YogaJustify.SPACE_AROUND → Arrangement.SpaceAround
 * - YogaJustify.CENTER → Arrangement.Center
 * - YogaJustify.FLEX_START → Arrangement.Top / Start (default)
 * - YogaJustify.FLEX_END → Arrangement.Bottom / End
 */
class SpacingBetweenElementsComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    return VerticalScroll {
      Column(style = Style.padding(all = 16.dp)) {
        child(
            Text(
                text = "Spacing Between Stacked Elements",
                textSize = 24.sp,
                textColor = 0xFF1C1E21.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(
            Text(
                text =
                    "Litho uses Yoga's justifyContent to control how children are distributed " +
                        "along the main axis. This is the equivalent of Compose's " +
                        "Arrangement.SpaceBetween, SpaceEvenly, and SpaceAround.",
                textSize = 14.sp,
                textColor = 0xFF65676B.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(
            Text(
                text =
                    "Important: justifyContent only works when there is extra space. " +
                        "The container must have explicit size (e.g., Style.height() for Column) " +
                        "or be constrained by its parent.",
                textSize = 13.sp,
                textColor = 0xFFE74C3C.toInt(),
                style = Style.margin(bottom = 24.dp),
            ),
        )

        // SPACE_BETWEEN — Column
        child(SectionTitle(title = "SPACE_BETWEEN (Column)"))
        child(
            Text(
                text = "First child at top, last at bottom, equal space between",
                textSize = 12.sp,
                textColor = 0xFF65676B.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(SpaceBetweenColumnDemo())

        // SPACE_EVENLY — Column
        child(SectionTitle(title = "SPACE_EVENLY (Column)"))
        child(
            Text(
                text = "Equal space between all children AND at the edges",
                textSize = 12.sp,
                textColor = 0xFF65676B.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(SpaceEvenlyColumnDemo())

        // SPACE_AROUND — Column
        child(SectionTitle(title = "SPACE_AROUND (Column)"))
        child(
            Text(
                text = "Equal space around each child (half-size space at edges)",
                textSize = 12.sp,
                textColor = 0xFF65676B.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(SpaceAroundColumnDemo())

        // SPACE_BETWEEN — Row (horizontal)
        child(SectionTitle(title = "SPACE_BETWEEN (Row)"))
        child(
            Text(
                text = "Same patterns work with Row for horizontal distribution",
                textSize = 12.sp,
                textColor = 0xFF65676B.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(SpaceBetweenRowDemo())

        // SPACE_EVENLY — Row (horizontal)
        child(SectionTitle(title = "SPACE_EVENLY (Row)"))
        child(SpaceEvenlyRowDemo())

        // Key patterns section
        child(
            Text(
                text = "justifyContent Mapping:",
                textSize = 16.sp,
                textColor = 0xFF1C1E21.toInt(),
                style = Style.margin(top = 24.dp, bottom = 8.dp),
            ),
        )

        val mappings = listOf(
            "• FLEX_START → Arrangement.Top/Start (default)",
            "• FLEX_END → Arrangement.Bottom/End",
            "• CENTER → Arrangement.Center",
            "• SPACE_BETWEEN → Arrangement.SpaceBetween",
            "• SPACE_AROUND → Arrangement.SpaceAround",
            "• SPACE_EVENLY → Arrangement.SpaceEvenly",
        )
        mappings.forEach { mapping ->
          child(
              Text(
                  text = mapping,
                  textSize = 13.sp,
                  textColor = 0xFF65676B.toInt(),
                  style = Style.margin(bottom = 4.dp),
              ),
          )
        }
      }
    }
  }
}

private class SectionTitle(private val title: String) : KComponent() {
  override fun ComponentScope.render(): Component = Text(
      text = title,
      textSize = 16.sp,
      textColor = 0xFF1C1E21.toInt(),
      style =
          Style.margin(top = 16.dp, bottom = 4.dp)
              .backgroundColor(0xFFF0F0F0.toInt())
              .padding(all = 8.dp),
  )
}

private class ColorBlock(
    private val color: Int,
    private val sizeDp: Int = 60,
) : KComponent() {
  override fun ComponentScope.render(): Component =
      Row(style = Style.width(sizeDp.dp).height(sizeDp.dp).backgroundColor(color))
}

// start_example
private class SpaceBetweenColumnDemo : KComponent() {
  override fun ComponentScope.render(): Component = Column(
      justifyContent = YogaJustify.SPACE_BETWEEN,
      alignItems = YogaAlign.CENTER,
      style = Style.height(250.dp).padding(all = 16.dp).backgroundColor(0xFFFAFAFA.toInt()),
  ) {
    child(ColorBlock(color = 0xFF4285F4.toInt()))
    child(ColorBlock(color = 0xFF34A853.toInt()))
    child(ColorBlock(color = 0xFFFBBC05.toInt()))
  }
}

// end_example

private class SpaceEvenlyColumnDemo : KComponent() {
  override fun ComponentScope.render(): Component = Column(
      justifyContent = YogaJustify.SPACE_EVENLY,
      alignItems = YogaAlign.CENTER,
      style = Style.height(300.dp).padding(horizontal = 16.dp).backgroundColor(0xFFFAFAFA.toInt()),
  ) {
    child(ColorBlock(color = 0xFFEA4335.toInt()))
    child(ColorBlock(color = 0xFF4285F4.toInt()))
    child(ColorBlock(color = 0xFF34A853.toInt()))
    child(ColorBlock(color = 0xFFFBBC05.toInt()))
  }
}

private class SpaceAroundColumnDemo : KComponent() {
  override fun ComponentScope.render(): Component = Column(
      justifyContent = YogaJustify.SPACE_AROUND,
      alignItems = YogaAlign.CENTER,
      style = Style.height(250.dp).padding(horizontal = 16.dp).backgroundColor(0xFFFAFAFA.toInt()),
  ) {
    child(ColorBlock(color = 0xFF4285F4.toInt()))
    child(ColorBlock(color = 0xFF34A853.toInt()))
    child(ColorBlock(color = 0xFFFBBC05.toInt()))
  }
}

private class SpaceBetweenRowDemo : KComponent() {
  override fun ComponentScope.render(): Component = Row(
      // justifyContent distributes extra space along the main axis (horizontal for Row).
      // A Row fills its parent's width by default, providing the extra space needed.
      // The explicit height sizes the cross-axis, and padding(horizontal) insets children
      // from the container edges — both are needed for the spacing to be visually correct.
      justifyContent = YogaJustify.SPACE_BETWEEN,
      alignItems = YogaAlign.CENTER,
      style = Style.height(100.dp).padding(horizontal = 16.dp).backgroundColor(0xFFFAFAFA.toInt()),
  ) {
    child(ColorBlock(color = 0xFF4285F4.toInt()))
    child(ColorBlock(color = 0xFF34A853.toInt()))
    child(ColorBlock(color = 0xFFFBBC05.toInt()))
  }
}

private class SpaceEvenlyRowDemo : KComponent() {
  override fun ComponentScope.render(): Component = Row(
      justifyContent = YogaJustify.SPACE_EVENLY,
      alignItems = YogaAlign.CENTER,
      style = Style.height(100.dp).padding(horizontal = 16.dp).backgroundColor(0xFFFAFAFA.toInt()),
  ) {
    child(ColorBlock(color = 0xFFEA4335.toInt()))
    child(ColorBlock(color = 0xFF4285F4.toInt()))
    child(ColorBlock(color = 0xFF34A853.toInt()))
    child(ColorBlock(color = 0xFFFBBC05.toInt()))
  }
}
