/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.measureAndLayout;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.helper.ComponentTestHelper.unbindComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import android.widget.FrameLayout;
import com.facebook.litho.testing.TestComponent;
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
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
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

    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testVisibleEventWithHeightRatio() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .visibleHeightRatio(0.4f)
                            .visibleHandler(visibleEventHandler)
                            .widthPx(10)
                            .heightPx(5)
                            .marginPx(YogaEdge.TOP, 5))
                    .build();
              }
            },
            true,
            10,
            10);

    content.getDispatchedEventHandlers().clear();

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 1), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 2), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 6), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 7), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testVisibleEventWithWidthRatio() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .visibleWidthRatio(0.4f)
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

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, 3, 10), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, 5, 10), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testVisibleEventWithHeightAndWidthRatio() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .visibleWidthRatio(0.4f)
                            .visibleHeightRatio(0.4f)
                            .visibleHandler(visibleEventHandler)
                            .widthPx(10)
                            .heightPx(5)
                            .marginPx(YogaEdge.TOP, 5))
                    .build();
              }
            },
            true,
            10,
            10);

    content.getDispatchedEventHandlers().clear();

    // Neither width or height are in visible range
    lithoView.performIncrementalMount(new Rect(LEFT, 0, 3, 6), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    // Width but not height are in visible range
    lithoView.performIncrementalMount(new Rect(LEFT, 0, 5, 6), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    // Height but not width are in visible range
    lithoView.performIncrementalMount(new Rect(LEFT, 0, 3, 8), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    // Height and width are both in visible range
    lithoView.performIncrementalMount(new Rect(LEFT, 0, 5, 8), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testFocusedOccupiesHalfViewport() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .focusedHandler(focusedEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true,
            10,
            10);

    content.getDispatchedEventHandlers().clear();

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(focusedEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler);
  }

  @Test
  public void testFocusedOccupiesLessThanHalfViewport() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .focusedHandler(focusedEventHandler)
                            .widthPx(10)
                            .heightPx(3))
                    .build();
              }
            },
            true,
            10,
            10);

    content.getDispatchedEventHandlers().clear();

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 2), true);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(focusedEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler);
  }

  @Test
  public void testMultipleFocusAndUnfocusEvents() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedHandler = new EventHandler<>(content, 2);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = new EventHandler<>(content, 3);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
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
    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 4, RIGHT, 10), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(focusedHandler);

    // Mount test view on the edge of the viewport (not focused)
    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 9, RIGHT, 14), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(unfocusedHandler);

    // Mount test view in the middle of the view port (focused)
    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 9), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(focusedHandler);

    // Mount test view on the edge of the viewport (not focused)
    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 1, RIGHT, 6), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(unfocusedHandler);
  }

  @Test
  public void testFullImpressionEvent() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FullImpressionVisibleEvent> fullImpressionVisibleEvent =
        new EventHandler<>(content, 2);

    mountComponent(
        mContext,
        mLithoView,
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    Wrapper.create(c)
                        .delegate(content)
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

    assertThat(content.getDispatchedEventHandlers())
        .contains(fullImpressionVisibleEvent);
  }

  @Test
  public void testInvisibleEvent() {
    final TestComponent content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
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

    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
  }

  @Test
  public void testVisibleAndInvisibleEvents() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
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

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 9), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 10, RIGHT, 15), true);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);
  }

  @Test
  public void testMultipleVisibleEvents() {
    final TestComponent content1 = create(mContext).build();
    final TestComponent content2 = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content2, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content1)
                            .visibleHandler(visibleEventHandler1)
                            .widthPx(10)
                            .heightPx(5))
                    .child(
                        Wrapper.create(c)
                            .delegate(content2)
                            .visibleHandler(visibleEventHandler2)
                            .widthPx(10)
                            .heightPx(5))
                    .build();
              }
            },
            true,
            10,
            10);

    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler2);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler2);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    lithoView.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 6), true);
    assertThat(content1.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
  }

  @Test
  public void testDetachWithReleasedTreeTriggersInvisibilityItems() {
    final TestComponent content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .invisibleHandler(invisibleEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), true);
    lithoView.release();

    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(invisibleEventHandler);
    unbindComponent(lithoView);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
  }

  @Test
  public void testSetComponentWithDifferentKeyGeneratesVisibilityEvents() {
    final TestComponent component1 = create(mContext).key("component1").build();
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
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(component1)
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

    assertThat(component1.getDispatchedEventHandlers())
        .contains(visibleEventHandler1);
    assertThat(component1.getDispatchedEventHandlers())
        .contains(focusedEventHandler1);

    final TestComponent component2 = create(mContext).key("component2").build();
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(component2, 3);

    lithoView.setComponentTree(
        ComponentTree.create(
                mContext,
                new InlineLayoutSpec() {
                  @Override
                  protected Component onCreateLayout(ComponentContext c) {
                    return Column.create(c)
                        .child(
                            Wrapper.create(c)
                                .delegate(component2)
                                .visibleHandler(visibleEventHandler2)
                                .widthPx(10)
                                .heightPx(10))
                        .build();
                  }
                })
            .build());

    measureAndLayout(lithoView);

    assertThat(component1.getDispatchedEventHandlers())
        .contains(invisibleEventHandler1);
    assertThat(component1.getDispatchedEventHandlers())
        .contains(unfocusedEventHandler1);
    assertThat(component2.getDispatchedEventHandlers())
        .contains(visibleEventHandler2);
  }

  @Test
  public void testTransientStateDoesNotTriggerVisibilityEvents() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Wrapper.create(c)
                            .delegate(content)
                            .visibleHandler(visibleEventHandler)
                            .widthPx(10)
                            .heightPx(10))
                    .build();
              }
            },
            true);

    lithoView.performIncrementalMount(new Rect(0, -10, 10, -5), true);
    content.getDispatchedEventHandlers().clear();

    lithoView.setHasTransientState(true);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    lithoView.setMountStateDirty();
    lithoView.performIncrementalMount(new Rect(0, -10, 10, -5), true);
    assertThat(content.getDispatchedEventHandlers())
        .doesNotContain(visibleEventHandler);

    lithoView.setHasTransientState(false);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testRemovingComponentTriggersInvisible() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);
    final Component wrappedContent =
        Wrapper.create(mContext)
            .delegate(content)
            .widthPx(10)
            .heightPx(5)
            .visibleHandler(visibleEventHandler)
            .invisibleHandler(invisibleEventHandler)
            .build();

    final LithoView lithoView =
        mountComponent(
            mContext,
            mLithoView,
            Column.create(mContext).child(wrappedContent).build(),
            true,
            10,
            10);

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);

    content.getDispatchedEventHandlers().clear();

    lithoView.setComponent(Column.create(mContext).build());
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    lithoView.setComponent(Column.create(mContext).child(wrappedContent).build());
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);
  }
}
