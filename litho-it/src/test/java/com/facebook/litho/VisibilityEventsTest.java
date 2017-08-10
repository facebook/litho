/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.ComponentTestHelper.measureAndLayout;
import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.ComponentTestHelper.unbindComponent;
import static com.facebook.litho.testing.TestViewComponent.create;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class VisibilityEventsTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testDetachWithReleasedTreeTriggersInvisibilityItems() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .invisibleHandler(invisibleEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            });

    lithoView.getComponentTree().mountComponent(new Rect(0, 0, 10, 10));
    lithoView.getComponentTree().release();

    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);
    unbindComponent(lithoView);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(invisibleEventHandler);
  }

  @Test
  public void testSetComponentWithDifferentKeyGeneratesVisibilityEvents() {
    final TestComponent<TestViewComponent> component1 = create(mContext).key("component1").build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(component1, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler1 = new EventHandler<>(component1, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, component1)
                            .visibleHandler(visibleEventHandler1)
                            .invisibleHandler(invisibleEventHandler1)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true);

    assertThat(component1.getLifecycle().getDispatchedEventHandlers())
        .containsOnly(visibleEventHandler1);

    final TestComponent<TestViewComponent> component2 = create(mContext).key("component2").build();
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(component2, 3);

    lithoView.setComponentTree(
        ComponentTree.create(
                mContext,
                new InlineLayoutSpec() {
                  @Override
                  protected ComponentLayout onCreateLayout(ComponentContext c) {
                    return Column.create(c)
                        .child(
                            Layout.create(c, component2)
                                .visibleHandler(visibleEventHandler2)
                                .widthPx(10)
                                .heightPx(10))
                        .build();
                  }
                })
            .build());

    measureAndLayout(lithoView);

    assertThat(component1.getLifecycle().getDispatchedEventHandlers())
        .contains(invisibleEventHandler1);
    assertThat(component2.getLifecycle().getDispatchedEventHandlers())
        .contains(visibleEventHandler2);
  }
}
