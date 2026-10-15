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

import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.Layout
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import com.facebook.litho.CommonProps
import com.facebook.litho.Component
import com.facebook.litho.ComponentHost
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LayerType
import com.facebook.litho.LithoView
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.Style
import com.facebook.litho.assertMatches
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.key
import com.facebook.litho.kotlin.widget.TextInput
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.match
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.transition.transitionKey
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Unit tests for common styles defined in [Style]. */
@RunWith(LithoTestRunner::class)
class ViewStylesTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Test
  fun background_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).background(ColorDrawable(Color.WHITE)))
        }

    assertHasColorDrawableOfColor(testLithoView.lithoView, Color.WHITE)
  }

  @Test
  fun background_readsDrawablePaddingOnceOnStyleCreation_notOnEachResolve() {
    val background = PaddingCountingDrawable(paddingLeft = 12)
    val style = Style.background(background)

    repeat(2) {
      val commonProps = CommonProps()
      style.applyCommonProps(lithoTestRule.context, commonProps)
      assertThat(commonProps.background).isSameAs(background)
      assertThat(commonProps.paddingFromBackground).isEqualTo(Rect(12, 0, 0, 0))
    }

    assertThat(background.getPaddingCallCount).isEqualTo(1)
  }

  /**
   * Reproduces the `DrawableContainerState.createAllFutures` race between two resolves of the same
   * Style: one is paused while inflating a child of a [StateListDrawable], the other finishes
   * inflation and nulls the futures, and the paused resolve then NPEs.
   */
  @Test
  fun background_concurrentResolvesOfSharedStateListDrawable_doNotCrash() {
    val drawables = PausableStateListDrawables()
    val style = Style.background(drawables.create())
    drawables.pauseNextInflation()

    val firstResolve = ErrorRecordingThread {
      style.applyCommonProps(lithoTestRule.context, CommonProps())
    }
    firstResolve.start()
    drawables.awaitInflationPaused()
    style.applyCommonProps(lithoTestRule.context, CommonProps())
    drawables.resumeInflation()
    firstResolve.join()

    assertThat(firstResolve.error).isNull()
  }

  /**
   * Same race when the Style is created in a component's render: two overlapping resolves both
   * create a Style from one drawable held across renders, so both make its first padding read.
   */
  @Test
  fun background_concurrentStyleCreationWithSharedStateListDrawable_doesNotCrash() {
    val drawables = PausableStateListDrawables()
    val background = drawables.create()
    drawables.pauseNextInflation()

    val firstRender = ErrorRecordingThread { Style.background(background) }
    firstRender.start()
    drawables.awaitInflationPaused()
    val secondRender = ErrorRecordingThread { Style.background(background) }
    secondRender.start()
    // Unsynchronized, the second read finishes inflation here; synchronized, it waits.
    secondRender.join(500)
    drawables.resumeInflation()
    firstRender.join()
    secondRender.join()

    assertThat(firstRender.error).isNull()
    assertThat(secondRender.error).isNull()
  }

  @Test
  fun backgroundColor_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).backgroundColor(Color.WHITE))
        }

    assertHasColorDrawableOfColor(testLithoView.lithoView, Color.WHITE)
  }

  @Test
  fun clickable_whenSet_isRespected() {
    val testLithoView1 =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).clickable(true))
        }

    assertThat(testLithoView1.lithoView.isClickable).isTrue

    val testLithoView2 =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).clickable(false))
        }

    assertThat(testLithoView2.lithoView.isClickable).isFalse
  }

  @Test
  fun clipChildren_whenSet_isRespected() {
    val testLithoView1 =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).clipChildren(true))
        }

    assertThat(testLithoView1.lithoView.clipChildren).isTrue

    val testLithoView2 =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).clipChildren(false))
        }

    assertThat(testLithoView2.lithoView.clipChildren).isFalse
  }

  @RequiresApi(Build.VERSION_CODES.O)
  @Test
  fun defaultFocusHighlightEnabled_whenSet_isRespected() {
    val testLithoView1 =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).defaultFocusHighlightEnabled(false))
        }

    assertThat(testLithoView1.lithoView.defaultFocusHighlightEnabled).isFalse

    val testLithoView2 =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).defaultFocusHighlightEnabled(true))
        }

    assertThat(testLithoView2.lithoView.defaultFocusHighlightEnabled).isTrue
  }

  @RequiresApi(Build.VERSION_CODES.O)
  @Test
  fun defaultFocusHighlightEnabled_whenNotSet_isDefault() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px))
        }

    // Platform default for View.defaultFocusHighlightEnabled is true.
    assertThat(testLithoView.lithoView.defaultFocusHighlightEnabled).isTrue
  }

  @Test
  fun `duplicateChildrenStates - when set to true enables node to duplicate children states`() {
    class DuplicateChildrenStatesComponent : KComponent() {
      override fun ComponentScope.render(): Component? =
          Row(style = Style.width(200.px).height(200.px).duplicateChildrenStates(true)) {
            child(Row(style = Style.width(100.px).height(100.px)))
          }
    }

    val node = lithoTestRule.render { DuplicateChildrenStatesComponent() }.currentRootNode?.node
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

    val node = lithoTestRule.render { DuplicateChildrenStatesComponent() }.currentRootNode?.node
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

    val node = lithoTestRule.render { DuplicateParentStateComponent() }.currentRootNode?.node
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

    val node = lithoTestRule.render { DuplicateParentStateComponent() }.currentRootNode?.node
    val childNode = node?.getChildAt(0)
    assertThat(childNode?.isDuplicateParentStateEnabled).isFalse
  }

  @Test
  fun focusable_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).focusable(true))
        }

    assertThat(testLithoView.lithoView.isFocusable).isTrue
  }

  @Test
  fun focusable_whenNotSet_isDefault() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px))
        }

    assertThat(testLithoView.lithoView.getFocusable()).isEqualTo(View.FOCUSABLE_AUTO)
  }

  @Test
  fun foreground_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).foreground(ColorDrawable(Color.WHITE)))
        }

    assertHasColorDrawableOfColor(testLithoView.lithoView, Color.WHITE)
  }

  @Test
  fun `onFocused Changed - invoked when set`() {
    val didFocusChangeTrigger = AtomicBoolean(false)

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                TextInput(
                    initialText = "Try again",
                    style =
                        Style.width(100.px).height(100.px).onFocusedChanged {
                          didFocusChangeTrigger.set(true)
                        },
                ),
            )
          }
        }

    assertThat(didFocusChangeTrigger.get()).isFalse
    testLithoView.findViewWithTextOrNull("Try again")!!.requestFocus()
    assertThat(didFocusChangeTrigger.get()).isTrue
  }

  @Test
  fun `onFocused Changed - respects enablement check`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                TextInput(
                    initialText = "Try again",
                    style =
                        Style.width(100.px).height(100.px).onFocusedChanged(enabled = false) {
                          error("We should not have hit this code")
                        },
                ),
            )
          }
        }

    testLithoView.findViewWithTextOrNull("Try again")!!.requestFocus()
  }

  @Test
  fun `onFocused Changed - overrides prior set one when new (disabled) one set`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                TextInput(
                    initialText = "Try again",
                    style =
                        Style.width(100.px)
                            .height(100.px)
                            .onFocusedChanged { error("This one should have been overridden") }
                            .onFocusedChanged(enabled = false) {
                              error("We should not have hit this code")
                            },
                ),
            )
          }
        }

    testLithoView.findViewWithTextOrNull("Try again")!!.requestFocus()
  }

  @Test
  fun `onFocused Changed - overrides prior set one when new (enabled) one set`() {
    val didFocusChangeTrigger = AtomicBoolean(false)

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                TextInput(
                    initialText = "Try again",
                    style =
                        Style.width(100.px)
                            .height(100.px)
                            .onFocusedChanged { error("This one should have been overridden") }
                            .onFocusedChanged(enabled = true) { didFocusChangeTrigger.set(true) },
                ),
            )
          }
        }

    assertThat(didFocusChangeTrigger.get()).isFalse
    testLithoView.findViewWithTextOrNull("Try again")!!.requestFocus()
    assertThat(didFocusChangeTrigger.get()).isTrue
  }

  @Test
  fun onClick_whenSet_isDispatchedOnClick() {
    val wasClicked = AtomicBoolean(false)

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("click_me").onClick {
                          wasClicked.set(true)
                        },
                ),
            )
          }
        }

    assertThat(wasClicked.get()).isFalse()
    testLithoView.findViewWithTag("click_me").performClick()
    assertThat(wasClicked.get()).isTrue()
  }

  @Test
  fun `onClick - when set respects enable check`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("click_me").onClick(
                            enabled = false,
                        ) {
                          error("We should have not executed this code block")
                        },
                ),
            )
          }
        }

    testLithoView.findViewWithTag("click_me").performClick()
  }

  @Test
  fun `onClick - when override respects last setup`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
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
                            },
                ),
            )
          }
        }

    testLithoView.findViewWithTag("click_me").performClick()
  }

  @Test
  fun onLongClick_whenSet_isDispatchedOnLongClick() {
    val wasLongClicked = AtomicBoolean(false)

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(
              style =
                  Style.width(100.px).height(100.px).viewTag("click_me").onLongClick {
                    wasLongClicked.set(true)
                    true
                  },
          )
        }

    assertThat(wasLongClicked.get()).isFalse()
    testLithoView.findViewWithTag("click_me").performLongClick()
    assertThat(wasLongClicked.get()).isTrue()
  }

  @Test
  fun `onLongClick - when set respects enable check`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("click_me").onLongClick(
                            enabled = false,
                        ) {
                          error("We should have not executed this code block")
                        },
                ),
            )
          }
        }

    testLithoView.findViewWithTag("click_me").performLongClick()
  }

  @Test
  fun `onTouch - when set is dispatched on touch`() {
    val wasTouched = AtomicBoolean(false)

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onTouch {
                          wasTouched.set(true)
                          true
                        },
                ),
            )
          }
        }

    assertThat(wasTouched.get()).isFalse()
    testLithoView.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
    assertThat(wasTouched.get()).isTrue()
  }

  @Test
  fun `onTouch - when set respects enable check`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onTouch(
                            enabled = false,
                        ) {
                          error("We should have not executed this code block")
                        },
                ),
            )
          }
        }

    testLithoView.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
  }

  @Test
  fun `onInterceptTouch - when set is dispatched on touch`() {
    val wasTouchedIntercepted = AtomicBoolean(false)

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onInterceptTouch {
                          wasTouchedIntercepted.set(true)
                          true
                        },
                ),
            )
          }
        }

    assertThat(wasTouchedIntercepted.get()).isFalse()
    testLithoView.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
    assertThat(wasTouchedIntercepted.get()).isTrue()
  }

  @Test
  fun `onInterceptTouch - when set respects enable check`() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(200.px).height(200.px)) {
            child(
                Row(
                    style =
                        Style.width(100.px).height(100.px).viewTag("touch_me").onInterceptTouch(
                            enabled = false,
                        ) {
                          error("We should have not executed this code block")
                        },
                ),
            )
          }
        }

    testLithoView.findViewWithTag("touch_me").dispatchTouchEvent(getMotionEvent())
  }

  @Test
  fun rotation_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(
              style =
                  Style.width(100.px).height(100.px).rotation(90f).rotationX(45f).rotationY(30f),
          )
        }

    assertThat(testLithoView.lithoView.rotation).isEqualTo(90f)
    assertThat(testLithoView.lithoView.rotationX).isEqualTo(45f)
    assertThat(testLithoView.lithoView.rotationY).isEqualTo(30f)
  }

  @Test
  fun scale_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).scale(0.5f))
        }

    assertThat(testLithoView.lithoView.scaleX).isEqualTo(0.5f)
    assertThat(testLithoView.lithoView.scaleY).isEqualTo(0.5f)
  }

  @Test
  fun selected_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).selected(true))
        }

    assertThat(testLithoView.lithoView.isSelected).isTrue
  }

  @Test
  fun wrapInView_whenSet_isRespected() {
    lithoTestRule
        .render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px)) {
            child(Row(style = Style.width(1.px).height(1.px).wrapInView()))
          }
        }
        .assertMatches(
            match<LithoView> {
              bounds(0, 0, 100, 100)
              child<ComponentHost>()
            },
        )
  }

  @Test
  fun viewTag_whenSet_isAddedToView() {
    assertThat(
        lithoTestRule
            .render(widthSpec = unspecified(), heightSpec = unspecified()) {
              Row(style = Style.width(200.px).height(200.px)) {
                child(Row(style = Style.width(100.px).height(100.px).viewTag("view_tag")))
              }
            }
            .findViewWithTagOrNull("view_tag"),
    )
        .isNotNull()
  }

  @Test
  fun viewTags_whenSet_areAddedToView() {
    val viewTags =
        SparseArray<String>().apply {
          append(123, "first tag")
          append(456, "second tag")
        }

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).viewTags(viewTags))
        }

    assertThat(testLithoView.lithoView.getTag(123)).isEqualTo("first tag")
    assertThat(testLithoView.lithoView.getTag(456)).isEqualTo("second tag")
  }

  @Test
  fun alpha_whenSet_isRespected() {
    val alpha = 0.5f

    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).alpha(alpha))
        }

    assertThat(testLithoView.lithoView.alpha).isEqualTo(alpha)
  }

  /**
   * Test is using [Layout] and [NodeInfo] classes as a workaround for the issue with 'libyoga.so
   * already loaded in another classloader exception' caused by multiple ClassLoaders trying to load
   * Yoga when using @Config to specify a different target sdk.
   */
  @Test
  fun elevation_whenSet_isRespected() {
    val elevation = 1f

    class ElevationComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.elevation(elevation.px))
      }
    }

    val node = lithoTestRule.render { ElevationComponent() }.currentRootNode?.node
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
                    spotShadowColor = Color.BLUE,
                ),
        )
      }
    }

    val node = lithoTestRule.render { ShadowComponent() }.currentRootNode?.node
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

    val node = lithoTestRule.render { OutlineProviderComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
  }

  /** Tests for clip() with ViewOutlineProvider */
  @Test
  fun clip_withViewOutlineProviderBounds_setsOutlineProvider() {
    val outlineProvider = ViewOutlineProvider.BOUNDS

    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(outlineProvider))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
  }

  @Test
  fun clip_withViewOutlineProviderBackground_setsOutlineProvider() {
    val outlineProvider = ViewOutlineProvider.BACKGROUND

    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(outlineProvider))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
  }

  @Test
  fun clip_withViewOutlineProviderPaddedBounds_setsOutlineProvider() {
    val outlineProvider = ViewOutlineProvider.PADDED_BOUNDS

    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(outlineProvider))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
  }

  @Test
  fun clip_withMockedViewOutlineProvider_setsOutlineProvider() {
    val outlineProvider = mock<ViewOutlineProvider>()

    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(outlineProvider))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
  }

  @Test
  fun clip_withNullViewOutlineProvider_setsNullOutlineProvider() {
    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(null as ViewOutlineProvider?))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isNull()
  }

  /** Tests for clip() with Shape */
  @Test
  fun clip_withShapeCircle_setsOutlineProviderAndClipToOutline() {
    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(Shape.Circle))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isNotNull
    assertThat(nodeInfo?.clipToOutline).isTrue
  }

  @Test
  fun clip_withShapeRoundedRectangle_setsOutlineProviderAndClipToOutline() {
    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(Shape.RoundedRectangle(16.dp)))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isNotNull
    assertThat(nodeInfo?.clipToOutline).isTrue
  }

  @Test
  fun clip_withShapePerCornerRoundedRectangle_setsOutlineProviderAndClipToOutline() {
    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(Shape.RoundedRectangle(topStart = 16.dp, topEnd = 16.dp)))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isNotNull
    assertThat(nodeInfo?.clipToOutline).isTrue
  }

  @Test
  fun roundedRectangle_withSingleRadius_resolvesToUniformShape() {
    assertThat(Shape.RoundedRectangle(12.dp)).isInstanceOf(RoundedRectangleShape::class.java)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withAllCornersEqual_isRoundRectOverBounds() {
    val outline = perCornerOutline(10f, 10f, 10f, 10f)

    assertRoundRect(outline, Rect(0, 0, 100, 50), 10f)
  }

  @Config(sdk = [Build.VERSION_CODES.O_MR1])
  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withTopCornersOnlyBeforeApi33_extendsPastBottomEdge() {
    val outline = perCornerOutline(topStart = 10f, topEnd = 10f)

    assertRoundRect(outline, Rect(0, 0, 100, 60), 10f)
  }

  @Config(sdk = [Build.VERSION_CODES.O_MR1])
  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withBottomCornersOnlyBeforeApi33_extendsPastTopEdge() {
    val outline = perCornerOutline(bottomEnd = 10f, bottomStart = 10f)

    assertRoundRect(outline, Rect(0, -10, 100, 50), 10f)
  }

  @Config(sdk = [Build.VERSION_CODES.O_MR1])
  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withStartCornersOnlyBeforeApi33_extendsPastRightEdgeInLtr() {
    val outline = perCornerOutline(topStart = 10f, bottomStart = 10f)

    assertRoundRect(outline, Rect(0, 0, 110, 50), 10f)
  }

  @Config(sdk = [Build.VERSION_CODES.O_MR1])
  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withStartCornersOnlyBeforeApi33_extendsPastLeftEdgeInRtl() {
    val outline = perCornerOutline(topStart = 10f, bottomStart = 10f, isRtl = true)

    assertRoundRect(outline, Rect(-10, 0, 100, 50), 10f)
  }

  @Config(sdk = [Build.VERSION_CODES.O_MR1])
  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_fromRtlView_resolvesStartToRight() {
    val androidContext = lithoTestRule.context.androidContext
    // Views only resolve to RTL when the app declares RTL support.
    androidContext.applicationInfo.flags =
        androidContext.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
    val view = View(androidContext)
    view.layoutDirection = View.LAYOUT_DIRECTION_RTL
    view.measure(exactly(100), exactly(50))
    view.layout(0, 0, 100, 50)
    val outline = Outline()

    PerCornerRoundedRectangleOutlineProvider(10f, 0f, 0f, 10f).getOutline(view, outline)

    assertRoundRect(outline, Rect(-10, 0, 100, 50), 10f)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withNoRadius_isRect() {
    val outline = perCornerOutline()

    assertRoundRect(outline, Rect(0, 0, 100, 50), 0f)
  }

  @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
  @Test
  fun perCornerOutline_withTopCornersOnlyOnApi33_isPathWithinBounds() {
    assertIsPath(perCornerOutline(topStart = 10f, topEnd = 10f))
  }

  @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
  @Test
  fun perCornerOutline_withMixedRadiiOnApi33_isPath() {
    assertIsPath(perCornerOutline(16f, 16f, 4f, 4f))
    assertIsPath(perCornerOutline(16f, 16f, 4f, 4f, isRtl = true))
  }

  @Config(sdk = [Build.VERSION_CODES.O_MR1])
  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  fun perCornerOutline_withMixedRadiiBeforeApi33_usesSmallestRadius() {
    val outline = perCornerOutline(16f, 16f, 4f, 0f)

    assertRoundRect(outline, Rect(0, 0, 100, 50), 4f)
  }

  private fun perCornerOutline(
      topStart: Float = 0f,
      topEnd: Float = 0f,
      bottomEnd: Float = 0f,
      bottomStart: Float = 0f,
      isRtl: Boolean = false,
  ): Outline {
    val outline = Outline()
    PerCornerRoundedRectangleOutlineProvider(topStart, topEnd, bottomEnd, bottomStart)
        .setOutline(outline, width = 100, height = 50, isRtl = isRtl)
    return outline
  }

  @RequiresApi(Build.VERSION_CODES.N)
  private fun assertRoundRect(outline: Outline, expectedRect: Rect, expectedRadius: Float) {
    val rect = Rect()
    assertThat(outline.getRect(rect)).isTrue
    assertThat(rect).isEqualTo(expectedRect)
    assertThat(outline.radius).isEqualTo(expectedRadius)
  }

  private fun assertIsPath(outline: Outline) {
    assertThat(outline.isEmpty).isFalse
    // Hidden Outline.MODE_PATH; MODE_ROUND_RECT is 1.
    assertThat(ReflectionHelpers.getField<Int>(outline, "mMode")).isEqualTo(2)
    val bounds = RectF()
    ReflectionHelpers.getField<Path>(outline, "mPath").computeBounds(bounds, true)
    assertThat(bounds).isEqualTo(RectF(0f, 0f, 100f, 50f))
  }

  @Test
  fun clip_withViewOutlineProvider_andClipToOutline_bothAreSet() {
    val outlineProvider = ViewOutlineProvider.BOUNDS

    class ClipComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.clip(outlineProvider).clipToOutline(true))
      }
    }

    val node = lithoTestRule.render { ClipComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.outlineProvider).isEqualTo(outlineProvider)
    assertThat(nodeInfo?.clipToOutline).isTrue
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun clipToOutline_whenSet_isRespected() {
    class ComponentThatClips : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = Style.clipToOutline(true))
      }
    }

    val node = lithoTestRule.render { ComponentThatClips() }.currentRootNode?.node
    assertThat(node?.nodeInfo?.clipToOutline).isTrue

    node?.mutableNodeInfo()?.clipToOutline = false
    assertThat(node?.nodeInfo?.clipToOutline).isFalse
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun transitionName_whenSet_isRespected() {
    class ElevationComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = Style.transitionName("test"))
      }
    }

    val node = lithoTestRule.render { ElevationComponent() }.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.transitionName).isEqualTo("test")
  }

  /** See comment on [elevation_whenSet_isRespected] above. */
  @Test
  fun testKey_whenSet_isRespected() {
    class TestKeyComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = Style.testKey("test"))
      }
    }

    val node = lithoTestRule.render { TestKeyComponent() }.currentRootNode?.node
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

    val node = lithoTestRule.render { key("root") { TestComponent() } }.currentRootNode?.node
    assertThat(node?.transitionKey).isEqualTo("test")
    assertThat(node?.transitionOwnerKey?.toString()).isEqualTo("\$root")
  }

  @Test
  fun keyboardNavigationCluster_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).keyboardNavigationCluster(true))
        }

    assertThat(testLithoView.lithoView.isKeyboardNavigationCluster).isTrue
  }

  @Test
  fun tooltipText_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).tooltipText("Tooltip Text"))
        }

    assertThat(testLithoView.lithoView.tooltipText).isEqualTo("Tooltip Text")
  }

  @Test
  fun layerType_whenSet_isRespected() {
    val testLithoView =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Row(style = Style.width(100.px).height(100.px).layerType(LayerType.LAYER_TYPE_HARDWARE))
        }

    assertThat(testLithoView.lithoView.layerType).isEqualTo(View.LAYER_TYPE_HARDWARE)
  }

  private fun assertHasColorDrawableOfColor(componentHost: ComponentHost, color: Int) {
    assertThat(componentHost.drawables).hasSize(1).first().isInstanceOf(MatrixDrawable::class.java)
    assertThat((componentHost.drawables[0] as MatrixDrawable<ColorDrawable>).mountedDrawable)
        .isInstanceOf(ColorDrawable::class.java)
        .extracting("color")
        .isEqualTo(color)
  }

  private fun getMotionEvent() = MotionEvent.obtain(0L, 1L, MotionEvent.ACTION_DOWN, 0f, 0f, 0)

  /**
   * Creates [StateListDrawable]s the way Resources#getDrawable does, as clones of a cached
   * ConstantState with uninflated children, and can pause the next child inflation.
   */
  private class PausableStateListDrawables {
    private val pauseNextInflation = AtomicBoolean(false)
    private val inflationPaused = CountDownLatch(1)
    private val resumeInflation = CountDownLatch(1)
    private val childState =
        object : Drawable.ConstantState() {
          override fun newDrawable(): Drawable {
            if (pauseNextInflation.compareAndSet(true, false)) {
              inflationPaused.countDown()
              resumeInflation.await(5, TimeUnit.SECONDS)
            }
            return Color.WHITE.toDrawable()
          }

          override fun getChangingConfigurations(): Int = 0
        }

    /** The clone eagerly inflates its current-state child, which leaves the other two pending. */
    fun create(): Drawable {
      val template =
          StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), ConstantStateDrawable(childState))
            addState(intArrayOf(android.R.attr.state_focused), ConstantStateDrawable(childState))
            addState(intArrayOf(), ConstantStateDrawable(childState))
          }
      return checkNotNull(template.constantState).newDrawable()
    }

    fun pauseNextInflation() = pauseNextInflation.set(true)

    fun awaitInflationPaused() {
      inflationPaused.await(1, TimeUnit.SECONDS)
    }

    fun resumeInflation() = resumeInflation.countDown()
  }

  private class ConstantStateDrawable(private val state: Drawable.ConstantState) :
      ColorDrawable(Color.WHITE) {
    override fun getConstantState(): Drawable.ConstantState = state
  }

  private class ErrorRecordingThread(private val block: () -> Unit) : Thread() {
    @Volatile var error: Throwable? = null

    override fun run() {
      try {
        block()
      } catch (t: Throwable) {
        error = t
      }
    }
  }

  private class PaddingCountingDrawable(private val paddingLeft: Int) : ColorDrawable(Color.WHITE) {

    var getPaddingCallCount: Int = 0
      private set

    override fun getPadding(padding: Rect): Boolean {
      getPaddingCallCount++
      padding.set(paddingLeft, 0, 0, 0)
      return true
    }
  }
}
