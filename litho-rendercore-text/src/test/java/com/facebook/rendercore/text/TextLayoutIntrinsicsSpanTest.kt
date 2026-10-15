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

import android.graphics.Color
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import kotlin.math.ceil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Covers [TextLayoutIntrinsics.longestWordWidth]'s span-aware width path: the paint-only
 * `measureText` fast path (no width-affecting span) vs. the `Layout.getDesiredWidth` fallback for
 * width-affecting spans. Runs in NATIVE graphics mode so span metrics are real.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextLayoutIntrinsicsSpanTest {

  private fun basePaint(): TextPaint = TextPaint().apply { textSize = 24f }

  @Test
  fun longestWordWidth_plainText_matchesGetDesiredWidth() {
    // Fast path (paint.measureText) must match the accurate Layout.getDesiredWidth for a plain
    // word.
    val paint = basePaint()
    val word = "supercalifragilistic"
    val expected = ceil(Layout.getDesiredWidth(word, paint).toDouble()).toInt()
    assertThat(TextLayoutIntrinsics.longestWordWidth(word, paint))
        .isBetween(expected - 1, expected + 1)
  }

  @Test
  fun longestWordWidth_colorSpan_matchesPlainText() {
    // ForegroundColorSpan doesn't affect advance width, so it stays on the fast path == plain text.
    val paint = basePaint()
    val plain = "a supercalifragilistic b"
    val colored =
        SpannableString(plain).apply {
          setSpan(ForegroundColorSpan(Color.RED), 2, 22, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    assertThat(TextLayoutIntrinsics.longestWordWidth(colored, paint))
        .isEqualTo(TextLayoutIntrinsics.longestWordWidth(plain, paint))
  }

  @Test
  fun longestWordWidth_sizeSpan_exceedsPlainTextViaFallback() {
    // A width-affecting size span must route through Layout.getDesiredWidth (which honors the
    // span),
    // yielding a wider min-content than the base-paint measureText fast path would.
    val paint = basePaint()
    val plain = "a supercalifragilistic b"
    val sized =
        SpannableString(plain).apply {
          setSpan(AbsoluteSizeSpan(72), 2, 22, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    assertThat(TextLayoutIntrinsics.longestWordWidth(sized, paint))
        .isGreaterThan(TextLayoutIntrinsics.longestWordWidth(plain, paint))
  }
}
