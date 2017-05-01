/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaAlign;

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
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .focusable(true)
                        .child(TestViewComponent.create(c)))
                .build();
          }
        });

    assertEquals(1, lithoView.getChildCount());
    // TODO(T16959291): The default varies between internal and external test runs, which indicates
    // that our Robolectric setup is not actually identical. Until we can figure out why,
    // we will compare against the dynamic default instead of asserting false.
    assertEquals(mFocusableDefault, lithoView.isFocusable());

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertTrue(innerHost.isFocusable());
  }

  @Test
  public void testRootHostFocusable() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .focusable(true)
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertEquals(0, lithoView.getChildCount());
    assertTrue(lithoView.isFocusable());
  }
}
