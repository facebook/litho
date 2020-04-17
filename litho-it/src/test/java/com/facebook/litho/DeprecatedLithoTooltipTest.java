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

import static org.mockito.Mockito.verify;

import android.view.View;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class DeprecatedLithoTooltipTest {

  private static final int HOST_WIDTH = 400;
  private static final int HOST_HEIGHT = 300;
  private static final int ANCHOR_WIDTH = 200;
  private static final int ANCHOR_HEIGHT = 100;
  private static final int MARGIN_LEFT = 20;
  private static final int MARGIN_TOP = 10;

  private static final String KEY_ANCHOR = "anchor";

  private ComponentContext mContext;
  private Component mComponent;
  @Mock public DeprecatedLithoTooltip mLithoTooltip;
  private ComponentTree mComponentTree;
  private LithoView mLithoView;
  private String mAnchorGlobalKey;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Row.create(c)
                .marginPx(YogaEdge.LEFT, MARGIN_LEFT)
                .marginPx(YogaEdge.TOP, MARGIN_TOP)
                .child(
                    TestDrawableComponent.create(c)
                        .key(KEY_ANCHOR)
                        .widthPx(ANCHOR_WIDTH)
                        .heightPx(ANCHOR_HEIGHT))
                .build();
          }
        };

    mComponentTree = ComponentTree.create(mContext, mComponent).build();

    Whitebox.setInternalState(mComponent, "mGlobalKey", mComponent.getKey());

    mContext = ComponentContext.withComponentTree(mContext, mComponentTree);
    mContext = ComponentContext.withComponentScope(mContext, mComponent);
    mLithoView = getLithoView(mComponentTree);

    mAnchorGlobalKey =
        ComponentKeyUtils.getKeyWithSeparator(Row.create(mContext).build().getTypeId(), KEY_ANCHOR);
  }

  @Test
  public void testBottomLeft() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.BOTTOM_LEFT);

    verify(mLithoTooltip)
        .showBottomLeft(mLithoView, MARGIN_LEFT, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT);
  }

  @Test
  public void testCenterBottom() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.CENTER_BOTTOM);

    verify(mLithoTooltip)
        .showBottomLeft(
            mLithoView, MARGIN_LEFT + ANCHOR_WIDTH / 2, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT);
  }

  @Test
  public void testBottomRight() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.BOTTOM_RIGHT);

    verify(mLithoTooltip)
        .showBottomLeft(
            mLithoView, MARGIN_LEFT + ANCHOR_WIDTH, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT);
  }

  @Test
  public void testCenterRight() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.CENTER_RIGHT);

    verify(mLithoTooltip)
        .showBottomLeft(
            mLithoView, MARGIN_LEFT + ANCHOR_WIDTH, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT / 2);
  }

  @Test
  public void testTopRight() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.TOP_RIGHT);

    verify(mLithoTooltip)
        .showBottomLeft(mLithoView, MARGIN_LEFT + ANCHOR_WIDTH, -HOST_HEIGHT + MARGIN_TOP);
  }

  @Test
  public void testCenterTop() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.CENTER_TOP);

    verify(mLithoTooltip)
        .showBottomLeft(mLithoView, MARGIN_LEFT + ANCHOR_WIDTH / 2, -HOST_HEIGHT + MARGIN_TOP);
  }

  @Test
  public void testTopLeft() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.TOP_LEFT);

    verify(mLithoTooltip).showBottomLeft(mLithoView, MARGIN_LEFT, -HOST_HEIGHT + MARGIN_TOP);
  }

  @Test
  public void testCenterLeft() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.CENTER_LEFT);

    verify(mLithoTooltip)
        .showBottomLeft(mLithoView, MARGIN_LEFT, -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT / 2);
  }

  @Test
  public void testCenter() {
    LithoTooltipController.showTooltip(
        mContext, mLithoTooltip, mAnchorGlobalKey, TooltipPosition.CENTER);

    verify(mLithoTooltip)
        .showBottomLeft(
            mLithoView,
            MARGIN_LEFT + ANCHOR_WIDTH / 2,
            -HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT / 2);
  }

  private LithoView getLithoView(ComponentTree componentTree) {
    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(HOST_WIDTH, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(HOST_HEIGHT, View.MeasureSpec.EXACTLY));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());
    return lithoView;
  }
}
