/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.view.View;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

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
public class LithoViewTest {
  private LithoView mLithoView;

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
        .layoutDiffing(false)
        .build();

    mLithoView = new LithoView(RuntimeEnvironment.application);
    mLithoView.setComponentTree(componentTree);
  }

  @Test
  public void measureBeforeBeingAttached() {
    mLithoView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    mLithoView.layout(
        0,
        0,
        mLithoView.getMeasuredWidth(),
        mLithoView.getMeasuredHeight());

    // View got measured.
    assertTrue(mLithoView.getMeasuredHeight() != 0 && mLithoView.getMeasuredWidth() != 0);

    // Attaching will automatically mount since we already have a layout fitting our size.
    ShadowView shadow = Shadows.shadowOf(mLithoView);
    shadow.callOnAttachedToWindow();

    assertEquals(2, getInternalMountItems(mLithoView).length);
  }

  private static long[] getInternalMountItems(LithoView lithoView) {
    MountState mountState = Whitebox.getInternalState(lithoView, "mMountState");
    return Whitebox.getInternalState(mountState, "mLayoutOutputsIds");
  }

  @Test
  public void testNullLithoViewDimensions() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return null;
      }
    };

    LithoView nullLithoView = new LithoView(RuntimeEnvironment.application);
    nullLithoView.setComponentTree(
        ComponentTree.create(
            new ComponentContext(RuntimeEnvironment.application),
            component)
            .incrementalMount(false)
            .build());

    nullLithoView.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    nullLithoView.layout(
        0,
        0,
        nullLithoView.getMeasuredWidth(),
        nullLithoView.getMeasuredHeight());

    assertTrue(nullLithoView.getMeasuredHeight() == 0
        && nullLithoView.getMeasuredWidth() == 0);
  }
}
