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

import com.facebook.yoga.YogaFlexDirection;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestViewComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class MountStateViewTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testViewPaddingAndBackground() {
    final int color = 0xFFFF0000;
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .child(
                    TestViewComponent.create(c)
                        .withLayout().flexShrink(0)
                        .paddingPx(LEFT, 5)
                        .paddingPx(TOP, 6)
                        .paddingPx(RIGHT, 7)
                        .paddingPx(BOTTOM, 8)
                        .backgroundColor(color))
                .build();
          }
        });
