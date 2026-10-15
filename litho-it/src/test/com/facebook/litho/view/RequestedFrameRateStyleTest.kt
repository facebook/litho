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

package com.facebook.litho.view

import android.view.View
import androidx.annotation.RequiresApi
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class RequestedFrameRateStyleTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun requestedFrameRate_onPreApi35_isNoOp() {
    // On API < 35, requestedFrameRate should be a no-op and not crash
    val testLithoView = lithoTestRule.render { FrameRateComponent(frameRate = 60f) }

    assertThat(testLithoView.findViewWithTag("test_view")).isNotNull
  }

  @Config(sdk = [35])
  @RequiresApi(35)
  @Test
  fun requestedFrameRate_whenSet_isApplied() {
    val testLithoView = lithoTestRule.render { FrameRateComponent(frameRate = 60f) }

    val view = testLithoView.findViewWithTag("test_view")
    assertThat(view.requestedFrameRate).isEqualTo(60f)
  }

  @Config(sdk = [35])
  @RequiresApi(35)
  @Test
  fun requestedFrameRate_whenUnbound_resetsToDefault() {
    val testLithoView = lithoTestRule.render { FrameRateComponent(frameRate = 120f) }

    val view = testLithoView.findViewWithTag("test_view")
    assertThat(view.requestedFrameRate).isEqualTo(120f)

    testLithoView.setRoot(FrameRateComponent(frameRate = null))

    // isEqualTo(float) compares with ==, which never matches the NaN sentinel.
    assertThat(View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT).isNaN()
    assertThat(view.requestedFrameRate).isNaN()
  }

  @Config(sdk = [35])
  @RequiresApi(35)
  @Test
  fun requestedFrameRate_whenUpdated_appliesNewValue() {
    val testLithoView = lithoTestRule.render { FrameRateComponent(frameRate = 60f) }

    val view = testLithoView.findViewWithTag("test_view")
    assertThat(view.requestedFrameRate).isEqualTo(60f)

    testLithoView.setRoot(FrameRateComponent(frameRate = 120f))

    assertThat(view.requestedFrameRate).isEqualTo(120f)
  }

  private class FrameRateComponent(
      private val frameRate: Float?,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      val style =
          Style.width(100.px).height(100.px).viewTag("test_view").wrapInView() +
              if (frameRate != null) {
                Style.requestedFrameRate(frameRate)
              } else {
                null
              }

      return Row(style = style) { child(Text("test")) }
    }
  }
}
