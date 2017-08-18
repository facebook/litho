/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Rect;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

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

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(1);
  }

  @Test
  public void testSetComponentAndAttachRequestsLayout() {
    mLithoView.setMeasured(10, 10);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(2);
  }

  @Test
  public void testSetSameSizeComponentAndAttachRequestsLayout() {
    mLithoView.setMeasured(100, 100);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(2);
  }

  @Test
  public void testSetComponentTwiceWithResetAndAttachRequestsLayout() {
    ComponentTree ct = create(mContext, mComponent)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();
    ct.setSizeSpec(100, 100);

    mLithoView.setComponentTree(ct);
    mLithoView.setMeasured(100, 100);
    mLithoView.onAttachedToWindow();

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(2);

    mLithoView.onDetachedFromWindow();

    mLithoView.resetRequestLayoutInvocationCount();

    mLithoView.setComponentTree(ct);
    mLithoView.onAttachedToWindow();

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(1);
  }

  @Test
  public void testAttachAndSetSameSizeComponentRequestsLayout() {
    mLithoView.setMeasured(100, 100);
    mLithoView.onAttachedToWindow();
    mLithoView.setComponentTree(mComponentTree);

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(1);
  }

  @Test
  public void testAttachAndSetComponentRequestsLayout() {
    mLithoView.setMeasured(10, 10);
    mLithoView.onAttachedToWindow();
    mLithoView.setComponentTree(mComponentTree);

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(1);
  }

  @Test
  public void testReAttachRequestsLayout() {
    mLithoView.setMeasured(100, 100);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(2);

    mLithoView.onDetachedFromWindow();
    mLithoView.resetRequestLayoutInvocationCount();
    mLithoView.onAttachedToWindow();

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(1);

    ComponentTree newComponentTree =
        create(mContext, mComponent)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();
    newComponentTree.setSizeSpec(
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    mLithoView.resetRequestLayoutInvocationCount();
    mLithoView.setComponentTree(newComponentTree);

    assertThat(mLithoView.getRequestLayoutInvocationCount()).isEqualTo(1);
  }

  @Test
  public void testSetHasTransientStateMountsEverythingIfIncrementalMountEnabled() {
    final TestComponent child1 = TestViewComponent.create(mContext).build();
    final TestComponent child2 = TestDrawableComponent.create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Layout.create(c, child1).widthPx(10).heightPx(10))
                    .child(Layout.create(c, child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.performIncrementalMount(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.setHasTransientState(true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
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
