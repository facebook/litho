/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.ComponentHost
import com.facebook.litho.LithoView
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.child
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import com.facebook.litho.view.wrapInView
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaPositionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for common styles defined in [Style]. */
@RunWith(AndroidJUnit4::class)
class FlexboxStylesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

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
            child(flexboxParams(flexBasis = 50.px) { Row(style = Style.wrapInView()) })
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 50, 100) }
            })
  }

  @Test
  fun flexGrow_whenSet_childTakesWholeSpace() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(flexboxParams(flexGrow = 1f) { Row(style = Style.wrapInView()) })
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
            child(
                flexboxParams(flexShrink = 1f) {
                  Row(style = Style.height(100.px).minWidth(50.px).wrapInView())
                })
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
            child(
                flexboxParams(alignSelf = YogaAlign.STRETCH) {
                  Row(style = Style.width(100.px).wrapInView())
                })
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(0, 0, 100, 100) }
            })
  }

  @Test
  fun padding_whenGranularPaddingSet_isRespected() {
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
                      .padding(start = left.px, top = top.px, end = right.px, bottom = bottom.px)) {
            child(flexboxParams(flexGrow = 1f) { Row(style = Style.wrapInView()) })
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
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
            child(flexboxParams(flexGrow = 1f) { Row(style = Style.wrapInView()) })
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
  fun padding_whenAllPaddingSet_isRespected() {
    val padding = 32

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              alignItems = YogaAlign.STRETCH,
              style = Style.width(100.px).height(100.px).padding(padding.px)) {
            child(flexboxParams(flexGrow = 1f) { Row(style = Style.wrapInView()) })
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
                flexboxParams(flexGrow = 1f) {
                  Row(
                      style =
                          Style.margin(
                                  start = left.px, top = top.px, end = right.px, bottom = bottom.px)
                              .wrapInView())
                })
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
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
                flexboxParams(flexGrow = 1f) {
                  Row(
                      style =
                          Style.margin(horizontal = horizontal.px, vertical = vertical.px)
                              .wrapInView())
                })
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
  fun margin_whenAllMarginSet_isRespected() {
    val margin = 32

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(alignItems = YogaAlign.STRETCH, style = Style.width(100.px).height(100.px)) {
            child(
                flexboxParams(flexGrow = 1f) { Row(style = Style.margin(margin.px).wrapInView()) })
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(margin, margin, 100 - 2 * margin, 100 - 2 * margin) }
            })
  }

  @Test
  fun position_whenSet_isRespected() {
    val left = 10
    val top = 20
    val right = 30
    val bottom = 40

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(
                flexboxParams(
                    positionType = YogaPositionType.ABSOLUTE,
                    positionStart = left.px,
                    positionTop = top.px,
                    positionEnd = right.px,
                    positionBottom = bottom.px) { Row(style = Style.wrapInView()) })
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
  }

  @Test
  fun childUsingFlexboxChild_whenSetOnJavaSpec_respectedFlexboxChildProperties() {
    val left = 10
    val top = 20
    val right = 30
    val bottom = 40

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row.create(lithoViewRule.context)
              .widthPx(100)
              .heightPx(100)
              .child(
                  flexboxParams(
                      positionType = YogaPositionType.ABSOLUTE,
                      positionStart = left.px,
                      positionTop = top.px,
                      positionEnd = right.px,
                      positionBottom = bottom.px) { Row(style = Style.wrapInView()) })
              .build()
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
  }
}
