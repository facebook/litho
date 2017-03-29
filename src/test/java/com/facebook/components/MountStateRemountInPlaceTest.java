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

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.components.ComponentsLogger.EVENT_PREPARE_MOUNT;
import static com.facebook.components.ComponentsLogger.PARAM_MOVED_COUNT;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(ComponentsTestRunner.class)
public class MountStateRemountInPlaceTest {
  private ComponentContext mContext;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = mock(ComponentsLogger.class);
    mContext = new ComponentContext(RuntimeEnvironment.application, "tag", mComponentsLogger);
  }

  @Test
  public void testMountUnmountWithShouldUpdate() {
    final TestComponent firstComponent =
        TestDrawableComponent.create(mContext)
            .unique()
            .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(firstComponent)
                .build();
          }
        });

    assertTrue(firstComponent.wasOnMountCalled());
    assertTrue(firstComponent.wasOnBindCalled());
    assertFalse(firstComponent.wasOnUnmountCalled());

    final TestComponent secondComponent =
        TestDrawableComponent.create(mContext)
            .unique()
            .build();

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(secondComponent)
            .build();
      }
    });

    assertTrue(secondComponent.wasOnMountCalled());
    assertTrue(secondComponent.wasOnBindCalled());
    assertTrue(firstComponent.wasOnUnmountCalled());
  }

  @Test
  public void testMountUnmountWithNoShouldUpdate() {
    final TestComponent firstComponent =
        TestDrawableComponent.create(mContext)
            .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(firstComponent)
                .build();
          }
        });

    assertTrue(firstComponent.wasOnMountCalled());
    assertTrue(firstComponent.wasOnBindCalled());
    assertFalse(firstComponent.wasOnUnmountCalled());

    final TestComponent secondComponent =
        TestDrawableComponent.create(mContext)
            .build();

