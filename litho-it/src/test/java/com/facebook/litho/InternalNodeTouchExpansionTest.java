/**
 * Copyright (c) 2017-present, Facebook, Inc.
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

import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
    mInternalNode.touchExpansionPx(LEFT, 10);
    assertThat(mInternalNode.getTouchExpansionLeft()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionTopWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(TOP, 10);
    assertThat(mInternalNode.getTouchExpansionTop()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionRightWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(RIGHT, 10);
    assertThat(mInternalNode.getTouchExpansionRight()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionBottomWithoutTouchHandling() {
    mInternalNode.touchHandler(null);
    mInternalNode.touchExpansionPx(BOTTOM, 10);
    assertThat(mInternalNode.getTouchExpansionBottom()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionLeftWithUndefinedStartEnd() {
    mInternalNode.touchExpansionPx(LEFT, 10);
    assertThat(mInternalNode.getTouchExpansionLeft()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedStart() {
    mInternalNode.touchExpansionPx(START, 5);
    mInternalNode.touchExpansionPx(LEFT, 10);
    assertThat(mInternalNode.getTouchExpansionLeft()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedEnd() {
    mInternalNode.touchExpansionPx(END, 5);
    mInternalNode.touchExpansionPx(LEFT, 10);
    assertThat(mInternalNode.getTouchExpansionLeft()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedStartInRtl() {
    setDirection(mInternalNode, RTL);
    mInternalNode.touchExpansionPx(START, 5);
    mInternalNode.touchExpansionPx(LEFT, 10);
    assertThat(mInternalNode.getTouchExpansionLeft()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedEndInRtl() {
    setDirection(mInternalNode, RTL);
    mInternalNode.touchExpansionPx(END, 5);
    mInternalNode.touchExpansionPx(LEFT, 10);
    assertThat(mInternalNode.getTouchExpansionLeft()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionRightWithUndefinedStartEnd() {
    mInternalNode.touchExpansionPx(RIGHT, 10);
    assertThat(mInternalNode.getTouchExpansionRight()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionRightWithDefinedStart() {
    mInternalNode.touchExpansionPx(START, 5);
    mInternalNode.touchExpansionPx(RIGHT, 10);
    assertThat(mInternalNode.getTouchExpansionRight()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionRightWithDefinedEnd() {
    mInternalNode.touchExpansionPx(END, 5);
    mInternalNode.touchExpansionPx(RIGHT, 10);
    assertThat(mInternalNode.getTouchExpansionRight()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionRightWithDefinedStartInRtl() {
    setDirection(mInternalNode, RTL);
    mInternalNode.touchExpansionPx(START, 5);
    mInternalNode.touchExpansionPx(RIGHT, 10);
    assertThat(mInternalNode.getTouchExpansionRight()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionRightWithDefinedEndInRtl() {
    setDirection(mInternalNode, RTL);
    mInternalNode.touchExpansionPx(END, 5);
    mInternalNode.touchExpansionPx(RIGHT, 10);
    assertThat(mInternalNode.getTouchExpansionRight()).isEqualTo(10);
  }
}
