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

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.rendercore.text.ClickableSpanListener
import com.facebook.rendercore.text.TouchableSpanListener
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
 * Every case runs under both settings of [ComponentsConfiguration.usePrimitiveText]; the same
 * assertions passing against the legacy `TextComponent` and the Primitive `RichText` is what makes
 * them behaviorally interchangeable.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextPrimitiveBehaviorParityTest(private val usePrimitiveText: Boolean) {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private var previousUsePrimitiveText = false
  private val events = mutableListOf<String>()

  @Before
  fun setUp() {
    RuntimeEnvironment.setQualifiers(TextPrimitiveParityTest.DENSITY_QUALIFIER)
    previousUsePrimitiveText = ComponentsConfiguration.usePrimitiveText
    ComponentsConfiguration.usePrimitiveText = usePrimitiveText
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.usePrimitiveText = previousUsePrimitiveText
  }

  @Test
  fun `tapping a clickable span invokes its onClick`() {
    val testLithoView = renderClickableText()

    tap(testLithoView)

    assertThat(events).containsExactly(SPAN_CLICK)
  }

  @Test
  fun `clickable span listener is notified before the span is clicked`() {
    val testLithoView = renderClickableText(spanListener = recordingSpanListener(consume = false))

    tap(testLithoView)

    assertThat(events).containsExactly(LISTENER_CLICK, SPAN_CLICK)
  }

  @Test
  fun `clickable span listener consuming the click prevents the span onClick`() {
    val testLithoView = renderClickableText(spanListener = recordingSpanListener(consume = true))

    tap(testLithoView)

    assertThat(events).containsExactly(LISTENER_CLICK)
  }

  @Test
  fun `touchable span listener receives touch events on spans`() {
    val touchedActions = mutableListOf<Int>()
    val testLithoView = renderClickableText(
        touchableSpanListener =
            TouchableSpanListener { _, motionEvent, _ ->
              touchedActions.add(motionEvent.actionMasked)
              false
            },
    )

    tap(testLithoView)

    assertThat(touchedActions).contains(MotionEvent.ACTION_DOWN)
  }

  @Test
  fun `exposes rendered text to test helpers`() {
    val testLithoView = lithoTestRule.render { Text.create(context).text(TEXT).build() }

    LithoAssertions.assertThat(testLithoView).hasVisibleText(TEXT)
  }

  @Test
  fun `updates rendered text when the text prop changes`() {
    val testLithoView = lithoTestRule.render { Text.create(context).text(TEXT).build() }

    testLithoView.setRoot(Text.create(lithoTestRule.context).text(UPDATED_TEXT).build())
    lithoTestRule.idle()

    LithoAssertions.assertThat(testLithoView).hasVisibleText(UPDATED_TEXT)
    LithoAssertions.assertThat(testLithoView).doesNotHaveVisibleText(TEXT)
  }

  private fun renderClickableText(
      spanListener: ClickableSpanListener? = null,
      touchableSpanListener: TouchableSpanListener? = null,
  ): TestLithoView {
    val text = SpannableString(TEXT)
    text.setSpan(
        object : ClickableSpan() {
          override fun onClick(widget: View) {
            events.add(SPAN_CLICK)
          }
        },
        0,
        TEXT.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return lithoTestRule.render {
      Text.create(context)
          .text(text)
          .spanListener(spanListener)
          .touchableSpanListener(touchableSpanListener)
          .build()
    }
  }

  private fun recordingSpanListener(consume: Boolean): ClickableSpanListener =
      object : ClickableSpanListener {
        override fun onClick(span: ClickableSpan, view: View): Boolean {
          events.add(LISTENER_CLICK)
          return consume
        }
      }

  private fun tap(testLithoView: TestLithoView) {
    val lithoView = testLithoView.lithoView
    lithoView.dispatchTouchEvent(motionEvent(MotionEvent.ACTION_DOWN))
    lithoView.dispatchTouchEvent(motionEvent(MotionEvent.ACTION_UP))
  }

  private fun motionEvent(action: Int): MotionEvent =
      MotionEvent.obtain(0, 0, action, TAP_X, TAP_Y, 0)

  companion object {
    private const val TEXT = "hello world"
    private const val UPDATED_TEXT = "goodbye world"
    private const val TAP_X = 1f
    private const val TAP_Y = 1f
    private const val SPAN_CLICK = "span.onClick"
    private const val LISTENER_CLICK = "listener.onClick"

    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "usePrimitiveText={0}")
    fun parameters(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
  }
}
