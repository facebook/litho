/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.FrameworkLogEvents.EVENT_MOUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_UNMOUNTED_COUNT;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewGroup;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestComponentContextWithView;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateIncrementalMountTest {
  private ComponentContext mContext;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = spy(new TestComponentsLogger());
    when(mComponentsLogger.newEvent(any(int.class))).thenCallRealMethod();
    when(mComponentsLogger.newPerformanceEvent(any(int.class))).thenCallRealMethod();

    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountVerticalViewStackScrollUp() {
    final TestComponent child1 = create(mContext)
        .build();
    final TestComponent child2 = create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 2);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(0, 1);

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);
  }

  @Test
  public void testIncrementalMountVerticalViewStackScrollDown() {
    final TestComponent child1 = create(mContext)
        .build();
    final TestComponent child2 = create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 2);

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a horizontal stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountHorizontalViewStack() {
    final TestComponent child1 = create(mContext)
        .build();
    final TestComponent child2 = create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Row.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(-10, 0, -5, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 2);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 5, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(5, 0, 15, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(15, 0, 25, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(0, 1);

    lithoView.getComponentTree().mountComponent(new Rect(20, 0, 30, 10), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a Drawable mount type.
   */
  @Test
  public void testIncrementalMountVerticalDrawableStack() {
    final TestComponent child1 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent child2 = TestDrawableComponent.create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .child(Wrapper.create(c).delegate(child2).widthPx(10).heightPx(10))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 2);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 15, 10, 25), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(0, 1);

    lithoView.getComponentTree().mountComponent(new Rect(0, 20, 10, 30), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a view mount item in a nested hierarchy.
   */
  @Test
  public void testIncrementalMountNestedView() {
    final TestComponent child = create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .wrapInView()
                    .paddingPx(ALL, 20)
                    .child(Wrapper.create(c).delegate(child).widthPx(10).heightPx(10))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 50, 20), true);
    assertThat(child.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 2);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 50, 40), true);
    assertThat(child.isMounted()).isTrue();
    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(30, 0, 50, 40), true);
    assertThat(child.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Verify that we can cope with a negative padding on a component that is wrapped in a view
   * (since the bounds of the component will be larger than the bounds of the view).
   */
  @Test
  public void testIncrementalMountVerticalDrawableStackNegativeMargin() {
    final TestComponent child1 = TestDrawableComponent.create(mContext)
        .build();
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(child1)
                            .widthPx(10)
                            .heightPx(10)
                            .clickHandler(c.newEventHandler(1))
                            .marginDip(YogaEdge.TOP, -10))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(2, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    verifyLoggingAndResetLogger(0, 0);
  }

  /**
   * Tests incremental mount behaviour of overlapping view mount items.
   */
  @Test
  public void testIncrementalMountOverlappingView() {
    final TestComponent child1 = create(mContext)
        .build();
    final TestComponent child2 = create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(child1)
                            .positionType(ABSOLUTE)
                            .positionPx(TOP, 0)
                            .positionPx(LEFT, 0)
                            .widthPx(10)
                            .heightPx(10))
                    .child(
                        Wrapper.create(c)
                            .delegate(child2)
                            .positionType(ABSOLUTE)
                            .positionPx(TOP, 5)
                            .positionPx(LEFT, 5)
                            .widthPx(10)
                            .heightPx(10))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    verifyLoggingAndResetLogger(3, 0);

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 5, 5), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);

    lithoView.getComponentTree().mountComponent(new Rect(5, 5, 10, 10), true);
    assertThat(child1.isMounted()).isTrue();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(1, 0);

    lithoView.getComponentTree().mountComponent(new Rect(10, 10, 15, 15), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isTrue();
    verifyLoggingAndResetLogger(0, 1);

    lithoView.getComponentTree().mountComponent(new Rect(15, 15, 20, 20), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child2.isMounted()).isFalse();
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a child component that mounts incrementally.
   */
  @Test
  public void testChildViewCanIncrementallyMount() {
    final TestLithoView mountedView = new TestLithoView(mContext);

    final TestComponentContextWithView testComponentContext =
        new TestComponentContextWithView(mContext, mountedView);
    final TestComponent child2 = TestViewComponent.create(testComponentContext).build();

    final LithoView lithoView =
        ComponentTestHelper.mountComponent(
            testComponentContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(child2)
                            .widthPx(10)
                            .heightPx(20)
                            .marginPx(YogaEdge.ALL, 2))
                    .build();
              }
            });

    for (int i = 0; i < 20; i++) {
      lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 3 + i), true);
      assertThat(mountedView.getPreviousIncrementalMountBounds()).isEqualTo(new Rect(0, 0, 8, 1 + i));
    }
  }

  @Test
  public void testChildLithoViewIncrementallyMounted() {
    final TestLithoView mountedView = new TestLithoView(mContext);
    mountedView.layout(0, 0, 100, 100);

    final TestComponentContextWithView testComponentContext =
        new TestComponentContextWithView(mContext, mountedView);

    final LithoView lithoView = mountComponent(
        create(testComponentContext));

    assertThat(mountedView.getPreviousIncrementalMountBounds().isEmpty()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(-10, -10, 10, 10), true);
    assertThat(mountedView.getPreviousIncrementalMountBounds()).isEqualTo(new Rect(0, 0, 10, 10));

    lithoView.getComponentTree().mountComponent(new Rect(80, 80, 120, 120), true);
    assertThat(mountedView.getPreviousIncrementalMountBounds()).isEqualTo(new Rect(80, 80, 100, 100));
  }

  @Test
  public void testChildViewGroupIncrementallyMounted() {
    final ViewGroup mountedView = mock(ViewGroup.class);
    when(mountedView.getLeft()).thenReturn(0);
    when(mountedView.getTop()).thenReturn(0);
    when(mountedView.getRight()).thenReturn(100);
    when(mountedView.getBottom()).thenReturn(100);
    when(mountedView.getChildCount()).thenReturn(3);

    final LithoView childView1 = getMockLithoViewWithBounds(new Rect(5, 10, 20, 30));
    when(mountedView.getChildAt(0)).thenReturn(childView1);

    final LithoView childView2 = getMockLithoViewWithBounds(new Rect(10, 10, 50, 60));
    when(mountedView.getChildAt(1)).thenReturn(childView2);

    final LithoView childView3 = getMockLithoViewWithBounds(new Rect(30, 35, 50, 60));
    when(mountedView.getChildAt(2)).thenReturn(childView3);

    final TestComponentContextWithView testComponentContext =
        new TestComponentContextWithView(mContext, mountedView);

    final LithoView lithoView = ComponentTestHelper.mountComponent(
        TestViewComponent.create(testComponentContext));

    // Can't verify directly as the object will have changed by the time we get the chance to
    // verify it.
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(10, 5, 15, 20))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView1)
        .performIncrementalMount(any(Rect.class), eq(true));

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(5, 5, 30, 30))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView2)
        .performIncrementalMount(any(Rect.class), eq(true));

    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Rect rect = (Rect) invocation.getArguments()[0];
                if (!rect.equals(new Rect(0, 0, 10, 5))) {
                  fail();
                }
                return null;
              }
            })
        .when(childView3)
        .performIncrementalMount(any(Rect.class), eq(true));

    lithoView.getComponentTree().mountComponent(new Rect(15, 15, 40, 40), true);

    verify(childView1).performIncrementalMount(any(Rect.class), eq(true));
    verify(childView2).performIncrementalMount(any(Rect.class), eq(true));
    verify(childView3).performIncrementalMount(any(Rect.class), eq(true));
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountDoesNotCauseMultipleUpdates() {
    final TestComponent child1 = create(mContext)
        .build();
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(Wrapper.create(c).delegate(child1).widthPx(10).heightPx(10))
                    .build();
              }
            });

    lithoView.getComponentTree().mountComponent(new Rect(0, -10, 10, -5), true);
    assertThat(child1.isMounted()).isFalse();
    assertThat(child1.wasOnUnbindCalled()).isTrue();
    assertThat(child1.wasOnUnmountCalled()).isTrue();

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 5), true);
    assertThat(child1.isMounted()).isTrue();

    child1.resetInteractions();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(child1.isMounted()).isTrue();

    assertThat(child1.wasOnBindCalled()).isFalse();
    assertThat(child1.wasOnMountCalled()).isFalse();
    assertThat(child1.wasOnUnbindCalled()).isFalse();
    assertThat(child1.wasOnUnmountCalled()).isFalse();
  }

  private void verifyLoggingAndResetLogger(int mountedCount, int unmountedCount) {
    final LogEvent event = mComponentsLogger.newPerformanceEvent(EVENT_MOUNT);
    event.addParam(PARAM_MOUNTED_COUNT, String.valueOf(mountedCount));
    event.addParam(PARAM_UNMOUNTED_COUNT, String.valueOf(unmountedCount));

    verify(mComponentsLogger).log(eq(event));
    reset(mComponentsLogger);
  }

  private static LithoView getMockLithoViewWithBounds(Rect bounds) {
    final LithoView lithoView = mock(LithoView.class);
    when(lithoView.getLeft()).thenReturn(bounds.left);
    when(lithoView.getTop()).thenReturn(bounds.top);
    when(lithoView.getRight()).thenReturn(bounds.right);
    when(lithoView.getBottom()).thenReturn(bounds.bottom);
    when(lithoView.getWidth()).thenReturn(bounds.width());
    when(lithoView.getHeight()).thenReturn(bounds.height());
    when(lithoView.isIncrementalMountEnabled()).thenReturn(true);

    return lithoView;
  }

  private static class TestLithoView extends LithoView {
    private final Rect mPreviousIncrementalMountBounds = new Rect();

    public TestLithoView(Context context) {
      super(context);
    }

    @Override
    public void performIncrementalMount(Rect visibleRect, boolean processVisibilityOutputs) {
      mPreviousIncrementalMountBounds.set(visibleRect);
    }

    private Rect getPreviousIncrementalMountBounds() {
      return mPreviousIncrementalMountBounds;
    }

    @Override
    public boolean isIncrementalMountEnabled() {
      return true;
    }
  }
}
