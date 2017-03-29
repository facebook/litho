/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class MountStateFocusableTest {

  private ComponentContext mContext;
  private boolean mFocusableDefault;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mFocusableDefault = new ComponentHost(mContext).isFocusable();
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
    // TODO(T16959291): The default varies between internal and external test runs, which indicates
    // that our Robolectric setup is not actually identical. Until we can figure out why,
    // we will compare against the dynamic default instead of asserting false.
    assertEquals(mFocusableDefault, componentView.isFocusable());

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
