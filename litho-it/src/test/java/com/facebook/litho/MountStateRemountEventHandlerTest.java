/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.Column.create;
import static com.facebook.litho.MountState.getComponentClickListener;
import static com.facebook.litho.MountState.getComponentFocusChangeListener;
import static com.facebook.litho.MountState.getComponentLongClickListener;
import static com.facebook.litho.MountState.getComponentTouchListener;
import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateRemountEventHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testReuseClickListenerOnSameView() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentClickListener clickListener =
        getComponentClickListener(lithoView);
    assertThat(clickListener).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .clickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertThat(clickListener == getComponentClickListener(lithoView)).isTrue();
  }

  @Test
  public void testReuseLongClickListenerOnSameView() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentLongClickListener longClickListener =
        getComponentLongClickListener(lithoView);
    assertThat(longClickListener).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .longClickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertThat(longClickListener == getComponentLongClickListener(lithoView)).isTrue();
  }

  @Test
  public void testReuseFocusChangeListenerListenerOnSameView() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .focusChangeHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    ComponentFocusChangeListener focusChangeListener =
        getComponentFocusChangeListener(lithoView);
    assertThat(focusChangeListener).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .focusChangeHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertThat(focusChangeListener == getComponentFocusChangeListener(lithoView)).isTrue();
  }

  @Test
  public void testReuseTouchListenerOnSameView() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .touchHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    final ComponentTouchListener touchListener =
        getComponentTouchListener(lithoView);
    assertThat(touchListener).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .touchHandler(c.newEventHandler(2))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    assertThat(getComponentTouchListener(lithoView)).isEqualTo(touchListener);
  }

  @Test
  public void testUnsetClickHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .clickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentClickListener(lithoView)).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentClickListener listener = getComponentClickListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNull();
  }

  @Test
  public void testUnsetLongClickHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentLongClickListener(lithoView)).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentLongClickListener listener =
        getComponentLongClickListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNull();
  }

  @Test
  public void testUnsetFocusChangeHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .focusChangeHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentFocusChangeListener(lithoView)).isNotNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentFocusChangeListener listener = getComponentFocusChangeListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNull();
  }

  @Test
  public void testUnsetTouchHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .touchHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentTouchListener listener = getComponentTouchListener(lithoView);
    assertThat(listener.getEventHandler()).isNull();
  }

  @Test
  public void testSetClickHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentClickListener(lithoView)).isNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .clickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentClickListener listener = getComponentClickListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNotNull();
  }

  @Test
  public void testSetLongClickHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentLongClickListener(lithoView)).isNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .longClickHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentLongClickListener listener =
        getComponentLongClickListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNotNull();
  }

  @Test
  public void testSetFocusChangeHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentFocusChangeListener(lithoView)).isNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .focusChangeHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentFocusChangeListener listener = getComponentFocusChangeListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNotNull();
  }

  @Test
  public void testSetTouchHandler() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(getComponentTouchListener(lithoView)).isNull();

    lithoView.getComponentTree().setRoot(new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return create(c)
            .touchHandler(c.newEventHandler(1))
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    });

    final ComponentTouchListener listener = getComponentTouchListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNotNull();
  }
}
