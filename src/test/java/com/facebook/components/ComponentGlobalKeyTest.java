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

import android.view.View;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.CardClip;
import com.facebook.litho.widget.Text;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentGlobalKeyTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testComponentKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .build();
    Assert.assertEquals(component.getKey(), component.getLifecycle().getId() + "");
    Assert.assertNull(component.getGlobalKey());
  }

  @Test
  public void testComponentManualKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .key("someKey")
        .build();
    Assert.assertEquals(component.getKey(), "someKey");
    Assert.assertNull(component.getGlobalKey());
  }

  @Test
  public void testComponentGlobalKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .build();
    System.out.println(component.getLifecycle().getId());
    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .build();
    ComponentView componentView = getComponentView(componentTree);

    Assert.assertEquals(
        componentView.getMountItemAt(0).getComponent().getGlobalKey(),
        component.getKey());
  }

  @Test
  public void testComponentGlobalKeyManualKey() {
    Component component = TestDrawableComponent
        .create(mContext)
        .key("someKey")
        .build();
    System.out.println(component.getLifecycle().getId());
    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .build();
    ComponentView componentView = getComponentView(componentTree);

    Assert.assertEquals(
        componentView.getMountItemAt(0).getComponent().getGlobalKey(),
        "someKey");
  }

  @Test
  public void testMultipleChildrenComponentKey() {
    Component component = getMultipleChildrenComponent();

    int layoutSpecId = component.getLifecycle().getId();
    int nestedLayoutSpecId = layoutSpecId - 1;

    ComponentTree componentTree = ComponentTree.create(mContext, component)
        .incrementalMount(false)
        .build();
