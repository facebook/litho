/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeResolvedPaddingTest {
  private InternalNode mInternalNode;

  @Before
  public void setup() {
    mInternalNode =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());
  }

  private static void setDirection(InternalNode node, YogaDirection direction) {
    node.layoutDirection(direction);
    node.calculateLayout();
    node.markLayoutSeen();
  }

  @Test
  public void testPaddingLeftWithUndefinedStartEnd() {
    mInternalNode.paddingPx(YogaEdge.LEFT, 10);
    setDirection(mInternalNode, YogaDirection.LTR);
    assertEquals(10, mInternalNode.getPaddingLeft());
  }

  @Test
  public void testPaddingLeftWithDefinedStart() {
    mInternalNode.paddingPx(YogaEdge.START, 5);
    mInternalNode.paddingPx(YogaEdge.LEFT, 10);
    setDirection(mInternalNode, YogaDirection.LTR);
    assertEquals(5, mInternalNode.getPaddingLeft());
  }

  @Test
  public void testPaddingLeftWithDefinedEnd() {
    mInternalNode.paddingPx(YogaEdge.END, 5);
    mInternalNode.paddingPx(YogaEdge.LEFT, 10);
    setDirection(mInternalNode, YogaDirection.LTR);
    assertEquals(10, mInternalNode.getPaddingLeft());
  }

  @Test
  public void testPaddingLeftWithDefinedStartInRtl() {
    mInternalNode.paddingPx(YogaEdge.START, 5);
    mInternalNode.paddingPx(YogaEdge.LEFT, 10);
    setDirection(mInternalNode, YogaDirection.RTL);
    assertEquals(10, mInternalNode.getPaddingLeft());
  }

  @Test
  public void testPaddingLeftWithDefinedEndInRtl() {
    mInternalNode.paddingPx(YogaEdge.END, 5);
    mInternalNode.paddingPx(YogaEdge.LEFT, 10);
    setDirection(mInternalNode, YogaDirection.RTL);
    assertEquals(5, mInternalNode.getPaddingLeft());
  }

  @Test
  public void testPaddingRightWithUndefinedStartEnd() {
    mInternalNode.paddingPx(YogaEdge.RIGHT, 10);
    setDirection(mInternalNode, YogaDirection.LTR);
    assertEquals(10, mInternalNode.getPaddingRight());
  }

  @Test
  public void testPaddingRightWithDefinedStart() {
    mInternalNode.paddingPx(YogaEdge.START, 5);
    mInternalNode.paddingPx(YogaEdge.RIGHT, 10);
    setDirection(mInternalNode, YogaDirection.LTR);
