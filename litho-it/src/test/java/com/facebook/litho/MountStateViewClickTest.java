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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class MountStateViewClickTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testInnerComponentHostClickable() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .clickHandler(c.newEventHandler(1))
                        .child(TestViewComponent.create(c)))
                .build();
          }
        });

    assertEquals(1, lithoView.getChildCount());
    assertFalse(lithoView.isClickable());

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertTrue(innerHost.isClickable());
  }

  @Test
  public void testInnerComponentHostClickableWithLongClickHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .longClickHandler(c.newEventHandler(1))
                        .child(TestViewComponent.create(c)))
                .build();
          }
        });

    assertEquals(1, lithoView.getChildCount());
    assertFalse(lithoView.isClickable());

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertTrue(innerHost.isLongClickable());
  }

  @Test
  public void testRootHostClickable() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertEquals(0, lithoView.getChildCount());
    assertTrue(lithoView.isClickable());
  }

  @Test
  public void testRootHostClickableWithLongClickHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertEquals(0, lithoView.getChildCount());
    assertTrue(lithoView.isLongClickable());
  }
}
