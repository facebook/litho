// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class MountStateFocusableTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testInnerComponentHostFocusable() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(
                    Container.create(c)
                        .focusable(true)
                        .child(TestViewComponent.create(c)))
                .build();
          }
        });

    assertEquals(1, componentView.getChildCount());
    assertFalse(componentView.isFocusable());

    ComponentHost innerHost = (ComponentHost) componentView.getChildAt(0);
    assertTrue(innerHost.isFocusable());
  }

  @Test
  public void testRootHostFocusable() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .focusable(true)
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertEquals(0, componentView.getChildCount());
    assertTrue(componentView.isFocusable());
  }
}
