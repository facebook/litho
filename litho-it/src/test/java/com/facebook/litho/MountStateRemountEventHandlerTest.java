/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaAlign;

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
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentClickListener clickListener =
        MountState.getComponentClickListener(lithoView);
    assertNotNull(clickListener);

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .clickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertTrue(clickListener == MountState.getComponentClickListener(lithoView));
  }

  @Test
  public void testReuseLongClickListenerOnSameView() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentLongClickListener longClickListener =
        MountState.getComponentLongClickListener(lithoView);
    assertNotNull(longClickListener);

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .longClickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertTrue(longClickListener == MountState.getComponentLongClickListener(lithoView));
  }

  @Test
  public void testReuseTouchListenerOnSameView() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .touchHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentTouchListener touchListener =
        MountState.getComponentTouchListener(lithoView);
    assertNotNull(touchListener);

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .touchHandler(c.newEventHandler(2))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertEquals(touchListener, MountState.getComponentTouchListener(lithoView));
  }

  @Test
  public void testUnsetClickHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNotNull(MountState.getComponentClickListener(lithoView));

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentClickListener listener = MountState.getComponentClickListener(lithoView);
    assertNotNull(listener);
    assertNull(listener.getEventHandler());
  }

  @Test
  public void testUnsetLongClickHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNotNull(MountState.getComponentLongClickListener(lithoView));

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentLongClickListener listener =
        MountState.getComponentLongClickListener(lithoView);
    assertNotNull(listener);
    assertNull(listener.getEventHandler());
  }

  @Test
  public void testUnsetTouchHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .touchHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentTouchListener listener = MountState.getComponentTouchListener(lithoView);
    assertNull(listener.getEventHandler());
  }

  @Test
  public void testSetClickHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNull(MountState.getComponentClickListener(lithoView));

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .clickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentClickListener listener = MountState.getComponentClickListener(lithoView);
    assertNotNull(listener);
    assertNotNull(listener.getEventHandler());
  }

  @Test
  public void testSetLongClickHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNull(MountState.getComponentLongClickListener(lithoView));

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .longClickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentLongClickListener listener =
        MountState.getComponentLongClickListener(lithoView);
    assertNotNull(listener);
    assertNotNull(listener.getEventHandler());
  }

  @Test
  public void testSetTouchHandler() {
    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertNull(MountState.getComponentClickListener(lithoView));

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .touchHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentTouchListener listener = MountState.getComponentTouchListener(lithoView);
    assertNotNull(listener);
    assertNotNull(listener.getEventHandler());
  }
}
