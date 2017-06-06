/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link LithoView} and {@link MountState} to make sure mount only happens once when
 * attaching the view and setting the component.
 */

@RunWith(ComponentsTestRunner.class)
public class LithoViewMountTest {
  private ComponentContext mContext;
  private TestLithoView mLithoView;
  private Component mComponent;
  private ComponentTree mComponentTree;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);

    mLithoView = new TestLithoView(mContext);
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
        .layoutDiffing(false)
        .build();
    mComponentTree.setSizeSpec(
        SizeSpec.makeSizeSpec(100, EXACTLY),
        SizeSpec.makeSizeSpec(100, EXACTLY));
  }

  @Test
  public void testOnlyRequestLayoutCalledUntilMeasured() {
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertEquals(1, mLithoView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testSetComponentAndAttachRequestsLayout() {
    mLithoView.setMeasured(10, 10);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testSetSameSizeComponentAndAttachRequestsLayout() {
    mLithoView.setMeasured(100, 100);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testSetComponentTwiceWithResetAndAttachRequestsLayout() {
    ComponentTree ct = ComponentTree.create(mContext, mComponent)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    ct.setSizeSpec(100, 100);

    mLithoView.setComponentTree(ct);
    mLithoView.setMeasured(100, 100);
    mLithoView.onAttachedToWindow();

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());

    mLithoView.onDetachedFromWindow();

    mLithoView.resetRequestLayoutInvocationCount();

    mLithoView.setComponentTree(ct);
    mLithoView.onAttachedToWindow();

    assertEquals(1, mLithoView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testAttachAndSetSameSizeComponentRequestsLayout() {
    mLithoView.setMeasured(100, 100);
    mLithoView.onAttachedToWindow();
    mLithoView.setComponentTree(mComponentTree);

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testAttachAndSetComponentRequestsLayout() {
    mLithoView.setMeasured(10, 10);
    mLithoView.onAttachedToWindow();
    mLithoView.setComponentTree(mComponentTree);

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());
  }

  @Test
  public void testReAttachRequestsLayout() {
    mLithoView.setMeasured(100, 100);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());

    mLithoView.onDetachedFromWindow();
    mLithoView.resetRequestLayoutInvocationCount();
    mLithoView.onAttachedToWindow();

    assertEquals(1, mLithoView.getRequestLayoutInvocationCount());

    ComponentTree newComponentTree =
        ComponentTree.create(mContext, mComponent)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    newComponentTree.setSizeSpec(
        SizeSpec.makeSizeSpec(100, EXACTLY),
        SizeSpec.makeSizeSpec(100, EXACTLY));

    mLithoView.resetRequestLayoutInvocationCount();
    mLithoView.setComponentTree(newComponentTree);

    assertEquals(2, mLithoView.getRequestLayoutInvocationCount());
  }

  private static class TestLithoView extends LithoView {
    private int mRequestLayoutInvocationCount = 0;

    public TestLithoView(Context context) {
      super(context);
    }

    @Override
    public void requestLayout() {
      super.requestLayout();
      mRequestLayoutInvocationCount++;
    }

    public int getRequestLayoutInvocationCount() {
      return mRequestLayoutInvocationCount;
    }

    public void resetRequestLayoutInvocationCount() {
      mRequestLayoutInvocationCount = 0;
    }

    public void setMeasured(int width, int height) {
      setMeasuredDimension(width, height);
    }
  }
}
