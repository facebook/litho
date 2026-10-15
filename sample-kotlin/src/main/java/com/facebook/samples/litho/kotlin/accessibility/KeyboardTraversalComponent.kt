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

package com.facebook.samples.litho.kotlin.accessibility

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.focusable
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.collection.LazyGrid
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.LazyStaggeredGrid
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/**
 * Keyboard TAB traversal through Litho lists, one row per LayoutManager.
 *
 * Attach a hardware keyboard (or `adb shell input keyevent 61`) and hold TAB. Focus should walk
 * card by card through each list and scroll it, rather than stopping at the last card that happened
 * to be on screen when the list was first laid out.
 *
 * Each card is a separate tab stop, so the card number tells you exactly where focus is.
 */
class KeyboardTraversalComponent : KComponent() {

  override fun ComponentScope.render(): Component =
      Column(style = Style.padding(all = 8.dp)) {
        child(label("Horizontal list (LinearLayoutInfo)"))
        child(
            LazyList(
                orientation = RecyclerView.HORIZONTAL,
                style = Style.height(CARD).margin(bottom = 8.dp),
            ) {
              children(items = (0 until COUNT), id = { it }) { card("L$it", COLOR_LINEAR) }
            },
        )

        child(label("Horizontal grid (GridLayoutInfo)"))
        child(
            LazyGrid(
                orientation = RecyclerView.HORIZONTAL,
                columns = 2,
                style = Style.height(CARD * 2).margin(bottom = 8.dp),
            ) {
              children(items = (0 until COUNT), id = { it }) { card("G$it", COLOR_GRID) }
            },
        )

        child(label("Horizontal staggered grid (StaggeredGridLayoutInfo)"))
        child(
            LazyStaggeredGrid(
                orientation = RecyclerView.HORIZONTAL,
                spans = 2,
                style = Style.height(CARD * 2).margin(bottom = 8.dp),
            ) {
              children(items = (0 until COUNT), id = { it }) { card("S$it", COLOR_STAGGERED) }
            },
        )

        child(label("Horizontal list, cards exposing a11y virtual views"))
        child(
            LazyList(
                orientation = RecyclerView.HORIZONTAL,
                style = Style.height(CARD).margin(bottom = 8.dp),
            ) {
              children(items = (0 until COUNT), id = { it }) { spanCard("A$it") }
            },
        )

        child(label("Vertical list (LinearLayoutInfo)"))
        child(
            LazyList(style = Style.flex(grow = 1f)) {
              children(items = (0 until COUNT), id = { it }) { card("V$it", COLOR_VERTICAL) }
            },
        )
      }

  private fun ResourcesScope.label(text: String): Component =
      Text(text = text, textSize = 13.sp, style = Style.padding(vertical = 4.dp))

  /** A card is one tab stop: focusable and clickable, like a real feed unit. */
  private fun ResourcesScope.card(name: String, color: Int): Component = Column(
      style =
          Style.width(CARD)
              .height(CARD)
              .margin(all = 4.dp)
              .backgroundColor(color)
              .focusable(true)
              .onClick {},
  ) {
    child(Text(text = name, textSize = 22.sp, style = Style.padding(all = 12.dp)))
  }

  /**
   * A card whose Text exposes one accessibility virtual view per ClickableSpan, the way ads text
   * does (`AdCaptionComponent`, `AdHeaderTitleComponent`). `ComponentHost.dispatchKeyEvent` routes
   * TAB to `ExploreByTouchHelper` for such hosts, so this row shows whether the links merely become
   * extra tab stops or actually trap focus.
   */
  private fun ResourcesScope.spanCard(name: String): Component {
    val text = SpannableString("$name link")
    text.setSpan(
        object : ClickableSpan() {
          override fun onClick(widget: View) = Unit
        },
        name.length + 1,
        text.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return Column(
        style =
            Style.width(CARD)
                .height(CARD)
                .margin(all = 4.dp)
                .backgroundColor(COLOR_SPANS)
                .focusable(true)
                .onClick {},
    ) {
      child(
          Text(
              text = text,
              textSize = 18.sp,
              accessibleClickableSpans = true,
              style = Style.padding(all = 12.dp),
          ),
      )
    }
  }

  companion object {
    private val CARD = 140.dp
    private const val COUNT = 20
    private const val COLOR_LINEAR = Color.LTGRAY
    private const val COLOR_GRID = 0xFFB3E5FC.toInt()
    private const val COLOR_STAGGERED = 0xFFC8E6C9.toInt()
    private const val COLOR_VERTICAL = 0xFFFFE0B2.toInt()
    private const val COLOR_SPANS = 0xFFF8BBD0.toInt()
  }
}
