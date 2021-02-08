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
import android.graphics.drawable.ColorDrawable
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.margin
import com.facebook.litho.flexbox.maxHeight
import com.facebook.litho.flexbox.maxWidth
import com.facebook.litho.flexbox.minHeight
import com.facebook.litho.flexbox.minWidth
import com.facebook.litho.flexbox.padding
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.flexbox.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.child
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaPositionType
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for common styles defined in [Style]. */
@RunWith(AndroidJUnit4::class)
class CommonStylesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun widthAndHeight_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px)) }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 100) })
  }

  @Test
  fun maxWidth_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style = Style.height(100.px).maxWidth(200.px),
              children =
                  listOf(
                      Row(style = Style.width(500.px)),
                  ))
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
          Row(
              style = Style.width(100.px).maxHeight(200.px),
              children =
                  listOf(
                      Row(style = Style.height(500.px)),
                  ))
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
          Row(
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(style = Style.flex(basis = 50.px).wrapInView()),
                  ))
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
          Row(
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(style = Style.flex(grow = 1f).wrapInView()),
                  ))
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
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(
                          style =
                              Style.height(100.px).minWidth(50.px).flex(shrink = 1f).wrapInView()),
                  ))
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
          Row(
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(style = Style.width(100.px).alignSelf(YogaAlign.STRETCH).wrapInView()),
                  ))
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
                      .padding(start = left.px, top = top.px, end = right.px, bottom = bottom.px),
              children =
                  listOf(
                      Row(style = Style.flex(grow = 1f).wrapInView()),
                  ))
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
                      .padding(horizontal = horizontal.px, vertical = vertical.px),
              children =
                  listOf(
                      Row(style = Style.flex(grow = 1f).wrapInView()),
                  ))
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
              style = Style.width(100.px).height(100.px).padding(padding.px),
              children =
                  listOf(
                      Row(style = Style.flex(grow = 1f).wrapInView()),
                  ))
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
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(
                          style =
                              Style.margin(
                                      start = left.px,
                                      top = top.px,
                                      end = right.px,
                                      bottom = bottom.px)
                                  .flex(grow = 1f)
                                  .wrapInView()),
                  ))
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
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(
                          style =
                              Style.margin(horizontal = horizontal.px, vertical = vertical.px)
                                  .flex(grow = 1f)
                                  .wrapInView()),
                  ))
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
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(style = Style.margin(margin.px).flex(grow = 1f).wrapInView()),
                  ))
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
          Row(
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(
                          style =
                              Style.positionType(YogaPositionType.ABSOLUTE)
                                  .position(
                                      start = left.px,
                                      top = top.px,
                                      end = right.px,
                                      bottom = bottom.px)
                                  .wrapInView()),
                  ))
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost> { bounds(left, top, 100 - left - right, 100 - top - bottom) }
            })
  }

  @Test
  fun background_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px).background(ColorDrawable(Color.WHITE)))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertHasColorDrawableOfColor(lithoViewRule.lithoView, Color.WHITE)
  }

  @Test
  fun foreground_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px).foreground(ColorDrawable(Color.WHITE)))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertHasColorDrawableOfColor(lithoViewRule.lithoView, Color.WHITE)
  }

  @Test
  fun onClick_whenSet_isDispatchedOnClick() {
    val wasClicked = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style = Style.width(200.px).height(200.px),
              children =
                  listOf(
                      Row(
                          style =
                              Style.width(100.px).height(100.px).viewTag("click_me").onClick {
                                wasClicked.set(true)
                              }),
                  ))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(wasClicked.get()).isFalse()
    lithoViewRule.findViewWithTag("click_me").performClick()
    assertThat(wasClicked.get()).isTrue()
  }

  @Test
  fun onLongClick_whenSet_isDispatchedOnLongClick() {
    val wasLongClicked = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style =
                  Style.width(100.px).height(100.px).viewTag("click_me").onLongClick {
                    wasLongClicked.set(true)
                    true
                  })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(wasLongClicked.get()).isFalse()
    lithoViewRule.findViewWithTag("click_me").performLongClick()
    assertThat(wasLongClicked.get()).isTrue()
  }

  @Test
  fun wrapInView_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style = Style.width(100.px).height(100.px),
              children =
                  listOf(
                      Row(style = Style.width(1.px).height(1.px).wrapInView()),
                  ))
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost>()
            })
  }

  @Test
  fun viewTag_whenSet_isAddedToView() {
    assertThat(
            lithoViewRule
                .setSizeSpecs(unspecified(), unspecified())
                .setRoot {
                  Row(
                      style = Style.width(200.px).height(200.px),
                      children =
                          listOf(
                              Row(style = Style.width(100.px).height(100.px).viewTag("view_tag")),
                          ))
                }
                .measure()
                .layout()
                .attachToWindow()
                .findViewWithTagOrNull("view_tag"))
        .isNotNull()
  }

  @Test
  fun onVisible_whenSet_firesWhenVisible() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).onVisible { eventFired.set(true) })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(eventFired.get()).isTrue()
  }

  @Test
  fun onFocusedVisible_whenSet_firesWhenVisible() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).onFocusedVisible { eventFired.set(true) })
        }
        .attachToWindow()

    // FocusedVisible requires a measured parent
    val frameLayout = FrameLayout(lithoViewRule.context.androidContext)
    frameLayout.addView(lithoViewRule.lithoView)
    frameLayout.measure(unspecified(), unspecified())
    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

    assertThat(eventFired.get()).isTrue()
  }

  @Test
  fun onFullImpression_whenSet_firesWhenVisible() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).onFullImpression { eventFired.set(true) })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(eventFired.get()).isTrue()
  }

  private fun assertHasColorDrawableOfColor(componentHost: ComponentHost, color: Int) {
    assertThat(componentHost.drawables).hasSize(1).first().isInstanceOf(MatrixDrawable::class.java)
    assertThat((componentHost.drawables[0] as MatrixDrawable<ColorDrawable>).mountedDrawable)
        .isInstanceOf(ColorDrawable::class.java)
        .extracting("color")
        .containsExactly(color)
  }
}
