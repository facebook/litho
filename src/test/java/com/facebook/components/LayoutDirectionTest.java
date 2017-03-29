/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;
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
                return Container.create(c)
                    .flexDirection(YogaFlexDirection.ROW)
                    .layoutDirection(YogaDirection.LTR)
                    .child(
                        Layout.create(c, child1)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Layout.create(c, child2)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
        20,
        10);

    View view1 = componentView.getChildAt(0);
    View view2 = componentView.getChildAt(1);
