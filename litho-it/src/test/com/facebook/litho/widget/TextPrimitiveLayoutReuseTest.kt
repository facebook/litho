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

import android.graphics.Color
import android.text.Layout
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoTestRule
import com.facebook.rendercore.RenderCoreConfig
import com.facebook.rendercore.text.RCTextView
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
 * A re-render of primitive Text with equal props keeps its measured layout, like legacy
 * TextComponent, and one with changed props lays out again. Truncation caching is off, so a reused
 * layout comes from the layout equivalence rather than from that cache.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextPrimitiveLayoutReuseTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private var previousUsePrimitiveText = false
  private var previousUseTruncationCaching = false

  @Before
  fun setUp() {
    RuntimeEnvironment.setQualifiers(TextPrimitiveParityTest.DENSITY_QUALIFIER)
    previousUsePrimitiveText = ComponentsConfiguration.usePrimitiveText
    previousUseTruncationCaching = RenderCoreConfig.useTruncationCaching
    ComponentsConfiguration.usePrimitiveText = true
    RenderCoreConfig.useTruncationCaching = false
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.usePrimitiveText = previousUsePrimitiveText
    RenderCoreConfig.useTruncationCaching = previousUseTruncationCaching
  }

  @Test
  fun `a re-render with equal props keeps the layout`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { text(context, textSize = 40) }
    val first = mountedLayout(testLithoView.lithoView)

    testLithoView.setRoot(text(lithoTestRule.context, textSize = 40))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView)).isSameAs(first)
  }

  @Test
  fun `a new text size lays out again`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { text(context, textSize = 40) }

    testLithoView.setRoot(text(lithoTestRule.context, textSize = 52))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).paint.textSize).isEqualTo(52f)
  }

  @Test
  fun `a new text color is applied`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { text(context, textSize = 40) }

    testLithoView.setRoot(text(lithoTestRule.context, textSize = 40, textColor = Color.BLUE))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).paint.color).isEqualTo(Color.BLUE)
  }

  @Test
  fun `new text lays out again`() {
    val testLithoView =
        lithoTestRule.render(widthPx = WIDTH_PX, heightPx = 2000) { text(context, textSize = 40) }

    testLithoView.setRoot(text(lithoTestRule.context, textSize = 40, text = "Goodbye"))
    lithoTestRule.idle()

    assertThat(mountedLayout(testLithoView.lithoView).text.toString()).isEqualTo("Goodbye")
  }

  private fun text(
      c: ComponentContext,
      textSize: Int,
      textColor: Int = Color.BLACK,
      text: String = "Hello world",
  ): Component = Text.create(c).text(text).textSizePx(textSize).textColor(textColor).build()

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
  }
}
