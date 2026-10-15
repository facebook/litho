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

import android.graphics.Paint.FontMetricsInt
import android.graphics.Rect
import android.text.TextPaint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextLayoutIntrinsicsTest {

  /**
   * Font metrics used across the height cases. Baseline is 0; `top`/`ascent` are negative (above
   * the baseline). Derived reference values:
   * - unpadded height (`descent - ascent`) = 3 - (-10) = 13
   * - padded height (`bottom - top`) = 5 - (-12) = 17
   */
  private val fontMetrics =
      FontMetricsInt().apply {
        top = -12
        ascent = -10
        descent = 3
        bottom = 5
      }

  private fun paint(fm: FontMetricsInt = fontMetrics): TextPaint =
      mock<TextPaint>().apply { whenever(fontMetricsInt).thenReturn(fm) }

  /** A paint whose `getTextBounds("T", ...)` reports a cap glyph of [capBoundsHeight] px. */
  private fun paintWithCap(capBoundsHeight: Int): TextPaint {
    val paint = mock<TextPaint>()
    doAnswer { invocation ->
          invocation.getArgument<Rect>(3).set(0, 0, 1, capBoundsHeight)
          null
        }
        .whenever(paint)
        .getTextBounds(eq("T"), eq(0), eq(1), any())
    return paint
  }

  @Test
  fun singleLineHeight_includeFontPadding_returnsPaddedFontHeight() {
    assertThat(TextLayoutIntrinsics.singleLineHeight(paint(), includeFontPadding = true))
        .isEqualTo(17) // bottom - top
  }

  @Test
  fun singleLineHeight_excludeFontPadding_returnsUnpaddedFontHeight() {
    assertThat(TextLayoutIntrinsics.singleLineHeight(paint(), includeFontPadding = false))
        .isEqualTo(13) // descent - ascent
  }

  @Test
  fun firstLineExtraSpacing_multiplierOne_returnsZero() {
    assertThat(TextLayoutIntrinsics.firstLineExtraSpacing(paint(), lineHeightMultiplier = 1f))
        .isEqualTo(0)
  }

  @Test
  fun firstLineExtraSpacing_multiplierBelowOne_returnsZero() {
    assertThat(TextLayoutIntrinsics.firstLineExtraSpacing(paint(), lineHeightMultiplier = 0.5f))
        .isEqualTo(0)
  }

  @Test
  fun firstLineExtraSpacing_multiplierAboveOne_scalesUnpaddedHeightTruncated() {
    // (descent - ascent) * (1.5 - 1) = 13 * 0.5 = 6.5, truncated -> 6
    assertThat(TextLayoutIntrinsics.firstLineExtraSpacing(paint(), lineHeightMultiplier = 1.5f))
        .isEqualTo(6)
  }

  @Test
  fun firstLineExtraSpacing_multiplierTwo_scalesFullUnpaddedHeight() {
    // (descent - ascent) * (2 - 1) = 13
    assertThat(TextLayoutIntrinsics.firstLineExtraSpacing(paint(), lineHeightMultiplier = 2f))
        .isEqualTo(13)
  }

  @Test
  fun singleLineHeightWithManualSpacing_isCapHeightPlusManualSpacings() {
    // capBoundsHeight 7 + manualCapSpacing 4 + manualBaselineSpacing 6 = 17
    assertThat(
        TextLayoutIntrinsics.singleLineHeightWithManualSpacing(
            paintWithCap(capBoundsHeight = 7),
            manualCapSpacing = 4,
            manualBaselineSpacing = 6,
        ),
    )
        .isEqualTo(17)
  }

  @Test
  fun singleLineHeightWithManualSpacing_otherValues() {
    assertThat(
        TextLayoutIntrinsics.singleLineHeightWithManualSpacing(
            paintWithCap(capBoundsHeight = 10),
            manualCapSpacing = 2,
            manualBaselineSpacing = 3,
        ),
    )
        .isEqualTo(15)
  }

  @Test
  fun longestWordWidth_emptyText_returnsZero() {
    assertThat(TextLayoutIntrinsics.longestWordWidth("", paint())).isEqualTo(0)
  }
}
