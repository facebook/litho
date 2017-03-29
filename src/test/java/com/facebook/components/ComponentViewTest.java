/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.view.View;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class ComponentViewTest {
  private ComponentView mComponentView;

  @Before
  public void setup() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return TestDrawableComponent.create(c)
            .withLayout()
            .widthPx(100)
            .heightPx(100)
            .build();
      }
    };

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final ComponentTree componentTree = ComponentTree.create(c, component)
        .incrementalMount(false)
        .build();

    mComponentView = new ComponentView(RuntimeEnvironment.application);
    mComponentView.setComponent(componentTree);
  }

  @Test
  public void measureBeforeBeingAttached() {
    mComponentView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    mComponentView.layout(
        0,
        0,
        mComponentView.getMeasuredWidth(),
        mComponentView.getMeasuredHeight());

    // View got measured.
    assertTrue(mComponentView.getMeasuredHeight() != 0 && mComponentView.getMeasuredWidth() != 0);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = Shadows.shadowOf(mComponentView);
