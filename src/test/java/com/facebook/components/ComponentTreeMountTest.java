/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import static org.junit.Assert.assertEquals;

@PrepareForTest(ThreadUtils.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class ComponentTreeMountTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testRemountsWithNewInputOnSameLayout() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        TestDrawableComponent.create(mContext)
          .color(Color.BLACK)
          .build());
    Shadows.shadowOf(componentView).callOnAttachedToWindow();

    assertEquals(1, componentView.getDrawables().size());
    assertEquals(Color.BLACK, ((ColorDrawable) componentView.getDrawables().get(0)).getColor());

    componentView.getComponent().setRoot(
        TestDrawableComponent.create(mContext)
            .color(Color.YELLOW)
            .build());
