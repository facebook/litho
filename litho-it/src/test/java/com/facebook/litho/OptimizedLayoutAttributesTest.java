/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.reference.DrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class OptimizedLayoutAttributesTest {

  private InternalNode mNode;
  private OptimizedLayoutAttributes mOptimizedLayoutAttributes;
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mNode = mock(InternalNode.class);
    when(mNode.build()).thenReturn(mNode);
    mOptimizedLayoutAttributes = new OptimizedLayoutAttributes();
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
    mOptimizedLayoutAttributes.init(mComponentContext, mNode);
  }

  @Test
  public void testSetAttributesAndBuild() {
    mOptimizedLayoutAttributes.layoutDirection(YogaDirection.INHERIT);
    mOptimizedLayoutAttributes.alignSelf(YogaAlign.AUTO);
    mOptimizedLayoutAttributes.positionType(YogaPositionType.ABSOLUTE);
    mOptimizedLayoutAttributes.flex(2);
    mOptimizedLayoutAttributes.flexGrow(3);
    mOptimizedLayoutAttributes.flexShrink(4);
    mOptimizedLayoutAttributes.flexBasisPx(5);
    mOptimizedLayoutAttributes.flexBasisPercent(6);

    mOptimizedLayoutAttributes.importantForAccessibility(
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    mOptimizedLayoutAttributes.duplicateParentState(false);

    mOptimizedLayoutAttributes.marginPx(YogaEdge.ALL, 5);
    mOptimizedLayoutAttributes.marginPx(YogaEdge.RIGHT, 6);
    mOptimizedLayoutAttributes.marginPx(YogaEdge.LEFT, 4);
    mOptimizedLayoutAttributes.marginPercent(YogaEdge.ALL, 10);
    mOptimizedLayoutAttributes.marginPercent(YogaEdge.VERTICAL, 12);
    mOptimizedLayoutAttributes.marginPercent(YogaEdge.RIGHT, 5);
    mOptimizedLayoutAttributes.marginAuto(YogaEdge.LEFT);
    mOptimizedLayoutAttributes.marginAuto(YogaEdge.TOP);
    mOptimizedLayoutAttributes.marginAuto(YogaEdge.RIGHT);
    mOptimizedLayoutAttributes.marginAuto(YogaEdge.BOTTOM);

    mOptimizedLayoutAttributes.paddingPx(YogaEdge.ALL, 1);
    mOptimizedLayoutAttributes.paddingPx(YogaEdge.RIGHT, 2);
    mOptimizedLayoutAttributes.paddingPx(YogaEdge.LEFT, 3);
    mOptimizedLayoutAttributes.paddingPercent(YogaEdge.VERTICAL, 7);
    mOptimizedLayoutAttributes.paddingPercent(YogaEdge.RIGHT, 6);
    mOptimizedLayoutAttributes.paddingPercent(YogaEdge.ALL, 5);

    mOptimizedLayoutAttributes.border(Border.create(mComponentContext).build());

    mOptimizedLayoutAttributes.positionPx(YogaEdge.ALL, 11);
    mOptimizedLayoutAttributes.positionPx(YogaEdge.RIGHT, 12);
    mOptimizedLayoutAttributes.positionPx(YogaEdge.LEFT, 13);
    mOptimizedLayoutAttributes.positionPercent(YogaEdge.VERTICAL, 17);
    mOptimizedLayoutAttributes.positionPercent(YogaEdge.RIGHT, 16);
    mOptimizedLayoutAttributes.positionPercent(YogaEdge.ALL, 15);

    mOptimizedLayoutAttributes.widthPx(5);
    mOptimizedLayoutAttributes.widthPercent(50);
    mOptimizedLayoutAttributes.minWidthPx(15);
    mOptimizedLayoutAttributes.minWidthPercent(100);
    mOptimizedLayoutAttributes.maxWidthPx(25);
    mOptimizedLayoutAttributes.maxWidthPercent(26);

    mOptimizedLayoutAttributes.heightPx(30);
    mOptimizedLayoutAttributes.heightPercent(31);
    mOptimizedLayoutAttributes.minHeightPx(32);
    mOptimizedLayoutAttributes.minHeightPercent(33);
    mOptimizedLayoutAttributes.maxHeightPx(34);
    mOptimizedLayoutAttributes.maxHeightPercent(35);

    mOptimizedLayoutAttributes.aspectRatio(20);

    mOptimizedLayoutAttributes.touchExpansionPx(YogaEdge.RIGHT, 22);
    mOptimizedLayoutAttributes.touchExpansionPx(YogaEdge.LEFT, 23);
    mOptimizedLayoutAttributes.touchExpansionPx(YogaEdge.ALL, 21);

    Reference<Drawable> drawableReference = DrawableReference.create().build();
    mOptimizedLayoutAttributes.background(drawableReference);
    Drawable foreground = new ColorDrawable(Color.BLACK);
    mOptimizedLayoutAttributes.foreground(foreground);

    mOptimizedLayoutAttributes.wrapInView();

    final EventHandler<ClickEvent> clickHandler = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler = mock(EventHandler.class);
    mOptimizedLayoutAttributes.clickHandler(clickHandler);
    mOptimizedLayoutAttributes.focusChangeHandler(focusChangedHandler);
    mOptimizedLayoutAttributes.longClickHandler(longClickHandler);
    mOptimizedLayoutAttributes.touchHandler(touchHandler);
    mOptimizedLayoutAttributes.interceptTouchHandler(interceptTouchHandler);

    mOptimizedLayoutAttributes.focusable(true);
    mOptimizedLayoutAttributes.enabled(false);
    mOptimizedLayoutAttributes.visibleHeightRatio(55);
    mOptimizedLayoutAttributes.visibleWidthRatio(56);

    final EventHandler<VisibleEvent> visibleHandler = mock(EventHandler.class);
    final EventHandler<FocusedVisibleEvent> focusedHandler = mock(EventHandler.class);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = mock(EventHandler.class);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler = mock(EventHandler.class);
    final EventHandler<InvisibleEvent> invisibleHandler = mock(EventHandler.class);
    mOptimizedLayoutAttributes.visibleHandler(visibleHandler);
    mOptimizedLayoutAttributes.focusedHandler(focusedHandler);
    mOptimizedLayoutAttributes.unfocusedHandler(unfocusedHandler);
    mOptimizedLayoutAttributes.fullImpressionHandler(fullImpressionHandler);
    mOptimizedLayoutAttributes.invisibleHandler(invisibleHandler);

    mOptimizedLayoutAttributes.contentDescription("test");

    Object viewTag = new Object();
    SparseArray<Object> viewTags = new SparseArray<>();
    mOptimizedLayoutAttributes.viewTag(viewTag);
    mOptimizedLayoutAttributes.viewTags(viewTags);

    mOptimizedLayoutAttributes.shadowElevationPx(60);

    mOptimizedLayoutAttributes.clipToOutline(false);
    mOptimizedLayoutAttributes.transitionKey("transitionKey");
    mOptimizedLayoutAttributes.testKey("testKey");

    final EventHandler<DispatchPopulateAccessibilityEventEvent>
        dispatchPopulateAccessibilityEventHandler = mock(EventHandler.class);
    final EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<OnInitializeAccessibilityNodeInfoEvent>
        onInitializeAccessibilityNodeInfoHandler = mock(EventHandler.class);
    final EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<OnRequestSendAccessibilityEventEvent>
        onRequestSendAccessibilityEventHandler = mock(EventHandler.class);
    final EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler =
        mock(EventHandler.class);
    final EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler =
        mock(EventHandler.class);
    final EventHandler<SendAccessibilityEventUncheckedEvent>
        sendAccessibilityEventUncheckedHandler = mock(EventHandler.class);
    mOptimizedLayoutAttributes.dispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    mOptimizedLayoutAttributes.onInitializeAccessibilityEventHandler(
        onInitializeAccessibilityEventHandler);
    mOptimizedLayoutAttributes.onInitializeAccessibilityNodeInfoHandler(
        onInitializeAccessibilityNodeInfoHandler);
    mOptimizedLayoutAttributes.onPopulateAccessibilityEventHandler(
        onPopulateAccessibilityEventHandler);
    mOptimizedLayoutAttributes.onRequestSendAccessibilityEventHandler(
        onRequestSendAccessibilityEventHandler);
    mOptimizedLayoutAttributes.performAccessibilityActionHandler(performAccessibilityActionHandler);
    mOptimizedLayoutAttributes.sendAccessibilityEventHandler(sendAccessibilityEventHandler);
    mOptimizedLayoutAttributes.sendAccessibilityEventUncheckedHandler(
        sendAccessibilityEventUncheckedHandler);

    ComponentLayout layout = mOptimizedLayoutAttributes.build();
    assertThat(layout).isSameAs(mNode);

    verify(mNode).layoutDirection(YogaDirection.INHERIT);
    verify(mNode).alignSelf(YogaAlign.AUTO);
    verify(mNode).positionType(YogaPositionType.ABSOLUTE);
    verify(mNode).flex(2);
    verify(mNode).flexGrow(3);
    verify(mNode).flexShrink(4);
    verify(mNode).flexBasisPx(5);
    verify(mNode).flexBasisPercent(6);

    verify(mNode)
        .importantForAccessibility(ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    verify(mNode).duplicateParentState(false);

    verify(mNode).marginPx(YogaEdge.ALL, 5);
    verify(mNode).marginPx(YogaEdge.RIGHT, 6);
    verify(mNode).marginPx(YogaEdge.LEFT, 4);
    verify(mNode).marginPercent(YogaEdge.ALL, 10);
    verify(mNode).marginPercent(YogaEdge.VERTICAL, 12);
    verify(mNode).marginPercent(YogaEdge.RIGHT, 5);
    verify(mNode).marginAuto(YogaEdge.LEFT);
    verify(mNode).marginAuto(YogaEdge.TOP);
    verify(mNode).marginAuto(YogaEdge.RIGHT);
    verify(mNode).marginAuto(YogaEdge.BOTTOM);

    verify(mNode).paddingPx(YogaEdge.ALL, 1);
    verify(mNode).paddingPx(YogaEdge.RIGHT, 2);
    verify(mNode).paddingPx(YogaEdge.LEFT, 3);
    verify(mNode).paddingPercent(YogaEdge.VERTICAL, 7);
    verify(mNode).paddingPercent(YogaEdge.RIGHT, 6);
    verify(mNode).paddingPercent(YogaEdge.ALL, 5);

    verify(mNode).border(any(Border.class));

    verify(mNode).positionPx(YogaEdge.ALL, 11);
    verify(mNode).positionPx(YogaEdge.RIGHT, 12);
    verify(mNode).positionPx(YogaEdge.LEFT, 13);
    verify(mNode).positionPercent(YogaEdge.VERTICAL, 17);
    verify(mNode).positionPercent(YogaEdge.RIGHT, 16);
    verify(mNode).positionPercent(YogaEdge.ALL, 15);

    verify(mNode).widthPx(5);
    verify(mNode).widthPercent(50);
    verify(mNode).minWidthPx(15);
    verify(mNode).minWidthPercent(100);
    verify(mNode).maxWidthPx(25);
    verify(mNode).maxWidthPercent(26);

    verify(mNode).heightPx(30);
    verify(mNode).heightPercent(31);
    verify(mNode).minHeightPx(32);
    verify(mNode).minHeightPercent(33);
    verify(mNode).maxHeightPx(34);
    verify(mNode).maxHeightPercent(35);

    verify(mNode).aspectRatio(20);

    verify(mNode).touchExpansionPx(YogaEdge.RIGHT, 22);
    verify(mNode).touchExpansionPx(YogaEdge.LEFT, 23);
    verify(mNode).touchExpansionPx(YogaEdge.ALL, 21);

    verify(mNode).background(drawableReference);
    verify(mNode).foreground(foreground);

    verify(mNode).wrapInView();

    verify(mNode).clickHandler(clickHandler);
    verify(mNode).focusChangeHandler(focusChangedHandler);
    verify(mNode).longClickHandler(longClickHandler);
    verify(mNode).touchHandler(touchHandler);
    verify(mNode).interceptTouchHandler(interceptTouchHandler);

    verify(mNode).focusable(true);
    verify(mNode).enabled(false);
    verify(mNode).visibleHeightRatio(55);
    verify(mNode).visibleWidthRatio(56);

    verify(mNode).visibleHandler(visibleHandler);
    verify(mNode).focusedHandler(focusedHandler);
    verify(mNode).unfocusedHandler(unfocusedHandler);
    verify(mNode).fullImpressionHandler(fullImpressionHandler);
    verify(mNode).invisibleHandler(invisibleHandler);

    verify(mNode).contentDescription("test");

    verify(mNode).viewTag(viewTag);
    verify(mNode).viewTags(viewTags);

    verify(mNode).shadowElevationPx(60);

    verify(mNode).clipToOutline(false);
    verify(mNode).transitionKey("transitionKey");
    verify(mNode).testKey("testKey");

    verify(mNode)
        .dispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
    verify(mNode).onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    verify(mNode)
        .onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    verify(mNode).onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    verify(mNode).onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    verify(mNode).performAccessibilityActionHandler(performAccessibilityActionHandler);
    verify(mNode).sendAccessibilityEventHandler(sendAccessibilityEventHandler);
    verify(mNode).sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);
  }
}
