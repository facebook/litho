/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    mLithoView = new TestLithoView(mContext.getAndroidContext());
    mComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
          }
        };

    mComponentTree = createComponentTree(false, false, 100, 100);
  }

  @Test
  public void testIncrementalMountTriggeredAfterUnmountAllWithSameDimensions() {
    mComponentTree = createComponentTree(true, true, 100, 100);

    final int WIDTH = 50;
    final int HEIGHT = 50;

    mLithoView.setMeasured(WIDTH, HEIGHT);
    mLithoView.setComponentTree(mComponentTree);
    mLithoView.onAttachedToWindow();

    mLithoView.layout(0, 0, mLithoView.getMeasuredWidth(), mLithoView.getMeasuredHeight());
    verify(mComponentTree).incrementalMountComponent();

    mLithoView.unmountAllItems();

    mLithoView.performLayout(false, 0, 0, WIDTH, HEIGHT);
    verify(mComponentTree, times(2)).incrementalMountComponent();
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
    ComponentTree ct = createComponentTree(false, false, 100, 100);

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

    ComponentTree newComponentTree = createComponentTree(false, false, 100, 100);

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
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.notifyVisibleBoundsChanged(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.setHasTransientState(true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
  }

  @Test
  public void testUnmountAllCausesRemountOfComponentTreeOnLayout() {
    final TestComponent child1 = TestViewComponent.create(mContext).build();
    final TestComponent child2 = TestDrawableComponent.create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.performLayout(false, 0, 0, 100, 100);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();

    lithoView.unmountAllItems();
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.performLayout(false, 0, 0, 100, 100);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
  }

  @Test
  public void testPerformLayoutWithDifferentBoundsMountsEverything() {
    final TestComponent child1 = TestViewComponent.create(mContext).build();
    final TestComponent child2 = TestDrawableComponent.create(mContext).build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.notifyVisibleBoundsChanged(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();

    lithoView.performLayout(false, 0, 0, 200, 200);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
  }

  private ComponentTree createComponentTree(
      boolean useSpy, boolean incMountEnabled, int width, int height) {
    ComponentTree componentTree =
        ComponentTree.create(mContext, mComponent)
            .incrementalMount(incMountEnabled)
            .layoutDiffing(false)
            .build();
    componentTree.setSizeSpec(
        SizeSpec.makeSizeSpec(width, EXACTLY), SizeSpec.makeSizeSpec(height, EXACTLY));

    return useSpy ? spy(componentTree) : componentTree;
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
