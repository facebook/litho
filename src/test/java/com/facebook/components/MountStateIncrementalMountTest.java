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
import android.graphics.Rect;
import android.view.ViewGroup;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestComponentContextWithView;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.components.ComponentsLogger.EVENT_MOUNT;
import static com.facebook.components.ComponentsLogger.PARAM_MOUNTED_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_UNMOUNTED_COUNT;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaFlexDirection.COLUMN;
import static com.facebook.yoga.YogaFlexDirection.ROW;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(ComponentsTestRunner.class)
public class MountStateIncrementalMountTest {
  private ComponentContext mContext;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = mock(ComponentsLogger.class);
    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountVerticalViewStackScrollUp() {
    final TestComponent child1 = TestViewComponent.create(mContext)
        .build();
    final TestComponent child2 = TestViewComponent.create(mContext)
        .build();
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2)
                        .widthPx(10)
                        .heightPx(10))
                .build();
          }
        });

    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect(0, -10, 10, -5));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 2);

    componentView.getComponent().mountComponent(new Rect(0, 0, 10, 5));
    assertTrue(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(0, 5, 10, 15));
    assertTrue(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(0, 15, 10, 25));
    assertFalse(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);

    componentView.getComponent().mountComponent(new Rect(0, 20, 10, 30));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);
  }

  @Test
  public void testIncrementalMountVerticalViewStackScrollDown() {
    final TestComponent child1 = TestViewComponent.create(mContext)
        .build();
    final TestComponent child2 = TestViewComponent.create(mContext)
        .build();
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2)
                        .widthPx(10)
                        .heightPx(10))
                .build();
          }
        });

    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect(0, 20, 10, 30));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 2);

    componentView.getComponent().mountComponent(new Rect(0, 15, 10, 25));
    assertFalse(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(0, 5, 10, 15));
    assertTrue(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(0, 0, 10, 10));
    assertTrue(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);

    componentView.getComponent().mountComponent(new Rect(0, -10, 10, -5));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a horizontal stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountHorizontalViewStack() {
    final TestComponent child1 = TestViewComponent.create(mContext)
        .build();
    final TestComponent child2 = TestViewComponent.create(mContext)
        .build();
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(ROW)
                .child(
                    Layout.create(c, child1)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2)
                        .widthPx(10)
                        .heightPx(10))
                .build();
          }
        });

    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect( -10, 0, -5, 10));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 2);

    componentView.getComponent().mountComponent(new Rect(0, 0, 5, 10));
    assertTrue(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(5, 0, 15, 10));
    assertTrue(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(15, 0, 25, 10));
    assertFalse(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);

    componentView.getComponent().mountComponent(new Rect(20, 0, 30, 10));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
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
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2)
                        .widthPx(10)
                        .heightPx(10))
                .build();
          }
        });

    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect(0, -10, 10, -5));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 2);

    componentView.getComponent().mountComponent(new Rect(0, 0, 10, 5));
    assertTrue(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(0, 5, 10, 15));
    assertTrue(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(0, 15, 10, 25));
    assertFalse(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);

    componentView.getComponent().mountComponent(new Rect(0, 20, 10, 30));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a view mount item in a nested hierarchy.
   */
  @Test
  public void testIncrementalMountNestedView() {
    final TestComponent child = TestViewComponent.create(mContext)
        .build();
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .wrapInView()
                .paddingPx(ALL, 20)
                .child(
                    Layout.create(c, child)
                        .widthPx(10)
                        .heightPx(10))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect(0, 0, 50, 20));
    assertFalse(child.isMounted());
    verifyLoggingAndResetLogger(0, 2);

    componentView.getComponent().mountComponent(new Rect(0, 0, 50, 40));
    assertTrue(child.isMounted());
    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect(30, 0, 50, 40));
    assertFalse(child.isMounted());
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
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1)
                        .widthPx(10)
                        .heightPx(10)
                        .clickHandler(c.newEventHandler(1))
                        .marginDip(YogaEdge.TOP, -10))
                .build();
          }
        });

    verifyLoggingAndResetLogger(2, 0);

    componentView.getComponent().mountComponent(new Rect(0, -10, 10, -5));
    verifyLoggingAndResetLogger(0, 0);
  }

  /**
   * Tests incremental mount behaviour of overlapping view mount items.
   */
  @Test
  public void testIncrementalMountOverlappingView() {
    final TestComponent child1 = TestViewComponent.create(mContext)
        .build();
    final TestComponent child2 = TestViewComponent.create(mContext)
        .build();
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1)
                        .positionType(ABSOLUTE)
                        .positionPx(TOP, 0)
                        .positionPx(LEFT, 0)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2)
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

    componentView.getComponent().mountComponent(new Rect(0, 0, 5, 5));
    assertTrue(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);

    componentView.getComponent().mountComponent(new Rect(5, 5, 10, 10));
    assertTrue(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(1, 0);

    componentView.getComponent().mountComponent(new Rect(10, 10, 15, 15));
    assertFalse(child1.isMounted());
    assertTrue(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);

    componentView.getComponent().mountComponent(new Rect(15, 15, 20, 20));
    assertFalse(child1.isMounted());
    assertFalse(child2.isMounted());
    verifyLoggingAndResetLogger(0, 1);
  }

  /**
   * Tests incremental mount behaviour of a child component that mounts incrementally.
   */
  @Test
  public void testChildViewCanIncrementallyMount() {
    final TestComponentView mountedView = new TestComponentView(mContext);

    final TestComponentContextWithView testComponentContext =
        new TestComponentContextWithView(mContext, mountedView);
    final TestComponent child2 = TestViewComponent.create(testComponentContext).build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        testComponentContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child2)
                        .widthPx(10)
                        .heightPx(20)
                        .marginPx(YogaEdge.ALL, 2))
                .build();
          }
        });

    for (int i = 0; i < 20; i++) {
      componentView.getComponent().mountComponent(new Rect(0, 0, 10, 3 + i));
      assertEquals(new Rect(0, 0, 8, 1 + i), mountedView.getPreviousIncrementalMountBounds());
    }
  }

  @Test
  public void testChildComponentViewIncrementallyMounted() {
    final TestComponentView mountedView = new TestComponentView(mContext);
    mountedView.layout(0, 0, 100, 100);

    final TestComponentContextWithView testComponentContext =
        new TestComponentContextWithView(mContext, mountedView);

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        TestViewComponent.create(testComponentContext));

    assertTrue(mountedView.getPreviousIncrementalMountBounds().isEmpty());

    componentView.getComponent().mountComponent(new Rect(-10, -10, 10, 10));
    assertEquals(new Rect(0, 0, 10, 10), mountedView.getPreviousIncrementalMountBounds());

    componentView.getComponent().mountComponent(new Rect(80, 80, 120, 120));
    assertEquals(new Rect(80, 80, 100, 100), mountedView.getPreviousIncrementalMountBounds());
  }

  @Test
  public void testChildViewGroupIncrementallyMounted() {
    final ViewGroup mountedView = mock(ViewGroup.class);
    when(mountedView.getLeft()).thenReturn(0);
    when(mountedView.getTop()).thenReturn(0);
    when(mountedView.getRight()).thenReturn(100);
    when(mountedView.getBottom()).thenReturn(100);
    when(mountedView.getChildCount()).thenReturn(3);

    final ComponentView childView1 = getMockComponentViewWithBounds(new Rect(5, 10, 20, 30));
    when(mountedView.getChildAt(0)).thenReturn(childView1);

    final ComponentView childView2 = getMockComponentViewWithBounds(new Rect(10, 10, 50, 60));
    when(mountedView.getChildAt(1)).thenReturn(childView2);

    final ComponentView childView3 = getMockComponentViewWithBounds(new Rect(30, 35, 50, 60));
    when(mountedView.getChildAt(2)).thenReturn(childView3);

    final TestComponentContextWithView testComponentContext =
        new TestComponentContextWithView(mContext, mountedView);

    final ComponentView componentView = ComponentTestHelper.mountComponent(
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
        }).when(childView1).performIncrementalMount(any(Rect.class));

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
        }).when(childView2).performIncrementalMount(any(Rect.class));

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
        }).when(childView3).performIncrementalMount(any(Rect.class));

    componentView.getComponent().mountComponent(new Rect(15, 15, 40, 40));

    verify(childView1).performIncrementalMount(any(Rect.class));
    verify(childView2).performIncrementalMount(any(Rect.class));
    verify(childView3).performIncrementalMount(any(Rect.class));
  }

  /**
   * Tests incremental mount behaviour of a vertical stack of components with a View mount type.
   */
  @Test
  public void testIncrementalMountDoesNotCauseMultipleUpdates() {
    final TestComponent child1 = TestViewComponent.create(mContext)
        .build();
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1)
