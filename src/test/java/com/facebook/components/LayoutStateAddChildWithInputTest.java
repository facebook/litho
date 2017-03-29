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

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateAddChildWithInputTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testNewEmptyLayout() {
    InternalNode node = (InternalNode) Container.create(mContext).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .child(TestLayoutComponent.create(mContext))
        .child(TestLayoutComponent.create(mContext))
        .build();

    assertEquals(2, node.getChildCount());
    assertEquals(0, node.getChildAt(0).getChildCount());
    assertEquals(0, node.getChildAt(1).getChildCount());
  }
}
