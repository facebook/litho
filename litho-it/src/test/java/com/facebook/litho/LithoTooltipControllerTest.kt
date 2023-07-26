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

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Image
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [LithoTooltipController]. */
@RunWith(LithoTestRunner::class)
class LithoTooltipControllerTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `shows tooltip on Text Spec`() {
    class ComponentWithTooltip(val tooltipHandle: Handle) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row {
          child(
              handle(tooltipHandle) {
                Text.create(context).text("I'm an anchor!").kotlinStyle(Style.margin(10.px)).build()
              })
        }
      }
    }

    val handle = Handle()
    val tooltip = RecordingLithoTooltip()

    lithoViewRule.render { ComponentWithTooltip(tooltipHandle = handle) }

    LithoTooltipController.showTooltipOnHandle(lithoViewRule.context, tooltip, handle)

    assertThat(tooltip.calls).hasSize(1)
    val call = tooltip.calls[0]
    assertThat(call.container).isNotNull()
    assertThat(call.anchorBounds.top).isEqualTo(10)
    assertThat(call.anchorBounds.left).isEqualTo(10)
  }

  @Test
  fun `shows tooltip on Row`() {
    class ComponentWithTooltip(val tooltipHandle: Handle) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row {
          child(
              handle(tooltipHandle) {
                Row(style = Style.margin(10.px)) {
                  Image(style = Style.margin(100.px), drawable = ColorDrawable(Color.BLUE))
                }
              })
        }
      }
    }

    val handle = Handle()
    val tooltip = RecordingLithoTooltip()

    lithoViewRule.render { ComponentWithTooltip(tooltipHandle = handle) }

    LithoTooltipController.showTooltipOnHandle(lithoViewRule.context, tooltip, handle)

    assertThat(tooltip.calls).hasSize(1)
    val call = tooltip.calls[0]
    assertThat(call.container).isNotNull()
    assertThat(call.anchorBounds.top).isEqualTo(10)
    assertThat(call.anchorBounds.left).isEqualTo(10)
  }

  @Test
  fun `shows tooltip on KComponent`() {
    class KComponentWithDrawable() : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Image(
            style = Style.width(100.px).height(100.px), drawable = ColorDrawable(Color.BLUE))
      }
    }

    class ComponentWithTooltip(val tooltipHandle: Handle) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row { child(handle(tooltipHandle) { KComponentWithDrawable() }) }
      }
    }

    val handle = Handle()
    val tooltip = RecordingLithoTooltip()

    lithoViewRule.render { ComponentWithTooltip(tooltipHandle = handle) }

    LithoTooltipController.showTooltipOnHandle(lithoViewRule.context, tooltip, handle)

    assertThat(tooltip.calls).hasSize(1)
    val call = tooltip.calls[0]
    assertThat(call.container).isNotNull()
    assertThat(call.anchorBounds.top).isEqualTo(0)
    assertThat(call.anchorBounds.left).isEqualTo(0)
    assertThat(call.anchorBounds.right).isEqualTo(100)
    assertThat(call.anchorBounds.bottom).isEqualTo(100)
  }

  data class ShowTooltipCall(
      val container: View?,
      val anchorBounds: Rect,
      val xOffset: Int = 0,
      val yOffset: Int = 0
  )

  class RecordingLithoTooltip : LithoTooltip {
    private val _calls = mutableListOf<ShowTooltipCall>()
    val calls: List<ShowTooltipCall>
      get() = _calls.toList()

    override fun showLithoTooltip(
        container: View?,
        anchorBounds: Rect,
        xOffset: Int,
        yOffset: Int
    ) {
      _calls.add(ShowTooltipCall(container, anchorBounds, xOffset, yOffset))
    }

    fun clear() {
      _calls.clear()
    }
  }
}
