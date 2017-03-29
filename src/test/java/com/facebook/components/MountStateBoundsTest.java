/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaFlexDirection;

import android.graphics.Rect;
import android.view.View;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaJustify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaFlexDirection.COLUMN;
import static org.junit.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class MountStateBoundsTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testMountedDrawableBounds() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c)
                .withLayout().flexShrink(0)
                .widthPx(10)
                .heightPx(10)
                .build();
          }
        });

    assertEquals(new Rect(0, 0, 10, 10), componentView.getDrawables().get(0).getBounds());
  }

  @Test
  public void testMountedViewBounds() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return TestViewComponent.create(c)
                .withLayout().flexShrink(0)
                .widthPx(10)
                .heightPx(10)
                .build();
          }
        });

    final View mountedView = componentView.getChildAt(0);
    assertEquals(
        new Rect(0, 0, 10, 10),
        new Rect(
            mountedView.getLeft(),
            mountedView.getTop(),
            mountedView.getRight(),
            mountedView.getBottom()));
  }

  @Test
  public void testInnerComponentHostBounds() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .child(
                    Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                        .widthPx(20)
                        .heightPx(20)
                        .wrapInView()
                        .child(
                            TestDrawableComponent.create(c)
                                .withLayout().flexShrink(0)
                                .widthPx(10)
                                .heightPx(10)))
                .build();
          }
        });

    final ComponentHost host = (ComponentHost) componentView.getChildAt(0);
    assertEquals(new Rect(0, 0, 10, 10), host.getDrawables().get(0).getBounds());
    assertEquals(
        new Rect(0, 0, 20, 20),
        new Rect(
            host.getLeft(),
            host.getTop(),
            host.getRight(),
            host.getBottom()));
  }

  @Test
  public void testDoubleInnerComponentHostBounds() {
