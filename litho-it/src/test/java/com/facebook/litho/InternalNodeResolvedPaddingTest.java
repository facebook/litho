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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Layout.createAndMeasureComponent;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaDirection.LTR;
import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaDirection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeResolvedPaddingTest {
  private InternalNode mInternalNode;

  @Before
  public void setup() {
    final ComponentContext context = new ComponentContext(getApplicationContext());
    context.setLayoutStateContextForTesting();

    mInternalNode =
        createAndMeasureComponent(
            context,
            Column.create(context).build(),
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED));
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
