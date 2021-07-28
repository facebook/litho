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
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.RenderState;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class InternalNodeTouchExpansionTest {

  private InternalNode mInternalNode;
  ComponentContext mContext;
  private LayoutStateContext mLayoutStateContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mLayoutStateContext = LayoutStateContext.getTestInstance(mContext);
    mContext.setLayoutStateContext(mLayoutStateContext);
    mInternalNode = Layout.create(mLayoutStateContext, mContext, Column.create(mContext).build());
    mInternalNode.getOrCreateNodeInfo().setTouchHandler(new EventHandler(null, 1));
  }

  private InternalNodeTouchExpansionTest setDirection(YogaDirection direction) {
    mInternalNode.layoutDirection(direction);
    return this;
  }

  private InternalNodeTouchExpansionTest touchExpansionPx(YogaEdge edge, int value) {
    mInternalNode.touchExpansionPx(edge, value);
    return this;
  }

  private LithoLayoutResult calculateLayout() {
    final RenderState.LayoutContext<LithoRenderContext> context =
        new RenderState.LayoutContext<>(
            mContext.getAndroidContext(),
            new LithoRenderContext(mLayoutStateContext, null, null),
            0,
            null,
            null);
    return mInternalNode.calculateLayout(context, UNSPECIFIED, UNSPECIFIED);
  }

  @Test
  public void testTouchExpansionLeftWithoutTouchHandling() {
    mInternalNode.getOrCreateNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionTopWithoutTouchHandling() {
    mInternalNode.getOrCreateNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(TOP, 10).calculateLayout();
    assertThat(result.getTouchExpansionTop()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionRightWithoutTouchHandling() {
    mInternalNode.getOrCreateNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionBottomWithoutTouchHandling() {
    mInternalNode.getOrCreateNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(BOTTOM, 10).calculateLayout();
    assertThat(result.getTouchExpansionBottom()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionLeftWithUndefinedStartEnd() {
    LithoLayoutResult result = touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedStart() {
    LithoLayoutResult result =
        touchExpansionPx(START, 5).touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedEnd() {
    LithoLayoutResult result =
        touchExpansionPx(END, 5).touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedStartInRtl() {
    LithoLayoutResult result =
        setDirection(RTL).touchExpansionPx(START, 5).touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionLeftWithDefinedEndInRtl() {
    LithoLayoutResult result =
        setDirection(RTL).touchExpansionPx(END, 5).touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionRightWithUndefinedStartEnd() {
    LithoLayoutResult result = touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionRightWithDefinedStart() {
    LithoLayoutResult result =
        touchExpansionPx(START, 5).touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(10);
  }

  @Test
  public void testTouchExpansionRightWithDefinedEnd() {
    LithoLayoutResult result =
        touchExpansionPx(END, 5).touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionRightWithDefinedStartInRtl() {
    LithoLayoutResult result =
        setDirection(RTL).touchExpansionPx(START, 5).touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(5);
  }

  @Test
  public void testTouchExpansionRightWithDefinedEndInRtl() {
    LithoLayoutResult result =
        setDirection(RTL).touchExpansionPx(END, 5).touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(10);
  }
}
