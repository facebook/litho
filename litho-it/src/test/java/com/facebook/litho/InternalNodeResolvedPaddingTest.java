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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.yoga.YogaDirection.LTR;
import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
    mInternalNode.paddingPx(LEFT, 10);
    setDirection(mInternalNode, LTR);
    assertThat(mInternalNode.getPaddingLeft()).isEqualTo(10);
  }

  @Test
  public void testPaddingLeftWithDefinedStart() {
    mInternalNode.paddingPx(START, 5);
    mInternalNode.paddingPx(LEFT, 10);
    setDirection(mInternalNode, LTR);
    assertThat(mInternalNode.getPaddingLeft()).isEqualTo(5);
  }

  @Test
  public void testPaddingLeftWithDefinedEnd() {
    mInternalNode.paddingPx(END, 5);
    mInternalNode.paddingPx(LEFT, 10);
    setDirection(mInternalNode, LTR);
    assertThat(mInternalNode.getPaddingLeft()).isEqualTo(10);
  }

  @Test
  public void testPaddingLeftWithDefinedStartInRtl() {
    mInternalNode.paddingPx(START, 5);
    mInternalNode.paddingPx(LEFT, 10);
    setDirection(mInternalNode, RTL);
    assertThat(mInternalNode.getPaddingLeft()).isEqualTo(10);
  }

  @Test
  public void testPaddingLeftWithDefinedEndInRtl() {
    mInternalNode.paddingPx(END, 5);
    mInternalNode.paddingPx(LEFT, 10);
    setDirection(mInternalNode, RTL);
    assertThat(mInternalNode.getPaddingLeft()).isEqualTo(5);
  }

  @Test
  public void testPaddingRightWithUndefinedStartEnd() {
    mInternalNode.paddingPx(RIGHT, 10);
    setDirection(mInternalNode, LTR);
    assertThat(mInternalNode.getPaddingRight()).isEqualTo(10);
  }

  @Test
  public void testPaddingRightWithDefinedStart() {
    mInternalNode.paddingPx(START, 5);
    mInternalNode.paddingPx(RIGHT, 10);
    setDirection(mInternalNode, LTR);
    assertThat(mInternalNode.getPaddingRight()).isEqualTo(10);
  }

  @Test
  public void testPaddingRightWithDefinedEnd() {
    mInternalNode.paddingPx(END, 5);
    mInternalNode.paddingPx(RIGHT, 10);
    setDirection(mInternalNode, LTR);
    assertThat(mInternalNode.getPaddingRight()).isEqualTo(5);
  }

  @Test
  public void testPaddingRightWithDefinedStartInRtl() {
    mInternalNode.paddingPx(START, 5);
    mInternalNode.paddingPx(RIGHT, 10);
    setDirection(mInternalNode, RTL);
    assertThat(mInternalNode.getPaddingRight()).isEqualTo(5);
  }

  @Test
  public void testPaddingRightWithDefinedEndInRtl() {
    mInternalNode.paddingPx(END, 5);
    mInternalNode.paddingPx(RIGHT, 10);
    setDirection(mInternalNode, RTL);
    assertThat(mInternalNode.getPaddingRight()).isEqualTo(10);
  }
}
