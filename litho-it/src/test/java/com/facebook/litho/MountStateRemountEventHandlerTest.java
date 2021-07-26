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

import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateRemountEventHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentClickListener clickListener = getComponentClickListener(lithoView.getChildAt(0));
    assertThat(clickListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .clickHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(clickListener == getComponentClickListener(lithoView.getChildAt(0))).isTrue();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentLongClickListener longClickListener =
        getComponentLongClickListener(lithoView.getChildAt(0));
    assertThat(longClickListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(longClickListener == getComponentLongClickListener(lithoView.getChildAt(0)))
        .isTrue();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    ComponentFocusChangeListener focusChangeListener =
        getComponentFocusChangeListener(lithoView.getChildAt(0));
    assertThat(focusChangeListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .focusChangeHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(focusChangeListener == getComponentFocusChangeListener(lithoView.getChildAt(0)))
        .isTrue();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentTouchListener touchListener = getComponentTouchListener(lithoView.getChildAt(0));
    assertThat(touchListener).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .touchHandler(c.newEventHandler(2))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(getComponentTouchListener(lithoView.getChildAt(0))).isEqualTo(touchListener);
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(getComponentClickListener(lithoView.getChildAt(0))).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    final ComponentClickListener listener = getComponentClickListener(lithoView);
    assertThat(listener).isNull();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(getComponentLongClickListener(lithoView.getChildAt(0))).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    final ComponentLongClickListener listener = getComponentLongClickListener(lithoView);
    assertThat(listener).isNull();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(getComponentFocusChangeListener(lithoView.getChildAt(0))).isNotNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    final ComponentFocusChangeListener listener = getComponentFocusChangeListener(lithoView);
    assertThat(listener).isNull();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    final ComponentTouchListener listener = getComponentTouchListener(lithoView);
    assertThat(listener).isNull();
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(getComponentClickListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .clickHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentClickListener listener = getComponentClickListener(lithoView.getChildAt(0));
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(getComponentLongClickListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentLongClickListener listener =
        getComponentLongClickListener(lithoView.getChildAt(0));
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(getComponentFocusChangeListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .focusChangeHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentFocusChangeListener listener =
        getComponentFocusChangeListener(lithoView.getChildAt(0));
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
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(getComponentTouchListener(lithoView)).isNull();

    lithoView
        .getComponentTree()
        .setRoot(
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .touchHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    final ComponentTouchListener listener = getComponentTouchListener(lithoView.getChildAt(0));
    assertThat(listener).isNotNull();
    assertThat(listener.getEventHandler()).isNotNull();
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
