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

package com.facebook.litho

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.OnClickCallbackComponent
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class TouchExpansionDelegateTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
  }

  @Test
  fun onTouchEventOnEmptyDelegate_shouldNoOp() {
    val host = ComponentHost(ApplicationProvider.getApplicationContext<Context>())
    val delegate = TouchExpansionDelegate(host)
    val handled =
        delegate.onTouchEvent(
            MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                0f,
                0f,
                0))
    assertThat(handled).describedAs("TouchEvent on empty delegate should not handle event").isFalse
  }

  @Test
  fun onTouchEventWithinBounds_shouldBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    legacyLithoViewRule.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue
    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1)
  }

  @Test
  fun onTouchEventOutsideBounds_shouldNotBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    legacyLithoViewRule.lithoView.emulateClickEvent(2, 2)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
  }

  @Test
  fun onTouchEventOnUnmount_shouldNotBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(legacyLithoViewRule.lithoView.touchExpansionDelegate.size())
        .describedAs("touch expansion delegates should be present")
        .isEqualTo(1)
    legacyLithoViewRule.lithoView.unmountAllItems()
    legacyLithoViewRule.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
    assertThat(legacyLithoViewRule.lithoView.touchExpansionDelegate.size())
        .describedAs("all touch expansion delegates should be released")
        .isEqualTo(0)
  }

  @Test
  fun onTouchEventOnUpdatedComponentWithoutTouchExpansion_shouldNotBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    val updated =
        Column.create(context)
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(updated)
    legacyLithoViewRule.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
    legacyLithoViewRule.lithoView.emulateClickEvent(11, 11)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue
    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1)
    legacyLithoViewRule.lithoView.unmountAllItems()
    assertThat(legacyLithoViewRule.lithoView.touchExpansionDelegate.size())
        .describedAs("all touch expansion delegates should be released")
        .isEqualTo(0)
  }

  @Test
  fun onTouchEventOnComponentMoved_shouldBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    val updated =
        Column.create(context)
            .child(Text.create(context).text("hello world").widthPx(10).heightPx(10))
            .child(
                OnClickCallbackComponent.create(context)
                    .widthPx(10)
                    .heightPx(10)
                    .callback(callback)
                    .touchExpansionPx(YogaEdge.ALL, 5))
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    legacyLithoViewRule.setRoot(updated).measure().layout()
    legacyLithoViewRule.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
    legacyLithoViewRule.lithoView.emulateClickEvent(7, 21)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue
    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1)
  }

  class ClickListenerCallback : View.OnClickListener {
    var handled = false
    var count = 0

    override fun onClick(v: View) {
      handled = true
      count++
    }

    fun reset() {
      handled = false
      count = 0
    }
  }

  companion object {
    fun View.emulateClickEvent(x: Int, y: Int) {
      val down =
          MotionEvent.obtain(
              SystemClock.uptimeMillis(),
              SystemClock.uptimeMillis(),
              MotionEvent.ACTION_DOWN,
              x.toFloat(),
              y.toFloat(),
              0)
      val up =
          MotionEvent.obtain(
              SystemClock.uptimeMillis() + 10,
              SystemClock.uptimeMillis() + 10,
              MotionEvent.ACTION_UP,
              x.toFloat(),
              y.toFloat(),
              0)
      dispatchTouchEvent(down)
      dispatchTouchEvent(up)
    }
  }
}
