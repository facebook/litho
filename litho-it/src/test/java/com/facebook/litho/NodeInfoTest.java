/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.NodeInfo.FOCUS_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.NodeInfo.FOCUS_UNSET;
import static com.facebook.litho.NodeInfo.SELECTED_SET_FALSE;
import static com.facebook.litho.NodeInfo.SELECTED_SET_TRUE;
import static com.facebook.litho.NodeInfo.SELECTED_UNSET;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

import android.util.SparseArray;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

@RunWith(ComponentsTestRunner.class)
public class NodeInfoTest {

  private NodeInfo mNodeInfo;
  private NodeInfo mUpdatedNodeInfo;

  @Before
  public void setup() {
    mNodeInfo = NodeInfo.acquire();
    mUpdatedNodeInfo = NodeInfo.acquire();
  }

  @Test
  public void testClickHandler() {
    EventHandler<ClickEvent> clickHandler = new EventHandler<>(null, 1);

    mNodeInfo.setClickHandler(clickHandler);
    assertThat(clickHandler).isSameAs(mNodeInfo.getClickHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(clickHandler).isSameAs(mUpdatedNodeInfo.getClickHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getClickHandler()).isNull();
  }

  @Test
  public void testTouchHandler() {
    EventHandler<TouchEvent> touchHandler = new EventHandler<>(null, 1);

    mNodeInfo.setTouchHandler(touchHandler);
    assertThat(touchHandler).isSameAs(mNodeInfo.getTouchHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(touchHandler).isSameAs(mUpdatedNodeInfo.getTouchHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getTouchHandler()).isNull();
  }

  @Test
  public void testFocusChangeHandler() {
    EventHandler<FocusChangedEvent> focusChangeHandler = new EventHandler<>(null, 1);

    mNodeInfo.setFocusChangeHandler(focusChangeHandler);
    assertThat(focusChangeHandler).isSameAs(mNodeInfo.getFocusChangeHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(focusChangeHandler).isSameAs(mUpdatedNodeInfo.getFocusChangeHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getFocusChangeHandler()).isNull();
  }

  @Test
  public void testInterceptTouchHandler() {
    EventHandler<InterceptTouchEvent> interceptTouchHandler = new EventHandler<>(null, 1);

    mNodeInfo.setInterceptTouchHandler(interceptTouchHandler);
    assertThat(interceptTouchHandler).isSameAs(mNodeInfo.getInterceptTouchHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(interceptTouchHandler).isSameAs(mUpdatedNodeInfo.getInterceptTouchHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getInterceptTouchHandler()).isNull();
  }

  @Test
  public void testAccessibilityRole() {
    @AccessibilityRole.AccessibilityRoleType final String role = AccessibilityRole.BUTTON;

    mNodeInfo.setAccessibilityRole(role);
    assertThat(role).isSameAs(mNodeInfo.getAccessibilityRole());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(role).isSameAs(mUpdatedNodeInfo.getAccessibilityRole());

    mNodeInfo.release();
    assertThat(mNodeInfo.getAccessibilityRole()).isNull();
  }

  @Test
  public void testAccessibilityRoleDescription() {
    final CharSequence roleDescription = "Test Role Description";

    mNodeInfo.setAccessibilityRoleDescription(roleDescription);
    assertThat(roleDescription).isSameAs(mNodeInfo.getAccessibilityRoleDescription());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(roleDescription).isSameAs(mUpdatedNodeInfo.getAccessibilityRoleDescription());

    mNodeInfo.release();
    assertThat(mNodeInfo.getAccessibilityRoleDescription()).isNull();
  }

  @Test
  public void testDispatchPopulateAccessibilityEventHandler() {
    EventHandler<DispatchPopulateAccessibilityEventEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setDispatchPopulateAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getDispatchPopulateAccessibilityEventHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getDispatchPopulateAccessibilityEventHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getDispatchPopulateAccessibilityEventHandler()).isNull();
  }

  @Test
  public void testOnInitializeAccessibilityEventHandler() {
    EventHandler<OnInitializeAccessibilityEventEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setOnInitializeAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnInitializeAccessibilityEventHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnInitializeAccessibilityEventHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getOnInitializeAccessibilityEventHandler()).isNull();
  }

  @Test
  public void testOnPopulateAccessibilityEventHandler() {
    EventHandler<OnPopulateAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnPopulateAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnPopulateAccessibilityEventHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnPopulateAccessibilityEventHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getOnPopulateAccessibilityEventHandler()).isNull();
  }

  @Test
  public void testOnInitializeAccessibilityNodeInfoHandler() {
    EventHandler<OnInitializeAccessibilityNodeInfoEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnInitializeAccessibilityNodeInfoHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnInitializeAccessibilityNodeInfoHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler()).isNull();
  }

