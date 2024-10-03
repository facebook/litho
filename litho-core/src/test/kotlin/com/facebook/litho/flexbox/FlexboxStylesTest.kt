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

package com.facebook.litho.flexbox

import android.graphics.Color
import android.text.Layout
import com.facebook.litho.Border
import com.facebook.litho.Component
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.YogaLayoutOutput.Companion.getYogaNode
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.marginPercent
import com.facebook.litho.core.maxHeight
import com.facebook.litho.core.maxWidth
import com.facebook.litho.core.minHeight
import com.facebook.litho.core.minWidth
import com.facebook.litho.core.padding
import com.facebook.litho.core.paddingPercent
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.testing.LegacyLithoTestRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.child
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for common styles defined in [Style]. */
@RunWith(LithoTestRunner::class)
class FlexboxStylesTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoTestRule()

  @Test
  fun widthAndHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px)) }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 100) })
  }

  @Test
  fun widthPercentAndHeightPercent_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.heightPercent(50f).widthPercent(50f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 50, 50) }
            })
  }

  @Test
  fun maxWidth_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.height(100.px).maxWidth(200.px)) {
            child(Row(style = Style.width(500.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 200, 100) })
  }

  @Test
  fun minWidth_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.height(100.px).minWidth(200.px)) }
        .assertMatches(match<LithoView> { bounds(0, 0, 200, 100) })
  }

  @Test
  fun maxHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).maxHeight(200.px)) {
            child(Row(style = Style.height(500.px)))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 200) })
  }

  @Test
  fun minHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).minHeight(200.px)) }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 200) })
  }

  @Test
  fun flexBasis_whenSet_becomesChildWidth() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.flex(basis = 50.px).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 50, 100) }
            })
  }

  @Test
  fun flexBasisPercent_whenSet_becomesChildWidth() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(100.px)) {
            child(Row(style = Style.flex(basisPercent = 50f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 200, 100)
              child<ComponentHost> { bounds(0, 0, 100, 100) }
            })
  }

  @Test
  fun flexGrow_whenSet_childTakesWholeSpace() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.flex(grow = 1f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 100, 100) }
            })
  }

  @Test
  fun flexShrink_whenSet_makesChildAsSmallAsPossible() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.height(100.px).minWidth(50.px).flex(shrink = 1f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 50, 100) }
            })
  }

  @Test
  fun alignSelf_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.width(100.px).alignSelf(YogaAlign.STRETCH).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 100, 100) }
            })
  }

  @Test
  fun padding_whenGranularPaddingSetWithLeftRight_isRespected() {
    val left = 10
    val top = 20
    val right = 30
    val bottom = 40

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style =
                  Style.width(100.px)
                      .height(100.px)
                      .padding(
                          left = left.px, top = top.px, right = right.px, bottom = bottom.px)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
  }

  @Test
  fun padding_whenStartEndPaddingSet_isRespected() {
    val start = 10
    val end = 20

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style = Style.width(100.px).height(100.px).padding(start = start.px, end = end.px)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(start, 0, 100 - start - end, 100) }
            })
  }

  @Test
  fun `padding, when start & end padding set with percent, is respected`() {
    val start = 10.0f
    val end = 20.0f

    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style = Style.width(100.px).height(100.px).paddingPercent(start = start, end = end)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(start.toInt(), 0, (100 - start - end).toInt(), 100) }
            })
  }

  @Test
  fun padding_whenHorizontalVerticalPaddingSet_isRespected() {
    val horizontal = 10
    val vertical = 20

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style =
                  Style.width(100.px)
                      .height(100.px)
                      .padding(horizontal = horizontal.px, vertical = vertical.px)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(horizontal, vertical, 100 - 2 * horizontal, 100 - 2 * vertical)
              }
            })
  }

  @Test
  fun `padding, when horizontal & vertical padding set with percent, is respected`() {
    val horizontal = 10.0f
    val vertical = 20.0f

    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot {
          Row(
              style =
                  Style.width(100.px)
                      .height(100.px)
                      .paddingPercent(horizontal = horizontal, vertical = vertical)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    horizontal.toInt(),
                    vertical.toInt(),
                    (100 - 2 * horizontal).toInt(),
                    (100 - 2 * vertical).toInt())
              }
            })
  }

  @Test
  fun padding_whenAllPaddingSet_isRespected() {
    val padding = 32

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style = Style.width(100.px).height(100.px).padding(padding.px)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(padding, padding, 100 - 2 * padding, 100 - 2 * padding)
              }
            })
  }

  @Test
  fun `padding, when all padding set with percent, is respected`() {
    val padding = 32.0f

    lithoViewRule
        .setSizeSpecs(exactly(100), exactly(100))
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style = Style.width(100.px).height(100.px).paddingPercent(all = padding)) {
                child(Row(style = Style.flex(grow = 1f).wrapInView()))
              }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    padding.toInt(),
                    padding.toInt(),
                    (100 - 2 * padding).toInt(),
                    (100 - 2 * padding).toInt())
              }
            })
  }

  @Test
  fun margin_whenGranularMarginSet_isRespected() {
    val left = 10
    val top = 20
    val right = 30
    val bottom = 40

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.margin(
                                left = left.px, top = top.px, right = right.px, bottom = bottom.px)
                            .flex(grow = 1f)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
  }

  @Test
  fun `margin, when granular margin set with percent, is respected`() {
    val left = 10.0f
    val top = 20.0f
    val right = 30.0f
    val bottom = 40.0f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.marginPercent(left = left, top = top, right = right, bottom = bottom)
                            .flex(grow = 1f)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    left.toInt(),
                    top.toInt(),
                    (100 - left - right).toInt(),
                    (100 - top - bottom).toInt())
              }
            })
  }

  @Test
  fun margin_whenMarginStartEndSet_isRespected() {
    val start = 10
    val end = 20

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.margin(start = start.px, end = end.px).flex(grow = 1f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(start, 0, 100 - start - end, 100) }
            })
  }

  @Test
  fun `margin, when margin start & end set with percent, is respected`() {
    val start = 10.0f
    val end = 20.0f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.marginPercent(start = start, end = end).flex(grow = 1f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(start.toInt(), 0, (100 - start - end).toInt(), 100) }
            })
  }

  @Test
  fun margin_whenHorizontalVerticalMarginSet_isRespected() {
    val horizontal = 10
    val vertical = 20

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.margin(horizontal = horizontal.px, vertical = vertical.px)
                            .flex(grow = 1f)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(horizontal, vertical, 100 - 2 * horizontal, 100 - 2 * vertical)
              }
            })
  }

  @Test
  fun `margin, when horizontal & vertical margin set with percent, is respected`() {
    val horizontal = 10.0f
    val vertical = 20.0f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.marginPercent(horizontal = horizontal, vertical = vertical)
                            .flex(grow = 1f)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    horizontal.toInt(),
                    vertical.toInt(),
                    (100 - 2 * horizontal).toInt(),
                    (100 - 2 * vertical).toInt())
              }
            })
  }

  @Test
  fun margin_whenAllMarginSet_isRespected() {
    val margin = 32

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.margin(margin.px).flex(grow = 1f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(margin, margin, 100 - 2 * margin, 100 - 2 * margin) }
            })
  }

  @Test
  fun `margin, when all margin set with percent, is respected`() {
    val margin = 32.0f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.marginPercent(margin).flex(grow = 1f).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    margin.toInt(),
                    margin.toInt(),
                    (100 - 2 * margin).toInt(),
                    (100 - 2 * margin).toInt())
              }
            })
  }

  @Test
  fun position_whenLeftRightSet_isRespected() {
    val left = 10
    val top = 20
    val right = 30
    val bottom = 40

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .position(
                                left = left.px, top = top.px, right = right.px, bottom = bottom.px)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
  }

  @Test
  fun `position, when left & right set with percent, is respected`() {
    val left = 10f
    val top = 20f
    val right = 30f
    val bottom = 40f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .positionPercent(left = left, top = top, right = right, bottom = bottom)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    left.toInt(),
                    top.toInt(),
                    (100 - left - right).toInt(),
                    (100 - top - bottom).toInt())
              }
            })
  }

  @Test
  fun position_whenStartEndSet_isRespected() {
    val start = 10
    val top = 20
    val end = 30
    val bottom = 40

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .position(
                                start = start.px, top = top.px, end = end.px, bottom = bottom.px)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(start, top, 100 - start - end, 100 - top - bottom) }
            })
  }

  @Test
  fun `position, when start & end set with percent, is respected`() {
    val start = 10f
    val top = 20f
    val end = 30f
    val bottom = 40f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .positionPercent(start = start, top = top, end = end, bottom = bottom)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    start.toInt(),
                    top.toInt(),
                    (100 - start - end).toInt(),
                    (100 - top - bottom).toInt())
              }
            })
  }

  @Test
  fun position_whenAllSet_isRespected() {
    val all = 10

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .position(all = all.px)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(all, all, 100 - 2 * all, 100 - 2 * all) }
            })
  }

  @Test
  fun `position, when all set with percent, is respected`() {
    val all = 10f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .positionPercent(all = all)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(all.toInt(), all.toInt(), (100 - 2 * all).toInt(), (100 - 2 * all).toInt())
              }
            })
  }

  @Test
  fun position_whenHorizontalSet_isRespected() {
    val horizontal = 10
    val all = 50

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .position(all = all.px, horizontal = horizontal.px)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(horizontal, 100 - all, 100 - 2 * horizontal, 100 - 2 * all)
              }
            })
  }

  @Test
  fun `position, when horizontal set with percent, is respected`() {
    val horizontal = 10f
    val all = 50f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .positionPercent(all = all, horizontal = horizontal)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    horizontal.toInt(),
                    (100 - all).toInt(),
                    (100 - 2 * horizontal).toInt(),
                    (100 - 2 * all).toInt())
              }
            })
  }

  @Test
  fun position_whenVerticalSet_isRespected() {
    val vertical = 10
    val all = 50

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .position(vertical = vertical.px, all = all.px)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(all, vertical, 100 - 2 * all, 100 - 2 * vertical) }
            })
  }

  @Test
  fun `position, when vertical set with percent, is respected`() {
    val vertical = 10f
    val all = 50f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                Row(
                    style =
                        Style.positionType(YogaPositionType.ABSOLUTE)
                            .positionPercent(all = all, vertical = vertical)
                            .wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> {
                bounds(
                    all.toInt(),
                    vertical.toInt(),
                    (100 - 2 * all).toInt(),
                    (100 - 2 * vertical).toInt())
              }
            })
  }

  @Test
  fun layoutDirection_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px).layoutDirection(YogaDirection.RTL))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.currentRootNode?.getYogaNode()?.layoutDirection)
        .isEqualTo(YogaDirection.RTL)
  }

  /**
   * Test is using [Layout] and [NodeInfo] classes as a workaround for the issue with 'libyoga.so
   * already loaded in another classloader exception' caused by multiple ClassLoaders trying to load
   * Yoga when using @Config to specify a different target sdk. See:
   * https://www.internalfb.com/intern/staticdocs/litho/docs/testing/unit-testing/
   */
  @Test
  fun border_whenSet_isRespected() {
    class ComponentWithBorder : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(
            style =
                Style.border(
                    Border.create(context)
                        .color(YogaEdge.LEFT, Color.BLUE)
                        .color(YogaEdge.TOP, Color.RED)
                        .color(YogaEdge.RIGHT, Color.BLACK)
                        .color(YogaEdge.BOTTOM, Color.WHITE)
                        .radiusDip(Border.Corner.TOP_LEFT, 5f)
                        .radiusDip(Border.Corner.TOP_RIGHT, 6f)
                        .radiusDip(Border.Corner.BOTTOM_RIGHT, 7f)
                        .radiusDip(Border.Corner.BOTTOM_LEFT, 8f)
                        .build()))
      }
    }

    val node = LegacyLithoTestRule.getRootLayout(lithoViewRule, ComponentWithBorder())?.node
    assertThat(node?.borderColors)
        .isEqualTo(intArrayOf(Color.BLUE, Color.RED, Color.BLACK, Color.WHITE))
    assertThat(node?.borderRadius).isEqualTo(floatArrayOf(5f, 6f, 7f, 8f))
  }

  @Test
  fun marginAuto_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(200, 200)
        .setRoot {
          Row(
              style =
                  Style.width(100.px)
                      .height(100.px)
                      .marginAuto(YogaEdge.LEFT)
                      .marginAuto(YogaEdge.TOP))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.currentRootNode?.getYogaNode()?.getMargin(YogaEdge.LEFT).toString())
        .isEqualTo("auto")
    assertThat(lithoViewRule.currentRootNode?.getYogaNode()?.getMargin(YogaEdge.TOP).toString())
        .isEqualTo("auto")
  }

  @Test
  fun isReferenceBaseline_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(200, 200)
        .setRoot { Row(style = Style.isReferenceBaseline(true)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.currentRootNode?.getYogaNode()?.isReferenceBaseline).isEqualTo(true)
  }

  @Test
  fun useHeightAsBaseline_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(200, 200)
        .setRoot { Row(style = Style.useHeightAsBaseline(true)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.currentRootNode?.getYogaNode()?.isBaselineDefined).isEqualTo(true)
  }

  @Test
  fun gap_whenSet_isRespected() {
    val gap = 10

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px).gap(all = gap.px)) {
            child(Row(style = Style.width(10.px).height(10.px).wrapInView()))
            child(Row(style = Style.width(20.px).height(20.px).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 10, 10) }
              child<ComponentHost> { bounds(10 + gap, 0, 20, 20) }
            })
  }
}
