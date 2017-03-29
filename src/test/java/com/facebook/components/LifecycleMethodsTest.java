/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class LifecycleMethodsTest {

  private enum LifecycleStep {
    ON_CREATE_LAYOUT,
    ON_PREPARE,
    ON_MEASURE,
    ON_BOUNDS_DEFINED,
    ON_CREATE_MOUNT_CONTENT,
    ON_MOUNT,
    ON_BIND,
    ON_UNBIND,
    ON_UNMOUNT
  }

  private ComponentView mComponentView;
  private ComponentTree mComponentTree;
  private LifecycleMethodsComponent mLifecycle;
  private LifecycleMethodsInstance mComponent;

  @Before
  public void setup() throws Exception {
    mComponentView = new ComponentView(RuntimeEnvironment.application);
    mLifecycle = new LifecycleMethodsComponent();
    mComponent = mLifecycle.create(10);

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    mComponentTree = ComponentTree.create(c, mComponent)
        .incrementalMount(false)
        .build();
    mComponentView.setComponent(mComponentTree);
  }

  @Test
  public void testLifecycle() {
    mComponentView.onAttachedToWindow();
    ComponentTestHelper.measureAndLayout(mComponentView);

    assertEquals(LifecycleStep.ON_BIND, mComponent.getCurrentStep());

    mComponentView.onDetachedFromWindow();
    assertEquals(LifecycleStep.ON_UNBIND, mComponent.getCurrentStep());

    mComponentView.onAttachedToWindow();
    assertEquals(LifecycleStep.ON_BIND, mComponent.getCurrentStep());

    mComponentTree.setRoot(mLifecycle.create(20));
    ComponentTestHelper.measureAndLayout(mComponentView);
    assertEquals(LifecycleStep.ON_UNMOUNT, mComponent.getCurrentStep());

    mComponentTree.setRoot(mComponent);
    ComponentTestHelper.measureAndLayout(mComponentView);
    assertEquals(LifecycleStep.ON_BIND, mComponent.getCurrentStep());

    mComponentView.onDetachedFromWindow();
    mComponentTree.setRoot(mComponent);
    ComponentTestHelper.measureAndLayout(mComponentView);
    assertEquals(LifecycleStep.ON_UNBIND, mComponent.getCurrentStep());
  }

  private class LifecycleMethodsComponent extends ComponentLifecycle {

    @Override
    protected ComponentLayout onCreateLayout(ComponentContext c, Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_CREATE_LAYOUT);

      return super.onCreateLayout(c, component);
    }

    @Override
    protected void onPrepare(ComponentContext c, Component<?> component) {
      LifecycleMethodsInstance instance = (LifecycleMethodsInstance) component;
      instance.setCurrentStep(LifecycleStep.ON_PREPARE);
    }

    @Override
    protected boolean canMeasure() {
      return true;
    }

    @Override
    protected void onMeasure(
