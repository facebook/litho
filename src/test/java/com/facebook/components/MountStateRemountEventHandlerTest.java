/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class MountStateRemountEventHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testReuseClickListenerOnSameView() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentClickListener clickListener =
        MountState.getComponentClickListener(componentView);
    assertNotNull(clickListener);

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .clickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertTrue(clickListener == MountState.getComponentClickListener(componentView));
  }

  @Test
  public void testReuseLongClickListenerOnSameView() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentLongClickListener longClickListener =
        MountState.getComponentLongClickListener(componentView);
    assertNotNull(longClickListener);

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .longClickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertTrue(longClickListener == MountState.getComponentLongClickListener(componentView));
  }

  @Test
  public void testReuseTouchListenerOnSameView() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .touchHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentTouchListener touchListener =
        MountState.getComponentTouchListener(componentView);
    assertNotNull(touchListener);

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .touchHandler(c.newEventHandler(2))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertEquals(touchListener, MountState.getComponentTouchListener(componentView));
  }

  @Test
  public void testUnsetClickHandler() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNotNull(MountState.getComponentClickListener(componentView));

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentClickListener listener = MountState.getComponentClickListener(componentView);
    assertNotNull(listener);
    assertNull(listener.getEventHandler());
  }

  @Test
  public void testUnsetLongClickHandler() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNotNull(MountState.getComponentLongClickListener(componentView));

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentLongClickListener listener =
        MountState.getComponentLongClickListener(componentView);
    assertNotNull(listener);
    assertNull(listener.getEventHandler());
  }

  @Test
  public void testUnsetTouchHandler() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .touchHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    componentView.getComponent().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentTouchListener listener = MountState.getComponentTouchListener(componentView);
    assertNull(listener.getEventHandler());
  }

  @Test
  public void testSetClickHandler() {
    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });
