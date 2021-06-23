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

package com.facebook.litho

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.flexbox.border
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.child
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [StyleCompat] */
@RunWith(AndroidJUnit4::class)
class StyleCompatTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun widthAndHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = StyleCompat.widthDip(100f).heightDip(200f).build()) }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 200) })
  }

  @Test
  fun widthPercentAndHeightPercent_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(style = StyleCompat.heightPercent(50f).widthPercent(50f).wrapInView().build()))
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
          Row(style = StyleCompat.heightPx(100).maxWidthPx(200).build()) {
            child(Row(style = StyleCompat.widthPx(500).build()))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 200, 100) })
  }

  @Test
  fun minWidth_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = StyleCompat.heightPx(100).minWidthPx(200).build()) }
        .assertMatches(match<LithoView> { bounds(0, 0, 200, 100) })
  }

  @Test
  fun maxHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = StyleCompat.widthPx(100).maxHeightPx(200).build()) {
            child(Row(style = StyleCompat.heightPx(500).build()))
          }
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 200) })
  }

  @Test
  fun minHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = StyleCompat.widthPx(100).minHeightPx(200).build()) }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 200) })
  }

  @Test
  fun flexBasis_whenSet_becomesChildWidth() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(Row(style = StyleCompat.flexBasisPx(50).wrapInView().build()))
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
          Row(style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(Row(style = StyleCompat.flexGrow(1f).wrapInView().build()))
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
          Row(
              alignItems = YogaAlign.STRETCH,
              style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(
                    style =
                        StyleCompat.heightPx(100)
                            .minWidthPx(50)
                            .flexShrink(1f)
                            .wrapInView()
                            .build()))
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
          Row(style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(
                    style =
                        StyleCompat.widthPx(100).alignSelf(YogaAlign.STRETCH).wrapInView().build()))
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
                  StyleCompat.widthPx(100)
                      .heightPx(100)
                      .paddingPx(YogaEdge.START, left)
                      .paddingPx(YogaEdge.TOP, top)
                      .paddingPx(YogaEdge.END, right)
                      .paddingPx(YogaEdge.BOTTOM, bottom)
                      .build()) {
            child(Row(style = StyleCompat.flexGrow(1f).wrapInView().build()))
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
                  StyleCompat.widthPx(100)
                      .heightPx(100)
                      .paddingPx(YogaEdge.HORIZONTAL, horizontal)
                      .paddingPx(YogaEdge.VERTICAL, vertical)
                      .build()) {
            child(Row(style = StyleCompat.flexGrow(1f).wrapInView().build()))
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
              style =
                  StyleCompat.widthPx(100).heightPx(100).paddingPx(YogaEdge.ALL, padding).build()) {
            child(Row(style = StyleCompat.flexGrow(1f).wrapInView().build()))
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
          Row(
              alignItems = YogaAlign.STRETCH,
              style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(
                    style =
                        StyleCompat.marginPx(YogaEdge.START, left)
                            .marginPx(YogaEdge.TOP, top)
                            .marginPx(YogaEdge.END, right)
                            .marginPx(YogaEdge.BOTTOM, bottom)
                            .flexGrow(1f)
                            .wrapInView()
                            .build()))
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
          Row(
              alignItems = YogaAlign.STRETCH,
              style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(
                    style =
                        StyleCompat.marginPx(YogaEdge.HORIZONTAL, horizontal)
                            .marginPx(YogaEdge.VERTICAL, vertical)
                            .flexGrow(1f)
                            .wrapInView()
                            .build()))
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
          Row(
              alignItems = YogaAlign.STRETCH,
              style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(
                    style =
                        StyleCompat.marginPx(YogaEdge.ALL, margin)
                            .flexGrow(1f)
                            .wrapInView()
                            .build()))
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
          Row(style = StyleCompat.widthPx(100).heightPx(100).build()) {
            child(
                Row(
                    style =
                        StyleCompat.positionType(YogaPositionType.ABSOLUTE)
                            .positionPx(YogaEdge.START, left)
                            .positionPx(YogaEdge.TOP, top)
                            .positionPx(YogaEdge.END, right)
                            .positionPx(YogaEdge.BOTTOM, bottom)
                            .wrapInView()
                            .build()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
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
      override fun ComponentScope.render(): Component {
        return Row(
            style =
                StyleCompat.border(
                        Border.create(context)
                            .color(YogaEdge.LEFT, Color.BLUE)
                            .color(YogaEdge.TOP, Color.RED)
                            .color(YogaEdge.RIGHT, Color.BLACK)
                            .color(YogaEdge.BOTTOM, Color.WHITE)
                            .radiusDip(Border.Corner.TOP_LEFT, 5f)
                            .radiusDip(Border.Corner.TOP_RIGHT, 6f)
                            .radiusDip(Border.Corner.BOTTOM_RIGHT, 7f)
                            .radiusDip(Border.Corner.BOTTOM_LEFT, 8f)
                            .build())
                    .build())
      }
    }

    val node = resolveComponentToNodeForTest(lithoViewRule.context, ComponentWithBorder())
    assertThat(node.borderColors)
        .isEqualTo(intArrayOf(Color.BLUE, Color.RED, Color.BLACK, Color.WHITE))
    assertThat(node.borderRadius).isEqualTo(floatArrayOf(5f, 6f, 7f, 8f))
  }
}
