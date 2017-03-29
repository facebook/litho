/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.content.Context;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.components.SizeSpec.EXACTLY;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ComponentView} and {@link MountState} to make sure mount only happens once when
 * attaching the view and setting the component.
 */

@RunWith(ComponentsTestRunner.class)
public class ComponentViewMountTest {
  private ComponentContext mContext;
  private TestComponentView mComponentView;
  private Component mComponent;
  private ComponentTree mComponentTree;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);

    mComponentView = new TestComponentView(mContext);
    mComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return TestDrawableComponent.create(c)
            .withLayout()
            .widthPx(100)
            .heightPx(100)
            .build();
      }
    };

    mComponentTree = ComponentTree.create(mContext, mComponent)
        .incrementalMount(false)
        .build();
    mComponentTree.setSizeSpec(
        SizeSpec.makeSizeSpec(100, EXACTLY),
        SizeSpec.makeSizeSpec(100, EXACTLY));
  }

  @Test
  public void testNothingCalledUntilMeasured() {
    mComponentView.setComponent(mComponentTree);
    mComponentView.onAttachedToWindow();

    assertEquals(0, mComponentView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testSetComponentAndAttachRequestsLayout() {
    mComponentView.setMeasured(10, 10);
    mComponentView.setComponent(mComponentTree);
    mComponentView.onAttachedToWindow();

    assertEquals(1, mComponentView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testSetSameSizeComponentAndAttachRequestsLayout() {
    mComponentView.setMeasured(100, 100);
    mComponentView.setComponent(mComponentTree);
    mComponentView.onAttachedToWindow();

    assertEquals(1, mComponentView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testSetComponentTwiceWithResetAndAttachRequestsLayout() {
    ComponentTree ct = ComponentTree.create(mContext, mComponent)
