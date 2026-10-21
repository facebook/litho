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

package com.facebook.litho.widget

import android.text.Layout
import android.text.TextUtils
import android.view.View
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * Legacy Text ([TextComponentSpec]) keeps a one-line custom ellipsis on the last visible line for
 * RTL captions with emoji, hashtags and a mid-text ellipsis, when
 * [ComponentsConfiguration.fitCustomEllipsisOnTruncatedLine] is on.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextLegacyCustomEllipsisFitTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private var previousUsePrimitiveText = false
  private var previousFixRtl = false
  private var previousOffsetFix = false
  private var previousFit = false

  @Before
  fun setUp() {
    RuntimeEnvironment.setQualifiers(TextPrimitiveParityTest.DENSITY_QUALIFIER)
    previousUsePrimitiveText = ComponentsConfiguration.usePrimitiveText
    previousFixRtl = ComponentsConfiguration.fixRtlCustomEllipsisTruncation
    previousOffsetFix = ComponentsConfiguration.enableFixForTextEllipsisOffset
    previousFit = ComponentsConfiguration.fitCustomEllipsisOnTruncatedLine
    ComponentsConfiguration.usePrimitiveText = false
    ComponentsConfiguration.fixRtlCustomEllipsisTruncation = true
    ComponentsConfiguration.enableFixForTextEllipsisOffset = false
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.usePrimitiveText = previousUsePrimitiveText
    ComponentsConfiguration.fixRtlCustomEllipsisTruncation = previousFixRtl
    ComponentsConfiguration.enableFixForTextEllipsisOffset = previousOffsetFix
    ComponentsConfiguration.fitCustomEllipsisOnTruncatedLine = previousFit
  }

  @Test
  fun `with the fit on the custom ellipsis ends the single visible line at every width`() {
    ComponentsConfiguration.fitCustomEllipsisOnTruncatedLine = true
    val broken = CAPTIONS.flatMap { (name, caption) ->
      WIDTHS_PX.mapNotNull { width ->
        val shown = render(caption, width)
        val text = shown.text.toString()
        if (text == caption) {
          null
        } else if (shown.lineCount != MAX_LINES || !text.endsWith(SUFFIX)) {
          "$name @${width}px: lines=${shown.lineCount} \"$text\""
        } else {
          null
        }
      }
    }
    assertThat(broken).isEmpty()
  }

  @Test
  fun `the fit changes nothing where the custom ellipsis already fits`() {
    val before = CAPTIONS.flatMap { (_, caption) -> WIDTHS_PX.map { render(caption, it) } }
    ComponentsConfiguration.fitCustomEllipsisOnTruncatedLine = true
    val after = CAPTIONS.flatMap { (_, caption) -> WIDTHS_PX.map { render(caption, it) } }

    val changed =
        before.indices.filter { i ->
          before[i].lineCount <= MAX_LINES && before[i].text.toString() != after[i].text.toString()
        }
    assertThat(changed).isEmpty()
  }

  @Test
  fun `with the fit off the rtl custom ellipsis wraps onto a clipped line`() {
    ComponentsConfiguration.fitCustomEllipsisOnTruncatedLine = false
    val overflowing = WIDTHS_PX.filter { width ->
      val shown = render(RTL_CAPTION, width)
      shown.lineCount > MAX_LINES && shown.text.toString().endsWith(SUFFIX)
    }
    assertThat(overflowing).isNotEmpty()
  }

  private fun render(text: CharSequence, widthPx: Int): Layout {
    val lithoView =
        lithoTestRule
            .render(
                widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            ) {
              Text.create(context)
                  .text(text)
                  .maxLines(MAX_LINES)
                  .ellipsize(TextUtils.TruncateAt.END)
                  .customEllipsisText(SUFFIX)
                  .build()
            }
            .lithoView
    return checkNotNull(lithoView.drawables.filterIsInstance<TextDrawable>().first().layout)
  }

  companion object {
    private const val MAX_LINES = 1
    private const val SUFFIX = "... more"
    private val WIDTHS_PX = (300..1080 step 7).toList()

    private const val RTL_CAPTION =
        "الكل بيحكي إنو #سرمدا🇸🇾📍 متطورة… اليوم ما رح احكيلك🚧رح فرجيك🏡"
    private const val HEBREW_CAPTION = "טקסט #בעברית 👍🏽 שממשיך… לשורות רבות מאוד בכל הרוחב"
    private const val LTR_CAPTION =
        "Chocolate Chip Recipe ⬇️ #baking with friends 🎉 and a long caption " +
            "that keeps going… so it wraps past one line on every width we try here."

    private val CAPTIONS: List<Pair<String, String>> =
        listOf("arabic" to RTL_CAPTION, "hebrew" to HEBREW_CAPTION, "ltr" to LTR_CAPTION)
  }
}
