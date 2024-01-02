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

package com.facebook.litho

import android.util.SparseArray
import com.facebook.litho.AccessibilityRole.AccessibilityRoleType
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class NodeInfoTest {

  private lateinit var nodeInfo: NodeInfo
  private lateinit var updatedNodeInfo: NodeInfo

  @Before
  fun setup() {
    nodeInfo = NodeInfo()
    updatedNodeInfo = NodeInfo()
  }

  @Test
  fun testClickHandler() {
    val clickHandler = EventHandlerTestUtil.create<ClickEvent>(1, null)
    nodeInfo.clickHandler = clickHandler
    assertThat(clickHandler).isSameAs(nodeInfo.clickHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(clickHandler).isSameAs(updatedNodeInfo.clickHandler)
  }

  @Test
  fun testTouchHandler() {
    val touchHandler = EventHandlerTestUtil.create<TouchEvent>(1, null)
    nodeInfo.touchHandler = touchHandler
    assertThat(touchHandler).isSameAs(nodeInfo.touchHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(touchHandler).isSameAs(updatedNodeInfo.touchHandler)
  }

  @Test
  fun testFocusChangeHandler() {
    val focusChangeHandler = EventHandlerTestUtil.create<FocusChangedEvent>(1, null)
    nodeInfo.focusChangeHandler = focusChangeHandler
    assertThat(focusChangeHandler).isSameAs(nodeInfo.focusChangeHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(focusChangeHandler).isSameAs(updatedNodeInfo.focusChangeHandler)
  }

  @Test
  fun testInterceptTouchHandler() {
    val interceptTouchHandler = EventHandlerTestUtil.create<InterceptTouchEvent>(1, null)
    nodeInfo.interceptTouchHandler = interceptTouchHandler
    assertThat(interceptTouchHandler).isSameAs(nodeInfo.interceptTouchHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(interceptTouchHandler).isSameAs(updatedNodeInfo.interceptTouchHandler)
  }

  @Test
  fun testAccessibilityHeadingTrue() {
    assertThat(nodeInfo.accessibilityHeadingState).isEqualTo(NodeInfo.ACCESSIBILITY_HEADING_UNSET)
    nodeInfo.setAccessibilityHeading(true)
    assertThat(nodeInfo.accessibilityHeadingState)
        .isEqualTo(NodeInfo.ACCESSIBILITY_HEADING_SET_TRUE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.accessibilityHeadingState)
        .isEqualTo(NodeInfo.ACCESSIBILITY_HEADING_SET_TRUE)
  }

  @Test
  fun testAccessibilityHeadingFalse() {
    assertThat(nodeInfo.accessibilityHeadingState).isEqualTo(NodeInfo.ACCESSIBILITY_HEADING_UNSET)
    nodeInfo.setAccessibilityHeading(false)
    assertThat(nodeInfo.accessibilityHeadingState)
        .isEqualTo(NodeInfo.ACCESSIBILITY_HEADING_SET_FALSE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.accessibilityHeadingState)
        .isEqualTo(NodeInfo.ACCESSIBILITY_HEADING_SET_FALSE)
  }

  @Test
  fun testAccessibilityRole() {
    @AccessibilityRoleType val role = AccessibilityRole.BUTTON
    nodeInfo.accessibilityRole = role
    assertThat(role).isSameAs(nodeInfo.accessibilityRole)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(role).isSameAs(updatedNodeInfo.accessibilityRole)
  }

  @Test
  fun testAccessibilityRoleDescription() {
    val roleDescription = "Test Role Description"
    nodeInfo.accessibilityRoleDescription = roleDescription
    assertThat(roleDescription).isSameAs(nodeInfo.accessibilityRoleDescription)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(roleDescription).isSameAs(updatedNodeInfo.accessibilityRoleDescription)
  }

  @Test
  fun testDispatchPopulateAccessibilityEventHandler() {
    val handler = EventHandlerTestUtil.create<DispatchPopulateAccessibilityEventEvent>(1, null)
    nodeInfo.dispatchPopulateAccessibilityEventHandler = handler
    assertThat(handler).isSameAs(nodeInfo.dispatchPopulateAccessibilityEventHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.dispatchPopulateAccessibilityEventHandler)
  }

  @Test
  fun testOnInitializeAccessibilityEventHandler() {
    val handler = EventHandlerTestUtil.create<OnInitializeAccessibilityEventEvent>(1, null)
    nodeInfo.onInitializeAccessibilityEventHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onInitializeAccessibilityEventHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onInitializeAccessibilityEventHandler)
  }

  @Test
  fun testOnPopulateAccessibilityEventHandler() {
    val handler = EventHandlerTestUtil.create<OnPopulateAccessibilityEventEvent>(1, null)
    nodeInfo.onPopulateAccessibilityEventHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onPopulateAccessibilityEventHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onPopulateAccessibilityEventHandler)
  }

  @Test
  fun testOnPopulateAccessibilityNodeHandler() {
    val handler = EventHandlerTestUtil.create<OnPopulateAccessibilityNodeEvent>(1, null)
    nodeInfo.onPopulateAccessibilityNodeHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onPopulateAccessibilityNodeHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onPopulateAccessibilityNodeHandler)
  }

  @Test
  fun testOnInitializeAccessibilityNodeInfoHandler() {
    val handler = EventHandlerTestUtil.create<OnInitializeAccessibilityNodeInfoEvent>(1, null)
    nodeInfo.onInitializeAccessibilityNodeInfoHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onInitializeAccessibilityNodeInfoHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onInitializeAccessibilityNodeInfoHandler)
  }

  @Test
  fun testOnRequestSendAccessibilityEventHandler() {
    val handler = EventHandlerTestUtil.create<OnRequestSendAccessibilityEventEvent>(1, null)
    nodeInfo.onRequestSendAccessibilityEventHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onRequestSendAccessibilityEventHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onRequestSendAccessibilityEventHandler)
  }

  @Test
  fun testPerformAccessibilityActionHandler() {
    val handler = EventHandlerTestUtil.create<PerformAccessibilityActionEvent>(1, null)
    nodeInfo.performAccessibilityActionHandler = handler
    assertThat(handler).isSameAs(nodeInfo.performAccessibilityActionHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.performAccessibilityActionHandler)
  }

  @Test
  fun testSendAccessibilityEventHandler() {
    val handler = EventHandlerTestUtil.create<SendAccessibilityEventEvent>(1, null)
    nodeInfo.sendAccessibilityEventHandler = handler
    assertThat(handler).isSameAs(nodeInfo.sendAccessibilityEventHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.sendAccessibilityEventHandler)
  }

  @Test
  fun testSendAccessibilityEventUncheckedHandler() {
    val handler = EventHandlerTestUtil.create<SendAccessibilityEventUncheckedEvent>(1, null)
    nodeInfo.sendAccessibilityEventUncheckedHandler = handler
    assertThat(handler).isSameAs(nodeInfo.sendAccessibilityEventUncheckedHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.sendAccessibilityEventUncheckedHandler)
  }

  @Test
  fun testOnPerformActionForVirtualViewHandler() {
    val handler = EventHandlerTestUtil.create<PerformActionForVirtualViewEvent>(1, null)
    nodeInfo.onPerformActionForVirtualViewHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onPerformActionForVirtualViewHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onPerformActionForVirtualViewHandler)
  }

  @Test
  fun testOnVirtualViewKeyboardFocusChangedHandler() {
    val handler = EventHandlerTestUtil.create<VirtualViewKeyboardFocusChangedEvent>(1, null)
    nodeInfo.onVirtualViewKeyboardFocusChangedHandler = handler
    assertThat(handler).isSameAs(nodeInfo.onVirtualViewKeyboardFocusChangedHandler)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(handler).isSameAs(updatedNodeInfo.onVirtualViewKeyboardFocusChangedHandler)
  }

  @Test
  fun testClickHandlerFlag() {
    nodeInfo.clickHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_CLICK_HANDLER_IS_SET")
  }

  @Test
  fun testLongClickHandlerFlag() {
    nodeInfo.longClickHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_LONG_CLICK_HANDLER_IS_SET")
  }

  @Test
  fun testFocusChangeHandlerFlag() {
    nodeInfo.focusChangeHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_FOCUS_CHANGE_HANDLER_IS_SET")
  }

  @Test
  fun testContentDescriptionFlag() {
    nodeInfo.contentDescription = "test"
    testFlagIsSetThenClear(nodeInfo, "PFLAG_CONTENT_DESCRIPTION_IS_SET")
  }

  @Test
  fun testAccessibilityRoleFlag() {
    nodeInfo.accessibilityRole = AccessibilityRole.BUTTON
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ACCESSIBILITY_ROLE_IS_SET")
  }

  @Test
  fun testAccessibilityRoleDescriptionFlag() {
    nodeInfo.accessibilityRoleDescription = "Test Role Description"
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET")
  }

  @Test
  fun testDispatchPopulateAccessibilityEventHandlerFlag() {
    nodeInfo.dispatchPopulateAccessibilityEventHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET")
  }

  @Test
  fun testOnInitializeAccessibilityEventHandlerFlag() {
    nodeInfo.onInitializeAccessibilityEventHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET")
  }

  @Test
  fun testOnPopulateAccessibilityEventHandlerFlag() {
    nodeInfo.onPopulateAccessibilityEventHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET")
  }

  @Test
  fun testOnPopulateAccessibilityNodeHandlerFlag() {
    nodeInfo.onPopulateAccessibilityNodeHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ON_POPULATE_ACCESSIBILITY_NODE_HANDLER_IS_SET")
  }

  @Test
  fun testOnInitializeAccessibilityNodeInfoHandlerFlag() {
    nodeInfo.onInitializeAccessibilityNodeInfoHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET")
  }

  @Test
  fun testOnRequestSendAccessibilityEventHandlerFlag() {
    nodeInfo.onRequestSendAccessibilityEventHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET")
  }

  @Test
  fun testPerformAccessibilityActionHandlerFlag() {
    nodeInfo.performAccessibilityActionHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET")
  }

  @Test
  fun testSendAccessibilityEventHandlerFlag() {
    nodeInfo.sendAccessibilityEventHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET")
  }

  @Test
  fun testSendAccessibilityEventUncheckedHandlerFlag() {
    nodeInfo.sendAccessibilityEventUncheckedHandler = EventHandlerTestUtil.create(1, null)
    testFlagIsSetThenClear(nodeInfo, "PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET")
  }

  @Test
  fun testViewTagsFlag() {
    nodeInfo.viewTags = SparseArray()
    testFlagIsSetThenClear(nodeInfo, "PFLAG_VIEW_TAGS_IS_SET")
  }

  @Test
  fun testFocusableTrue() {
    assertThat(nodeInfo.focusState).isEqualTo(NodeInfo.FOCUS_UNSET)
    nodeInfo.setFocusable(true)
    assertThat(nodeInfo.focusState).isEqualTo(NodeInfo.FOCUS_SET_TRUE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.focusState).isEqualTo(NodeInfo.FOCUS_SET_TRUE)
  }

  @Test
  fun testFocusableFalse() {
    assertThat(nodeInfo.focusState).isEqualTo(NodeInfo.FOCUS_UNSET)
    nodeInfo.setFocusable(false)
    assertThat(nodeInfo.focusState).isEqualTo(NodeInfo.FOCUS_SET_FALSE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.focusState).isEqualTo(NodeInfo.FOCUS_SET_FALSE)
  }

  @Test
  fun testSelectedTrue() {
    assertThat(nodeInfo.selectedState).isEqualTo(NodeInfo.SELECTED_UNSET)
    nodeInfo.setSelected(true)
    assertThat(nodeInfo.selectedState).isEqualTo(NodeInfo.SELECTED_SET_TRUE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.selectedState).isEqualTo(NodeInfo.SELECTED_SET_TRUE)
  }

  @Test
  fun testSelectedFalse() {
    assertThat(nodeInfo.selectedState).isEqualTo(NodeInfo.SELECTED_UNSET)
    nodeInfo.setSelected(false)
    assertThat(nodeInfo.selectedState).isEqualTo(NodeInfo.SELECTED_SET_FALSE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.selectedState).isEqualTo(NodeInfo.SELECTED_SET_FALSE)
  }

  @Test
  fun testEnabledTrue() {
    assertThat(nodeInfo.enabledState).isEqualTo(NodeInfo.ENABLED_UNSET)
    nodeInfo.setEnabled(true)
    assertThat(nodeInfo.enabledState).isEqualTo(NodeInfo.ENABLED_SET_TRUE)
  }

  @Test
  fun testEnabledFalse() {
    assertThat(nodeInfo.enabledState).isEqualTo(NodeInfo.ENABLED_UNSET)
    nodeInfo.setEnabled(false)
    assertThat(nodeInfo.enabledState).isEqualTo(NodeInfo.ENABLED_SET_FALSE)
  }

  @Test
  fun testKeyboardNavigationClusterTrue() {
    assertThat(nodeInfo.keyboardNavigationClusterState)
        .isEqualTo(NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_UNSET)
    nodeInfo.setKeyboardNavigationCluster(true)
    assertThat(nodeInfo.keyboardNavigationClusterState)
        .isEqualTo(NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_TRUE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.keyboardNavigationClusterState)
        .isEqualTo(NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_TRUE)
  }

  @Test
  fun testKeyboardNavigationClusterFalse() {
    assertThat(nodeInfo.keyboardNavigationClusterState)
        .isEqualTo(NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_UNSET)
    nodeInfo.setKeyboardNavigationCluster(false)
    assertThat(nodeInfo.keyboardNavigationClusterState)
        .isEqualTo(NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_FALSE)
    nodeInfo.copyInto(updatedNodeInfo)
    assertThat(updatedNodeInfo.keyboardNavigationClusterState)
        .isEqualTo(NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_FALSE)
  }

  companion object {
    private fun testFlagIsSetThenClear(nodeInfo: NodeInfo?, flagName: String) {
      assertThat(isFlagSet(nodeInfo, flagName)).isTrue
      clearFlag(nodeInfo, flagName)
      assertEmptyFlags(nodeInfo)
    }

    private fun isFlagSet(nodeInfo: NodeInfo?, flagName: String): Boolean {
      val flagPosition = Whitebox.getInternalState<Long>(NodeInfo::class.java, flagName)
      val flags = Whitebox.getInternalState<Long>(nodeInfo, "mPrivateFlags")
      return flags and flagPosition != 0L
    }

    private fun clearFlag(nodeInfo: NodeInfo?, flagName: String) {
      val flagPosition = Whitebox.getInternalState<Long>(NodeInfo::class.java, flagName)
      var flags = Whitebox.getInternalState<Long>(nodeInfo, "mPrivateFlags")
      flags = flags and flagPosition.inv()
      Whitebox.setInternalState(nodeInfo, "mPrivateFlags", flags)
    }

    private fun assertEmptyFlags(nodeInfo: NodeInfo?) {
      assertThat(Whitebox.getInternalState<Any>(nodeInfo, "mPrivateFlags") as Long == 0L).isTrue
    }
  }
}
