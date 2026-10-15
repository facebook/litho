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

import android.content.Context
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import com.facebook.litho.AccessibilityUtils
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.accessibilityProbe
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.text.RCTextView
import com.facebook.rendercore.text.TextStyle
import com.facebook.widget.accessibility.delegates.AccessibleClickableSpan
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.GraphicsMode

/**
 * With truncation caching on, a truncated primitive Text still re-lays out when its props change.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextTruncationCacheTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private var previousUsePrimitiveText = false
  private var previousUseTruncationCaching = false

  @Before
  fun setUp() {
    RuntimeEnvironment.setQualifiers(TextPrimitiveParityTest.DENSITY_QUALIFIER)
    previousUsePrimitiveText = ComponentsConfiguration.usePrimitiveText
    previousUseTruncationCaching = RenderCoreConfig.useTruncationCaching
    ComponentsConfiguration.usePrimitiveText = true
    RenderCoreConfig.useTruncationCaching = true
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.usePrimitiveText = previousUsePrimitiveText
    RenderCoreConfig.useTruncationCaching = previousUseTruncationCaching
  }

  @Test
  fun `a new maxLines is applied`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) {
          text(context, maxLines = 2, alignment = TextAlignment.TEXT_START)
        }
    assertThat(mountedLayout(testLithoView.lithoView).lineCount).isEqualTo(2)

    testLithoView.setRoot(
        text(lithoTestRule.context, maxLines = 4, alignment = TextAlignment.TEXT_START),
    )
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).lineCount).isEqualTo(4)
  }

  @Test
  fun `a new alignment is applied`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) {
          text(context, maxLines = 2, alignment = TextAlignment.TEXT_START)
        }

    testLithoView.setRoot(
        text(lithoTestRule.context, maxLines = 2, alignment = TextAlignment.CENTER),
    )
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).alignment)
        .isEqualTo(Layout.Alignment.ALIGN_CENTER)
  }

  @Test
  fun `a new clickable span is used when the text is unchanged`() {
    val manager =
        RuntimeEnvironment.getApplication().getSystemService(Context.ACCESSIBILITY_SERVICE)
            as AccessibilityManager
    val shadowManager = Shadows.shadowOf(manager)
    shadowManager.setEnabled(true)
    shadowManager.setTouchExplorationEnabled(true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    try {
      val testLithoView =
          lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) {
            spannedText(context, label = "Old label")
          }

      testLithoView.setRoot(spannedText(lithoTestRule.context, label = "New label"))
      lithoTestRule.idle()

      val node = testLithoView.accessibilityProbe().nodes().single { it.isVirtual }.info()
      assertThat(node.contentDescription.toString()).isEqualTo("New label")
    } finally {
      shadowManager.setEnabled(false)
      shadowManager.setTouchExplorationEnabled(false)
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    }
  }

  @Test
  fun `the same style and shown text reuse the cached layout`() {
    val style = truncatedStyle()
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { richText(LONG_TEXT, style) }
    val first = mountedLayout(testLithoView.lithoView)

    // A change past the truncation point needs a new layout pass but shows the same text.
    testLithoView.setRoot(richText("$LONG_TEXT And more.", style))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView)).isSameAs(first)
  }

  @Test
  fun `a new style with other letter spacing is laid out again`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) {
          richText(LONG_TEXT, truncatedStyle())
        }

    testLithoView.setRoot(richText(LONG_TEXT, truncatedStyle().apply { letterSpacing = 0.2f }))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).paint.letterSpacing).isEqualTo(0.2f)
  }

  @Test
  fun `a new non-clickable span on the same characters is drawn`() {
    val style = truncatedStyle()
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { richText(LONG_TEXT, style) }

    val bold =
        SpannableString(LONG_TEXT).apply {
          setSpan(StyleSpan(Typeface.BOLD), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    testLithoView.setRoot(richText(bold, style))
    lithoTestRule.idle()

    val drawn = mountedLayout(testLithoView.lithoView).text as Spanned
    assertThat(drawn.getSpans(0, 4, StyleSpan::class.java)).hasSize(1)
  }

  @Test
  fun `a new text size on the same style object is laid out again`() {
    val style = truncatedStyle()
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { richText(LONG_TEXT, style) }

    // Same style object, changed in place, and a change past the cut so it lays out again.
    style.textSize = 52
    testLithoView.setRoot(richText("$LONG_TEXT And more.", style))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).paint.textSize).isEqualTo(52f)
  }

  @Test
  fun `new max lines on the same style object are laid out again`() {
    val style = truncatedStyle()
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { richText(LONG_TEXT, style) }

    style.maxLines = 4
    testLithoView.setRoot(richText("$LONG_TEXT And more.", style))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).lineCount).isEqualTo(4)
  }

  @Test
  fun `a new click span in the custom ellipsis is used`() {
    val first = ClickableSpanProbe()
    val style = truncatedStyle().apply { customEllipsisText = suffixWith(first) }
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { richText(LONG_TEXT, style) }

    // Same style object (so the cache may reuse) and a change past the cut (so it lays out again).
    val second = ClickableSpanProbe()
    style.customEllipsisText = suffixWith(second)
    testLithoView.setRoot(richText("$LONG_TEXT And more.", style))
    lithoTestRule.idle()

    val drawn = mountedLayout(testLithoView.lithoView).text as Spanned
    assertThat(drawn.getSpans(0, drawn.length, ClickableSpanProbe::class.java))
        .containsExactly(second)
  }

  private class ClickableSpanProbe : ClickableSpan() {
    override fun onClick(widget: View): Unit = Unit
  }

  private fun suffixWith(span: ClickableSpan): CharSequence =
      SpannableString(" more").apply { setSpan(span, 1, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }

  @Test
  fun `turning the custom ellipsis offset fix on or off lays the text out again`() {
    val style = truncatedStyle()
    for (startWithFix in listOf(false, true)) {
      val testLithoView =
          lithoTestRule.render(widthPx = OFFSET_FIX_WIDTH_PX, heightPx = 2000) {
            richText(LONG_TEXT, style, useOffsetFix = startWithFix, performant = true)
          }

      testLithoView.setRoot(
          richText(LONG_TEXT, style, useOffsetFix = !startWithFix, performant = true),
      )
      lithoTestRule.idle()
      val toggled = mountedLayout(testLithoView.lithoView).text.toString()

      val fresh =
          lithoTestRule.render(widthPx = OFFSET_FIX_WIDTH_PX, heightPx = 2000) {
            richText(LONG_TEXT, truncatedStyle(), useOffsetFix = !startWithFix, performant = true)
          }
      assertThat(toggled)
          .describedAs("offset fix %s -> %s", startWithFix, !startWithFix)
          .isEqualTo(mountedLayout(fresh.lithoView).text.toString())
    }
  }

  // As Litho Text configures it: resolved direction on, with a caller-owned style.
  private fun richText(
      text: CharSequence,
      style: TextStyle,
      useOffsetFix: Boolean = false,
      performant: Boolean = RenderCoreConfig.usePerformantTruncation,
  ): Component = RichText(
      text = text,
      textStyle = style,
      useTruncationCaching = true,
      useResolvedLayoutDirection = true,
      usePerformantTruncation = performant,
      useLegacyTextCustomEllipsisOffsetFix = useOffsetFix,
  )

  private fun truncatedStyle(): TextStyle =
      TextStyle().apply {
        textSize = 40
        maxLines = 2
        customEllipsisText = " more"
      }

  private fun spannedText(c: ComponentContext, label: String): Component {
    val span =
        object : AccessibleClickableSpan(label) {
          override fun onClick(widget: View) = Unit
        }
    val text = SpannableString(LONG_TEXT).apply { setSpan(span, 0, 4, 0) }
    return Text.create(c)
        .text(text)
        .customEllipsisText(" more")
        .maxLines(2)
        .accessibleClickableSpans(true)
        .build()
  }

  private fun text(c: ComponentContext, maxLines: Int, alignment: TextAlignment): Component =
      Text.create(c)
          .text(LONG_TEXT)
          .customEllipsisText(" more")
          .maxLines(maxLines)
          .alignment(alignment)
          .build()

  private fun mountedLayout(view: View): Layout = checkNotNull(findRCTextView(view)?.layout)

  private fun findRCTextView(view: View): RCTextView? {
    if (view is RCTextView) {
      return view
    }
    if (view !is ViewGroup) {
      return null
    }
    return (0 until view.childCount).firstNotNullOfOrNull { findRCTextView(view.getChildAt(it)) }
  }

  private companion object {
    const val WIDTH_PX = 1080
    // A width at which the offset fix moves the cut (found by sweeping widths for LONG_TEXT).
    const val OFFSET_FIX_WIDTH_PX = 300
    const val LONG_TEXT =
        "Text that extends over many lines like lorem ipsum dolor so I'll just keep typing for a " +
            "while to make sure that it wraps a couple of times, even on wide screens. And then " +
            "some more so that four lines are not enough to hold all of it at this width, really."
  }
}
