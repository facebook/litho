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

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewGroup;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestComponentContextWithView;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.litho.ComponentsLogger.EVENT_MOUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_MOUNTED_COUNT;
import static com.facebook.litho.ComponentsLogger.PARAM_UNMOUNTED_COUNT;
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
            return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1).flexShrink(0)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2).flexShrink(0)
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
            return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .flexDirection(COLUMN)
                .child(
                    Layout.create(c, child1).flexShrink(0)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2).flexShrink(0)
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
            return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .flexDirection(ROW)
                .child(
                    Layout.create(c, child1).flexShrink(0)
                        .widthPx(10)
                        .heightPx(10))
                .child(
                    Layout.create(c, child2).flexShrink(0)
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
