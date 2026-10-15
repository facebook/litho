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

package com.facebook.litho.widget.focus

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.focusable
import com.facebook.litho.view.onClick
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.px
import com.facebook.rendercore.sp

/** List items used by [LinearLayoutInfoKeyboardTraversalTest]. */

/**
 * A card whose focusable elements sit *inside* the tree, so they only exist as Views once Litho has
 * mounted the item. That is what makes an off-screen item report no focusables.
 */
class TraversalCard(private val focusables: Int, private val sizePx: Int) : KComponent() {
  override fun ComponentScope.render(): Component =
      Column(style = Style.width(sizePx.px).height(sizePx.px)) {
        repeat(focusables) {
          child(
              Column(
                  style = Style.width(sizePx.px).height((sizePx / focusables).px).focusable(true),
              ),
          )
        }
      }
}

/**
 * A focusable card whose Text exposes one accessibility virtual view per ClickableSpan, the way ads
 * text does (`AdCaptionComponent`, `AdHeaderTitleComponent`).
 */
class SpanTextCard(private val sizePx: Int) : KComponent() {
  override fun ComponentScope.render(): Component {
    val text = SpannableString(LINK_TEXT)
    text.setSpan(
        object : ClickableSpan() {
          override fun onClick(widget: View) = Unit
        },
        0,
        LINK_SPAN_END,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return Column(style = Style.width(sizePx.px).height(sizePx.px).focusable(true).onClick {}) {
      child(Text(text = text, textSize = 14.sp, accessibleClickableSpans = true))
    }
  }

  companion object {
    private const val LINK_TEXT = "open link"
    private const val LINK_SPAN_END = 4
  }
}

/** A focusable card that reports when it becomes visible, for asserting impressions fire once. */
class ImpressionCard(private val sizePx: Int, private val onVisible: Runnable) : KComponent() {
  override fun ComponentScope.render(): Component = Column(
      style = Style.width(sizePx.px).height(sizePx.px).onVisible { onVisible.run() },
  ) {
    child(Column(style = Style.width(sizePx.px).height(sizePx.px).focusable(true)))
  }
}
