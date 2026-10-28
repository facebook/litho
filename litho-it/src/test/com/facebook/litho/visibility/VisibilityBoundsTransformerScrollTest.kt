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

package com.facebook.litho.visibility

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.facebook.litho.Style
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import com.facebook.rendercore.visibility.VisibilityBoundsTransformer
import com.facebook.rendercore.visibility.VisibilityOutput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class VisibilityBoundsTransformerScrollTest {

  private val occlusion = BottomBandOcclusion()

  @Rule
  @JvmField
  val lithoTestRule = LithoTestRule(
      componentsConfiguration =
          ComponentsConfiguration.defaultInstance.copy(visibilityBoundsTransformer = occlusion),
  )

  @Test
  fun `a row that one scroll frame carries from clear to fully under the band stops counting as visible`() {
    occlusion.bandTopPx = 80
    var isVisible = false
    val row =
        renderRowInList(Style.onVisible { isVisible = true }.onInvisible { isVisible = false })
    assertThat(isVisible).isTrue()

    row.lithoView.offsetTopAndBottom(80)

    assertThat(isVisible).isFalse()
  }

  @Test
  fun `a row that one scroll frame carries from fully under the band to clear is fully seen`() {
    occlusion.bandTopPx = 80
    var fullImpressions = 0
    val row = renderRowInList(Style.onFullImpression { fullImpressions++ })
    row.lithoView.offsetTopAndBottom(80)
    val fullImpressionsUnderBand = fullImpressions

    row.lithoView.offsetTopAndBottom(-80)

    assertThat(fullImpressions).isEqualTo(fullImpressionsUnderBand + 1)
  }

  /** Renders a 100 x 20 px row at the top of a 100 x 100 px list. */
  private fun renderRowInList(style: Style): TestLithoView {
    val list = FrameLayout(lithoTestRule.context.androidContext)
    list.measure(exactly(100), exactly(100))
    list.layout(0, 0, 100, 100)
    val row = lithoTestRule.createTestLithoView()
    list.addView(row.lithoView)
    return lithoTestRule.render(lithoView = row.lithoView, widthPx = 100, heightPx = 20) {
      Row(style = Style.width(100.px).height(20.px) + style)
    }
  }

  /** Occludes everything in the list below [bandTopPx]. */
  private class BottomBandOcclusion : VisibilityBoundsTransformer {
    var bandTopPx = 0

    override fun getTransformedLocalVisibleRect(host: ViewGroup, outRect: Rect) {
      if (!host.getLocalVisibleRect(outRect)) {
        outRect.setEmpty()
        return
      }
      outRect.bottom = minOf(outRect.bottom, bandTopPx - host.top)
      if (outRect.isEmpty) {
        outRect.setEmpty()
      }
    }

    override fun getViewportRect(view: View, outRect: Rect) {
      view.getLocalVisibleRect(outRect)
    }

    override fun shouldUseTransformedVisibleRect(visibilityOutput: VisibilityOutput): Boolean = true

    override fun shouldRecordTransformedVisibleRectAtEveryPass(): Boolean = true
  }
}
