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
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeTouchExpansionTest {
  private InternalNode mInternalNode;

  @Before
  public void setup() {
    mInternalNode =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());
    mInternalNode.touchHandler(new EventHandler(null, 1));
  }

  private static void setDirection(InternalNode node, YogaDirection direction) {
    node.layoutDirection(direction);
    node.calculateLayout();
  }

  @Test
  public void testTouchExpansionLeftWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 10);
    assertEquals(0, mInternalNode.getTouchExpansionLeft());
  }

  @Test
  public void testTouchExpansionTopWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(YogaEdge.TOP, 10);
    assertEquals(0, mInternalNode.getTouchExpansionTop());
  }

  @Test
  public void testTouchExpansionRightWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(YogaEdge.RIGHT, 10);
    assertEquals(0, mInternalNode.getTouchExpansionRight());
  }

  @Test
  public void testTouchExpansionBottomWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(YogaEdge.BOTTOM, 10);
    assertEquals(0, mInternalNode.getTouchExpansionBottom());
  }

  @Test
  public void testTouchExpansionLeftWithUndefinedStartEnd() {
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 10);
    assertEquals(10, mInternalNode.getTouchExpansionLeft());
  }

  @Test
  public void testTouchExpansionLeftWithDefinedStart() {
    mInternalNode.touchExpansionPx(YogaEdge.START, 5);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 10);
    assertEquals(5, mInternalNode.getTouchExpansionLeft());
  }

  @Test
  public void testTouchExpansionLeftWithDefinedEnd() {
    mInternalNode.touchExpansionPx(YogaEdge.END, 5);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 10);
    assertEquals(10, mInternalNode.getTouchExpansionLeft());
  }

  @Test
  public void testTouchExpansionLeftWithDefinedStartInRtl() {
    setDirection(mInternalNode, YogaDirection.RTL);
    mInternalNode.touchExpansionPx(YogaEdge.START, 5);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 10);
    assertEquals(10, mInternalNode.getTouchExpansionLeft());
  }

  @Test
  public void testTouchExpansionLeftWithDefinedEndInRtl() {
    setDirection(mInternalNode, YogaDirection.RTL);
    mInternalNode.touchExpansionPx(YogaEdge.END, 5);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 10);
    assertEquals(5, mInternalNode.getTouchExpansionLeft());
  }

  @Test
  public void testTouchExpansionRightWithUndefinedStartEnd() {
    mInternalNode.touchExpansionPx(YogaEdge.RIGHT, 10);
    assertEquals(10, mInternalNode.getTouchExpansionRight());
  }

  @Test
  public void testTouchExpansionRightWithDefinedStart() {
