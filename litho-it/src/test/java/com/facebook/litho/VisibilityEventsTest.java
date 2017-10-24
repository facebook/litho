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
import android.widget.FrameLayout;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class VisibilityEventsTest {
  private static final int LEFT = 0;
  private static final int RIGHT = 10;

  private ComponentContext mContext;
  private LithoView mLithoView;
  private FrameLayout mParent;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);

    mLithoView = new LithoView(mContext);
    mParent = new FrameLayout(mContext);
    mParent.setLeft(0);
    mParent.setTop(0);
    mParent.setRight(10);
    mParent.setBottom(10);
    mParent.addView(mLithoView);
  }

  @Test
  public void testVisibleEvent() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .visibleHandler(visibleEventHandler)
                            .widthPx(10)
                            .heightPx(5)
                            .marginPx(YogaEdge.TOP, 5))
                    .build();
              }
            },
            true,
            10,
            5);

    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testFocusedOccupiesHalfViewport() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .focusedHandler(focusedEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true,
            10,
            10);

    content.getLifecycle().getDispatchedEventHandlers().clear();

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(focusedEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(focusedEventHandler);
  }

  @Test
  public void testFocusedOccupiesLessThanHalfViewport() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .focusedHandler(focusedEventHandler)
                            .widthPx(10)
                            .heightPx(3))
                    .build();
              }
            },
            true,
            10,
            10);

    content.getLifecycle().getDispatchedEventHandlers().clear();

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 2), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(focusedEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(focusedEventHandler);
  }

  @Test
  public void testMultipleFocusAndUnfocusEvents() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedHandler = new EventHandler<>(content, 2);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = new EventHandler<>(content, 3);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .focusedHandler(focusedHandler)
                            .unfocusedHandler(unfocusedHandler)
                            .widthPx(10)
                            .heightPx(7)
                            .marginPx(YogaEdge.TOP, 3))
                    .build();
              }
            },
            true,
            100,
            100);

    lithoView.performIncrementalMount(new Rect(0, 0, 0, 0), true);

    // Mount test view in the middle of the view port (focused)
    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 4, RIGHT, 10), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).containsOnly(focusedHandler);

    // Mount test view on the edge of the viewport (not focused)
    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 9, RIGHT, 14), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).containsOnly(unfocusedHandler);

    // Mount test view in the middle of the view port (focused)
    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 9), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).containsOnly(focusedHandler);

    // Mount test view on the edge of the viewport (not focused)
    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 1, RIGHT, 6), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).containsOnly(unfocusedHandler);
  }

  @Test
  public void testFullImpressionEvent() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<FullImpressionVisibleEvent> fullImpressionVisibleEvent =
        new EventHandler<>(content, 2);

    mountComponent(
        mContext,
        mLithoView,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Layout.create(c, content)
                        .fullImpressionHandler(fullImpressionVisibleEvent)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build();
          }
        },
        true,
        10,
        10);

    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .contains(fullImpressionVisibleEvent);
  }

  @Test
  public void testInvisibleEvent() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .invisibleHandler(invisibleEventHandler)
                            .widthPx(10)
                            .heightPx(5)
                            .marginPx(YogaEdge.TOP, 5))
                    .build();
              }
            },
            true,
            10,
            10);

    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(invisibleEventHandler);
  }

  @Test
  public void testVisibleAndInvisibleEvents() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .visibleHandler(visibleEventHandler)
                            .invisibleHandler(invisibleEventHandler)
                            .widthPx(10)
                            .heightPx(5)
                            .marginPx(YogaEdge.TOP, 5))
                    .build();
              }
            },
            true,
            10,
            10);

    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);

    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 9), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);

    content.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 10, RIGHT, 15), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);
  }

  @Test
  public void testMultipleVisibleEvents() {
    final TestComponent<?> content1 = create(mContext).build();
    final TestComponent<?> content2 = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content1, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content1)
                            .visibleHandler(visibleEventHandler1)
                            .widthPx(10)
                            .heightPx(5))
                    .child(
                        Layout.create(c, content2)
                            .visibleHandler(visibleEventHandler2)
                            .widthPx(10)
                            .heightPx(5))
                    .build();
              }
            },
            true,
            10,
            10);

    assertThat(content1.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler2);

    content1.getLifecycle().getDispatchedEventHandlers().clear();
    content2.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler1);
    assertThat(content2.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler2);

    content1.getLifecycle().getDispatchedEventHandlers().clear();
    content2.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content1.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler2);

    content1.getLifecycle().getDispatchedEventHandlers().clear();
    content2.getLifecycle().getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 6), true);
    assertThat(content1.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler1);
    assertThat(content2.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler2);
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
            },
            true);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), true);
    lithoView.release();

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
    final EventHandler<FocusedVisibleEvent> focusedEventHandler1 =
        new EventHandler<>(component1, 3);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler1 =
        new EventHandler<>(component1, 4);

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
                            .focusedHandler(focusedEventHandler1)
                            .unfocusedHandler(unfocusedEventHandler1)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true);

    assertThat(component1.getLifecycle().getDispatchedEventHandlers())
        .contains(visibleEventHandler1);
    assertThat(component1.getLifecycle().getDispatchedEventHandlers())
        .contains(focusedEventHandler1);

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
    assertThat(component1.getLifecycle().getDispatchedEventHandlers())
        .contains(unfocusedEventHandler1);
    assertThat(component2.getLifecycle().getDispatchedEventHandlers())
        .contains(visibleEventHandler2);
  }

  @Test
  public void testTransientStateDoesNotTriggerVisibilityEvents() {
    final TestComponent<?> content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected ComponentLayout onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Layout.create(c, content)
                            .visibleHandler(visibleEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.performIncrementalMount(new Rect(0, -10, 10, -5), true);
    content.getLifecycle().getDispatchedEventHandlers().clear();

    lithoView.setHasTransientState(true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    lithoView.setMountStateDirty();
    lithoView.performIncrementalMount(new Rect(0, -10, 10, -5), true);
    assertThat(content.getLifecycle().getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    lithoView.setHasTransientState(false);
    assertThat(content.getLifecycle().getDispatchedEventHandlers()).contains(visibleEventHandler);
  }
}
