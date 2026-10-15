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

package com.facebook.litho.touch

import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.child
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.performTouchInput
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onTouch
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Characterizes touch ownership when a Primitive mounts a custom View that does its own touch
 * handling: who wins between the View, the Litho styles on the same Primitive, and an ancestor.
 *
 * This is the case with no existing coverage, and the one a change to touch dispatch is most likely
 * to break, since it depends on the exact ordering inside `View.dispatchTouchEvent`.
 */
@RunWith(LithoTestRunner::class)
class CustomViewPrimitiveTouchTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  /** Collects what the mounted View saw, and controls whether it consumes. */
  private class TouchRecorder(var consume: Boolean = false) {
    val actions = mutableListOf<Int>()
  }

  private class RecordingTouchView(context: Context) : View(context) {
    var recorder: TouchRecorder? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
      val current = recorder
      current?.actions?.add(event.actionMasked)
      return if (current?.consume == true) true else super.onTouchEvent(event)
    }
  }

  private class RecordingTouchPrimitive(
      private val recorder: TouchRecorder,
      private val style: Style = Style,
  ) : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(SIZE, SIZE),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> RecordingTouchView(context) }) {
              bind(recorder) { view ->
                view.recorder = recorder
                onUnbind { view.recorder = null }
              }
            },
        style = style,
    )
  }

  /** Primitive wrapped in a clickable ancestor, to observe what reaches the ancestor. */
  private class WithClickableAncestor(
      private val recorder: TouchRecorder,
      private val onAncestorClick: () -> Unit,
      private val primitiveStyle: Style = Style,
  ) : KComponent() {
    override fun ComponentScope.render(): Component =
        Row(style = Style.onClick { onAncestorClick() }) {
          child(RecordingTouchPrimitive(recorder, primitiveStyle))
        }
  }

  @Test
  fun `consuming custom view receives the whole gesture`() {
    val recorder = TouchRecorder(consume = true)
    val testLithoView = lithoTestRule.render {
      WithClickableAncestor(recorder, onAncestorClick = {})
    }

    testLithoView.performTouchInput {
      down(CENTER, CENTER)
      moveBy(dx = 1f, dy = 1f)
      up()
    }

    assertThat(recorder.actions)
        .containsExactly(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)
  }

  @Test
  fun `consuming custom view prevents the ancestor click`() {
    var ancestorClicks = 0
    val recorder = TouchRecorder(consume = true)
    val testLithoView = lithoTestRule.render {
      WithClickableAncestor(recorder, onAncestorClick = { ancestorClicks++ })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(recorder.actions).isNotEmpty()
    assertThat(ancestorClicks)
        .describedAs("A View that consumes touch must starve the enclosing click handler")
        .isEqualTo(0)
  }

  @Test
  fun `non-consuming custom view lets the ancestor click fire`() {
    var ancestorClicks = 0
    val recorder = TouchRecorder(consume = false)
    val testLithoView = lithoTestRule.render {
      WithClickableAncestor(recorder, onAncestorClick = { ancestorClicks++ })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(recorder.actions).contains(MotionEvent.ACTION_DOWN)
    assertThat(ancestorClicks).isEqualTo(1)
  }

  /** `setOnClickListener` runs from super.onTouchEvent, which a consuming View never calls. */
  @Test
  fun `consuming custom view starves onClick on the same primitive`() {
    var clicks = 0
    val recorder = TouchRecorder(consume = true)
    val testLithoView = lithoTestRule.render {
      RecordingTouchPrimitive(recorder, Style.onClick { clicks++ })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(recorder.actions).contains(MotionEvent.ACTION_DOWN)
    assertThat(clicks).isEqualTo(0)
  }

  @Test
  fun `non-consuming custom view allows onClick on the same primitive`() {
    var clicks = 0
    val recorder = TouchRecorder(consume = false)
    val testLithoView = lithoTestRule.render {
      RecordingTouchPrimitive(recorder, Style.onClick { clicks++ })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(clicks).isEqualTo(1)
  }

  /** `OnTouchListener` runs before `onTouchEvent`, so Style.onTouch can starve the View. */
  @Test
  fun `consuming onTouch style starves the custom view`() {
    val recorder = TouchRecorder(consume = true)
    val testLithoView = lithoTestRule.render {
      RecordingTouchPrimitive(recorder, Style.onTouch { true })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(recorder.actions)
        .describedAs("Style.onTouch consuming the stream must run before the View's onTouchEvent")
        .isEmpty()
  }

  @Test
  fun `non-consuming onTouch style still reaches the custom view`() {
    // The View consumes so the trace is unambiguous about reach; the variable under test is the
    // style's return value, which is false here and true in the paired test above.
    val recorder = TouchRecorder(consume = true)
    val testLithoView = lithoTestRule.render {
      RecordingTouchPrimitive(recorder, Style.onTouch { false })
    }

    testLithoView.performTouchInput { click(CENTER, CENTER) }

    assertThat(recorder.actions).contains(MotionEvent.ACTION_DOWN)
  }

  companion object {
    private const val SIZE = 100
    private const val CENTER = SIZE / 2f
  }
}
