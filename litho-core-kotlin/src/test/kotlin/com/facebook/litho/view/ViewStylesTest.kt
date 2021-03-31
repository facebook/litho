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

package com.facebook.litho.view

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Layout
import android.util.SparseArray
import android.view.ViewOutlineProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Border
import com.facebook.litho.Component
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.flexbox.height
import com.facebook.litho.flexbox.width
import com.facebook.litho.px
import com.facebook.litho.resolveComponentToNodeForTest
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.child
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import com.facebook.yoga.YogaEdge
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for common styles defined in [Style]. */
@RunWith(AndroidJUnit4::class)
class ViewStylesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

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
  fun backgroundColor_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).backgroundColor(Color.WHITE)) }
        .measure()
        .layout()
        .attachToWindow()

    assertHasColorDrawableOfColor(lithoViewRule.lithoView, Color.WHITE)
  }

  @Test
  fun clickable_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).clickable(true)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.isClickable).isTrue

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).clickable(false)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.isClickable).isFalse
  }

  @Test
  fun clipChildren_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).clipChildren(true)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.clipChildren).isTrue

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).clipChildren(false)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.clipChildren).isFalse
  }

  @Test
  fun focusable_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).focusable(true)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.isFocusable).isTrue
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
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("click_me").onClick {
                          wasClicked.set(true)
                        }))
          }
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
  fun rotation_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style =
                  Style.width(100.px).height(100.px).rotation(90f).rotationX(45f).rotationY(30f))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.rotation).isEqualTo(90f)
    assertThat(lithoViewRule.lithoView.rotationX).isEqualTo(45f)
    assertThat(lithoViewRule.lithoView.rotationY).isEqualTo(30f)
  }

  @Test
  fun scale_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).scale(0.5f)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.scaleX).isEqualTo(0.5f)
    assertThat(lithoViewRule.lithoView.scaleY).isEqualTo(0.5f)
  }

  @Test
  fun selected_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).selected(true)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.isSelected).isTrue
  }

  @Test
  fun wrapInView_whenSet_isRespected() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.width(1.px).height(1.px).wrapInView()))
          }
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
                  Row(style = Style.width(200.px).height(200.px)) {
                    child(Row(style = Style.width(100.px).height(100.px).viewTag("view_tag")))
                  }
                }
                .measure()
                .layout()
                .attachToWindow()
                .findViewWithTagOrNull("view_tag"))
        .isNotNull()
  }

  @Test
  fun viewTags_whenSet_areAddedToView() {
    val viewTags =
        SparseArray<String>().apply {
          append(123, "first tag")
          append(456, "second tag")
        }

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).viewTags(viewTags)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.getTag(123)).isEqualTo("first tag")
    assertThat(lithoViewRule.lithoView.getTag(456)).isEqualTo("second tag")
  }

  @Test
  fun alpha_whenSet_isRespected() {
    val alpha = 0.5f

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot { Row(style = Style.width(100.px).height(100.px).alpha(alpha)) }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.alpha).isEqualTo(alpha)
  }

  /**
   * Test is using [Layout] and [NodeInfo] classes as a workaround for the issue with 'libyoga.so
   * already loaded in another classloader exception' caused by multiple ClassLoaders trying to load
   * Yoga when using @Config to specify a different target sdk. See:
   * https://www.internalfb.com/intern/staticdocs/litho/docs/testing/unit-testing/
   */
  @Test
  fun elevation_whenSet_isRespected() {
    val elevation = 0.5f

    class ElevationComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.elevation(elevation))
      }
    }

    val node = resolveComponentToNodeForTest(lithoViewRule.context, ElevationComponent())
    val nodeInfo = node.orCreateNodeInfo
    assertThat(nodeInfo.shadowElevation).isEqualTo(elevation)
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun outlineProvider_whenSet_isRespected() {
    val outlineProvider = ViewOutlineProvider.BOUNDS
    class OutlineProviderComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.outlineProvider(outlineProvider))
      }
    }

    val node = resolveComponentToNodeForTest(lithoViewRule.context, OutlineProviderComponent())
    val nodeInfo = node.orCreateNodeInfo
    assertThat(nodeInfo.outlineProvider).isEqualTo(outlineProvider)
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
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

    val node = resolveComponentToNodeForTest(lithoViewRule.context, ComponentWithBorder())
    assertThat(node.borderColors)
        .isEqualTo(intArrayOf(Color.BLUE, Color.RED, Color.BLACK, Color.WHITE))
    assertThat(node.borderRadius).isEqualTo(floatArrayOf(5f, 6f, 7f, 8f))
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun clipToOutline_whenSet_isRespected() {
    class ComponentThatClips : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clipToOutline(true))
      }
    }

    val node = resolveComponentToNodeForTest(lithoViewRule.context, ComponentThatClips())
    assertThat(node.getOrCreateNodeInfo().clipToOutline).isTrue

    node.getOrCreateNodeInfo().setClipToOutline(false)
    assertThat(node.getOrCreateNodeInfo().clipToOutline).isFalse
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun transitionName_whenSet_isRespected() {
    class ElevationComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.transitionName("test"))
      }
    }

    val node = resolveComponentToNodeForTest(lithoViewRule.context, ElevationComponent())
    val nodeInfo = node.orCreateNodeInfo
    assertThat(nodeInfo.transitionName).isEqualTo("test")
  }

  private fun assertHasColorDrawableOfColor(componentHost: ComponentHost, color: Int) {
    assertThat(componentHost.drawables).hasSize(1).first().isInstanceOf(MatrixDrawable::class.java)
    assertThat((componentHost.drawables[0] as MatrixDrawable<ColorDrawable>).mountedDrawable)
        .isInstanceOf(ColorDrawable::class.java)
        .extracting("color")
        .containsExactly(color)
  }
}
