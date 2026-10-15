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
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.LithoView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoTestRule
import com.facebook.rendercore.text.RCTextView
import com.facebook.yoga.YogaDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * Primitive Text shows a custom ellipsis whole, with no Android ellipsis drawn over it, at every
 * width, with legacy Text's custom-ellipsis offset fix on and off.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextCustomEllipsisTest(private val enableFixForTextEllipsisOffset: Boolean) {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private var previousUsePrimitiveText = false
  private var previousEnableFix = false

  @Before
  fun setUp() {
    RuntimeEnvironment.setQualifiers(TextPrimitiveParityTest.DENSITY_QUALIFIER)
    previousUsePrimitiveText = ComponentsConfiguration.usePrimitiveText
    previousEnableFix = ComponentsConfiguration.enableFixForTextEllipsisOffset
    ComponentsConfiguration.usePrimitiveText = true
    ComponentsConfiguration.enableFixForTextEllipsisOffset = enableFixForTextEllipsisOffset
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.usePrimitiveText = previousUsePrimitiveText
    ComponentsConfiguration.enableFixForTextEllipsisOffset = previousEnableFix
  }

  @Test
  fun `the custom ellipsis is shown whole at every width`() {
    val broken = TEXTS.flatMap { (name, text, direction) ->
      WIDTHS_PX.mapNotNull { width ->
        val shown = render(text, direction, width)
        val hasPlatformEllipsis = (0 until shown.lineCount).any { shown.getEllipsisCount(it) > 0 }
        val text = shown.text.toString()
        if (!text.endsWith(SUFFIX) || hasPlatformEllipsis) "$name @${width}px: $text" else null
      }
    }

    assertThat(broken).isEmpty()
  }

  @Test
  fun `the offset fix keeps the character before the ellipsis`() {
    val shown = render(LONG_TEXT, YogaDirection.INHERIT, widthPx = 300).text.toString()

    assertThat(shown)
        .isEqualTo(
            if (enableFixForTextEllipsisOffset) "Text that exte$SUFFIX" else "Text that ext$SUFFIX",
        )
  }

  @Test
  fun `a custom ellipsis that fits is shown whole for text with larger spans`() {
    val broken = SPAN_SCALES.flatMap { scale ->
      SPAN_WIDTHS_PX.mapNotNull { width ->
        val text =
            SpannableString("i".repeat(1000)).apply {
              setSpan(RelativeSizeSpan(scale), 0, 500, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val shown = render(
            text,
            YogaDirection.INHERIT,
            width,
            maxLines = 1,
            suffix = LONG_SUFFIX,
            textSizePx = 20,
        )
        val hasPlatformEllipsis = (0 until shown.lineCount).any { shown.getEllipsisCount(it) > 0 }
        if (!shown.text.toString().endsWith(LONG_SUFFIX) || hasPlatformEllipsis) {
          "scale $scale @${width}px: ${shown.text.toString().takeLast(40)}"
        } else {
          null
        }
      }
    }

    assertThat(broken).isEmpty()
  }

  @Test
  fun `a fitting custom ellipsis is shown whole and never splits a combining grapheme`() {
    // With the fix off, truncation is the unchanged RenderCore path, which cuts by UTF-16 unit.
    if (!enableFixForTextEllipsisOffset) {
      return
    }
    val broken = SPAN_SCALES.flatMap { scale ->
      listOf(700, 1000).mapNotNull { width ->
        val text =
            SpannableString("i\u0301".repeat(1000)).apply {
              setSpan(RelativeSizeSpan(scale), 0, 1000, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        val shown = render(
            text,
            YogaDirection.INHERIT,
            width,
            maxLines = 1,
            suffix = LONG_SUFFIX,
            textSizePx = 20,
        )
        val shownText = shown.text.toString()
        val hasPlatformEllipsis = (0 until shown.lineCount).any { shown.getEllipsisCount(it) > 0 }
        val prefixLength = shownText.length - LONG_SUFFIX.length
        // Every grapheme is "i" plus its accent, two chars.
        val splitsGrapheme = prefixLength % 2 != 0
        if (!shownText.endsWith(LONG_SUFFIX) || hasPlatformEllipsis || splitsGrapheme) {
          "scale $scale @${width}px: prefix $prefixLength, ${shownText.takeLast(40)}"
        } else {
          null
        }
      }
    }

    assertThat(broken).isEmpty()
  }

  @Test
  fun `a fitting multiline custom ellipsis survives stepping back across a line`() {
    if (!enableFixForTextEllipsisOffset) {
      return
    }
    val broken = mutableListOf<String>()
    var fittingSuffixes = 0
    for (maxLines in listOf(2, 3)) {
      for (width in listOf(400, 700, 1000)) {
        for (repeats in 2..6) {
          val suffix = LONG_SUFFIX.repeat(repeats)
          val suffixLayout = render(suffix, YogaDirection.INHERIT, width, maxLines, suffix, 20)
          if ((0 until suffixLayout.lineCount).any { suffixLayout.getEllipsisCount(it) > 0 }) {
            continue
          }
          fittingSuffixes++
          val text =
              SpannableString("i\u0301".repeat(1000)).apply {
                setSpan(RelativeSizeSpan(2f), 0, 1000, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
              }
          val shown = render(text, YogaDirection.INHERIT, width, maxLines, suffix, 20)
          val shownText = shown.text.toString()
          val splitsGrapheme = (shownText.length - suffix.length) % 2 != 0
          if (
              !shownText.endsWith(suffix) ||
                  (0 until shown.lineCount).any { shown.getEllipsisCount(it) > 0 } ||
                  splitsGrapheme
          ) {
            broken.add("lines $maxLines width $width repeats $repeats")
          }
        }
      }
    }
    assertThat(fittingSuffixes).isGreaterThan(0)
    assertThat(broken).isEmpty()
  }

  private fun render(
      text: CharSequence,
      direction: YogaDirection,
      widthPx: Int,
      maxLines: Int = 2,
      suffix: String = SUFFIX,
      textSizePx: Int? = null,
  ): Layout {
    val lithoView: LithoView =
        lithoTestRule
            .render(
                widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            ) {
              Text.create(context)
                  .text(text)
                  .maxLines(maxLines)
                  .apply { textSizePx?.let { textSizePx(it) } }
                  .ellipsize(TextUtils.TruncateAt.END)
                  .customEllipsisText(suffix)
                  .layoutDirection(direction)
                  .build()
            }
            .lithoView
    return checkNotNull(findRCTextView(lithoView)?.layout)
  }

  private fun findRCTextView(view: View): RCTextView? {
    if (view is RCTextView) {
      return view
    }
    if (view !is ViewGroup) {
      return null
    }
    return (0 until view.childCount).firstNotNullOfOrNull { findRCTextView(view.getChildAt(it)) }
  }

  companion object {
    private const val SUFFIX = "\u2026 See more"
    private const val LONG_SUFFIX = "\u2026 Continue reading this article"
    private val SPAN_SCALES = listOf(1.5f, 2f)
    private val SPAN_WIDTHS_PX = listOf(400, 700, 1000)
    private val WIDTHS_PX = (300..1080 step 37).toList()

    private const val LONG_TEXT =
        "Text that extends over many lines like lorem ipsum dolor so I'll just keep typing for a " +
            "while to make sure that it wraps a couple of times, even on wide screens."
    private val HEBREW_TEXT =
        ("\u05D8\u05E7\u05E1\u05D8 \u05D1\u05E2\u05D1\u05E8\u05D9\u05EA " +
                "\u05E9\u05DE\u05DE\u05E9\u05D9\u05DA \u05DC\u05E9\u05D5\u05E8\u05D5\u05EA " +
                "\u05E8\u05D1\u05D5\u05EA ")
            .repeat(4)

    private val TEXTS: List<Triple<String, String, YogaDirection>> = listOf(
        Triple("ltr", LONG_TEXT, YogaDirection.INHERIT),
        Triple("ltr in rtl subtree", LONG_TEXT, YogaDirection.RTL),
        Triple("rtl", HEBREW_TEXT, YogaDirection.INHERIT),
    )

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "enableFixForTextEllipsisOffset={0}")
    fun parameters(): Collection<Array<Any>> = listOf(arrayOf(true), arrayOf(false))
  }
}
