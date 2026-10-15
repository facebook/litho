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
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.touchExpansion
import com.facebook.litho.widget.OnClickCallbackComponent
import com.facebook.litho.widget.Text
import com.facebook.rendercore.px
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

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = lithoTestRule.context
  }

  @Test
  fun findExpandedTouchTarget_returnsTargetWithinExpansionWithoutDispatching() {
    val callback = ClickListenerCallback()
    val lithoView = renderExpandedComponent(callback)

    val target = lithoView.findExpandedTouchTarget(5, 5)

    assertThat(target).isNotNull
    assertThat(callback.handled).isFalse
    assertThat(callback.count).isZero()
  }

  @Test
  fun findExpandedTouchTarget_usesInclusiveStartAndExclusiveEndBounds() {
    val lithoView = renderExpandedComponent(ClickListenerCallback())

    assertThat(lithoView.findExpandedTouchTarget(5, 5)).isNotNull
    assertThat(lithoView.findExpandedTouchTarget(24, 24)).isNotNull
    assertThat(lithoView.findExpandedTouchTarget(4, 5)).isNull()
    assertThat(lithoView.findExpandedTouchTarget(25, 24)).isNull()
    assertThat(lithoView.findExpandedTouchTarget(24, 25)).isNull()
  }

  @Test
  fun findExpandedTouchTarget_returnsHighestMountIndexWhenExpansionsOverlap() {
    val lithoView =
        lithoTestRule
            .render {
              Column(style = Style.padding(all = 10.px)) {
                child(
                    OnClickCallbackComponent(
                        callback = ClickListenerCallback(),
                        style = Style.width(10.px).height(10.px).touchExpansion(all = 10.px),
                    ),
                )
                child(
                    OnClickCallbackComponent(
                        callback = ClickListenerCallback(),
                        style = Style.width(10.px).height(10.px).touchExpansion(all = 10.px),
                    ),
                )
              }
            }
            .lithoView

    // The lower child's expansion covers y in [0, 30); the upper child's covers y in [10, 40).
    // (10, 15) sits in both, so the highest mount index (upper) must win; identity comparison
    // avoids coupling the assertion to a legacy View id.
    val overlapTarget = lithoView.findExpandedTouchTarget(10, 15)
    val lowerOnlyTarget = lithoView.findExpandedTouchTarget(10, 5)
    val upperOnlyTarget = lithoView.findExpandedTouchTarget(10, 35)

    assertThat(overlapTarget).isNotNull
    assertThat(overlapTarget).isSameAs(upperOnlyTarget)
    assertThat(overlapTarget).isNotSameAs(lowerOnlyTarget)
  }

  @Test
  fun findExpandedTouchTarget_doesNotPrimeGestureState() {
    val callback = ClickListenerCallback()
    val lithoView = renderExpandedComponent(callback)

    assertThat(lithoView.findExpandedTouchTarget(5, 5)).isNotNull
    lithoView.dispatchTouchEvent(
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP,
            5f,
            5f,
            0,
        ),
    )

    assertThat(callback.handled).isFalse
    assertThat(callback.count).isZero()
  }

  @Test
  fun findExpandedTouchTarget_returnsNullAfterUnmount() {
    val lithoView = renderExpandedComponent(ClickListenerCallback())
    assertThat(lithoView.findExpandedTouchTarget(5, 5)).isNotNull

    lithoView.unmountAllItems()

    assertThat(lithoView.findExpandedTouchTarget(5, 5)).isNull()
  }

  @Test
  fun findExpandedTouchTarget_returnsNullWhenAndroidDelegateIsReplaced() {
    val lithoView = renderExpandedComponent(ClickListenerCallback())
    assertThat(lithoView.findExpandedTouchTarget(5, 5)).isNotNull

    lithoView.touchDelegate = TouchDelegate(Rect(), lithoView)

    assertThat(lithoView.findExpandedTouchTarget(5, 5)).isNull()
  }

  @Test
  fun onTouchEventOnEmptyDelegate_shouldNoOp() {
    val host = ComponentHost(ApplicationProvider.getApplicationContext<Context>(), null)
    val delegate = TouchExpansionDelegate(host)
    val handled =
        delegate.onTouchEvent(
            MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                0f,
                0f,
                0,
            ),
        )
    assertThat(handled).describedAs("TouchEvent on empty delegate should not handle event").isFalse
  }

  @Test
  fun onTouchEventWithinBounds_shouldBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    val testLithoView = lithoTestRule.render { component }
    testLithoView.lithoView.emulateClickEvent(7, 7)
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
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    val testLithoView = lithoTestRule.render { component }
    testLithoView.lithoView.emulateClickEvent(2, 2)
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
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    val testLithoView = lithoTestRule.render { component }
    assertThat(testLithoView.lithoView.touchExpansionDelegate?.size())
        .describedAs("touch expansion delegates should be present")
        .isEqualTo(1)
    testLithoView.lithoView.unmountAllItems()
    testLithoView.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
    assertThat(testLithoView.lithoView.touchExpansionDelegate?.size())
        .describedAs("all touch expansion delegates should be released")
        .isEqualTo(0)
  }

  @Test
  fun onTouchEventOnUpdatedComponentWithoutTouchExpansion_shouldNotBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    val testLithoView = lithoTestRule.render { component }
    val updated =
        Column.create(context)
            .child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    testLithoView.setRoot(updated)
    assertThat(testLithoView.lithoView.findExpandedTouchTarget(7, 7)).isNull()
    testLithoView.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
    testLithoView.lithoView.emulateClickEvent(11, 11)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue
    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1)
    testLithoView.lithoView.unmountAllItems()
    assertThat(testLithoView.lithoView.touchExpansionDelegate?.size())
        .describedAs("all touch expansion delegates should be released")
        .isEqualTo(0)
  }

  @Test
  fun onTouchEventOnComponentMoved_shouldBeHandled() {
    val callback = ClickListenerCallback()
    val component =
        Column.create(context)
            .child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    val testLithoView = lithoTestRule.render { component }
    val updated =
        Column.create(context)
            .child(Text.create(context).text("hello world").widthPx(10).heightPx(10))
            .child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
            .paddingPx(YogaEdge.ALL, 10)
            .build()
    testLithoView.setRoot(updated).measure().layout()
    assertThat(testLithoView.lithoView.findExpandedTouchTarget(7, 7)).isNull()
    assertThat(testLithoView.lithoView.findExpandedTouchTarget(7, 21)).isNotNull
    testLithoView.lithoView.emulateClickEvent(7, 7)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should not be handled")
        .isFalse
    testLithoView.lithoView.emulateClickEvent(7, 21)
    assertThat(callback.handled)
        .describedAs("TouchEvent within bounds bounds should be handled")
        .isTrue
    assertThat(callback.count)
        .describedAs("TouchEvent within bounds bounds should be handled only once")
        .isEqualTo(1)
  }

  private fun renderExpandedComponent(callback: ClickListenerCallback): LithoView {
    return lithoTestRule
        .render {
          Column(style = Style.padding(all = 10.px)) {
            child(
                OnClickCallbackComponent(
                    callback = callback,
                    style = Style.width(10.px).height(10.px).touchExpansion(all = 5.px),
                ),
            )
          }
        }
        .lithoView
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
              0,
          )
      val up =
          MotionEvent.obtain(
              SystemClock.uptimeMillis() + 10,
              SystemClock.uptimeMillis() + 10,
              MotionEvent.ACTION_UP,
              x.toFloat(),
              y.toFloat(),
              0,
          )
      dispatchTouchEvent(down)
      dispatchTouchEvent(up)
    }
  }
}
