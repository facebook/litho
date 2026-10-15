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

package com.facebook.litho

import android.text.Layout
import android.view.View
import com.facebook.rendercore.text.RCTextView

/**
 * Reports the text of an [RCTextView] mounted in a [ComponentHost], so the host's [TextContent]
 * covers primitive text the same way it covers Litho's legacy text drawable.
 */
class RCTextViewTextContent
internal constructor(
    val view: RCTextView,
    private val renderUnit: LithoRenderUnit? = null,
) : TextContent {

  override val items: List<TextContent.Item>
    get() {
      val layout: Layout = view.layout ?: return emptyList()
      val text: CharSequence = mountedText ?: return emptyList()
      return listOf(
          LayoutTextContentItem(
              layout,
              text,
              view.fullText ?: text,
              view.isTextTruncated,
              color = view.currentTextColor,
              isMounted = { view.layout === layout },
          ),
      )
    }

  override val textList: List<CharSequence>
    get() {
      if (view.layout == null) {
        return emptyList()
      }
      val text = mountedText ?: return emptyList()
      return listOf(text)
    }

  // RCTextView clears its text on unmount despite declaring it non-null.
  private val mountedText: CharSequence?
    get() = view.text

  /**
   * Whether test lookups should match this text through its host, like legacy Text which draws into
   * its host. True for Litho's Text, unless legacy Text would have had a wrapper host of its own
   * (click handler, view tag, focusable, a11y, ...): then the text view stands in for that wrapper.
   * Direct primitive users are always matched on the text view, and so is hidden text, so a
   * visible-text lookup checks the text view's own visibility rather than its host's.
   */
  val isMatchedThroughHost: Boolean
    get() {
      val unit = renderUnit ?: return false
      return view.visibility == View.VISIBLE &&
          (unit.flags and LithoRenderUnit.LAYOUT_FLAG_NEEDS_HOST_VIEW_AS_DRAWABLE) == 0
    }
}
