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

package com.facebook.samples.litho.kotlin.playground

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.kotlin.widget.VerticalScroll
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.widget.RichText
import com.facebook.litho.widget.SolidColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.rendercore.text.TextStyle

class SelectableTextDemoKComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val selectableTextStyle = useCached {
      TextStyle.createDefaultConfiguredTextStyle(androidContext).apply { isTextSelectable = true }
    }

    val sampleText = useCached {
      val text =
          "This text is selectable with links. \uD83D\uDC4B " +
              "Visit Google for search, or check out Android Developers for docs. " +
              "Long-press to select a word \uD83D\uDC49, then drag the handles to adjust. " +
              "Here are some emojis to test selection across multi-codepoint characters: " +
              "\uD83D\uDE00\uD83D\uDE02\uD83E\uDD23 simple emojis, " +
              "\uD83D\uDC68\u200D\uD83D\uDCBB man technologist (ZWJ), " +
              "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66 family (multi-ZWJ), " +
              "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08 rainbow flag, " +
              "\uD83D\uDC4D\uD83C\uDFFD thumbs up (skin tone), " +
              "\uD83C\uDDE9\uD83C\uDDEA flag DE (regional indicators). " +
              "Try selecting across a link like Wikipedia to see how it behaves. " +
              "You can copy it to the clipboard using the floating toolbar. \uD83D\uDCCB"
      SpannableString(text).apply {
        fun setLink(word: String, url: String) {
          val start = text.indexOf(word)
          if (start >= 0) {
            setSpan(URLSpan(url), start, start + word.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
          }
        }
        setLink("Google", "https://www.google.com")
        setLink("Android Developers", "https://developer.android.com")
        setLink("Wikipedia", "https://en.wikipedia.org")
      }
    }
    return VerticalScroll {
      Column(style = Style.padding(16.dp)) {
        child(Text(text = "Selectable Text Demo", textSize = 20.sp))
        child(SolidColor(color = Color.LTGRAY, style = Style.height(400.dp)))
        child(Text(text = "RichText (custom selection)", textSize = 16.sp))
        child(
            RichText(
                text = sampleText,
                textStyle = selectableTextStyle,
            ),
        )
        child(SolidColor(color = Color.LTGRAY, style = Style.height(200.dp)))
        child(Text(text = "TextView (native selection)", textSize = 16.sp))
        child(SelectableTextView(text = sampleText))
        child(SolidColor(color = Color.LTGRAY, style = Style.height(400.dp)))
        child(Text(text = "End of content", textSize = 16.sp))
        child(SolidColor(color = Color.LTGRAY, style = Style.height(200.dp)))
      }
    }
  }
}
