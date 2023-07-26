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

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EventHandler
import com.facebook.litho.KComponent
import com.facebook.litho.OnInitializeAccessibilityEventEvent
import com.facebook.litho.OnInitializeAccessibilityNodeInfoEvent
import com.facebook.litho.OnPopulateAccessibilityEventEvent
import com.facebook.litho.OnPopulateAccessibilityNodeEvent
import com.facebook.litho.OnRequestSendAccessibilityEventEvent
import com.facebook.litho.PerformAccessibilityActionEvent
import com.facebook.litho.Row
import com.facebook.litho.SendAccessibilityEventEvent
import com.facebook.litho.SendAccessibilityEventUncheckedEvent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Unit tests for accessibility styles. */
@RunWith(LithoTestRunner::class)
class AccessibilityStylesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun contentDescription_whenSet_isSetOnView() {
    val testLithoView =
        lithoViewRule.render {
          Row(style = Style.width(200.px).height(200.px).contentDescription("Accessibility Test"))
        }

    assertThat(testLithoView.lithoView.contentDescription).isEqualTo("Accessibility Test")
  }

  @Test
  fun contentDescription_whenNull_isNotSet() {
    val testLithoView =
        lithoViewRule.render {
          Row(style = Style.width(200.px).height(200.px).contentDescription(null))
        }

    assertThat(testLithoView.lithoView.contentDescription).isNull()
  }

  @Test
  fun importantForAccessibility_whenSet_isSetOnView() {
    val testLithoView =
        lithoViewRule.render {
          Row(
              style =
                  Style.width(200.px)
                      .height(200.px)
                      .importantForAccessibility(ImportantForAccessibility.YES))
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
    val testLithoView = lithoViewRule.render { TestComponent() }
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
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
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
    val testLithoView = lithoViewRule.render { TestComponent() }
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
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
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
    val testLithoView = lithoViewRule.render { TestComponent() }
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
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
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
    val testLithoView = lithoViewRule.render { TestComponent() }
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
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
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
    val testLithoView = lithoViewRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onRequestSendAccessibilityEventHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun onRequestSendAccessibilityEvent_whenSet_isSetOnView() {
    val eventHandler: EventHandler<OnRequestSendAccessibilityEventEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onRequestSendAccessibilityEvent { eventHandler })
      }
    }
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.onRequestSendAccessibilityEventHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun performAccessibilityAction_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoViewRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.performAccessibilityActionHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun performAccessibilityAction_whenSet_isSetOnView() {
    val eventHandler: EventHandler<PerformAccessibilityActionEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.performAccessibilityAction { eventHandler })
      }
    }
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.performAccessibilityActionHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun sendAccessibilityEvent_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoViewRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun sendAccessibilityEvent_whenSet_isSetOnView() {
    val eventHandler: EventHandler<SendAccessibilityEventEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.sendAccessibilityEvent { eventHandler })
      }
    }
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventHandler).isNotNull
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun sendAccessibilityEventUnchecked_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px))
      }
    }
    val testLithoView = lithoViewRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventUncheckedHandler).isNull()
  }

  /** See comment on [onInitializeAccessibilityNodeInfo_whenNotSet_isNotSetOnView] above. */
  @Test
  fun sendAccessibilityEventUnchecked_whenSet_isSetOnView() {
    val eventHandler: EventHandler<SendAccessibilityEventUncheckedEvent> = mock()

    class TestComponentWithHandler : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.sendAccessibilityEventUnchecked { eventHandler })
      }
    }
    val testLithoView = lithoViewRule.render { TestComponentWithHandler() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.sendAccessibilityEventUncheckedHandler).isNotNull
  }

  @Test
  fun accessibilityRole_whenNotSet_isNotSetOnView() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.width(200.px).accessibilityRole(null))
      }
    }
    val testLithoView = lithoViewRule.render { TestComponent() }
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
    val testLithoView = lithoViewRule.render { TestComponent() }
    val node = testLithoView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.accessibilityRoleDescription).isNull()
  }
}
