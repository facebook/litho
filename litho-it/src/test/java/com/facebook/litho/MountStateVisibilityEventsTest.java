/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;
import android.support.v4.util.LongSparseArray;

import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.litho.testing.TestViewComponent.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.reflect.Whitebox.setInternalState;

@RunWith(ComponentsTestRunner.class)
public class MountStateVisibilityEventsTest {

  private static final int VISIBLE = 1;
  private static final int FOCUSED = 2;
  private static final int FULL_IMPRESSION = 3;
  private static final int INVISIBLE = 4;
  private static final int UNFOCUSED = 5;

  private static final int LEFT = 0;
  private static final int RIGHT = 10;

  private static final int VIEWPORT_HEIGHT = 5;
  private static final int VIEWPORT_WIDTH = 10;

  private long mLastVisibilityOutputId = 0;
  private ComponentContext mContext;
  private MountState mMountState;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);

    ComponentTree mockComponentTree = mock(ComponentTree.class);
    doReturn(mContext).when(mockComponentTree).getContext();

    ComponentHost mockParent = mock(ComponentHost.class);
    doReturn(VIEWPORT_WIDTH).when(mockParent).getWidth();
    doReturn(VIEWPORT_HEIGHT).when(mockParent).getHeight();

    LithoView attachedView = spy(new LithoView(mContext));
    Whitebox.setInternalState(attachedView, "mComponentTree", mockComponentTree);
    doReturn(mockParent).when(attachedView).getParent();

    mMountState = new MountState(attachedView);
    Whitebox.setInternalState(mMountState, "mIsDirty", false);

    mLastVisibilityOutputId = 0;
  }

  @Test
  public void testVisibleEvent() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = create(mContext).build();
    setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler visibleHandler = createEventHandler(content, VISIBLE);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 10),
        visibleHandler,
        null,
        null,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 0, RIGHT, 5));
    checkNoVisibilityEventsDispatched(mockLifecycle);
    assertThat(getVisibilityItemMapSize()).isEqualTo(0);

    mMountState.mount(layoutState, new Rect(LEFT, 5, RIGHT, 10));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(visibleHandler),
        isA(VisibleEvent.class));
    assertThat(getVisibilityItemMapSize()).isEqualTo(1);
  }

  @Test
  public void testFocusedOccupiesHalfViewport() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = TestViewComponent.create(mContext).build();
    Whitebox.setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler focusedHandler = createEventHandler(content, FOCUSED);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 10),
        null,
        focusedHandler,
        null,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    Whitebox.setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 1, RIGHT, 6));
    checkNoVisibilityEventsDispatched(mockLifecycle);

    mMountState.mount(layoutState, new Rect(LEFT, 3, RIGHT, 8));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(focusedHandler),
        isA(FocusedVisibleEvent.class));
  }

  @Test
  public void testFocusedSmallerThanHalfViewport() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = TestViewComponent.create(mContext).build();
    Whitebox.setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler focusedHandler = createEventHandler(content, FOCUSED);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 8),
        null,
        focusedHandler,
        null,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    Whitebox.setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 1, RIGHT, 6));
    checkNoVisibilityEventsDispatched(mockLifecycle);

    mMountState.mount(layoutState, new Rect(LEFT, 2, RIGHT, 7));
    checkNoVisibilityEventsDispatched(mockLifecycle);

    mMountState.mount(layoutState, new Rect(LEFT, 3, RIGHT, 8));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(focusedHandler),
        isA(FocusedVisibleEvent.class));
  }

  @Test
  public void testMultipleFocusAndUnfocusEvents() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = TestViewComponent.create(mContext).build();
    Whitebox.setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler focusedHandler = createEventHandler(content, FOCUSED);
    final EventHandler unfocusedHandler = createEventHandler(content, UNFOCUSED);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 12),
        null,
        focusedHandler,
        unfocusedHandler,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    Whitebox.setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    //Mount test view in the middle of the view port (focused)
    mMountState.mount(layoutState, new Rect(LEFT, 6, RIGHT, 11));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(focusedHandler),
        isA(FocusedVisibleEvent.class));

    //Mount test view on the edge of the viewport (not focused)
    mMountState.mount(layoutState, new Rect(LEFT, 11, RIGHT, 16));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(unfocusedHandler),
        isA(UnfocusedVisibleEvent.class));

    //Mount test view in the middle of the view port (focused)
    mMountState.mount(layoutState, new Rect(LEFT, 4, RIGHT, 9));
    verify(mockLifecycle, times(2)).dispatchOnEvent(
        eq(focusedHandler),
        isA(FocusedVisibleEvent.class));

    //Mount test view off the edge of the view port (unfocused)
    mMountState.mount(layoutState, new Rect(LEFT, 1, RIGHT, 6));
    verify(mockLifecycle, times(2)).dispatchOnEvent(
        eq(unfocusedHandler),
        isA(UnfocusedVisibleEvent.class));
  }

  @Test
  public void testNoUnfocusEvents() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = TestViewComponent.create(mContext).build();
    Whitebox.setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler unfocusedHandler = createEventHandler(content, UNFOCUSED);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 12),
        null,
        null,
        unfocusedHandler,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    Whitebox.setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    //Mount test view in the middle of the view port (focused)
    mMountState.mount(layoutState, new Rect(LEFT, 6, RIGHT, 11));
    verify(mockLifecycle, times(0)).dispatchOnEvent(
        eq(unfocusedHandler),
        isA(UnfocusedVisibleEvent.class));
  }

  @Test
  public void testFullImpression() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = TestViewComponent.create(mContext).build();
    Whitebox.setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler fullImpressionHandler = createEventHandler(content, FULL_IMPRESSION);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();

    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 10),
        null,
        null,
        null,
        fullImpressionHandler,
        null));

    final LayoutState layoutState = new LayoutState();
    Whitebox.setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 4, RIGHT, 9));
    checkNoVisibilityEventsDispatched(mockLifecycle);

    mMountState.mount(layoutState, new Rect(LEFT, 9, RIGHT, 14));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(fullImpressionHandler),
        isA(FullImpressionVisibleEvent.class));
  }

  @Test
  public void testInvisibleEvent() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = create(mContext).build();
    setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler invisibleHandler = createEventHandler(content, INVISIBLE);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();
    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 10),
        null,
        null,
        null,
        null,
        invisibleHandler));

    final LayoutState layoutState = new LayoutState();
    setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 5, RIGHT, 10));
    checkNoVisibilityEventsDispatched(mockLifecycle);
    assertThat(getVisibilityItemMapSize()).isEqualTo(1);

    mMountState.mount(layoutState, new Rect(LEFT, 0, RIGHT, 5));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(invisibleHandler),
        isA(InvisibleEvent.class));
    assertThat(getVisibilityItemMapSize()).isEqualTo(0);
  }

  @Test
  public void testBothVisibilityEvents() {
    ComponentLifecycle mockLifecycle = createLifecycleMock();
    Component<?> content = create(mContext).build();
    setInternalState(content, "mLifecycle", mockLifecycle);

    final EventHandler visibleHandler = createEventHandler(content, VISIBLE);
    final EventHandler invisibleHandler = createEventHandler(content, INVISIBLE);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();
    visibilityOutputs.add(createVisibilityOutput(
        content,
        new Rect(LEFT, 5, RIGHT, 10),
        visibleHandler,
        null,
        null,
        null,
        invisibleHandler));

    final LayoutState layoutState = new LayoutState();
    setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 0, RIGHT, 5));
    checkNoVisibilityEventsDispatched(mockLifecycle);
    assertThat(getVisibilityItemMapSize()).isEqualTo(0);

    mMountState.mount(layoutState, new Rect(LEFT, 5, RIGHT, 10));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(visibleHandler),
        isA(VisibleEvent.class));
    assertThat(getVisibilityItemMapSize()).isEqualTo(1);

    mMountState.mount(layoutState, new Rect(LEFT, 10, RIGHT, 15));
    verify(mockLifecycle, times(1)).dispatchOnEvent(
        eq(invisibleHandler),
        isA(InvisibleEvent.class));
    assertThat(getVisibilityItemMapSize()).isEqualTo(0);
  }

  @Test
  public void testVisibilityEventsMultipleOutputs() {
    ComponentLifecycle mockLifecycle1 = createLifecycleMock();
    ComponentLifecycle mockLifecycle2 = createLifecycleMock();

    Component<?> content1 = create(mContext).build();
    Component<?> content2 = TestLayoutComponent.create(mContext).build();
    setInternalState(content1, "mLifecycle", mockLifecycle1);
    setInternalState(content2, "mLifecycle", mockLifecycle2);

    final EventHandler visibleHandler1 = createEventHandler(content1, VISIBLE);
    final EventHandler invisibleHandler1 = createEventHandler(content1, INVISIBLE);
    final EventHandler visibleHandler2 = createEventHandler(content2, VISIBLE);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();
    visibilityOutputs.add(createVisibilityOutput(
        content1,
        new Rect(LEFT, 5, RIGHT, 10),
        visibleHandler1,
        null,
        null,
        null,
        invisibleHandler1));

    visibilityOutputs.add(createVisibilityOutput(
        content2,
        new Rect(LEFT, 10, RIGHT, 15),
        visibleHandler2,
        null,
        null,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 5, RIGHT, 10));
    verify(mockLifecycle1, times(1)).dispatchOnEvent(
        eq(visibleHandler1),
        isA(VisibleEvent.class));
    checkNoVisibilityEventsDispatched(mockLifecycle2);
    assertThat(getVisibilityItemMapSize()).isEqualTo(1);

    mMountState.mount(layoutState, new Rect(LEFT, 10, RIGHT, 15));
    verify(mockLifecycle1, times(1)).dispatchOnEvent(
        eq(invisibleHandler1),
        isA(InvisibleEvent.class));
    verify(mockLifecycle2, times(1)).dispatchOnEvent(
        eq(visibleHandler2),
        isA(VisibleEvent.class));
    assertThat(getVisibilityItemMapSize()).isEqualTo(1);
  }

  @Test
  public void testVisibilityEventsMultipleVisible() {
    ComponentLifecycle mockLifecycle1 = createLifecycleMock();
    ComponentLifecycle mockLifecycle2 = createLifecycleMock();

    Component<?> content1 = create(mContext).build();
    Component<?> content2 = TestLayoutComponent.create(mContext).build();
    setInternalState(content1, "mLifecycle", mockLifecycle1);
    setInternalState(content2, "mLifecycle", mockLifecycle2);

    final EventHandler visibleHandler1 = createEventHandler(content1, VISIBLE);
    final EventHandler visibleHandler2 = createEventHandler(content2, VISIBLE);

    final List<VisibilityOutput> visibilityOutputs = new ArrayList<>();
    visibilityOutputs.add(createVisibilityOutput(
        content1,
        new Rect(LEFT, 5, RIGHT, 10),
        visibleHandler1,
        null,
        null,
        null,
        null));

    visibilityOutputs.add(createVisibilityOutput(
        content2,
        new Rect(LEFT, 10, RIGHT, 15),
        visibleHandler2,
        null,
        null,
        null,
        null));

    final LayoutState layoutState = new LayoutState();
    setInternalState(layoutState, "mVisibilityOutputs", visibilityOutputs);

    mMountState.mount(layoutState, new Rect(LEFT, 7, RIGHT, 12));
    verify(mockLifecycle1, times(1)).dispatchOnEvent(
        eq(visibleHandler1),
        isA(VisibleEvent.class));
    verify(mockLifecycle2, times(1)).dispatchOnEvent(
        eq(visibleHandler2),
        isA(VisibleEvent.class));
    assertThat(getVisibilityItemMapSize()).isEqualTo(2);
  }

  private int getVisibilityItemMapSize() {
    return ((LongSparseArray) Whitebox.getInternalState(
        mMountState,
        "mVisibilityIdToItemMap")).size();
  }

  private static void checkNoVisibilityEventsDispatched(ComponentLifecycle lifecycle) {
    verify(lifecycle, times(0)).dispatchOnEvent(isA(EventHandler.class), isA(VisibleEvent.class));
    verify(lifecycle, times(0)).dispatchOnEvent(
        isA(EventHandler.class),
        isA(FocusedVisibleEvent.class));
    verify(lifecycle, times(0)).dispatchOnEvent(
        isA(EventHandler.class),
        isA(FullImpressionVisibleEvent.class));
    verify(lifecycle, times(0)).dispatchOnEvent(isA(EventHandler.class), isA(InvisibleEvent.class));
  }

  private static EventHandler createEventHandler(Component<?> component, int type) {
    EventHandler handler = new EventHandler(component, type);
    return handler;
  }

  private static ComponentLifecycle createLifecycleMock() {
    ComponentLifecycle mock = mock(ComponentLifecycle.class);
    doReturn(null).when(mock).dispatchOnEvent(any(EventHandler.class), any());

    return mock;
  }

  private VisibilityOutput createVisibilityOutput(
      Component<?> component,
      Rect bounds,
      EventHandler visibleHandler,
      EventHandler focusedHandler,
      EventHandler unfocusedHandler,
      EventHandler fullImpressionHandler,
      EventHandler invisibleHandler) {
    VisibilityOutput visibilityOutput = new VisibilityOutput();

    Whitebox.setInternalState(visibilityOutput, "mComponent", component);
    visibilityOutput.setBounds(bounds);
    visibilityOutput.setVisibleEventHandler(visibleHandler);
    visibilityOutput.setFocusedEventHandler(focusedHandler);
    visibilityOutput.setUnfocusedEventHandler(unfocusedHandler);
    visibilityOutput.setFullImpressionEventHandler(fullImpressionHandler);
    visibilityOutput.setInvisibleEventHandler(invisibleHandler);

    mLastVisibilityOutputId += 1;
    visibilityOutput.setId(mLastVisibilityOutputId);

    return visibilityOutput;
  }
}
