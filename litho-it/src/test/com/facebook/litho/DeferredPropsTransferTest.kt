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

import android.os.Looper.getMainLooper
import com.facebook.litho.animation.Easing
import com.facebook.litho.animation.transition
import com.facebook.litho.animation.tween
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoAnimationTestRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.ms
import com.facebook.rendercore.px
import com.facebook.rendercore.visibility.VisibilityUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

/**
 * A component that lays out with the size it is given (here `RenderWithConstraints`) hands its own
 * props, such as `onVisible`, to the content it resolves at layout. That must happen once per
 * content node, however often the content is laid out: the visibility handlers are appended, so a
 * second transfer makes one visibility event reach the same handler twice.
 */
@OptIn(ExperimentalLithoApi::class)
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class DeferredPropsTransferTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Rule @JvmField val animationRule = LithoAnimationTestRule { shadowOf(getMainLooper()).idle() }

  private var expanded: State<Boolean>? = null
  private var holderVisible = 0
  private var contentVisible = 0

  @Test
  fun `a deferred component's onVisible reaches its handler once`() {
    val testView = lithoTestRule.render { Deferred(contentHasOwnHandler = false) }
    settle(testView)

    assertThat(visibleDispatches(testView)).describedAs("first layout").isEqualTo(1 to 0)
  }

  @Test
  fun `a deferred component's onVisible reaches its handler once after a reflow frame`() {
    val testView = lithoTestRule.render { Deferred(contentHasOwnHandler = false) }
    settle(testView)
    reflowOneFrame(testView)

    assertThat(visibleDispatches(testView)).describedAs("after a reflow frame").isEqualTo(1 to 0)
  }

  @Test
  fun `a deferred component and its content each reach their own onVisible once after a reflow frame`() {
    val testView = lithoTestRule.render { Deferred(contentHasOwnHandler = true) }
    settle(testView)
    assertThat(visibleDispatches(testView)).describedAs("first layout").isEqualTo(1 to 1)

    reflowOneFrame(testView)

    assertThat(visibleDispatches(testView)).describedAs("after a reflow frame").isEqualTo(1 to 1)
  }

  @Test
  fun `a deferred component's onVisible reaches its handler once after a state update without reflow`() {
    val testView = lithoTestRule.render { Deferred(contentHasOwnHandler = true, reflow = false) }
    settle(testView)
    checkNotNull(expanded).update { true }
    settle(testView)

    assertThat(visibleDispatches(testView)).describedAs("after a state update").isEqualTo(1 to 1)
  }

  @Test
  fun `a wrapper's onVisible reaches its handler once when the measured component it wraps renders a deferred component`() {
    val testView = lithoTestRule.render { MeasuredTwice() }
    settle(testView)

    assertThat(visibleDispatches(testView)).describedAs("first layout").isEqualTo(1 to 0)
  }

  private fun settle(testView: TestLithoView) {
    lithoTestRule.idle()
    testView.measure().layout()
  }

  /** Expands the reflowing row inside the deferred content and steps one animation frame. */
  private fun reflowOneFrame(testView: TestLithoView) {
    checkNotNull(expanded).update { true }
    settle(testView)
    animationRule.advanceBy(72.ms)
    testView.measure().layout()
  }

  /**
   * Dispatches every visible callback of the committed layout once, and returns how many times the
   * holder's handler and the content's own handler ran.
   */
  private fun visibleDispatches(testView: TestLithoView): Pair<Int, Int> {
    holderVisible = 0
    contentVisible = 0
    val layoutState = checkNotNull(testView.committedLayoutState)
    for (i in 0 until layoutState.visibilityOutputCount) {
      layoutState.getVisibilityOutputAt(i).onVisible?.let {
        VisibilityUtils.dispatchOnVisible(it.callback, null, VisibilityChangeReason.UNKNOWN)
      }
    }
    return holderVisible to contentVisible
  }

  private inner class Deferred(
      private val contentHasOwnHandler: Boolean,
      private val reflow: Boolean = true,
  ) : KComponent() {
    override fun ComponentScope.render(): Component =
        Column(style = Style.width(200.px)) {
          child(
              RenderWithConstraints(style = Style.onVisible { holderVisible++ }) { _ ->
                val expandedState = useState { false }.also { expanded = it }
                Column(
                    style =
                        Style.width(200.px).let {
                          if (contentHasOwnHandler) it.onVisible { contentVisible++ } else it
                        },
                ) {
                  child(
                      Row(
                          style =
                              Style.width(200.px)
                                  .height(if (expandedState.value) 250.px else 100.px)
                                  .let {
                                    if (!reflow) it
                                    else
                                        it.transition {
                                          bounds(
                                              tween(duration = 144.ms, easing = Easing.Linear),
                                              reflow = true,
                                          )
                                        }
                                  },
                      ),
                  )
                }
              },
          )
        }
  }

  /**
   * Measures [RendersDeferred] while laying out, then places it twice. The first takes the cached
   * measurement, so the second, wrapped with an `onVisible`, resolves it again. Its holder holds
   * `RendersDeferred`, and the `RenderWithConstraints` that component renders is only in the
   * content.
   */
  private inner class MeasuredTwice : KComponent() {
    override fun ComponentScope.render(): Component =
        Column(style = Style.width(200.px)) {
          child(
              RenderWithConstraints { constraints ->
                val measured = RendersDeferred()
                measured.measure(constraints)
                Column {
                  child(measured)
                  child(Wrapper(style = Style.onVisible { holderVisible++ }) { measured })
                }
              },
          )
        }
  }

  private class RendersDeferred : KComponent() {
    override fun ComponentScope.render(): Component = RenderWithConstraints { _ ->
      Row(style = Style.width(200.px).height(100.px))
    }
  }
}
