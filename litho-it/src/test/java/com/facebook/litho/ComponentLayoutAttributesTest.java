/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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
public class ComponentLayoutAttributesTest {

  private InternalNode mNode;
  private ComponentLayoutAttributes mComponentLayoutAttributes;
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mNode = mock(InternalNode.class);
    when(mNode.build()).thenReturn(mNode);
    mComponentLayoutAttributes = new ComponentLayoutAttributes();
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testSetAttributesAndBuild() {
    mComponentLayoutAttributes.layoutDirection(YogaDirection.INHERIT);
    mComponentLayoutAttributes.alignSelf(YogaAlign.AUTO);
    mComponentLayoutAttributes.positionType(YogaPositionType.ABSOLUTE);
    mComponentLayoutAttributes.flex(2);
    mComponentLayoutAttributes.flexGrow(3);
    mComponentLayoutAttributes.flexShrink(4);
    mComponentLayoutAttributes.flexBasisPx(5);
    mComponentLayoutAttributes.flexBasisPercent(6);

    mComponentLayoutAttributes.importantForAccessibility(
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    mComponentLayoutAttributes.duplicateParentState(false);

    mComponentLayoutAttributes.marginPx(YogaEdge.ALL, 5);
    mComponentLayoutAttributes.marginPx(YogaEdge.RIGHT, 6);
    mComponentLayoutAttributes.marginPx(YogaEdge.LEFT, 4);
    mComponentLayoutAttributes.marginPercent(YogaEdge.ALL, 10);
    mComponentLayoutAttributes.marginPercent(YogaEdge.VERTICAL, 12);
    mComponentLayoutAttributes.marginPercent(YogaEdge.RIGHT, 5);
    mComponentLayoutAttributes.marginAuto(YogaEdge.LEFT);
    mComponentLayoutAttributes.marginAuto(YogaEdge.TOP);
    mComponentLayoutAttributes.marginAuto(YogaEdge.RIGHT);
    mComponentLayoutAttributes.marginAuto(YogaEdge.BOTTOM);

    mComponentLayoutAttributes.paddingPx(YogaEdge.ALL, 1);
    mComponentLayoutAttributes.paddingPx(YogaEdge.RIGHT, 2);
    mComponentLayoutAttributes.paddingPx(YogaEdge.LEFT, 3);
    mComponentLayoutAttributes.paddingPercent(YogaEdge.VERTICAL, 7);
    mComponentLayoutAttributes.paddingPercent(YogaEdge.RIGHT, 6);
    mComponentLayoutAttributes.paddingPercent(YogaEdge.ALL, 5);

    mComponentLayoutAttributes.border(Border.create(mComponentContext).build());

    mComponentLayoutAttributes.positionPx(YogaEdge.ALL, 11);
    mComponentLayoutAttributes.positionPx(YogaEdge.RIGHT, 12);
    mComponentLayoutAttributes.positionPx(YogaEdge.LEFT, 13);
    mComponentLayoutAttributes.positionPercent(YogaEdge.VERTICAL, 17);
    mComponentLayoutAttributes.positionPercent(YogaEdge.RIGHT, 16);
    mComponentLayoutAttributes.positionPercent(YogaEdge.ALL, 15);

    mComponentLayoutAttributes.widthPx(5);
    mComponentLayoutAttributes.widthPercent(50);
    mComponentLayoutAttributes.minWidthPx(15);
    mComponentLayoutAttributes.minWidthPercent(100);
    mComponentLayoutAttributes.maxWidthPx(25);
    mComponentLayoutAttributes.maxWidthPercent(26);

    mComponentLayoutAttributes.heightPx(30);
    mComponentLayoutAttributes.heightPercent(31);
    mComponentLayoutAttributes.minHeightPx(32);
    mComponentLayoutAttributes.minHeightPercent(33);
    mComponentLayoutAttributes.maxHeightPx(34);
    mComponentLayoutAttributes.maxHeightPercent(35);

    mComponentLayoutAttributes.aspectRatio(20);

    mComponentLayoutAttributes.touchExpansionPx(YogaEdge.RIGHT, 22);
    mComponentLayoutAttributes.touchExpansionPx(YogaEdge.LEFT, 23);
    mComponentLayoutAttributes.touchExpansionPx(YogaEdge.ALL, 21);

    Reference<Drawable> drawableReference = DrawableReference.create().build();
    mComponentLayoutAttributes.background(drawableReference);
    Drawable foreground = new ColorDrawable(Color.BLACK);
    mComponentLayoutAttributes.foreground(foreground);

    mComponentLayoutAttributes.wrapInView();

    final EventHandler<ClickEvent> clickHandler = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler = mock(EventHandler.class);
    mComponentLayoutAttributes.clickHandler(clickHandler);
    mComponentLayoutAttributes.focusChangeHandler(focusChangedHandler);
    mComponentLayoutAttributes.longClickHandler(longClickHandler);
    mComponentLayoutAttributes.touchHandler(touchHandler);
    mComponentLayoutAttributes.interceptTouchHandler(interceptTouchHandler);

    mComponentLayoutAttributes.focusable(true);
    mComponentLayoutAttributes.enabled(false);
    mComponentLayoutAttributes.visibleHeightRatio(55);
    mComponentLayoutAttributes.visibleWidthRatio(56);

    final EventHandler<VisibleEvent> visibleHandler = mock(EventHandler.class);
    final EventHandler<FocusedVisibleEvent> focusedHandler = mock(EventHandler.class);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = mock(EventHandler.class);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler = mock(EventHandler.class);
    final EventHandler<InvisibleEvent> invisibleHandler = mock(EventHandler.class);
    mComponentLayoutAttributes.visibleHandler(visibleHandler);
    mComponentLayoutAttributes.focusedHandler(focusedHandler);
    mComponentLayoutAttributes.unfocusedHandler(unfocusedHandler);
    mComponentLayoutAttributes.fullImpressionHandler(fullImpressionHandler);
    mComponentLayoutAttributes.invisibleHandler(invisibleHandler);

    mComponentLayoutAttributes.contentDescription("test");

    Object viewTag = new Object();
    SparseArray<Object> viewTags = new SparseArray<>();
    mComponentLayoutAttributes.viewTag(viewTag);
    mComponentLayoutAttributes.viewTags(viewTags);

    mComponentLayoutAttributes.shadowElevationPx(60);

    mComponentLayoutAttributes.clipToOutline(false);
    mComponentLayoutAttributes.transitionKey("transitionKey");
    mComponentLayoutAttributes.testKey("testKey");

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
    mComponentLayoutAttributes.dispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    mComponentLayoutAttributes.onInitializeAccessibilityEventHandler(
        onInitializeAccessibilityEventHandler);
    mComponentLayoutAttributes.onInitializeAccessibilityNodeInfoHandler(
        onInitializeAccessibilityNodeInfoHandler);
    mComponentLayoutAttributes.onPopulateAccessibilityEventHandler(
        onPopulateAccessibilityEventHandler);
    mComponentLayoutAttributes.onRequestSendAccessibilityEventHandler(
        onRequestSendAccessibilityEventHandler);
    mComponentLayoutAttributes.performAccessibilityActionHandler(performAccessibilityActionHandler);
    mComponentLayoutAttributes.sendAccessibilityEventHandler(sendAccessibilityEventHandler);
    mComponentLayoutAttributes.sendAccessibilityEventUncheckedHandler(
        sendAccessibilityEventUncheckedHandler);

    mComponentLayoutAttributes.copyInto(mComponentContext, mNode);

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
