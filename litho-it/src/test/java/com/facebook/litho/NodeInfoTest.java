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

package com.facebook.litho;

import static com.facebook.litho.NodeInfo.ACCESSIBILITY_HEADING_SET_FALSE;
import static com.facebook.litho.NodeInfo.ACCESSIBILITY_HEADING_SET_TRUE;
import static com.facebook.litho.NodeInfo.ACCESSIBILITY_HEADING_UNSET;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_UNSET;
import static com.facebook.litho.NodeInfo.FOCUS_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.NodeInfo.FOCUS_UNSET;
import static com.facebook.litho.NodeInfo.SELECTED_SET_FALSE;
import static com.facebook.litho.NodeInfo.SELECTED_SET_TRUE;
import static com.facebook.litho.NodeInfo.SELECTED_UNSET;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.util.SparseArray;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class NodeInfoTest {

  private DefaultNodeInfo mNodeInfo;
  private DefaultNodeInfo mUpdatedNodeInfo;

  @Before
  public void setup() {
    mNodeInfo = new DefaultNodeInfo();
    mUpdatedNodeInfo = new DefaultNodeInfo();
  }

  @Test
  public void testClickHandler() {
    EventHandler<ClickEvent> clickHandler = new EventHandler<>(null, 1);

    mNodeInfo.setClickHandler(clickHandler);
    assertThat(clickHandler).isSameAs(mNodeInfo.getClickHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(clickHandler).isSameAs(mUpdatedNodeInfo.getClickHandler());
  }

  @Test
  public void testTouchHandler() {
    EventHandler<TouchEvent> touchHandler = new EventHandler<>(null, 1);

    mNodeInfo.setTouchHandler(touchHandler);
    assertThat(touchHandler).isSameAs(mNodeInfo.getTouchHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(touchHandler).isSameAs(mUpdatedNodeInfo.getTouchHandler());
  }

  @Test
  public void testFocusChangeHandler() {
    EventHandler<FocusChangedEvent> focusChangeHandler = new EventHandler<>(null, 1);

    mNodeInfo.setFocusChangeHandler(focusChangeHandler);
    assertThat(focusChangeHandler).isSameAs(mNodeInfo.getFocusChangeHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(focusChangeHandler).isSameAs(mUpdatedNodeInfo.getFocusChangeHandler());
  }

  @Test
  public void testInterceptTouchHandler() {
    EventHandler<InterceptTouchEvent> interceptTouchHandler = new EventHandler<>(null, 1);

    mNodeInfo.setInterceptTouchHandler(interceptTouchHandler);
    assertThat(interceptTouchHandler).isSameAs(mNodeInfo.getInterceptTouchHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(interceptTouchHandler).isSameAs(mUpdatedNodeInfo.getInterceptTouchHandler());
  }

  @Test
  public void testAccessibilityHeadingTrue() {
    assertThat(mNodeInfo.getAccessibilityHeadingState()).isEqualTo(ACCESSIBILITY_HEADING_UNSET);
    mNodeInfo.setAccessibilityHeading(true);

    assertThat(mNodeInfo.getAccessibilityHeadingState()).isEqualTo(ACCESSIBILITY_HEADING_SET_TRUE);

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(mUpdatedNodeInfo.getAccessibilityHeadingState())
        .isEqualTo(ACCESSIBILITY_HEADING_SET_TRUE);
  }

  @Test
  public void testAccessibilityHeadingFalse() {
    assertThat(mNodeInfo.getAccessibilityHeadingState()).isEqualTo(ACCESSIBILITY_HEADING_UNSET);
    mNodeInfo.setAccessibilityHeading(false);

    assertThat(mNodeInfo.getAccessibilityHeadingState()).isEqualTo(ACCESSIBILITY_HEADING_SET_FALSE);

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(mUpdatedNodeInfo.getAccessibilityHeadingState())
        .isEqualTo(ACCESSIBILITY_HEADING_SET_FALSE);
  }

  @Test
  public void testAccessibilityRole() {
    @AccessibilityRole.AccessibilityRoleType final String role = AccessibilityRole.BUTTON;

    mNodeInfo.setAccessibilityRole(role);
    assertThat(role).isSameAs(mNodeInfo.getAccessibilityRole());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(role).isSameAs(mUpdatedNodeInfo.getAccessibilityRole());
  }

  @Test
  public void testAccessibilityRoleDescription() {
    final CharSequence roleDescription = "Test Role Description";

    mNodeInfo.setAccessibilityRoleDescription(roleDescription);
    assertThat(roleDescription).isSameAs(mNodeInfo.getAccessibilityRoleDescription());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(roleDescription).isSameAs(mUpdatedNodeInfo.getAccessibilityRoleDescription());
  }

  @Test
  public void testDispatchPopulateAccessibilityEventHandler() {
    EventHandler<DispatchPopulateAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setDispatchPopulateAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getDispatchPopulateAccessibilityEventHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getDispatchPopulateAccessibilityEventHandler());
  }

  @Test
  public void testOnInitializeAccessibilityEventHandler() {
    EventHandler<OnInitializeAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnInitializeAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnInitializeAccessibilityEventHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnInitializeAccessibilityEventHandler());
  }

  @Test
  public void testOnPopulateAccessibilityEventHandler() {
    EventHandler<OnPopulateAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnPopulateAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnPopulateAccessibilityEventHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnPopulateAccessibilityEventHandler());
  }

  @Test
  public void testOnInitializeAccessibilityNodeInfoHandler() {
    EventHandler<OnInitializeAccessibilityNodeInfoEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnInitializeAccessibilityNodeInfoHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnInitializeAccessibilityNodeInfoHandler());
  }

  @Test
  public void testOnRequestSendAccessibilityEventHandler() {
    EventHandler<OnRequestSendAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnRequestSendAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getOnRequestSendAccessibilityEventHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getOnRequestSendAccessibilityEventHandler());
  }

  @Test
  public void testPerformAccessibilityActionHandler() {
    EventHandler<PerformAccessibilityActionEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setPerformAccessibilityActionHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getPerformAccessibilityActionHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getPerformAccessibilityActionHandler());
  }

  @Test
  public void testSendAccessibilityEventHandler() {
    EventHandler<SendAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setSendAccessibilityEventHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getSendAccessibilityEventHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getSendAccessibilityEventHandler());
  }

  @Test
  public void testSendAccessibilityEventUncheckedHandler() {
    EventHandler<SendAccessibilityEventUncheckedEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setSendAccessibilityEventUncheckedHandler(handler);
    assertThat(handler).isSameAs(mNodeInfo.getSendAccessibilityEventUncheckedHandler());

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(handler).isSameAs(mUpdatedNodeInfo.getSendAccessibilityEventUncheckedHandler());
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

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(mUpdatedNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_TRUE);
  }

  @Test
  public void testFocusableFalse() {
    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_UNSET);
    mNodeInfo.setFocusable(false);

    assertThat(mNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_FALSE);

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(mUpdatedNodeInfo.getFocusState()).isEqualTo(FOCUS_SET_FALSE);
  }

  @Test
  public void testSelectedTrue() {
    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_UNSET);
    mNodeInfo.setSelected(true);

    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_TRUE);

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(mUpdatedNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_TRUE);
  }

  @Test
  public void testSelectedFalse() {
    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_UNSET);
    mNodeInfo.setSelected(false);

    assertThat(mNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_FALSE);

    mNodeInfo.copyInto(mUpdatedNodeInfo);
    assertThat(mUpdatedNodeInfo.getSelectedState()).isEqualTo(SELECTED_SET_FALSE);
  }

  @Test
  public void testEnabledTrue() {
    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_UNSET);
    mNodeInfo.setEnabled(true);

    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_SET_TRUE);
  }

  @Test
  public void testEnabledFalse() {
    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_UNSET);
    mNodeInfo.setEnabled(false);

    assertThat(mNodeInfo.getEnabledState()).isEqualTo(ENABLED_SET_FALSE);
  }

  private static void testFlagIsSetThenClear(DefaultNodeInfo nodeInfo, String flagName) {
    assertThat(isFlagSet(nodeInfo, flagName)).isTrue();
    clearFlag(nodeInfo, flagName);
    assertEmptyFlags(nodeInfo);
  }

  private static boolean isFlagSet(DefaultNodeInfo nodeInfo, String flagName) {
    int flagPosition = Whitebox.getInternalState(DefaultNodeInfo.class, flagName);
    int flags = Whitebox.getInternalState(nodeInfo, "mPrivateFlags");

    return ((flags & flagPosition) != 0);
  }

  private static void clearFlag(DefaultNodeInfo nodeInfo, String flagName) {
    int flagPosition = Whitebox.getInternalState(DefaultNodeInfo.class, flagName);
    int flags = Whitebox.getInternalState(nodeInfo, "mPrivateFlags");
    flags &= ~flagPosition;
    Whitebox.setInternalState(nodeInfo, "mPrivateFlags", flags);
  }

  private static void assertEmptyFlags(DefaultNodeInfo nodeInfo) {
    assertThat(((int) getInternalState(nodeInfo, "mPrivateFlags")) == 0).isTrue();
  }
}
