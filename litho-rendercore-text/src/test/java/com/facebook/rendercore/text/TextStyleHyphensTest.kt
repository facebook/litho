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

package com.facebook.rendercore.text

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Covers [TextStyle.maybeSetHyphens], which maps the `hyphens` attribute onto minikin's hyphenation
 * frequency, and the layout that frequency produces.
 *
 * Whether a hyphen is actually inserted depends on the platform shipping a dictionary for the
 * language of the text, which this mapping does not control. Android ships no Dutch dictionary, so
 * `auto` is a no-op for `nl` while German breaks at its compound boundaries.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextStyleHyphensTest {

  @Test
  fun maybeSetHyphens_auto_requestsNormalFrequency() {
    val textStyle = TextStyle()

    TextStyle.maybeSetHyphens(textStyle, "auto")

    assertThat(textStyle.hyphenationFrequency).isEqualTo(Layout.HYPHENATION_FREQUENCY_NORMAL)
  }

  @Test
  fun maybeSetHyphens_manual_requestsNoFrequency() {
    val textStyle = TextStyle()

    TextStyle.maybeSetHyphens(textStyle, "manual")

    assertThat(textStyle.hyphenationFrequency).isEqualTo(Layout.HYPHENATION_FREQUENCY_NONE)
  }

  @Test
  fun maybeSetHyphens_unset_leavesFrequencyUntouched() {
    val textStyle = TextStyle()
    textStyle.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_FULL

    TextStyle.maybeSetHyphens(textStyle, null)

    assertThat(textStyle.hyphenationFrequency).isEqualTo(Layout.HYPHENATION_FREQUENCY_FULL)
  }

  @Test
  fun maybeSetHyphens_unrecognizedValue_requestsNoFrequency() {
    val textStyle = TextStyle()
    textStyle.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_FULL

    TextStyle.maybeSetHyphens(textStyle, "not_a_hyphens_value")

    assertThat(textStyle.hyphenationFrequency).isEqualTo(Layout.HYPHENATION_FREQUENCY_NONE)
  }

  @Test
  fun auto_breaksGermanCompoundAtMorphemeBoundary() {
    assertThat(layoutLines(GERMAN, Locale.GERMANY, "auto"))
        .containsExactly("Verbesserte ", "Sicherheits", "funktionen")
  }

  @Test
  fun manual_breaksGermanCompoundMidMorpheme() {
    assertThat(layoutLines(GERMAN, Locale.GERMANY, "manual")).doesNotContain("Sicherheits")
  }

  private fun layoutLines(text: String, locale: Locale, hyphens: String?): List<String> {
    val textStyle = TextStyle()
    TextStyle.maybeSetHyphens(textStyle, hyphens)
    val paint =
        TextPaint().apply {
          textSize = 24f
          textLocale = locale
        }
    val layout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, LAYOUT_WIDTH_PX)
            .setHyphenationFrequency(textStyle.hyphenationFrequency)
            .build()
    return (0 until layout.lineCount).map {
      text.substring(layout.getLineStart(it), layout.getLineEnd(it))
    }
  }

  companion object {
    private const val GERMAN = "Verbesserte Sicherheitsfunktionen"
    private const val LAYOUT_WIDTH_PX = 160
  }
}
