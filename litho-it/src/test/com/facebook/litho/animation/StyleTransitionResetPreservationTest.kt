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

package com.facebook.litho.animation

import android.app.Activity
import android.content.Intent
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.litho.view.alpha
import com.facebook.litho.view.scale
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.dp
import com.facebook.rendercore.ms
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.LooperMode

/**
 * A settled transition property on a node must survive an unrelated transition on that same node. A
 * node still present as a current target is updated in place, not recycled, so its declared
 * properties must not be reset — otherwise toggling `bounds` after `alpha` has settled snaps alpha
 * back to its default.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@OptIn(ExperimentalLithoApi::class)
@RunWith(LithoTestRunner::class)
class StyleTransitionResetPreservationTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private lateinit var activityController: ActivityController<Activity>
  private var dimmed: State<Boolean>? = null
  private var wide: State<Boolean>? = null
  private var present: State<Boolean>? = null

  @Before
  fun setUp() {
    activityController = Robolectric.buildActivity(Activity::class.java, Intent())
  }

  @Test
  fun boundsTransition_doesNotResetSettledAlpha() {
    val testLithoView = lithoTestRule.render { AlphaAndBoundsComponent() }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    val alpha = { checkNotNull(testLithoView.findViewWithTag(TAG)).alpha }
    assertThat(alpha()).isEqualTo(1f)

    // Animate alpha to 0.4 and let it settle.
    checkNotNull(dimmed).updateSync { true }
    lithoTestRule.idle()
    assertThat(alpha()).isEqualTo(0.4f)

    // Toggle bounds (an unrelated transition on the same, still-present node). Alpha must be kept.
    checkNotNull(wide).updateSync { true }
    lithoTestRule.idle()
    assertThat(alpha()).isEqualTo(0.4f)
  }

  private inner class AlphaAndBoundsComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val dimmedState = useState { false }.also { dimmed = it }
      val wideState = useState { false }.also { wide = it }
      return Column {
        child(
            Row(
                style =
                    Style.width(if (wideState.value) 200.dp else 100.dp)
                        .height(100.dp)
                        .alpha(if (dimmedState.value) 0.4f else 1f)
                        .transition {
                          bounds = tween(120.ms)
                          alpha = tween(120.ms)
                        }
                        .viewTag(TAG),
            ),
        )
      }
    }
  }

  @Test
  fun leavingNode_resetsTransitionPropsForRecycling() {
    val testLithoView = lithoTestRule.render { RecyclableComponent() }
    activityController.get().setContentView(testLithoView.lithoView)
    activityController.resume().visible()

    // The scaled node's view carries its declared transform while mounted.
    val scaled = checkNotNull(testLithoView.findViewWithTag(SCALED_TAG))
    assertThat(scaled.scaleX).isEqualTo(0.5f)
    assertThat(scaled.scaleY).isEqualTo(0.5f)

    // Remove the node: it is no longer a current target, so its view is reset before returning to
    // the pool — a reused view must not inherit the stale transform.
    checkNotNull(present).updateSync { false }
    lithoTestRule.idle()
    assertThat(scaled.scaleX).isEqualTo(1f)
    assertThat(scaled.scaleY).isEqualTo(1f)
  }

  private inner class RecyclableComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val presentState = useState { true }.also { present = it }
      return Column {
        if (presentState.value) {
          child(
              Row(
                  style =
                      Style.width(50.dp)
                          .height(50.dp)
                          .scale(0.5f)
                          .transition { scale = tween(120.ms) }
                          .viewTag(SCALED_TAG),
              ),
          )
        }
      }
    }
  }

  companion object {
    private const val TAG = "reset_preservation_row"
    private const val SCALED_TAG = "reset_recycle_row"
  }
}
