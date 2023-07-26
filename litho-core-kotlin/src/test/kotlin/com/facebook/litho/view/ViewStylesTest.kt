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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Layout
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.key
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.child
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.transition.transitionKey
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Unit tests for common styles defined in [Style]. */
@RunWith(LithoTestRunner::class)
class ViewStylesTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()

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
  fun `duplicateChildrenStates - when set to true enables node to duplicate children states`() {
    class DuplicateChildrenStatesComponent : KComponent() {
      override fun ComponentScope.render(): Component? =
          Row(style = Style.width(200.px).height(200.px).duplicateChildrenStates(true)) {
            child(Row(style = Style.width(100.px).height(100.px)))
          }
    }

    val node =
        LegacyLithoViewRule.getRootLayout(lithoViewRule, DuplicateChildrenStatesComponent())?.node
    assertThat(node?.isDuplicateChildrenStatesEnabled).isTrue
  }

  @Test
  fun `duplicateChildrenStates - when set to false disables node from duplicating children states`() {
    class DuplicateChildrenStatesComponent : KComponent() {
      override fun ComponentScope.render(): Component? =
          Row(style = Style.width(200.px).height(200.px).duplicateChildrenStates(false)) {
            child(Row(style = Style.width(100.px).height(100.px)))
          }
    }

    val node =
        LegacyLithoViewRule.getRootLayout(lithoViewRule, DuplicateChildrenStatesComponent())?.node
    assertThat(node?.isDuplicateChildrenStatesEnabled).isFalse
  }

  @Test
  fun `duplicateParentState - when set to true sets node enabled to duplicate parent state`() {
    class DuplicateParentStateComponent : KComponent() {
      override fun ComponentScope.render(): Component? =
          Row(style = Style.width(200.px).height(200.px)) {
            child(Row(style = Style.width(100.px).height(100.px).duplicateParentState(true)))
          }
    }

    val node =
        LegacyLithoViewRule.getRootLayout(lithoViewRule, DuplicateParentStateComponent())?.node
    val childNode = node?.getChildAt(0)
    assertThat(childNode?.isDuplicateParentStateEnabled).isTrue
  }

  @Test
  fun `duplicateParentState - when set to false sets node disabled from duplicating parent state`() {
    class DuplicateParentStateComponent : KComponent() {
      override fun ComponentScope.render(): Component? =
          Row(style = Style.width(200.px).height(200.px)) {
            child(Row(style = Style.width(100.px).height(100.px).duplicateParentState(false)))
          }
    }

    val node =
        LegacyLithoViewRule.getRootLayout(lithoViewRule, DuplicateParentStateComponent())?.node
    val childNode = node?.getChildAt(0)
    assertThat(childNode?.isDuplicateParentStateEnabled).isFalse
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
  fun `onClick - when set respects enable check`() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("click_me").onClick(
                            enabled = false) {
                              error("We should have not executed this code block")
                            }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("click_me").performClick()
  }

  @Test
  fun `onClick - when override respects last setup`() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px)
                            .height(100.px)
                            .viewTag("click_me")
                            .onClick { error("We should have not executed this code block") }
                            .onClick(enabled = false) {
                              error("We should have not executed this code block")
                            }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("click_me").performClick()
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
  fun `onLongClick - when set respects enable check`() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("click_me").onLongClick(
                            enabled = false) {
                              error("We should have not executed this code block")
                              true
                            }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("click_me").performLongClick()
  }

  @Test
  fun `onTouch - when set is dispatched on touch`() {
    val wasTouched = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onTouch {
                          wasTouched.set(true)
                          true
                        }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(wasTouched.get()).isFalse()
    lithoViewRule.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
    assertThat(wasTouched.get()).isTrue()
  }

  @Test
  fun `onTouch - when set respects enable check`() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onTouch(
                            enabled = false) {
                              error("We should have not executed this code block")
                              true
                            }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
  }

  @Test
  fun `onInterceptTouch - when set is dispatched on touch`() {
    val wasTouchedIntercepted = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onInterceptTouch {
                          wasTouchedIntercepted.set(true)
                          true
                        }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(wasTouchedIntercepted.get()).isFalse()
    lithoViewRule.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
    assertThat(wasTouchedIntercepted.get()).isTrue()
  }

  @Test
  fun `onInterceptTouch - when set respects enable check`() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onInterceptTouch(
                            enabled = false) {
                              error("We should have not executed this code block")
                              true
                            }))
          }
        }
        .measure()
        .layout()
        .attachToWindow()

    lithoViewRule.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
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
    val elevation = 1f

    class ElevationComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.elevation(elevation.px))
      }
    }

    val node = LegacyLithoViewRule.getRootLayout(lithoViewRule, ElevationComponent())?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.shadowElevation).isEqualTo(elevation)
  }

  @Test
  fun shadow_whenSet_isRespected() {
    val outlineProvider = mock<ViewOutlineProvider>()

    class ShadowComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(
            style =
                Style.shadow(
                    elevation = 16.px,
                    outlineProvider = outlineProvider,
                    ambientShadowColor = Color.RED,
                    spotShadowColor = Color.BLUE))
      }
    }

    val node = LegacyLithoViewRule.getRootLayout(lithoViewRule, ShadowComponent())?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.shadowElevation).isEqualTo(16.0f)
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
    assertThat(nodeInfo?.ambientShadowColor).isEqualTo(Color.RED)
    assertThat(nodeInfo?.spotShadowColor).isEqualTo(Color.BLUE)
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

    val node = LegacyLithoViewRule.getRootLayout(lithoViewRule, OutlineProviderComponent())?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun clipToOutline_whenSet_isRespected() {
    class ComponentThatClips : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clipToOutline(true))
      }
    }

    val node = LegacyLithoViewRule.getRootLayout(lithoViewRule, ComponentThatClips())?.node
    assertThat(node?.nodeInfo?.clipToOutline).isTrue

    node?.mutableNodeInfo()?.setClipToOutline(false)
    assertThat(node?.nodeInfo?.clipToOutline).isFalse
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun transitionName_whenSet_isRespected() {
    class ElevationComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.transitionName("test"))
      }
    }

    val node = LegacyLithoViewRule.getRootLayout(lithoViewRule, ElevationComponent())?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.transitionName).isEqualTo("test")
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun testKey_whenSet_isRespected() {
    class TestKeyComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.testKey("test"))
      }
    }

    val node = LegacyLithoViewRule.getRootLayout(lithoViewRule, TestKeyComponent())?.node
    assertThat(node?.testKey).isEqualTo("test")
  }

  @Test
  fun transitionOwnerKey_whenSet_isFromCorrectComponentContext() {
    class TestKComponent(private val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = style)
      }
    }

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return key("testKcomponent") {
          TestKComponent(style = Style.transitionKey(context, "test"))
        }
      }
    }

    val node =
        LegacyLithoViewRule.getRootLayout(lithoViewRule, key("root") { TestComponent() })?.node
    assertThat(node?.transitionKey).isEqualTo("test")
    assertThat(node?.transitionOwnerKey).isEqualTo("\$root")
  }

  private fun assertHasColorDrawableOfColor(componentHost: ComponentHost, color: Int) {
    assertThat(componentHost.drawables).hasSize(1).first().isInstanceOf(MatrixDrawable::class.java)
    assertThat((componentHost.drawables[0] as MatrixDrawable<ColorDrawable>).mountedDrawable)
        .isInstanceOf(ColorDrawable::class.java)
        .extracting("color")
        .containsExactly(color)
  }

  private fun getMotionEvent() = MotionEvent.obtain(0L, 1L, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
}