  @Test
  public void testOnRequestSendAccessibilityEventHandler() {
    EventHandler<OnRequestSendAccessibilityEventEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setOnRequestSendAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnRequestSendAccessibilityEventHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnRequestSendAccessibilityEventHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getOnRequestSendAccessibilityEventHandler()).isNull();
  }

  @Test
  public void testPerformAccessibilityActionHandler() {
    EventHandler<PerformAccessibilityActionEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setPerformAccessibilityActionHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getPerformAccessibilityActionHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getPerformAccessibilityActionHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getPerformAccessibilityActionHandler()).isNull();
  }

  @Test
  public void testSendAccessibilityEventHandler() {
    EventHandler<SendAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setSendAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getSendAccessibilityEventHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getSendAccessibilityEventHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getSendAccessibilityEventHandler()).isNull();
  }

  @Test
  public void testSendAccessibilityEventUncheckedHandler() {
    EventHandler<SendAccessibilityEventUncheckedEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setSendAccessibilityEventUncheckedHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getSendAccessibilityEventUncheckedHandler());

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getSendAccessibilityEventUncheckedHandler());

    mNodeInfo.release();
    assertThat(mNodeInfo.getSendAccessibilityEventUncheckedHandler()).isNull();
  }

  @Test
  public void testClickHandlerFlag() {
    mNodeInfo.setClickHandler(new EventHandler(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_CLICK_HANDLER_IS_SET");
  }

  @Test
  public void testLongClickHandlerFlag() {
    mNodeInfo.setLongClickHandler(new EventHandler(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_LONG_CLICK_HANDLER_IS_SET");
  }

  @Test
  public void testFocusChangeHandlerFlag() {
    mNodeInfo.setFocusChangeHandler(new EventHandler(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_FOCUS_CHANGE_HANDLER_IS_SET");
  }

  @Test
  public void testContentDescriptionFlag() {
    mNodeInfo.setContentDescription("test");
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_CONTENT_DESCRIPTION_IS_SET");
  }

  @Test
  public void testAccessibilityRoleFlag() {
    mNodeInfo.setAccessibilityRole(AccessibilityRole.BUTTON);
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_ACCESSIBILITY_ROLE_IS_SET");
  }

  @Test
  public void testAccessibilityRoleDescriptionFlag() {
    mNodeInfo.setAccessibilityRoleDescription("Test Role Description");
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET");
  }

  @Test
  public void testDispatchPopulateAccessibilityEventHandlerFlag() {
    mNodeInfo.setDispatchPopulateAccessibilityEventHandler(
        new EventHandler<DispatchPopulateAccessibilityEventEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET");
  }

  @Test
  public void testOnInitializeAccessibilityEventHandlerFlag() {
    mNodeInfo.setOnInitializeAccessibilityEventHandler(
        new EventHandler<OnInitializeAccessibilityEventEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET");
  }

  @Test
  public void testOnPopulateAccessibilityEventHandlerFlag() {
    mNodeInfo.setOnPopulateAccessibilityEventHandler(
        new EventHandler<OnPopulateAccessibilityEventEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET");
  }

  @Test
  public void testOnInitializeAccessibilityNodeInfoHandlerFlag() {
    mNodeInfo.setOnInitializeAccessibilityNodeInfoHandler(
        new EventHandler<OnInitializeAccessibilityNodeInfoEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET");
  }

  @Test
  public void testOnRequestSendAccessibilityEventHandlerFlag() {
    mNodeInfo.setOnRequestSendAccessibilityEventHandler(
        new EventHandler<OnRequestSendAccessibilityEventEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET");
  }

  @Test
  public void testPerformAccessibilityActionHandlerFlag() {
    mNodeInfo.setPerformAccessibilityActionHandler(
        new EventHandler<PerformAccessibilityActionEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET");
  }

  @Test
  public void testSendAccessibilityEventHandlerFlag() {
    mNodeInfo.setSendAccessibilityEventHandler(
        new EventHandler<SendAccessibilityEventEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET");
  }

  @Test
  public void testSendAccessibilityEventUncheckedHandlerFlag() {
    mNodeInfo.setSendAccessibilityEventUncheckedHandler(
        new EventHandler<SendAccessibilityEventUncheckedEvent>(null, 1));
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET");
  }

  @Test
  public void testViewTagsFlag() {
    mNodeInfo.setViewTags(new SparseArray<>());
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_VIEW_TAGS_IS_SET");
  }

  @Test
  public void testFocusableTrue() {
    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_UNSET);
    mNodeInfo.setFocusable(true);

    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_TRUE);

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(mUpdatedNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_TRUE);

    mNodeInfo.release();
    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_UNSET);
  }

  @Test
  public void testFocusableFalse() {
    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_UNSET);
    mNodeInfo.setFocusable(false);

    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_FALSE);

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(mUpdatedNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_FALSE);

    mNodeInfo.release();
    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_UNSET);
  }

  @Test
  public void testSelectedTrue() {
    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_UNSET);
    mNodeInfo.setSelected(true);

    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_TRUE);

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(mUpdatedNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_TRUE);

    mNodeInfo.release();
    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_UNSET);
  }

  @Test
  public void testSelectedFalse() {
    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_UNSET);
    mNodeInfo.setSelected(false);

    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_FALSE);

    mUpdatedNodeInfo.updateWith(mNodeInfo);
    assertThat(mUpdatedNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_FALSE);

    mNodeInfo.release();
    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_UNSET);
  }

  @Test
  public void testEnabledTrue() {
    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_UNSET);
    mNodeInfo.setEnabled(true);

    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_SET_TRUE);

    mNodeInfo.release();
    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_UNSET);
  }

  @Test
  public void testEnabledFalse() {
    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_UNSET);
    mNodeInfo.setEnabled(false);

    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_SET_FALSE);

    mNodeInfo.release();
    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_UNSET);
  }

  @Test
  public void testRefCountAcquiringReleasedNode() {
    NodeInfo nodeInfo = NodeInfo.acquire();

    nodeInfo.acquireRef();
    nodeInfo.release();
    nodeInfo.release(); // Now it should be back in the pool.

    try {
      nodeInfo.acquireRef();
      fail("Acquiring a released to pool reference should have thrown an exception.");
    } catch (Exception e) {
      // Expected exception.
    }

    // Drain pool of bad NodeInfo instances for subsequent tests.
    clearNodeInfoPool();
  }

  @Test
  public void testRefCountDoubleReleasingToPool() {
    NodeInfo nodeInfo = NodeInfo.acquire();

    nodeInfo.acquireRef();
    nodeInfo.release();
    nodeInfo.release(); // Now it should be back in the pool.

    try {
      nodeInfo.release();
      fail("Releasing a NodeInfo that is already in the pool.");
    } catch (Exception e) {
      // Expected exception.
    }

    // Drain pool of bad NodeInfo instances for subsequent tests.
    clearNodeInfoPool();
  }

  private static void testFlagIsSetThenClear(NodeInfo nodeInfo, String flagName) {
    assertThat(isFlagSet(nodeInfo, flagName)).isTrue();
    clearFlag(nodeInfo, flagName);
    assertEmptyFlags(nodeInfo);
  }

  private static boolean isFlagSet(NodeInfo nodeInfo, String flagName) {
    int flagPosition = Whitebox.getInternalState(NodeInfo.class, flagName);
    int flags = Whitebox.getInternalState(nodeInfo, "mPrivateFlags");

    return ((flags & flagPosition) != 0);
  }

  private static void clearFlag(NodeInfo nodeInfo, String flagName) {
    int flagPosition = Whitebox.getInternalState(NodeInfo.class, flagName);
    int flags = Whitebox.getInternalState(nodeInfo, "mPrivateFlags");
    flags &= ~flagPosition;
    Whitebox.setInternalState(nodeInfo, "mPrivateFlags", flags);
  }

  private static void assertEmptyFlags(NodeInfo nodeInfo) {
    assertThat(((int) getInternalState(nodeInfo, "mPrivateFlags")) == 0).isTrue();
  }

  private static void clearNodeInfoPool() {
    final RecyclePool<NodeInfo> nodeInfoPool =
        Whitebox.getInternalState(ComponentsPools.class, "sNodeInfoPool");

    while (nodeInfoPool.acquire() != null) {
      // Run.
    }
  }
}
