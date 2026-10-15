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

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import androidx.core.text.TextDirectionHeuristicsCompat
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
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
 * Renders every [Text] scenario under every width constraint through both implementations selected
 * by [ComponentsConfiguration.usePrimitiveText] (legacy `TextComponent` and Primitive `RichText`)
 * and asserts they agree on size, line layout and text appearance. NATIVE graphics gives real font
 * metrics; Robolectric's legacy graphics measures ~1px per character and never wraps realistically.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextPrimitiveParityTest(
    private val scenario: Scenario,
    private val constraint: WidthConstraint,
) {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private var previousUsePrimitiveText = false

  @Before
  fun setUp() {
    RuntimeEnvironment.setQualifiers(DENSITY_QUALIFIER)
    previousUsePrimitiveText = ComponentsConfiguration.usePrimitiveText
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.usePrimitiveText = previousUsePrimitiveText
  }

  @Test
  fun `measures and mounts at the same size`() {
    assertThat(render(usePrimitiveText = true).size)
        .isEqualTo(render(usePrimitiveText = false).size)
  }

  @Test
  fun `breaks and positions lines identically`() {
    assertThat(render(usePrimitiveText = true).lineLayout)
        .isEqualTo(render(usePrimitiveText = false).lineLayout)
  }

  @Test
  fun `renders the same text with the same paint`() {
    assertThat(render(usePrimitiveText = true).appearance)
        .isEqualTo(render(usePrimitiveText = false).appearance)
  }

  private fun render(usePrimitiveText: Boolean): RenderedText {
    ComponentsConfiguration.usePrimitiveText = usePrimitiveText
    val lithoView =
        lithoTestRule
            .render(widthSpec = constraint.widthSpec(), heightSpec = unspecifiedSpec()) {
              scenario.build(context)
            }
            .lithoView
    return RenderedText.from(lithoView)
  }

  class Scenario(private val name: String, val build: (ComponentContext) -> Component) {
    override fun toString(): String = name
  }

  // Specs are built lazily: parameters are listed outside the Robolectric sandbox, where android.*
  // calls throw "Stub!".
  enum class WidthConstraint(private val sizePx: Int, private val mode: Int) {
    EXACTLY_WIDE(WIDE_PX, View.MeasureSpec.EXACTLY),
    AT_MOST_WIDE(WIDE_PX, View.MeasureSpec.AT_MOST),
    AT_MOST_NARROW(NARROW_PX, View.MeasureSpec.AT_MOST),
    UNSPECIFIED(0, View.MeasureSpec.UNSPECIFIED);

    fun widthSpec(): Int = View.MeasureSpec.makeMeasureSpec(sizePx, mode)
  }

  data class Size(val width: Int, val height: Int, val contentBounds: Rect?)

  data class Line(
      val start: Int,
      val end: Int,
      val top: Int,
      val bottom: Int,
      val baseline: Int,
      val left: Float,
      val right: Float,
      val ellipsisStart: Int,
      val ellipsisCount: Int,
      val paragraphDirection: Int,
  )

  data class LineLayout(val width: Int?, val height: Int?, val lines: List<Line>)

  data class Appearance(
      val text: String?,
      val spans: List<String>,
      val textSize: Float?,
      val color: Int?,
      val linkColor: Int?,
      val typefaceStyle: Int?,
      val isFakeBold: Boolean?,
      val textSkewX: Float?,
      val letterSpacing: Float?,
      val shadow: List<Number>?,
  )

  private class RenderedText(
      val size: Size,
      val lineLayout: LineLayout,
      val appearance: Appearance,
  ) {
    companion object {
      fun from(lithoView: LithoView): RenderedText {
        val mounted = findMountedText(lithoView)
        val layout = mounted?.layout
        return RenderedText(
            size = Size(lithoView.measuredWidth, lithoView.measuredHeight, mounted?.bounds),
            lineLayout =
                LineLayout(
                    width = layout?.width,
                    height = layout?.height,
                    lines = layout?.let(::linesOf).orEmpty(),
                ),
            appearance = appearanceOf(layout),
        )
      }

      private fun linesOf(layout: Layout): List<Line> =
          (0 until layout.lineCount).map { i ->
            Line(
                start = layout.getLineStart(i),
                end = layout.getLineEnd(i),
                top = layout.getLineTop(i),
                bottom = layout.getLineBottom(i),
                baseline = layout.getLineBaseline(i),
                left = layout.getLineLeft(i),
                right = layout.getLineRight(i),
                ellipsisStart = layout.getEllipsisStart(i),
                ellipsisCount = layout.getEllipsisCount(i),
                paragraphDirection = layout.getParagraphDirection(i),
            )
          }

      private fun appearanceOf(layout: Layout?): Appearance {
        val paint = layout?.paint
        return Appearance(
            text = layout?.text?.toString(),
            spans = layout?.text?.let(::userSpansOf).orEmpty(),
            textSize = paint?.textSize,
            color = paint?.color,
            linkColor = paint?.linkColor,
            typefaceStyle = paint?.typeface?.style,
            isFakeBold = paint?.isFakeBoldText,
            textSkewX = paint?.textSkewX,
            letterSpacing = paint?.letterSpacing,
            shadow = paint?.let(::shadowOf),
        )
      }

      // Robolectric runs these tests on an SDK with the shadow-layer getters.
      private fun shadowOf(paint: Paint): List<Number>? =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                paint.shadowLayerRadius,
                paint.shadowLayerDx,
                paint.shadowLayerDy,
                paint.shadowLayerColor,
            )
          } else {
            null
          }

      private fun userSpansOf(text: CharSequence): List<String> {
        if (text !is Spanned) {
          return emptyList()
        }
        return text
            .getSpans(0, text.length, Any::class.java)
            .mapNotNull { span ->
              val type =
                  when (span) {
                    is ClickableSpan -> "ClickableSpan"
                    is StyleSpan -> "StyleSpan(${span.style})"
                    is AbsoluteSizeSpan -> "AbsoluteSizeSpan(${span.size})"
                    is ForegroundColorSpan -> "ForegroundColorSpan(${span.foregroundColor})"
                    else -> null
                  } ?: return@mapNotNull null
              "$type[${text.getSpanStart(span)}, ${text.getSpanEnd(span)})"
            }
            .sorted()
      }
    }
  }

  private class MountedText(val layout: Layout?, val bounds: Rect)

  companion object {
    // Phone density, so sp/dp props resolve to realistic pixel sizes.
    internal const val DENSITY_QUALIFIER = "+xxhdpi"
    private const val WIDE_PX = 1080
    private const val NARROW_PX = 240

    private const val SHORT_TEXT = "Facebook"
    private const val LONG_TEXT =
        "Text that extends over many lines like lorem ipsum dolor so I'll just keep typing for a " +
            "while to make sure that it wraps a couple of times, even on wide screens."
    private const val RTL_TEXT =
        "\u05D8\u05E7\u05E1\u05D8 \u05D1\u05E2\u05D1\u05E8\u05D9\u05EA " +
            "\u05E9\u05DE\u05DE\u05E9\u05D9\u05DA \u05DC\u05E9\u05D5\u05E8\u05D5\u05EA \u05E8\u05D1\u05D5\u05EA"
    private const val EMOJI_TEXT =
        "Party \uD83C\uDF89 time \uD83D\uDC4D\uD83C\uDFFD with friends " +
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"
    // No strong directional characters, so only the layout direction decides its direction.
    private const val NEUTRAL_TEXT = "1234 5678 90"
    private const val MIXED_DIRECTION_TEXT =
        "Hello \u05E9\u05DC\u05D5\u05DD world \u05E2\u05D5\u05DC\u05DD"

    private fun unspecifiedSpec(): Int =
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    private fun findMountedText(lithoView: LithoView): MountedText? {
      lithoView.drawables.filterIsInstance<TextDrawable>().firstOrNull()?.let {
        return MountedText(it.layout, Rect(it.bounds))
      }
      return findRCTextView(lithoView)?.let {
        MountedText(it.layout, Rect(it.left, it.top, it.right, it.bottom))
      }
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

    private fun scenario(
        name: String,
        text: () -> CharSequence? = { LONG_TEXT },
        configure: Text.Builder.() -> Unit = {},
    ): Scenario = Scenario(name) { c -> Text.create(c).text(text()).apply(configure).build() }

    private fun spanned(text: String, vararg spans: Pair<Any, IntRange>): CharSequence {
      val spannable = SpannableString(text)
      spans.forEach { (span, range) ->
        spannable.setSpan(span, range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }
      return spannable
    }

    private val SCENARIOS: List<Scenario> = listOf(
        // Content
        scenario("short text", { SHORT_TEXT }),
        scenario("long wrapping text"),
        scenario("explicit newlines", { "Line one\nLine two\n\nLine four" }),
        scenario("trailing newline and spaces", { "Ends with whitespace   \n" }),
        scenario("empty text", { "" }),
        scenario("null text", { null }),
        scenario("unbreakable word", { "Supercalifragilisticexpialidocious".repeat(4) }),
        scenario("emoji", { EMOJI_TEXT }),
        // Line limits and truncation
        scenario("max lines 2 ellipsize end") {
          maxLines(2)
          ellipsize(TextUtils.TruncateAt.END)
        },
        scenario("max lines 2 without ellipsize") { maxLines(2) },
        scenario("max lines 2 custom ellipsis") {
          maxLines(2)
          ellipsize(TextUtils.TruncateAt.END)
          customEllipsisText("\u2026 See more")
        },
        // Matches FDS's RTL screenshot: an LTR body inside an RTL subtree.
        scenario("rtl subtree with ltr custom ellipsis") {
          maxLines(2)
          ellipsize(TextUtils.TruncateAt.END)
          customEllipsisText("\u2026 See more")
          layoutDirection(YogaDirection.RTL)
        },
        scenario("single line ellipsize end") {
          isSingleLine(true)
          ellipsize(TextUtils.TruncateAt.END)
        },
        scenario("single line ellipsize start") {
          isSingleLine(true)
          ellipsize(TextUtils.TruncateAt.START)
        },
        scenario("single line ellipsize middle") {
          isSingleLine(true)
          ellipsize(TextUtils.TruncateAt.MIDDLE)
        },
        scenario("min lines 3 on short text", { SHORT_TEXT }) { minLines(3) },
        scenario("min lines 3 max lines 1") {
          minLines(3)
          maxLines(1)
        },
        // Font
        scenario("text size 48px") { textSizePx(48) },
        scenario("text size 10sp") { textSizeSp(10f) },
        scenario("bold") { textStyle(Typeface.BOLD) },
        scenario("italic") { textStyle(Typeface.ITALIC) },
        scenario("monospace typeface") { typeface(Typeface.MONOSPACE) },
        scenario("letter spacing") { letterSpacing(0.1f) },
        // Line spacing
        scenario("spacing multiplier 1.5") { spacingMultiplier(1.5f) },
        scenario("extra spacing 12px") { extraSpacingPx(12f) },
        scenario("line height 60px") {
          textSizePx(24)
          lineHeightPx(60f)
        },
        scenario("line height with min lines", { SHORT_TEXT }) {
          textSizePx(24)
          lineHeightPx(50f)
          minLines(3)
        },
        scenario("no font padding") { shouldIncludeFontPadding(false) },
        // Alignment and direction
        scenario("alignment center") { alignment(TextAlignment.CENTER) },
        scenario("alignment text end") { alignment(TextAlignment.TEXT_END) },
        scenario("alignment layout end") { alignment(TextAlignment.LAYOUT_END) },
        scenario("alignment right") { alignment(TextAlignment.RIGHT) },
        scenario("deprecated text alignment opposite") {
          textAlignment(Layout.Alignment.ALIGN_OPPOSITE)
        },
        scenario("rtl text", { RTL_TEXT }),
        // TEXT_START/TEXT_END align to the text's own direction, not the locale's.
        scenario("rtl text aligned to text start", { RTL_TEXT }) {
          alignment(TextAlignment.TEXT_START)
        },
        scenario("rtl text aligned to text end", { RTL_TEXT }) {
          alignment(TextAlignment.TEXT_END)
        },
        scenario("rtl text with rtl direction", { RTL_TEXT }) {
          textDirection(TextDirectionHeuristicsCompat.RTL)
        },
        scenario("mixed direction first strong", { MIXED_DIRECTION_TEXT }) {
          textDirection(TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR)
        },
        scenario("rtl text ellipsized", { RTL_TEXT.repeat(3) }) {
          maxLines(1)
          ellipsize(TextUtils.TruncateAt.END)
        },
        // A subtree whose layout direction differs from the app's locale
        scenario("rtl subtree with neutral text", { NEUTRAL_TEXT }) {
          layoutDirection(YogaDirection.RTL)
        },
        scenario("rtl subtree with latin text") { layoutDirection(YogaDirection.RTL) },
        scenario("rtl subtree with layout start") {
          layoutDirection(YogaDirection.RTL)
          alignment(TextAlignment.LAYOUT_START)
        },
        scenario("rtl subtree with layout end") {
          layoutDirection(YogaDirection.RTL)
          alignment(TextAlignment.LAYOUT_END)
        },
        scenario("rtl subtree with text end") {
          layoutDirection(YogaDirection.RTL)
          alignment(TextAlignment.TEXT_END)
        },
        scenario("ltr subtree with rtl text", { RTL_TEXT }) { layoutDirection(YogaDirection.LTR) },
        // Line breaking
        scenario("break strategy simple") { breakStrategy(Layout.BREAK_STRATEGY_SIMPLE) },
        scenario("break strategy high quality") {
          breakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
        },
        scenario("hyphenation full") {
          hyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL)
        },
        scenario("justified inter word") {
          justificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
        },
        // Width modifiers
        scenario("min ems on short text", { SHORT_TEXT }) { minEms(12) },
        scenario("max ems") { maxEms(8) },
        scenario("min text width on short text", { SHORT_TEXT }) { minTextWidthPx(400) },
        scenario("max text width") { maxTextWidthPx(200) },
        scenario("minimally wide") { minimallyWide(true) },
        scenario("minimally wide with threshold") {
          minimallyWide(true)
          minimallyWideThresholdPx(20)
        },
        // Colors and decorations. Draw-time props (highlight, outline, clipToBounds, vertical
        // gravity, clickable span expanded offset) leave the layout untouched, so they're covered
        // by
        // their own tests and the comparison screenshots instead.
        scenario("text color") { textColor(Color.RED) },
        scenario("text color state list") {
          textColorStateList(ColorStateList.valueOf(Color.MAGENTA))
        },
        scenario("link color") { linkColor(Color.GREEN) },
        scenario("shadow") {
          shadowRadiusPx(4f)
          shadowDxPx(2f)
          shadowDyPx(2f)
          shadowColor(Color.BLACK)
        },
        // Spans
        scenario(
            "styled spans",
            {
              spanned(
                  LONG_TEXT,
                  StyleSpan(Typeface.BOLD) to 0..3,
                  ForegroundColorSpan(Color.RED) to 10..20,
              )
            },
        ),
        scenario(
            "large absolute size span",
            { spanned(LONG_TEXT, AbsoluteSizeSpan(64) to 5..12) },
        ),
        scenario(
            "clickable span",
            {
              spanned(
                  LONG_TEXT,
                  object : ClickableSpan() {
                    override fun onClick(widget: View) = Unit
                  } to 0..9,
              )
            },
        ),
        scenario(
            "spans with ellipsize",
            { spanned(LONG_TEXT, StyleSpan(Typeface.BOLD) to 60..80) },
        ) {
          maxLines(1)
          ellipsize(TextUtils.TruncateAt.END)
        },
    )

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0} @ {1}")
    fun parameters(): Collection<Array<Any>> = SCENARIOS.flatMap { scenario ->
      WidthConstraint.entries.map { constraint -> arrayOf<Any>(scenario, constraint) }
    }
  }
}
