/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static org.junit.Assert.assertEquals;

@Config(
    manifest = Config.NONE,
    sdk = LOLLIPOP,
    shadows = {
        LayoutDirectionViewShadow.class,
        LayoutDirectionViewGroupShadow.class})
@RunWith(ComponentsTestRunner.class)
public class LayoutDirectionTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  /**
   * Test that view mount items are laid out in the correct positions for LTR and RTL layout
   * directions.
   */
  @Test
  public void testViewChildrenLayoutDirection() {
    final TestComponent child1 =
        TestViewComponent.create(mContext, true, true, true, false)
            .build();
    final TestComponent child2 =
        TestViewComponent.create(mContext, true, true, true, false)
            .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.LTR)
                    .child(
                        Layout.create(c, child1).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Layout.create(c, child2).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
        20,
        10);

    View view1 = componentView.getChildAt(0);
    View view2 = componentView.getChildAt(1);

    assertEquals(
        new Rect(0, 0, 10, 10),
        new Rect(
            view1.getLeft(),
            view1.getTop(),
            view1.getRight(),
            view1.getBottom()));

    assertEquals(
        new Rect(10, 0, 20, 10),
        new Rect(
            view2.getLeft(),
            view2.getTop(),
            view2.getRight(),
            view2.getBottom()));

    ComponentTestHelper.mountComponent(
        mContext,
        componentView,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.RTL)
                    .child(
                        Layout.create(c, child1).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Layout.create(c, child2).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
        20,
        10);

    view1 = componentView.getChildAt(0);
    view2 = componentView.getChildAt(1);

    assertEquals(
        new Rect(10, 0, 20, 10),
        new Rect(
            view1.getLeft(),
            view1.getTop(),
            view1.getRight(),
            view1.getBottom()));

    assertEquals(
        new Rect(0, 0, 10, 10),
        new Rect(
            view2.getLeft(),
            view2.getTop(),
            view2.getRight(),
            view2.getBottom()));
  }

  /**
   * Test that drawable mount items are laid out in the correct positions for LTR and RTL layout
   * directions.
   */
  @Test
  public void testDrawableChildrenLayoutDirection() {
    final TestComponent child1 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent child2 = TestDrawableComponent.create(mContext)
        .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.LTR)
                    .child(
                        Layout.create(c, child1).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Layout.create(c, child2).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
        20,
        10);

    Drawable drawable1 = componentView.getDrawables().get(0);
    Drawable drawable2 = componentView.getDrawables().get(1);

    assertEquals(new Rect(0, 0, 10, 10), drawable1.getBounds());
    assertEquals(new Rect(10, 0, 20, 10), drawable2.getBounds());

    ComponentTestHelper.mountComponent(
        mContext,
        componentView,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.RTL)
                    .child(
                        Layout.create(c, child1).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Layout.create(c, child2).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
        20,
        10);

    drawable1 = componentView.getDrawables().get(0);
    drawable2 = componentView.getDrawables().get(1);

    assertEquals(new Rect(10, 0, 20, 10), drawable1.getBounds());
    assertEquals(new Rect(0, 0, 10, 10), drawable2.getBounds());
  }

  /**
   * Test that layout direction is propagated properly throughout a component hierarchy. This is the
   * default behaviour of layout direction.
   */
  @Test
  public void testInheritLayoutDirection() {
    final TestComponent child1 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent child2 = TestDrawableComponent.create(mContext)
        .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.RTL)
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .flexDirection(YogaFlexDirection.ROW)
                            .wrapInView()
                            .child(
                                Layout.create(c, child1).flexShrink(0)
                                    .widthPx(10)
                                    .heightPx(10))
                            .child(
                                Layout.create(c, child2).flexShrink(0)
                                    .widthPx(10)
                                    .heightPx(10)))
                    .build();
              }
            },
        20,
        10);

    final ComponentHost host = (ComponentHost) componentView.getChildAt(0);
    final Drawable drawable1 = host.getDrawables().get(0);
    final Drawable drawable2 = host.getDrawables().get(1);

    assertEquals(new Rect(10, 0, 20, 10), drawable1.getBounds());
    assertEquals(new Rect(0, 0, 10, 10), drawable2.getBounds());
  }

  /**
   * Test that layout direction is correctly set on child components when it differs from the layout
   * direction of it's parent.
   */
  @Test
  public void testNestedComponentWithDifferentLayoutDirection() {
    final TestComponent child1 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent child2 = TestDrawableComponent.create(mContext)
        .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.RTL)
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .flexDirection(YogaFlexDirection.ROW)
                            .layoutDirection(YogaDirection.LTR)
                            .wrapInView()
                            .child(
                                Layout.create(c, child1).flexShrink(0)
                                    .widthPx(10)
                                    .heightPx(10))
                            .child(
                                Layout.create(c, child2).flexShrink(0)
                                    .widthPx(10)
                                    .heightPx(10)))
                    .build();
              }
            },
        20,
        10);

    final ComponentHost host = (ComponentHost) componentView.getChildAt(0);
    final Drawable drawable1 = host.getDrawables().get(0);
    final Drawable drawable2 = host.getDrawables().get(1);

    assertEquals(new Rect(0, 0, 10, 10), drawable1.getBounds());
    assertEquals(new Rect(10, 0, 20, 10), drawable2.getBounds());
  }

  /**
   * Test that margins on START and END are correctly applied to the correct side of the component
   * depending upon the applied layout direction.
   */
  @Test
  public void testMargin() {
    final TestComponent child = TestDrawableComponent.create(mContext)
        .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .layoutDirection(YogaDirection.LTR)
                    .child(
                        Layout.create(c, child).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10)
                            .marginPx(YogaEdge.START, 10)
                            .marginPx(YogaEdge.END, 20))
                    .build();
              }
            },
        40,
        10);

    Drawable drawable = componentView.getDrawables().get(0);
    assertEquals(new Rect(10, 0, 20, 10), drawable.getBounds());

    ComponentTestHelper.mountComponent(
        mContext,
        componentView,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .layoutDirection(YogaDirection.RTL)
                    .child(
                        Layout.create(c, child).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10)
                            .marginPx(YogaEdge.START, 10)
                            .marginPx(YogaEdge.END, 20))
                    .build();
              }
            },
        40,
        10);

    drawable = componentView.getDrawables().get(0);
    assertEquals(new Rect(20, 0, 30, 10), drawable.getBounds());
  }

  /**
   * Test that paddings on START and END are correctly applied to the correct side of the component
   * depending upon the applied layout direction.
   */
  @Test
  public void testPadding() {
    final TestComponent child = TestDrawableComponent.create(mContext)
        .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .layoutDirection(YogaDirection.LTR)
                    .paddingPx(YogaEdge.START, 10)
                    .paddingPx(YogaEdge.END, 20)
                    .child(
                        Layout.create(c, child).flexShrink(0)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
        40,
        10);

