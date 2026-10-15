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

import android.view.View
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.testing.RenderCoreTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Checks `RichTextLayoutBehavior.minIntrinsicHeight` against the height of a real single-line
 * [android.text.StaticLayout], across font-padding, multiplier, extra, fixed-line-height, and
 * manual cap/baseline spacing combinations.
 *
 * Runs in NATIVE graphics mode so font metrics and the layout are real; a 1px tolerance absorbs the
 * probe's rounding.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextLayoutIntrinsicsParityTest {

  @Rule @JvmField val renderCoreTestRule = RenderCoreTestRule()

  private fun assertMinIntrinsicHeightMatchesRealLayout(style: TextStyle) {
    val context = renderCoreTestRule.context
    val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    val realHeight =
        TextMeasurementUtils.layout(context, unspecified, unspecified, TEXT, style, false)
            .first
            .height()

    val probeHeight =
        RichTextPrimitive(id = 1, text = TEXT, style = style, hasIntrinsicSize = true)
            .calculateMinIntrinsicHeight(
                LayoutContext(context, null, -1, LayoutCache(), null),
                SizeConstraints.fromMeasureSpecs(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST),
                ),
            )

    assertThat(probeHeight).isBetween(realHeight - 1, realHeight + 1)
  }

  private fun style(configure: TextStyle.() -> Unit): TextStyle =
      TextStyle().apply {
        textSize = 24
        configure()
      }

  @Test fun defaultStyle() = assertMinIntrinsicHeightMatchesRealLayout(style {})

  @Test
  fun excludeFontPadding() =
      assertMinIntrinsicHeightMatchesRealLayout(style { includeFontPadding = false })

  @Test
  fun lineHeightMultiplier() =
      assertMinIntrinsicHeightMatchesRealLayout(style { lineHeightMultiplier = 1.5f })

  @Test
  fun lineHeightMultiplierNoFontPadding() = assertMinIntrinsicHeightMatchesRealLayout(
      style {
        lineHeightMultiplier = 2.0f
        includeFontPadding = false
      },
  )

  @Test
  fun lineSpacingExtra() =
      assertMinIntrinsicHeightMatchesRealLayout(style { lineSpacingExtra = 8f })

  @Test
  fun fixedLineHeight() = assertMinIntrinsicHeightMatchesRealLayout(style { lineHeight = 40f })

  @Test
  fun fixedLineHeightNoFontPadding() = assertMinIntrinsicHeightMatchesRealLayout(
      style {
        lineHeight = 40f
        includeFontPadding = false
      },
  )

  @Test
  fun lineHeightMultiplierAppliedToFirstLine() = assertMinIntrinsicHeightMatchesRealLayout(
      style { TextStyle.maybeSetLineHeightMultiplier(this, 1.5f) },
  )

  @Test
  fun manualCapBaselineSpacing() = assertMinIntrinsicHeightMatchesRealLayout(
      style {
        manualCapSpacing = 10
        manualBaselineSpacing = 12
      },
  )

  @Test
  fun manualCapBaselineSpacingWithFirstLineMultiplier() = assertMinIntrinsicHeightMatchesRealLayout(
      style {
        manualCapSpacing = 10
        manualBaselineSpacing = 12
        TextStyle.maybeSetLineHeightMultiplier(this, 1.5f)
      },
  )

  companion object {
    // Mixed ascenders/descenders so the font's full ascent/descent extent is exercised.
    private const val TEXT = "Igjypq"
  }
}
