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

package com.facebook.litho.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.AccessibilityUtils
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.ErrorEventHandler
import com.facebook.litho.EventDispatcherUtils
import com.facebook.litho.EventHandler
import com.facebook.litho.KComponent
import com.facebook.litho.OnInitializeAccessibilityEventEvent
import com.facebook.litho.OnInitializeAccessibilityNodeInfoEvent
import com.facebook.litho.OnPopulateAccessibilityEventEvent
import com.facebook.litho.OnPopulateAccessibilityNodeEvent
import com.facebook.litho.PerformActionForVirtualViewEvent
import com.facebook.litho.SendAccessibilityEventEvent
import com.facebook.litho.SendAccessibilityEventUncheckedEvent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.accessibilityProbe
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAccessibilityManager

/** Unit tests for accessibility styles. */
@RunWith(LithoTestRunner::class)
class AccessibilityStylesTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private lateinit var shadowManager: ShadowAccessibilityManager

  @Before
  fun setUp() {
    val manager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowManager = Shadows.shadowOf(manager)
    shadowManager.setEnabled(true)
    shadowManager.setTouchExplorationEnabled(true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @Test
  fun contentDescription_whenSet_isSetOnView() {
    val testLithoView = lithoTestRule.render {
      Row(style = Style.width(200.px).height(200.px).contentDescription("Accessibility Test"))
    }

    assertThat(testLithoView.lithoView.contentDescription).isEqualTo("Accessibility Test")
  }

  @Test
  fun contentDescription_whenNull_isNotSet() {
    val testLithoView = lithoTestRule.render {
      Row(style = Style.width(200.px).height(200.px).contentDescription(null))
    }

    assertThat(testLithoView.lithoView.contentDescription).isNull()
  }

  @Test
  fun importantForAccessibility_whenSet_isSetOnView() {
    val testLithoView = lithoTestRule.render {
      Row(
          style =
              Style.width(200.px)
                  .height(200.px)
                  .importantForAccessibility(ImportantForAccessibility.YES),
      )
    }

    assertThat(testLithoView.lithoView.importantForAccessibility)
        .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
  }

  /**
   * Test is using [Layout] and [NodeInfo] classes as a workaround to be able to get the
   * onInitializeAccessibilityNodeInfoHandler value from the Node
   */
  @Test
  fun onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onInitializeAccessibilityNodeInfoHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onInitializeAccessibilityNodeInfo_whenSet_isSetOnView() {
    val eventHandler: EventHandler<OnInitializeAccessibilityNodeInfoEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onInitializeAccessibilityNodeInfo { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onInitializeAccessibilityNodeInfoHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onInitializeAccessibilityEvent_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onInitializeAccessibilityEventHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onInitializeAccessibilityEvent_whenSet_isSetOnView() {
    val eventHandler: EventHandler<OnInitializeAccessibilityEventEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onInitializeAccessibilityEvent { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onInitializeAccessibilityEventHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onPopulateAccessibilityEvent_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onPopulateAccessibilityEventHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onPopulateAccessibilityEvent_whenSet_isSetOnView() {
    val eventHandler: EventHandler<OnPopulateAccessibilityEventEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onPopulateAccessibilityEvent { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onPopulateAccessibilityEventHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onPopulateAccessibilityNode_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onPopulateAccessibilityNodeHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onPopulateAccessibilityNode_whenSet_isSetOnView() {
    val eventHandler: EventHandler<OnPopulateAccessibilityNodeEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onPopulateAccessibilityNode { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onPopulateAccessibilityNodeHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onRequestSendAccessibilityEvent_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onRequestSendAccessibilityEventHandler).isNull()
  }

  @Test
  fun onRequestSendAccessibilityEventWithResult_whenTrue_propagatesDescendantEvent() {
    val testLithoView = lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onRequestSendAccessibilityEventWithResult {
                it.superDelegate.onRequestSendAccessibilityEvent(it.host, it.child, it.event)
              },
      ) {
        child(
            Row(
                style = Style.width(100.px).height(100.px).contentDescription(DESCENDANT_LABEL),
            ),
        )
      }
    }

    val eventCountBefore = shadowManager.sentAccessibilityEvents.size
    testLithoView
        .findViewWithContentDescription(DESCENDANT_LABEL)
        .sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

    assertThat(shadowManager.sentAccessibilityEvents.drop(eventCountBefore).map { it.eventType })
        .contains(AccessibilityEvent.TYPE_VIEW_FOCUSED)
  }

  @Test
  fun onRequestSendAccessibilityEventWithResult_whenFalse_blocksDescendantEvent() {
    val testLithoView = lithoTestRule.render {
      Row(
          style =
              Style.width(200.px).height(200.px).onRequestSendAccessibilityEventWithResult {
                false
              },
      ) {
        child(
            Row(
                style = Style.width(100.px).height(100.px).contentDescription(DESCENDANT_LABEL),
            ),
        )
      }
    }

    val eventCountBefore = shadowManager.sentAccessibilityEvents.size
    testLithoView
        .findViewWithContentDescription(DESCENDANT_LABEL)
        .sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)

    assertThat(shadowManager.sentAccessibilityEvents.drop(eventCountBefore).map { it.eventType })
        .doesNotContain(AccessibilityEvent.TYPE_VIEW_FOCUSED)
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun performAccessibilityAction_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.performAccessibilityActionHandler).isNull()
  }

  @Test
  fun onPerformAccessibilityActionWithResult_returnsWhetherActionWasHandled() {
    val probe =
        lithoTestRule
            .render {
              Row(
                  style =
                      Style.width(200.px)
                          .height(200.px)
                          .contentDescription(ACTION_LABEL)
                          .onPerformAccessibilityActionWithResult {
                            it.action == AccessibilityNodeInfo.ACTION_CLICK
                          },
              )
            }
            .accessibilityProbe()
    val target = probe.require { it.contentDescription == ACTION_LABEL }

    assertThat(target.performAction(AccessibilityNodeInfo.ACTION_CLICK)).isTrue()
    assertThat(target.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)).isFalse()
  }

  @Test
  fun onPerformAccessibilityActionWithResult_returnsSuperResult() {
    val target =
        lithoTestRule
            .render {
              Row(
                  style =
                      Style.width(200.px)
                          .height(200.px)
                          .contentDescription(ACTION_LABEL)
                          .onPerformAccessibilityActionWithResult {
                            it.superDelegate.performAccessibilityAction(
                                it.host,
                                it.action,
                                it.args,
                            )
                          },
              )
            }
            .accessibilityProbe()
            .require { it.contentDescription == ACTION_LABEL }

    assertThat(target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)).isTrue()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onSendAccessibilityEvent_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onSendAccessibilityEvent_whenSet_isSetOnView() {
    val eventHandler: EventHandler<SendAccessibilityEventEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onSendAccessibilityEvent { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onSendAccessibilityEventUnchecked_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventUncheckedHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onSendAccessibilityEventUnchecked_whenSet_isSetOnView() {
    val eventHandler: EventHandler<SendAccessibilityEventUncheckedEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onSendAccessibilityEventUnchecked { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventUncheckedHandler).isNotNull
  }

  @Test
  fun performActionForVirtualView_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onPerformActionForVirtualViewHandler).isNull()
  }

  @Test
  fun performActionForVirtualView_whenSet_isSetOnView() {
    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onPerformActionForVirtualViewWithResult { false })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onPerformActionForVirtualViewHandler).isNotNull
  }

  @Test
  fun onPerformActionForVirtualViewWithResult_returnsCallbackResult() {
    var handled = true
    val rendered = lithoTestRule.render {
      Row(
          style =
              Style.width(200.px)
                  .height(200.px)
                  .contentDescription(ACTION_LABEL)
                  .onPerformActionForVirtualViewWithResult { handled },
      )
    }

    assertThat(rendered.dispatchVirtualViewAction(AccessibilityNodeInfo.ACTION_CLICK)).isTrue()
    handled = false
    assertThat(rendered.dispatchVirtualViewAction(AccessibilityNodeInfo.ACTION_CLICK)).isFalse()
  }

  @Test
  fun virtualViewKeyboardFocusChanged_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onVirtualViewKeyboardFocusChangedHandler).isNull()
  }

  @Test
  fun virtualViewKeyboardFocusChanged_whenSet_isSetOnView() {
    val eventHandler: EventHandler<PerformActionForVirtualViewEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onVirtualViewKeyboardFocusChanged { eventHandler })
      }
    }
    val testLithoView = lithoTestRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onVirtualViewKeyboardFocusChangedHandler).isNotNull
  }

  @Test
  fun accessibilityRole_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px).accessibilityRole(null))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.accessibilityRole).isNull()
  }

  @Test
  fun accessibilityRoleDescription_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px).accessibilityRoleDescription(null))
      }
    }
    val testLithoView = lithoTestRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.accessibilityRoleDescription).isNull()
  }

  @Test
  fun onPerformAccessibilityActionWithResult_whenCallbackThrows_usesErrorBoundary() =
      assertCallbackExceptionHandled(Callback.ACTION)

  @Test
  fun onRequestSendAccessibilityEventWithResult_whenCallbackThrows_usesErrorBoundary() =
      assertCallbackExceptionHandled(Callback.REQUEST)

  @Test
  fun onPerformActionForVirtualViewWithResult_whenCallbackThrows_usesErrorBoundary() =
      assertCallbackExceptionHandled(Callback.VIRTUAL_VIEW_ACTION)

  private enum class Callback {
    ACTION,
    REQUEST,
    VIRTUAL_VIEW_ACTION,
  }

  private fun assertCallbackExceptionHandled(callback: Callback) {
    val errors = mutableListOf<Exception>()
    val tree =
        ComponentTree.create(lithoTestRule.context)
            .componentsConfiguration(
                lithoTestRule.context.lithoConfiguration.componentsConfig.copy(
                    errorEventHandler =
                        object : ErrorEventHandler() {
                          override fun onError(cc: ComponentContext, e: Exception): Component? {
                            errors.add(e)
                            return null
                          }
                        },
                ),
            )
            .build()
    val baseStyle = Style.width(200.px).height(200.px).contentDescription(ACTION_LABEL)
    val style =
        when (callback) {
          Callback.ACTION ->
              baseStyle.onPerformAccessibilityActionWithResult {
                throw IllegalStateException("callback failure")
              }
          Callback.REQUEST ->
              baseStyle.onRequestSendAccessibilityEventWithResult {
                throw IllegalStateException("callback failure")
              }
          Callback.VIRTUAL_VIEW_ACTION ->
              baseStyle.onPerformActionForVirtualViewWithResult {
                throw IllegalStateException("callback failure")
              }
        }
    val rendered =
        lithoTestRule.render(componentTree = tree) {
          Row(style = style) {
            child(
                Row(
                    style = Style.width(100.px).height(100.px).contentDescription(DESCENDANT_LABEL),
                ),
            )
          }
        }
    val host = rendered.findViewWithContentDescription(ACTION_LABEL)
    val delegate = checkNotNull(ViewCompat.getAccessibilityDelegate(host))
    val outcome = runCatching {
      when (callback) {
        Callback.ACTION ->
            delegate.performAccessibilityAction(host, AccessibilityNodeInfo.ACTION_CLICK, null)
        Callback.REQUEST ->
            delegate.onRequestSendAccessibilityEvent(
                host as ViewGroup,
                rendered.findViewWithContentDescription(DESCENDANT_LABEL),
                newFocusedAccessibilityEvent(),
            )
        Callback.VIRTUAL_VIEW_ACTION ->
            rendered.dispatchVirtualViewAction(AccessibilityNodeInfo.ACTION_CLICK)
      }
    }
    assertThat(errors).describedAs("configured boundary must receive callback exception").hasSize(1)
    assertThat(outcome.isSuccess).describedAs("callback exception must not escape").isTrue()
    assertThat(outcome.getOrNull()).isFalse()
  }

  /**
   * Dispatches [action] to the root's virtual-view handler as the accessibility delegate would. The
   * delegate itself never reaches this handler: it dispatches it only for non-Spec components,
   * which have no virtual views.
   */
  private fun TestLithoView.dispatchVirtualViewAction(action: Int): Boolean {
    val host = findViewWithContentDescription(ACTION_LABEL)
    val handler =
        checkNotNull(currentRootNode?.node?.nodeInfo?.onPerformActionForVirtualViewHandler)
    return EventDispatcherUtils.dispatchPerformActionForVirtualView(
        handler,
        host,
        AccessibilityNodeInfoCompat.wrap(host.createAccessibilityNodeInfo()),
        0,
        action,
        null,
    )
  }

  // The constructor requires API 30; the factory remains necessary for the API 21 minimum.
  @SuppressLint("DeprecatedMethod")
  @Suppress("DEPRECATION")
  private fun newFocusedAccessibilityEvent(): AccessibilityEvent =
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED)

  companion object {
    private const val ACTION_LABEL = "Action"
    private const val DESCENDANT_LABEL = "Descendant"
  }
}
