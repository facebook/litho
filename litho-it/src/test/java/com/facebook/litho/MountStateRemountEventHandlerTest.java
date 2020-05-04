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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.MountState.getComponentClickListener;
import static com.facebook.litho.MountState.getComponentFocusChangeListener;
import static com.facebook.litho.MountState.getComponentLongClickListener;
import static com.facebook.litho.MountState.getComponentTouchListener;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateRemountEventHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testReuseClickListenerOnSameView() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .clickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    final ComponentClickListener clickListener = getComponentClickListener(lithoView);
    assertThat(clickListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    final ComponentLongClickListener longClickListener = getComponentLongClickListener(lithoView);
    assertThat(longClickListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .focusChangeHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    ComponentFocusChangeListener focusChangeListener = getComponentFocusChangeListener(lithoView);
    assertThat(focusChangeListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .touchHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    final ComponentTouchListener touchListener = getComponentTouchListener(lithoView);
    assertThat(touchListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .clickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentClickListener(lithoView)).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentLongClickListener(lithoView)).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    final ComponentLongClickListener listener = getComponentLongClickListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNull();
  }

  @Test
  public void testUnsetFocusChangeHandler() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .focusChangeHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentFocusChangeListener(lithoView)).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .touchHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentClickListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentLongClickListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    final ComponentLongClickListener listener = getComponentLongClickListener(lithoView);
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNotNull();
  }

  @Test
  public void testSetFocusChangeHandler() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentFocusChangeListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(getComponentTouchListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
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
