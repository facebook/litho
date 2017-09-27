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
public class LayoutAttributesTest {

  private InternalNode mNode;
  private LayoutAttributes mLayoutAttributes;

  @Before
  public void setup() {
    mNode = mock(InternalNode.class);
    when(mNode.build()).thenReturn(mNode);
    mLayoutAttributes = new LayoutAttributes();
    mLayoutAttributes.init(new ComponentContext(RuntimeEnvironment.application), mNode);
  }

  @Test
  public void testSetAttributesAndBuild() {
    mLayoutAttributes.layoutDirection(YogaDirection.INHERIT);
    mLayoutAttributes.alignSelf(YogaAlign.AUTO);
    mLayoutAttributes.positionType(YogaPositionType.ABSOLUTE);
    mLayoutAttributes.flex(2);
    mLayoutAttributes.flexGrow(3);
    mLayoutAttributes.flexShrink(4);
    mLayoutAttributes.flexBasisPx(5);
    mLayoutAttributes.flexBasisPercent(6);

    mLayoutAttributes.importantForAccessibility(
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    mLayoutAttributes.duplicateParentState(false);

    mLayoutAttributes.marginPx(YogaEdge.ALL, 5);
    mLayoutAttributes.marginPx(YogaEdge.RIGHT, 6);
    mLayoutAttributes.marginPx(YogaEdge.LEFT, 4);
    mLayoutAttributes.marginPercent(YogaEdge.ALL, 10);
    mLayoutAttributes.marginPercent(YogaEdge.VERTICAL, 12);
    mLayoutAttributes.marginPercent(YogaEdge.RIGHT, 5);
    mLayoutAttributes.marginAuto(YogaEdge.LEFT);
    mLayoutAttributes.marginAuto(YogaEdge.TOP);
    mLayoutAttributes.marginAuto(YogaEdge.RIGHT);
    mLayoutAttributes.marginAuto(YogaEdge.BOTTOM);

    mLayoutAttributes.paddingPx(YogaEdge.ALL, 1);
    mLayoutAttributes.paddingPx(YogaEdge.RIGHT, 2);
    mLayoutAttributes.paddingPx(YogaEdge.LEFT, 3);
    mLayoutAttributes.paddingPercent(YogaEdge.VERTICAL, 7);
    mLayoutAttributes.paddingPercent(YogaEdge.RIGHT, 6);
    mLayoutAttributes.paddingPercent(YogaEdge.ALL, 5);

    mLayoutAttributes.positionPx(YogaEdge.ALL, 11);
    mLayoutAttributes.positionPx(YogaEdge.RIGHT, 12);
    mLayoutAttributes.positionPx(YogaEdge.LEFT, 13);
    mLayoutAttributes.positionPercent(YogaEdge.VERTICAL, 17);
    mLayoutAttributes.positionPercent(YogaEdge.RIGHT, 16);
    mLayoutAttributes.positionPercent(YogaEdge.ALL, 15);

    mLayoutAttributes.widthPx(5);
    mLayoutAttributes.widthPercent(50);
    mLayoutAttributes.minWidthPx(15);
    mLayoutAttributes.minWidthPercent(100);
    mLayoutAttributes.maxWidthPx(25);
    mLayoutAttributes.maxWidthPercent(26);

    mLayoutAttributes.heightPx(30);
    mLayoutAttributes.heightPercent(31);
    mLayoutAttributes.minHeightPx(32);
    mLayoutAttributes.minHeightPercent(33);
    mLayoutAttributes.maxHeightPx(34);
    mLayoutAttributes.maxHeightPercent(35);

    mLayoutAttributes.aspectRatio(20);

    mLayoutAttributes.touchExpansionPx(YogaEdge.RIGHT, 22);
    mLayoutAttributes.touchExpansionPx(YogaEdge.LEFT, 23);
    mLayoutAttributes.touchExpansionPx(YogaEdge.ALL, 21);

    Reference<Drawable> drawableReference = DrawableReference.create().build();
    mLayoutAttributes.background(drawableReference);
    Drawable foreground = new ColorDrawable(Color.BLACK);
    mLayoutAttributes.foreground(foreground);

    mLayoutAttributes.wrapInView();

    final EventHandler<ClickEvent> clickHandler = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler = mock(EventHandler.class);
    mLayoutAttributes.clickHandler(clickHandler);
    mLayoutAttributes.focusChangeHandler(focusChangedHandler);
    mLayoutAttributes.longClickHandler(longClickHandler);
    mLayoutAttributes.touchHandler(touchHandler);
    mLayoutAttributes.interceptTouchHandler(interceptTouchHandler);

    mLayoutAttributes.focusable(true);
    mLayoutAttributes.enabled(false);
    mLayoutAttributes.visibleHeightRatio(55);
    mLayoutAttributes.visibleWidthRatio(56);

    final EventHandler<VisibleEvent> visibleHandler = mock(EventHandler.class);
    final EventHandler<FocusedVisibleEvent> focusedHandler = mock(EventHandler.class);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = mock(EventHandler.class);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler = mock(EventHandler.class);
    final EventHandler<InvisibleEvent> invisibleHandler = mock(EventHandler.class);
    mLayoutAttributes.visibleHandler(visibleHandler);
    mLayoutAttributes.focusedHandler(focusedHandler);
    mLayoutAttributes.unfocusedHandler(unfocusedHandler);
    mLayoutAttributes.fullImpressionHandler(fullImpressionHandler);
    mLayoutAttributes.invisibleHandler(invisibleHandler);

    mLayoutAttributes.contentDescription("test");

    Object viewTag = new Object();
    SparseArray<Object> viewTags = new SparseArray<>();
    mLayoutAttributes.viewTag(viewTag);
    mLayoutAttributes.viewTags(viewTags);

    mLayoutAttributes.shadowElevationPx(60);

    mLayoutAttributes.clipToOutline(false);
    mLayoutAttributes.transitionKey("transitionKey");
    mLayoutAttributes.testKey("testKey");

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
    mLayoutAttributes.dispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    mLayoutAttributes.onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    mLayoutAttributes.onInitializeAccessibilityNodeInfoHandler(
        onInitializeAccessibilityNodeInfoHandler);
    mLayoutAttributes.onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    mLayoutAttributes.onRequestSendAccessibilityEventHandler(
        onRequestSendAccessibilityEventHandler);
    mLayoutAttributes.performAccessibilityActionHandler(performAccessibilityActionHandler);
    mLayoutAttributes.sendAccessibilityEventHandler(sendAccessibilityEventHandler);
    mLayoutAttributes.sendAccessibilityEventUncheckedHandler(
        sendAccessibilityEventUncheckedHandler);

    ComponentLayout layout = mLayoutAttributes.build();
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
