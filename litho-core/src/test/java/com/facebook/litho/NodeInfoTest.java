/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.Pools;
import android.util.SparseArray;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static com.facebook.litho.NodeInfo.FOCUS_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.NodeInfo.FOCUS_UNSET;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class NodeInfoTest {

  private NodeInfo mNodeInfo;

  @Before
  public void setup() {
    mNodeInfo = NodeInfo.acquire();
  }

  @Test
  public void testTouchHandler() {
    EventHandler touchHandler = new EventHandler(null, 1);

    mNodeInfo.setTouchHandler(touchHandler);
    assertSame(touchHandler, mNodeInfo.getTouchHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getTouchHandler());
  }

  @Test
  public void testDispatchPopulateAccessibilityEventHandler() {
    EventHandler<DispatchPopulateAccessibilityEventEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setDispatchPopulateAccessibilityEventHandler(handler);
    assertSame(handler, mNodeInfo.getDispatchPopulateAccessibilityEventHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getDispatchPopulateAccessibilityEventHandler());
  }

  @Test
  public void testOnInitializeAccessibilityEventHandler() {
    EventHandler<OnInitializeAccessibilityEventEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setOnInitializeAccessibilityEventHandler(handler);
    assertSame(handler, mNodeInfo.getOnInitializeAccessibilityEventHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getOnInitializeAccessibilityEventHandler());
  }

  @Test
  public void testOnPopulateAccessibilityEventHandler() {
    EventHandler<OnPopulateAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnPopulateAccessibilityEventHandler(handler);
    assertSame(handler, mNodeInfo.getOnPopulateAccessibilityEventHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getOnPopulateAccessibilityEventHandler());
  }

  @Test
  public void testOnInitializeAccessibilityNodeInfoHandler() {
    EventHandler<OnInitializeAccessibilityNodeInfoEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setOnInitializeAccessibilityNodeInfoHandler(handler);
    assertSame(handler, mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getOnInitializeAccessibilityNodeInfoHandler());
  }

  @Test
  public void testOnRequestSendAccessibilityEventHandler() {
    EventHandler<OnRequestSendAccessibilityEventEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setOnRequestSendAccessibilityEventHandler(handler);
    assertSame(handler, mNodeInfo.getOnRequestSendAccessibilityEventHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getOnRequestSendAccessibilityEventHandler());
  }

  @Test
  public void testPerformAccessibilityActionHandler() {
    EventHandler<PerformAccessibilityActionEvent> handler =
        new EventHandler<>(null, 1);

    mNodeInfo.setPerformAccessibilityActionHandler(handler);
    assertSame(handler, mNodeInfo.getPerformAccessibilityActionHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getPerformAccessibilityActionHandler());
  }

  @Test
  public void testSendAccessibilityEventHandler() {
    EventHandler<SendAccessibilityEventEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setSendAccessibilityEventHandler(handler);
    assertSame(handler, mNodeInfo.getSendAccessibilityEventHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getSendAccessibilityEventHandler());
  }

  @Test
  public void testSendAccessibilityEventUncheckedHandler() {
    EventHandler<SendAccessibilityEventUncheckedEvent> handler = new EventHandler<>(null, 1);

    mNodeInfo.setSendAccessibilityEventUncheckedHandler(handler);
    assertSame(handler, mNodeInfo.getSendAccessibilityEventUncheckedHandler());

    mNodeInfo.release();
    assertNull(mNodeInfo.getSendAccessibilityEventUncheckedHandler());
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
  public void testContentDescriptionFlag() {
    mNodeInfo.setContentDescription("test");
    testFlagIsSetThenClear(mNodeInfo, "PFLAG_CONTENT_DESCRIPTION_IS_SET");
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
  public void testSendAccessiblityEventHandlerFlag() {
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
    assertEquals(FOCUS_UNSET, mNodeInfo.getFocusState());
    mNodeInfo.setFocusable(true);

    assertEquals(FOCUS_SET_TRUE, mNodeInfo.getFocusState());

    mNodeInfo.release();
    assertEquals(FOCUS_UNSET, mNodeInfo.getFocusState());
  }

  @Test
  public void testFocusableFalse() {
    assertEquals(FOCUS_UNSET, mNodeInfo.getFocusState());
    mNodeInfo.setFocusable(false);

    assertEquals(FOCUS_SET_FALSE, mNodeInfo.getFocusState());

    mNodeInfo.release();
    assertEquals(FOCUS_UNSET, mNodeInfo.getFocusState());
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
    assertTrue(isFlagSet(nodeInfo, flagName));
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
    assertTrue(((int) Whitebox.getInternalState(nodeInfo, "mPrivateFlags")) == 0);
  }

  private static void clearNodeInfoPool() {
    final Pools.SynchronizedPool<NodeInfo> nodeInfoPool =
        Whitebox.getInternalState(ComponentsPools.class, "sNodeInfoPool");

    while (nodeInfoPool.acquire() != null) {
      // Run.
    }
  }
}
