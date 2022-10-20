/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.LayoutContext;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LithoNodeTouchExpansionTest {

  private LithoNode mNode;
  ComponentContext mContext;
  private LayoutStateContext mLayoutStateContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());

    final RenderStateContext renderStateContext = mContext.setRenderStateContextForTests();

    mNode = ResolvedTree.resolve(renderStateContext, mContext, Column.create(mContext).build());
    mNode.mutableNodeInfo().setTouchHandler(new EventHandler(null, 1));

    mLayoutStateContext =
        new LayoutStateContext(
            renderStateContext.getCache(),
            mContext,
            renderStateContext.getTreeState(),
            mContext.getComponentTree(),
            renderStateContext.getLayoutVersion(),
            null,
            null);
  }

  private LithoNodeTouchExpansionTest setDirection(YogaDirection direction) {
    mNode.layoutDirection(direction);
    return this;
  }

  private LithoNodeTouchExpansionTest touchExpansionPx(YogaEdge edge, int value) {
    mNode.touchExpansionPx(edge, value);
    return this;
  }

  private LithoLayoutResult calculateLayout() {
    final LayoutContext<LithoRenderContext> context =
        new LayoutContext<>(
            mContext.getAndroidContext(),
            new LithoRenderContext(mLayoutStateContext),
            0,
            null,
            null);
    return mNode.calculateLayout(context, UNSPECIFIED, UNSPECIFIED);
  }

  @Test
  public void testTouchExpansionLeftWithoutTouchHandling() {
    mNode.mutableNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(LEFT, 10).calculateLayout();
    assertThat(result.getTouchExpansionLeft()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionTopWithoutTouchHandling() {
    mNode.mutableNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(TOP, 10).calculateLayout();
    assertThat(result.getTouchExpansionTop()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionRightWithoutTouchHandling() {
    mNode.mutableNodeInfo().setTouchHandler(null);
    LithoLayoutResult result = touchExpansionPx(RIGHT, 10).calculateLayout();
    assertThat(result.getTouchExpansionRight()).isEqualTo(0);
  }

  @Test
  public void testTouchExpansionBottomWithoutTouchHandling() {
    mNode.mutableNodeInfo().setTouchHandler(null);
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
