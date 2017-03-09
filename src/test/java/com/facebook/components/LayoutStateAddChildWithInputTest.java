// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestLayoutComponent;

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
    InternalNode node = (InternalNode) Container.create(mContext)
        .child(TestLayoutComponent.create(mContext))
        .child(TestLayoutComponent.create(mContext))
        .build();

    assertEquals(2, node.getChildCount());
    assertEquals(0, node.getChildAt(0).getChildCount());
    assertEquals(0, node.getChildAt(1).getChildCount());
  }
}
