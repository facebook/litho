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

package com.facebook.samples.litho.kotlin.collection

import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
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
import com.facebook.litho.useCached
import com.facebook.litho.view.Shape
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.clip
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.LinearSpacing
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/**
 * Demonstrates a horizontal card carousel pattern using Litho KComponent.
 *
 * Key patterns:
 * - LazyList with orientation = RecyclerView.HORIZONTAL for horizontal scrolling
 * - Explicit Style.height() on LazyList (required for bounded cross axis)
 * - child(id = ..., component = ...) with unique IDs for efficient recycling
 * - Fixed width on each card for consistent sizing in horizontal scroll
 * - RoundedRect background for rounded card corners
 * - LinearSpacing for consistent gaps between cards
 */
class HorizontalCardCarouselComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val patterns = useCached {
      listOf(
          "• LazyList(orientation = RecyclerView.HORIZONTAL)",
          "• Style.height() on LazyList (required for bounded axis)",
          "• children(items, id = { ... }) for efficient recycling",
          "• Fixed width on each card (150.dp)",
          "• RoundedRect(color, cornerRadius) for rounded corners",
          "• LinearSpacing(all = ...) for gaps between cards",
          "• Row with backgroundColor as image placeholder",
      )
    }
    return VerticalScroll {
      Column(style = Style.padding(all = 16.dp)) {
        child(
            Text(
                text = "Horizontal Card Carousel Demo",
                textSize = 24.sp,
                textColor = 0xFF1C1E21.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(
            Text(
                text =
                    "A horizontally scrolling list of cards using LazyList with " +
                        "RecyclerView.HORIZONTAL. Each card has a fixed width, rounded corners, " +
                        "and a vertical layout of image placeholder + text content.",
                textSize = 14.sp,
                textColor = 0xFF65676B.toInt(),
                style = Style.margin(bottom = 16.dp),
            ),
        )

        // Carousel section
        child(
            Text(
                text = "Basic Carousel",
                textSize = 18.sp,
                textColor = 0xFF1C1E21.toInt(),
                style = Style.margin(bottom = 8.dp),
            ),
        )
        child(BasicCarousel())

        child(
            Text(
                text = "Carousel with Spacing",
                textSize = 18.sp,
                textColor = 0xFF1C1E21.toInt(),
                style = Style.margin(top = 24.dp, bottom = 8.dp),
            ),
        )
        child(SpacedCarousel())

        // Key patterns section
        child(
            Text(
                text = "Key Litho Patterns Used:",
                textSize = 16.sp,
                textColor = 0xFF1C1E21.toInt(),
                style = Style.margin(top = 24.dp, bottom = 8.dp),
            ),
        )

        patterns.forEach { pattern ->
          child(
              Text(
                  text = pattern,
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

private val carouselItems = listOf(
    CardItem("Sunset", "Golden hour vibes", 0xFFFF6B6B.toInt()),
    CardItem("Ocean", "Deep blue waves", 0xFF4ECDC4.toInt()),
    CardItem("Forest", "Lush green canopy", 0xFF45B7D1.toInt()),
    CardItem("Mountain", "Snowy peak views", 0xFF96CEB4.toInt()),
    CardItem("Desert", "Sandy dune sunset", 0xFFFECEA8.toInt()),
    CardItem("City", "Urban skyline", 0xFF778899.toInt()),
)

// start_example
private class BasicCarousel : KComponent() {

  override fun ComponentScope.render(): Component = LazyList(
      orientation = RecyclerView.HORIZONTAL,
      style = Style.height(200.dp),
  ) {
    children(items = carouselItems, id = { it.title }) { item ->
      CarouselCard(
          title = item.title,
          subtitle = item.subtitle,
          imageColor = item.imageColor,
      )
    }
  }
}

// end_example

/**
 * A carousel that uses [LinearSpacing] to add consistent gaps between items.
 *
 * [LinearSpacing] is an [androidx.recyclerview.widget.RecyclerView.ItemDecoration] provided by
 * Litho that adds equal spacing around each item in a [LazyList]. The [all] parameter sets uniform
 * spacing on all sides (start, end, top, bottom) of every item, which results in even gaps between
 * adjacent cards without needing to add margin to individual items.
 *
 * Other available parameters:
 * - [between]: spacing only between items (no leading/trailing space)
 * - [start] / [end]: spacing at the start or end of the list
 */
private class SpacedCarousel : KComponent() {

  override fun ComponentScope.render(): Component = LazyList(
      orientation = RecyclerView.HORIZONTAL,
      itemDecoration = LinearSpacing(all = 12.dp),
      style = Style.height(200.dp),
  ) {
    children(items = carouselItems, id = { it.title }) { item ->
      CarouselCard(
          title = item.title,
          subtitle = item.subtitle,
          imageColor = item.imageColor,
      )
    }
  }
}

private class CarouselCard(
    private val title: String,
    private val subtitle: String,
    private val imageColor: Int,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val cardShape = useCached { Shape.RoundedRectangle(12.dp) }
    return Column(
        style = Style.width(150.dp).backgroundColor(0xFFF5F5F5.toInt()).clip(cardShape),
    ) {
      // Image placeholder — replace with Image(drawable = drawableRes(R.drawable.card_image))
      child(
          Row(
              style = Style.height(100.dp).backgroundColor(imageColor),
          ),
      )
      child(
          Column(style = Style.padding(all = 12.dp).flex(grow = 1f)) {
            child(
                Text(
                    text = title,
                    textSize = 14.sp,
                    textColor = Color.BLACK,
                ),
            )
            child(
                Text(
                    text = subtitle,
                    textSize = 12.sp,
                    textColor = 0xFF666666.toInt(),
                    style = Style.margin(top = 4.dp),
                ),
            )
          },
      )
    }
  }
}

private class CardItem(val title: String, val subtitle: String, val imageColor: Int)
