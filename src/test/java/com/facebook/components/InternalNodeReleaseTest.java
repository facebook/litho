/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeReleaseTest {
  private InternalNode mInternalNode;

  @Before
  public void setup() {
    mInternalNode = ComponentsPools.acquireInternalNode(
        new ComponentContext(RuntimeEnvironment.application),
        RuntimeEnvironment.application.getResources());
  }

  private static void assertDefaultValues(InternalNode node) {
    assertEquals(false, node.isForceViewWrapping());

    assertNull(node.getNodeInfo());
    assertNull(node.getTouchExpansion());
    assertNull(node.getTestKey());
  }

  @Test
  public void testDefaultValues() {
    assertDefaultValues(mInternalNode);

    mInternalNode.touchExpansionPx(YogaEdge.ALL, 1);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 1);
    mInternalNode.touchExpansionPx(YogaEdge.TOP, 1);
    mInternalNode.touchExpansionPx(YogaEdge.RIGHT, 1);
    mInternalNode.touchExpansionPx(YogaEdge.BOTTOM, 1);
    mInternalNode.touchExpansionPx(YogaEdge.START, 1);
    mInternalNode.touchExpansionPx(YogaEdge.END, 1);
    mInternalNode.testKey("testkey");
    mInternalNode.wrapInView();
