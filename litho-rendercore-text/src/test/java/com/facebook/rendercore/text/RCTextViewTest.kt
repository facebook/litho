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

import android.text.Spannable
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import com.facebook.rendercore.DefaultNode
import com.facebook.rendercore.DefaultTextNode
import com.facebook.rendercore.HostView
import com.facebook.rendercore.YogaProps
import com.facebook.rendercore.testing.RenderCoreTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RCTextViewTest {

  @Rule @JvmField val renderCoreTestRule = RenderCoreTestRule()

  private fun setupClickableSpanTest(
      shouldSpanBeNull: Boolean = false
  ): Pair<ArrayList<Int>, RCTextView> {
    val eventsFired = ArrayList<Int>()

    val textRenderUnit = TextRenderUnit(1)
    val textStyle = TextStyle()
    textStyle.setTextSize(20)
    textStyle.setTouchableSpanListener { span, motionEvent, _ ->
      eventsFired.add(motionEvent.action)
      if (shouldSpanBeNull) {
        assertThat(span).isNull()
      } else {
        assertThat(span).isNotNull()
      }
      true
    }
    val clickableText = Spannable.Factory.getInstance().newSpannable("Some text.")
    val clickableSpan: ClickableSpan =
        object : ClickableSpan() {
          override fun onClick(widget: View) {
            // No action
          }
        }
    clickableText.setSpan(clickableSpan, 0, 1, 0)

    val root: DefaultNode = DefaultTextNode(YogaProps(), clickableText, textRenderUnit, textStyle)
    renderCoreTestRule.useRootNode(root).setSizePx(100, 100).render()
    val host = renderCoreTestRule.rootHost as HostView

    val textView = host.getChildAt(0) as RCTextView

    return Pair(eventsFired, textView)
  }

  private fun setupClickableSpanListenerTest(
      shouldSpanBeNull: Boolean = false
  ): Pair<ArrayList<String>, RCTextView> {
    val eventsFired = ArrayList<String>()

    val clickableSpanListener =
        object : ClickableSpanListener {
          override fun onClick(span: ClickableSpan, view: View): Boolean {
            eventsFired.add("onClick")
            if (shouldSpanBeNull) {
              assertThat(span).isNull()
            } else {
              assertThat(span).isNotNull()
            }
            return true
          }

          override fun onLongClick(span: LongClickableSpan, view: View): Boolean {
            eventsFired.add("onLongClick")
            if (shouldSpanBeNull) {
              assertThat(span).isNull()
            } else {
              assertThat(span).isNotNull()
            }
            return true
          }
        }

    val textRenderUnit = TextRenderUnit(1)
    val textStyle = TextStyle()
    textStyle.setTextSize(20)
    textStyle.setClickableSpanListener(clickableSpanListener)

    val clickableText = Spannable.Factory.getInstance().newSpannable("Some text.")
    val clickableSpan =
        object : LongClickableSpan() {
          override fun onLongClick(view: View): Boolean = false

          override fun onClick(widget: View) = Unit
        }
    clickableText.setSpan(clickableSpan, 0, 1, 0)

    val root: DefaultNode = DefaultTextNode(YogaProps(), clickableText, textRenderUnit, textStyle)
    renderCoreTestRule.useRootNode(root).setSizePx(100, 100).render()
    val host = renderCoreTestRule.rootHost as HostView

    val textView = host.getChildAt(0) as RCTextView

    return Pair(eventsFired, textView)
  }

  private fun performActionDownOnSpan(eventsFired: ArrayList<Int>, textView: RCTextView) {
    // click on the span
    val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(downEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains(MotionEvent.ACTION_DOWN)
    eventsFired.clear()
  }

  private fun performActionDownOutsideOfSpan(eventsFired: ArrayList<Int>, textView: RCTextView) {
    // click outside of the span
    val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 9000f, 9000f, 0)
    assertThat(textView.onTouchEvent(downEvent)).isEqualTo(false)
    assertThat(eventsFired.size).isEqualTo(0)
  }

  @Test
  fun testClickableSpanTouchEventHandlingForActionDownAndUpWithinSpan() {
    val (eventsFired, textView) = setupClickableSpanTest()

    performActionDownOnSpan(eventsFired, textView)

    // action up within the bounds
    val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(upEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains(MotionEvent.ACTION_UP)
  }

  @Test
  fun testClickableSpanTouchEventHandlingForActionDownAndCancelWithinSpan() {
    val (eventsFired, textView) = setupClickableSpanTest()

    performActionDownOnSpan(eventsFired, textView)

    // action cancel within the bounds
    val cancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(cancelEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains(MotionEvent.ACTION_CANCEL)
  }

  @Test
  fun testClickableSpanTouchEventHandlingForActionDownAndCancelOutsideSpan() {
    val (eventsFired, textView) = setupClickableSpanTest(shouldSpanBeNull = true)

    performActionDownOutsideOfSpan(eventsFired, textView)

    // action cancel within the bounds
    val cancelEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(cancelEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains(MotionEvent.ACTION_CANCEL)
  }

  @Test
  fun testClickableSpanTouchEventHandlingForActionDownAndUpOutsideSpan() {
    val (eventsFired, textView) = setupClickableSpanTest(shouldSpanBeNull = true)

    performActionDownOutsideOfSpan(eventsFired, textView)

    // action up within the bounds
    val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(upEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains(MotionEvent.ACTION_UP)
  }

  @Test
  fun testClickableSpanClickEventHandlingForActionDownAndUpWithinSpan() {
    val (eventsFired, textView) = setupClickableSpanListenerTest(shouldSpanBeNull = false)

    // click on the span
    val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(downEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(0)

    // action up within the bounds
    val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(upEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains("onClick")
  }

  @Test
  fun testClickableSpanClickEventHandlingForActionDownAndUpOutsideOfSpan() {
    val (eventsFired, textView) = setupClickableSpanListenerTest(shouldSpanBeNull = false)

    // click on the span
    val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
    assertThat(textView.onTouchEvent(downEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(0)

    // action up outside of the bounds
    val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 9000f, 9000f, 0)
    assertThat(textView.onTouchEvent(upEvent)).isEqualTo(true)
    assertThat(eventsFired.size).isEqualTo(1)
    assertThat(eventsFired).contains("onClick")
  }

  @Test
  fun testClickableSpanClickEventHandlingForActionDownAndUpBothOutsideOfSpan() {
    val (eventsFired, textView) = setupClickableSpanListenerTest(shouldSpanBeNull = true)

    // click on the span
    val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 9000f, 9000f, 0)
    assertThat(textView.onTouchEvent(downEvent)).isEqualTo(false)
    assertThat(eventsFired.size).isEqualTo(0)

    // action up outside of the bounds
    val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 9000f, 9000f, 0)
    assertThat(textView.onTouchEvent(upEvent)).isEqualTo(false)
    assertThat(eventsFired.size).isEqualTo(0)
  }
}
