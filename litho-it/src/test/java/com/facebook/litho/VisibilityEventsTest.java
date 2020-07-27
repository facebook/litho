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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.TestViewComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.measureAndLayout;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.helper.ComponentTestHelper.unbindComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.widget.LayoutSpecVisibilityEventTester;
import com.facebook.rendercore.HostListenerExtension;
import com.facebook.rendercore.visibility.VisibilityItem;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class VisibilityEventsTest {
  private static final int LEFT = 0;
  private static final int RIGHT = 10;

  private ComponentContext mContext;
  private LithoView mLithoView;
  private FrameLayout mParent;
  private boolean configVisExtension;
  private boolean configIncMountExtension;
  final boolean mUseMountDelegateTarget;
  final boolean mDelegateToRenderCore;
  final boolean mUseVisibilityExtensionInMountState;
  final boolean mUseIncrementalMountExtensionInMountState;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @ParameterizedRobolectricTestRunner.Parameters(
      name =
          "useMountDelegateTarget={0}, delegateToRenderCore={1}, useVisibilityExtensionInMountState={2}, useIncrementalMountExtensionInMountState={3}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false, false, false, false},
          {true, false, false, false},
          {false, false, true, false},
          {true, false, true, false},
          {false, false, true, true},
          {true, true, false, false}
        });
  }

  public VisibilityEventsTest(
      boolean useMountDelegateTarget,
      boolean delegateToRenderCore,
      boolean useVisibilityExtensionInMountState,
      boolean useIncrementalMountExtensionInMountState) {
    mUseMountDelegateTarget = useMountDelegateTarget;
    mDelegateToRenderCore = delegateToRenderCore;
    mUseVisibilityExtensionInMountState = useVisibilityExtensionInMountState;
    mUseIncrementalMountExtensionInMountState = useIncrementalMountExtensionInMountState;
  }

  @Before
  public void setup() {
    configVisExtension = ComponentsConfiguration.useVisibilityExtension;
    configIncMountExtension = ComponentsConfiguration.useIncrementalMountExtension;

    ComponentsConfiguration.useVisibilityExtension = mUseVisibilityExtensionInMountState;
    ComponentsConfiguration.useIncrementalMountExtension =
        mUseIncrementalMountExtensionInMountState;

    mContext = mLithoViewRule.getContext();
    mLithoView = new LithoView(mContext, mUseMountDelegateTarget, mDelegateToRenderCore);
    mLithoViewRule.useLithoView(mLithoView);

    mParent = new FrameLayout(mContext.getAndroidContext());
    mParent.setLeft(0);
    mParent.setTop(0);
    mParent.setRight(10);
    mParent.setBottom(10);
    mParent.addView(mLithoView);
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.useVisibilityExtension = configVisExtension;
    ComponentsConfiguration.useIncrementalMountExtension = configIncMountExtension;
  }

  @Test
  public void testVisibleEvent() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 10), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testVisibleEventWithHeightRatio() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHeightRatio(0.4f)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    content.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 1), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 2), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 6), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 7), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testVisibleEventWithWidthRatio() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleWidthRatio(0.4f)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, 3, 10), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, 5, 10), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testVisibleEventWithHeightAndWidthRatio() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleWidthRatio(0.4f)
                    .visibleHeightRatio(0.4f)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    content.getDispatchedEventHandlers().clear();

    // Neither width or height are in visible range
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, 3, 6), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    // Width but not height are in visible range
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, 5, 6), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    // Height but not width are in visible range
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, 3, 8), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    // Height and width are both in visible range
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, 5, 8), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void testFocusedOccupiesHalfViewport() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .focusedHandler(focusedEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    content.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler);
  }

  @Test
  public void testFocusedOccupiesLessThanHalfViewport() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .focusedHandler(focusedEventHandler)
                    .widthPx(10)
                    .heightPx(3))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    content.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 2), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler);
  }

  @Test
  public void testMultipleFocusAndUnfocusEvents() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FocusedVisibleEvent> focusedHandler = new EventHandler<>(content, 2);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = new EventHandler<>(content, 3);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .focusedHandler(focusedHandler)
                    .unfocusedHandler(unfocusedHandler)
                    .widthPx(10)
                    .heightPx(7)
                    .marginPx(YogaEdge.TOP, 3))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    mLithoView.notifyVisibleBoundsChanged(new Rect(0, 0, 0, 0), true);

    // Mount test view in the middle of the view port (focused)
    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 4, RIGHT, 10), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(focusedHandler);

    // Mount test view on the edge of the viewport (not focused)
    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 9, RIGHT, 14), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(unfocusedHandler);

    // Mount test view in the middle of the view port (focused)
    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 9), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(focusedHandler);

    // Mount test view on the edge of the viewport (not focused)
    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 1, RIGHT, 6), true);
    assertThat(content.getDispatchedEventHandlers()).containsOnly(unfocusedHandler);
  }

  @Test
  public void testFullImpressionEvent() {
    final TestComponent content = create(mContext).build();
    final EventHandler<FullImpressionVisibleEvent> fullImpressionVisibleEvent =
        new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .fullImpressionHandler(fullImpressionVisibleEvent)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).contains(fullImpressionVisibleEvent);
  }

  @Test
  public void testVisibility1fTop() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEvent = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEvent)
                    .visibleHeightRatio(1f)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEvent);
  }

  @Test
  public void testVisibility1fBottom() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEvent = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEvent)
                    .visibleHeightRatio(1f)
                    .widthPx(10)
                    .heightPx(5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEvent);
  }

  @Test
  public void testInvisibleEvent() {
    final TestComponent content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
  }

  @Test
  public void testVisibleRectChangedEventItemVisible() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
        new EventHandler<>(content, 3);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    VisibilityChangedEvent visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);
    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(10);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(10);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(100f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(100f);

    content.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);

    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(4);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(10);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(40f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(100f);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);

    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(5);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(10);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(50f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(100f);
  }

  @Test
  public void testVisibleRectChangedEventItemNotVisible() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
        new EventHandler<>(content, 3);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    content.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    VisibilityChangedEvent visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);

    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(0);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(0);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(0.0f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(0.0f);
  }

  @Test
  public void testVisibleRectChangedEventLargeView() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
        new EventHandler<>(content, 3);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    VisibilityChangedEvent visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);
    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(10);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(10);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(100f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(100f);

    content.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 4), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);

    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(4);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(10);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(40f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(100f);

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    visibilityChangedEvent =
        (VisibilityChangedEvent) content.getEventState(visibilityChangedHandler);

    assertThat(visibilityChangedEvent.visibleHeight).isEqualTo(5);
    assertThat(visibilityChangedEvent.visibleWidth).isEqualTo(10);
    assertThat(visibilityChangedEvent.percentVisibleHeight).isEqualTo(50f);
    assertThat(visibilityChangedEvent.percentVisibleWidth).isEqualTo(100f);
  }

  @Test
  public void testVisibleAndInvisibleEvents() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 5), true);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 9), true);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 10, RIGHT, 15), true);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);
  }

  @Test
  public void testMultipleVisibleEvents() {
    final TestComponent content1 = create(mContext).build();
    final TestComponent content2 = create(mContext).build();
    final TestComponent content3 = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<VisibleEvent> visibleEventHandler3 = new EventHandler<>(content3, 3);
    final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
        new EventHandler<>(content3, 4);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content1)
                    .visibleHandler(visibleEventHandler1)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .delegate(content2)
                    .visibleHandler(visibleEventHandler2)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .delegate(content3)
                    .visibleHandler(visibleEventHandler3)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibilityChangedHandler);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 11), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibilityChangedHandler);
  }

  @Test
  public void testMultipleVisibleAndInvisibleEvents() {
    final TestComponent content1 = create(mContext).build();
    final TestComponent content2 = create(mContext).build();
    final TestComponent content3 = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<VisibleEvent> visibleEventHandler3 = new EventHandler<>(content3, 3);
    final EventHandler<InvisibleEvent> invisibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<InvisibleEvent> invisibleEventHandler3 = new EventHandler<>(content3, 3);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .delegate(content2)
                    .visibleHandler(visibleEventHandler2)
                    .invisibleHandler(invisibleEventHandler2)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .delegate(content3)
                    .visibleHandler(visibleEventHandler3)
                    .invisibleHandler(invisibleEventHandler3)
                    .widthPx(10)
                    .heightPx(5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(15, EXACTLY), makeSizeSpec(15, EXACTLY))
        .measure()
        .layout();

    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 15), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).contains(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 11), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 5, RIGHT, 11), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).contains(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);
  }

  @Test
  public void testSkipFullyVisible() {
    final TestComponent content1 = create(mContext).key("tc1").build();
    final TestComponent content2 = create(mContext).key("tc2").build();
    final TestComponent content3 = create(mContext).key("tc3").build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<VisibleEvent> visibleEventHandler3 = new EventHandler<>(content3, 3);
    final EventHandler<InvisibleEvent> invisibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<InvisibleEvent> invisibleEventHandler3 = new EventHandler<>(content3, 3);
    final Component root =
        Column.create(mContext)
            .key("root")
            .child(
                Wrapper.create(mContext)
                    .key("child1")
                    .delegate(content1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .key("child2")
                    .delegate(content2)
                    .visibleHandler(visibleEventHandler2)
                    .invisibleHandler(invisibleEventHandler2)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .key("child3")
                    .delegate(content3)
                    .visibleHandler(visibleEventHandler3)
                    .invisibleHandler(invisibleEventHandler3)
                    .widthPx(10)
                    .heightPx(5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(15, EXACTLY), makeSizeSpec(15, EXACTLY))
        .measure()
        .layout();

    Map<String, VisibilityItem> visibilityItemMap = getVisibilityIdToItemMap();
    for (String key : visibilityItemMap.keySet()) {
      VisibilityItem item = visibilityItemMap.get(key);
      assertThat(item.wasFullyVisible()).isTrue();
    }

    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 15), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    visibilityItemMap = getVisibilityIdToItemMap();
    assertThat(visibilityItemMap.size()).isEqualTo(3);
    for (String key : visibilityItemMap.keySet()) {
      VisibilityItem item = visibilityItemMap.get(key);
      assertThat(item.wasFullyVisible()).isTrue();
    }

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 12), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    visibilityItemMap = getVisibilityIdToItemMap();
    assertThat(visibilityItemMap.size()).isEqualTo(3);

    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child1", "tc1"))
                .wasFullyVisible())
        .isFalse();
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child2", "tc2"))
                .wasFullyVisible())
        .isTrue();
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child3", "tc3"))
                .wasFullyVisible())
        .isFalse();

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).contains(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(invisibleEventHandler3);

    visibilityItemMap = getVisibilityIdToItemMap();
    assertThat(visibilityItemMap.size()).isEqualTo(0);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 12), true);
    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    visibilityItemMap = getVisibilityIdToItemMap();
    assertThat(visibilityItemMap.size()).isEqualTo(3);
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child1", "tc1"))
                .wasFullyVisible())
        .isFalse();
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child2", "tc2"))
                .wasFullyVisible())
        .isTrue();
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child3", "tc3"))
                .wasFullyVisible())
        .isFalse();

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 15), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler3);

    visibilityItemMap = getVisibilityIdToItemMap();
    assertThat(visibilityItemMap.size()).isEqualTo(3);
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child1", "tc1"))
                .wasFullyVisible())
        .isTrue();
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child2", "tc2"))
                .wasFullyVisible())
        .isTrue();
    assertThat(
            visibilityItemMap
                .get(ComponentKeyUtils.getKeyWithSeparator("root", "child3", "tc3"))
                .wasFullyVisible())
        .isTrue();
  }

  @Test
  public void testDispatchFocusedHandler() {
    final TestComponent content1 = create(mContext).key("tc1").build();
    final TestComponent content2 = create(mContext).key("tc2").build();
    final TestComponent content3 = create(mContext).key("tc3").build();

    final EventHandler<FocusedVisibleEvent> focusedEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler3 = new EventHandler<>(content3, 3);

    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler1 =
        new EventHandler<>(content1, 4);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler2 =
        new EventHandler<>(content2, 5);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler3 =
        new EventHandler<>(content3, 6);

    final Component root =
        Column.create(mContext)
            .key("root")
            .child(
                Wrapper.create(mContext)
                    .key("child1")
                    .delegate(content1)
                    .focusedHandler(focusedEventHandler1)
                    .unfocusedHandler(unfocusedEventHandler1)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .key("child2")
                    .delegate(content2)
                    .focusedHandler(focusedEventHandler2)
                    .unfocusedHandler(unfocusedEventHandler2)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .key("child3")
                    .delegate(content3)
                    .focusedHandler(focusedEventHandler3)
                    .unfocusedHandler(unfocusedEventHandler3)
                    .widthPx(10)
                    .heightPx(5))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(15, EXACTLY))
        .measure()
        .layout();

    Map<String, VisibilityItem> visibilityItemLongSparseArray = getVisibilityIdToItemMap();
    for (String key : visibilityItemLongSparseArray.keySet()) {
      VisibilityItem item = visibilityItemLongSparseArray.get(key);
      assertThat(item.wasFullyVisible());
    }

    assertThat(content1.getDispatchedEventHandlers()).contains(focusedEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(focusedEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(focusedEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 4, RIGHT, 15), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).contains(unfocusedEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler3);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 15), true);
    assertThat(content1.getDispatchedEventHandlers()).contains(focusedEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(focusedEventHandler3);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(unfocusedEventHandler3);
  }

  @Test
  public void testDetachWithReleasedTreeTriggersInvisibilityItems() {
    final TestComponent content = create(mContext).build();
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 10), true);
    mLithoView.release();

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
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(component1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .focusedHandler(focusedEventHandler1)
                    .unfocusedHandler(unfocusedEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(component1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(component1.getDispatchedEventHandlers()).contains(focusedEventHandler1);

    final TestComponent component2 = create(mContext).key("component2").build();
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(component2, 3);

    final Component newRoot =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(component2)
                    .visibleHandler(visibleEventHandler2)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule.setRoot(newRoot);

    measureAndLayout(mLithoView);

    assertThat(component1.getDispatchedEventHandlers()).contains(invisibleEventHandler1);
    assertThat(component1.getDispatchedEventHandlers()).contains(unfocusedEventHandler1);
    assertThat(component2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
  }

  @Test
  public void testSetDifferentComponentTreeWithSameKeysStillCallsInvisibleAndVisibleEvents() {
    final TestComponent firstComponent = create(mContext).build();
    final TestComponent secondComponent = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(firstComponent, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler1 =
        new EventHandler<>(firstComponent, 2);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(secondComponent, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler2 =
        new EventHandler<>(secondComponent, 2);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(firstComponent)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();
    assertThat(firstComponent.getDispatchedEventHandlers()).containsExactly(visibleEventHandler1);

    firstComponent.getDispatchedEventHandlers().clear();

    final Component newRoot =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(secondComponent)
                    .visibleHandler(visibleEventHandler2)
                    .invisibleHandler(invisibleEventHandler2)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(newRoot)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(firstComponent.getDispatchedEventHandlers()).containsExactly(invisibleEventHandler1);
    assertThat(secondComponent.getDispatchedEventHandlers()).containsExactly(visibleEventHandler2);
  }

  @Test
  public void testSetComponentTreeToNullDispatchesInvisibilityEvents() {
    final TestComponent component = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(component, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(component, 2);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(component)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(component.getDispatchedEventHandlers()).containsExactly(visibleEventHandler);

    component.getDispatchedEventHandlers().clear();

    mLithoViewRule.useComponentTree(null);

    assertThat(component.getDispatchedEventHandlers()).containsExactly(invisibleEventHandler);
  }

  @Test
  public void testTransientStateDoesNotTriggerVisibilityEvents() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();
    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    mLithoView.notifyVisibleBoundsChanged(new Rect(0, -10, 10, -5), true);
    content.getDispatchedEventHandlers().clear();

    mLithoView.setHasTransientState(true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.setMountStateDirty();
    mLithoView.notifyVisibleBoundsChanged(new Rect(0, -10, 10, -5), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.setHasTransientState(false);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void
      visibilityOutputs_setTransientStateFalse_parentInTransientState_processVisibilityOutputs() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    final View view = (View) mLithoView.getParent();
    view.setHasTransientState(true);

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    mLithoView.notifyVisibleBoundsChanged(new Rect(0, -10, 10, -5), true);
    content.getDispatchedEventHandlers().clear();

    mLithoView.setHasTransientState(true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.setMountStateDirty();
    mLithoView.notifyVisibleBoundsChanged(new Rect(0, -10, 10, -5), true);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.setHasTransientState(false);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    view.setHasTransientState(false);
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

    final Component root = Column.create(mContext).child(wrappedContent).build();
    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);

    content.getDispatchedEventHandlers().clear();

    mLithoView.setComponent(Column.create(mContext).build());
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    content.getDispatchedEventHandlers().clear();
    mLithoView.setComponent(Column.create(mContext).child(wrappedContent).build());
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);
  }

  @Test
  public void testMultipleVisibilityEventsOnSameNode() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content, 2);
    final EventHandler<VisibleEvent> visibleEventHandler3 = new EventHandler<>(content, 3);
    final EventHandler<InvisibleEvent> invisibleEventHandler1 = new EventHandler<>(content, 4);
    final EventHandler<InvisibleEvent> invisibleEventHandler2 = new EventHandler<>(content, 5);
    final EventHandler<InvisibleEvent> invisibleEventHandler3 = new EventHandler<>(content, 6);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler1 = new EventHandler<>(content, 7);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler2 = new EventHandler<>(content, 8);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler3 = new EventHandler<>(content, 9);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler1 =
        new EventHandler<>(content, 10);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler2 =
        new EventHandler<>(content, 11);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler3 =
        new EventHandler<>(content, 12);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionVisibleEventHandler1 =
        new EventHandler<>(content, 13);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionVisibleEventHandler2 =
        new EventHandler<>(content, 14);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionVisibleEventHandler3 =
        new EventHandler<>(content, 15);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(
                        Wrapper.create(mContext)
                            .delegate(
                                Wrapper.create(mContext)
                                    .delegate(content)
                                    .visibleHandler(visibleEventHandler1)
                                    .invisibleHandler(invisibleEventHandler1)
                                    .focusedHandler(focusedEventHandler1)
                                    .unfocusedHandler(unfocusedEventHandler1)
                                    .fullImpressionHandler(fullImpressionVisibleEventHandler1)
                                    .widthPx(10)
                                    .heightPx(5)
                                    .build())
                            .visibleHandler(visibleEventHandler2)
                            .invisibleHandler(invisibleEventHandler2)
                            .focusedHandler(focusedEventHandler2)
                            .unfocusedHandler(unfocusedEventHandler2)
                            .fullImpressionHandler(fullImpressionVisibleEventHandler2)
                            .build())
                    .visibleHandler(visibleEventHandler3)
                    .invisibleHandler(invisibleEventHandler3)
                    .focusedHandler(focusedEventHandler3)
                    .unfocusedHandler(unfocusedEventHandler3)
                    .fullImpressionHandler(fullImpressionVisibleEventHandler3))
            .build();

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler1);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler2);
    assertThat(content.getDispatchedEventHandlers()).contains(focusedEventHandler3);
    assertThat(content.getDispatchedEventHandlers()).contains(fullImpressionVisibleEventHandler1);
    assertThat(content.getDispatchedEventHandlers()).contains(fullImpressionVisibleEventHandler2);
    assertThat(content.getDispatchedEventHandlers()).contains(fullImpressionVisibleEventHandler3);

    content.getDispatchedEventHandlers().clear();

    unbindComponent(mLithoView);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler1);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler2);
    assertThat(content.getDispatchedEventHandlers()).contains(invisibleEventHandler3);
    assertThat(content.getDispatchedEventHandlers()).contains(unfocusedEventHandler1);
    assertThat(content.getDispatchedEventHandlers()).contains(unfocusedEventHandler2);
    assertThat(content.getDispatchedEventHandlers()).contains(unfocusedEventHandler3);
  }

  @Test
  public void testSetVisibilityHint() {
    final TestComponent component = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(component, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(component, 2);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(component, 3);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler =
        new EventHandler<>(component, 4);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler =
        new EventHandler<>(component, 5);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(component)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .focusedHandler(focusedEventHandler)
                    .unfocusedHandler(unfocusedEventHandler)
                    .fullImpressionHandler(fullImpressionHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(component.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(focusedEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(fullImpressionHandler);

    component.getDispatchedEventHandlers().clear();
    mLithoView.setVisibilityHint(false);

    assertThat(component.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(unfocusedEventHandler);

    component.getDispatchedEventHandlers().clear();
    mLithoView.setVisibilityHint(true);

    assertThat(component.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(focusedEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(fullImpressionHandler);
  }

  @Test
  public void testSetVisibilityHintRecursive() {
    // TODO(festevezga, T68365308) - replace with SimpleMountSpecTesterSpec
    final TestComponent testComponentInner = TestDrawableComponent.create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandlerInner =
        new EventHandler<>(testComponentInner, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandlerInner =
        new EventHandler<>(testComponentInner, 2);

    final Component mountedTestComponentInner =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(testComponentInner)
                    .visibleHandler(visibleEventHandlerInner)
                    .invisibleHandler(invisibleEventHandlerInner)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).build())
        .setRoot(mountedTestComponentInner)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    final LithoView child = mountComponent(mContext, mountedTestComponentInner, true, true);

    assertThat(testComponentInner.getDispatchedEventHandlers().size()).isEqualTo(1);
    assertThat(testComponentInner.getDispatchedEventHandlers().contains(visibleEventHandlerInner));
    testComponentInner.getDispatchedEventHandlers().clear();

    final ViewGroupWithLithoViewChildren viewGroup =
        new ViewGroupWithLithoViewChildren(mContext.getAndroidContext());

    final ViewGroup parent = (ViewGroup) child.getParent();
    parent.removeView(child);
    viewGroup.addView(child);

    final LithoView parentView =
        mountComponent(
            mContext, TestViewComponent.create(mContext).testView(viewGroup).build(), true, true);

    parentView.setVisibilityHint(false);

    assertThat(testComponentInner.getDispatchedEventHandlers().size()).isEqualTo(1);
    assertThat(
        testComponentInner.getDispatchedEventHandlers().contains(invisibleEventHandlerInner));
    testComponentInner.getDispatchedEventHandlers().clear();

    parentView.setVisibilityHint(true);

    assertThat(testComponentInner.getDispatchedEventHandlers().size()).isEqualTo(1);
    assertThat(testComponentInner.getDispatchedEventHandlers().contains(visibleEventHandlerInner));
  }

  @Test
  public void testMultipleVisibleEventsIncrementalMountDisabled() {
    final TestComponent content1 = create(mContext).build();
    final TestComponent content2 = create(mContext).build();
    final TestComponent content3 = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler1 = new EventHandler<>(content1, 1);
    final EventHandler<VisibleEvent> visibleEventHandler2 = new EventHandler<>(content2, 2);
    final EventHandler<VisibleEvent> visibleEventHandler3 = new EventHandler<>(content3, 3);
    final EventHandler<VisibilityChangedEvent> visibilityChangedHandler =
        new EventHandler<>(content3, 4);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content1)
                    .visibleHandler(visibleEventHandler1)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .delegate(content2)
                    .visibleHandler(visibleEventHandler2)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(mContext)
                    .delegate(content3)
                    .visibleHandler(visibleEventHandler3)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(5))
            .build();

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).incrementalMount(false).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(10, EXACTLY))
        .measure()
        .layout();

    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 0), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibilityChangedHandler);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    content3.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 0, RIGHT, 3), true);
    assertThat(content1.getDispatchedEventHandlers()).contains(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).doesNotContain(visibilityChangedHandler);

    content1.getDispatchedEventHandlers().clear();
    content2.getDispatchedEventHandlers().clear();
    mLithoView.notifyVisibleBoundsChanged(new Rect(LEFT, 3, RIGHT, 11), true);
    assertThat(content1.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler1);
    assertThat(content2.getDispatchedEventHandlers()).contains(visibleEventHandler2);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibleEventHandler3);
    assertThat(content3.getDispatchedEventHandlers()).contains(visibilityChangedHandler);
  }

  @Test
  public void testSetVisibilityHintIncrementalMountDisabled() {
    final TestComponent component = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(component, 1);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(component, 2);
    final EventHandler<FocusedVisibleEvent> focusedEventHandler = new EventHandler<>(component, 3);
    final EventHandler<UnfocusedVisibleEvent> unfocusedEventHandler =
        new EventHandler<>(component, 4);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler =
        new EventHandler<>(component, 5);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(component)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .focusedHandler(focusedEventHandler)
                    .unfocusedHandler(unfocusedEventHandler)
                    .fullImpressionHandler(fullImpressionHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build();

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).incrementalMount(false).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(component.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(focusedEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(fullImpressionHandler);

    component.getDispatchedEventHandlers().clear();
    mLithoView.setVisibilityHint(false);

    assertThat(component.getDispatchedEventHandlers()).contains(invisibleEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(unfocusedEventHandler);

    component.getDispatchedEventHandlers().clear();
    mLithoView.setVisibilityHint(true);

    assertThat(component.getDispatchedEventHandlers()).contains(visibleEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(focusedEventHandler);
    assertThat(component.getDispatchedEventHandlers()).contains(fullImpressionHandler);
  }

  @Test
  public void testVisibilityProcessingNoScrollChange() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    mLithoViewRule
        .useComponentTree(ComponentTree.create(mContext).incrementalMount(false).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, EXACTLY), makeSizeSpec(5, EXACTLY))
        .measure()
        .layout();

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    mLithoView.setBottom(10);
    mLithoView.performLayout(true, 0, 0, RIGHT, 10);

    assertThat(content.getDispatchedEventHandlers()).contains(visibleEventHandler);
  }

  @Test
  public void setNewComponentTree_noMount_noVisibilityEventsDispatched() {
    final TestComponent content = create(mContext).build();
    final EventHandler<VisibleEvent> visibleEventHandler = new EventHandler<>(content, 2);
    final EventHandler<InvisibleEvent> invisibleEventHandler = new EventHandler<>(content, 1);

    final Component root =
        Column.create(mContext)
            .child(
                Wrapper.create(mContext)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build();

    final ComponentTree componentTree = ComponentTree.create(mContext, root).build();
    mLithoView.setComponentTree(componentTree);

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);

    content.getDispatchedEventHandlers().clear();

    final ComponentTree newComponentTree = ComponentTree.create(mContext, root).build();
    mLithoView.setComponentTree(newComponentTree);

    assertThat(content.getDispatchedEventHandlers()).doesNotContain(invisibleEventHandler);
    assertThat(content.getDispatchedEventHandlers()).doesNotContain(visibleEventHandler);
  }

  @Test
  public void processVisibility_componentIsMounted() {
    final Output<View> foundView = new Output<>();

    final Component root =
        Column.create(mContext)
            .child(
                LayoutSpecVisibilityEventTester.create(mContext)
                    .foundView(foundView)
                    .rootView(mLithoViewRule.getLithoView()))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(foundView.get()).isNotNull();
  }

  private Map<String, VisibilityItem> getVisibilityIdToItemMap() {
    if (!mUseMountDelegateTarget) {
      return mLithoView.getMountState().getVisibilityIdToItemMap();
    }

    LithoHostListenerCoordinator lithoHostListenerCoordinator =
        Whitebox.getInternalState(mLithoView, "mLithoHostListenerCoordinator");
    List<HostListenerExtension> extensions =
        Whitebox.getInternalState(lithoHostListenerCoordinator, "mMountExtensions");
    for (int i = 0, size = extensions.size(); i < size; i++) {
      if (extensions.get(i) instanceof VisibilityOutputsExtension) {
        VisibilityOutputsExtension visibilityOutputsExtension =
            (VisibilityOutputsExtension) extensions.get(i);
        return visibilityOutputsExtension.getVisibilityIdToItemMap();
      }
    }

    return null;
  }
}
