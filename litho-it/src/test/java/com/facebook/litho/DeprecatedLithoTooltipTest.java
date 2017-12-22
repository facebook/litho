/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.mockito.Mockito.verify;

import android.view.View;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class DeprecatedLithoTooltipTest {

  private static final int HOST_WIDTH = 400;
  private static final int HOST_HEIGHT = 300;
  private static final int ANCHOR_WIDTH = 200;
  private static final int ANCHOR_HEIGHT = 100;
  private static final int MARGIN_LEFT = 20;
  private static final int MARGIN_TOP = 10;

  private ComponentContext mContext;
  private Component mComponent;
  @Mock public DeprecatedLithoTooltip mLithoTooltip;
  private ComponentTree mComponentTree;
  private LithoView mLithoView;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mContext = new ComponentContext(RuntimeEnvironment.application);

    final Component child =
        TestDrawableComponent.create(mContext)
            .key("anchor")
            .widthPx(ANCHOR_WIDTH)
            .heightPx(ANCHOR_HEIGHT)
            .build();

    mComponent =
        new InlineLayoutSpec() {
          @Override
          @OnCreateLayout
          protected Component onCreateLayout(ComponentContext c) {
            return Row.create(c)
                .marginPx(YogaEdge.LEFT, MARGIN_LEFT)
                .marginPx(YogaEdge.TOP, MARGIN_TOP)
                .child(child)
                .build();
          }
        };

    mComponentTree = ComponentTree.create(mContext, mComponent)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    Whitebox.setInternalState(
        mComponent,
        "mGlobalKey",
        mComponent.getTypeId() + "" + Row.create(mContext).build().getTypeId());

    List<Component> children = new ArrayList<>();
    children.add(child);

    Whitebox.setInternalState(mComponent, "mChildren", children);
    mContext = ComponentContext.withComponentTree(mContext, mComponentTree);
    mContext = ComponentContext.withComponentScope(mContext, mComponent);
    mLithoView = getLithoView(mComponentTree);
  }

  @Test
  public void testBottomLeft() {
    LithoTooltipController.showTooltip(
        mContext,
        mLithoTooltip,
        "anchor",
        TooltipPosition.BOTTOM_LEFT);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT,
        - HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT);
  }

  @Test
  public void testCenterBottom() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.CENTER_BOTTOM);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT + ANCHOR_WIDTH/2,
        - HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT);
  }

  @Test
  public void testBottomRight() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.BOTTOM_RIGHT);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT + ANCHOR_WIDTH,
        - HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT);
  }

  @Test
  public void testCenterRight() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.CENTER_RIGHT);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT + ANCHOR_WIDTH,
        - HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT/2);
  }

  @Test
  public void testTopRight() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.TOP_RIGHT);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT + ANCHOR_WIDTH,
        - HOST_HEIGHT + MARGIN_TOP);
  }

  @Test
  public void testCenterTop() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.CENTER_TOP);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT + ANCHOR_WIDTH/2,
        - HOST_HEIGHT + MARGIN_TOP);
  }

  @Test
  public void testTopLeft() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.TOP_LEFT);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT,
        - HOST_HEIGHT + MARGIN_TOP);
  }

  @Test
  public void testCenterLeft() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.CENTER_LEFT);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT,
        - HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT/2);
  }

  @Test
  public void testCenter() {
    LithoTooltipController.showTooltip(mContext, mLithoTooltip, "anchor", TooltipPosition.CENTER);

    verify(mLithoTooltip).showBottomLeft(
        mLithoView,
        MARGIN_LEFT + ANCHOR_WIDTH/2,
        - HOST_HEIGHT + MARGIN_TOP + ANCHOR_HEIGHT/2);
  }

  private LithoView getLithoView(ComponentTree componentTree) {
    LithoView lithoView = new LithoView(mContext);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        View.MeasureSpec.makeMeasureSpec(HOST_WIDTH, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(HOST_HEIGHT, View.MeasureSpec.EXACTLY));
    lithoView.layout(
        0,
        0,
        lithoView.getMeasuredWidth(),
        lithoView.getMeasuredHeight());
    return lithoView;
  }


}
