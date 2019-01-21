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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.util.SparseArray;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.drawable.ComparableDrawable;
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
public class CommonPropsTest {

  private InternalNode mNode;
  private CommonPropsHolder mCommonProps;
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mNode = mock(InternalNode.class);
    mCommonProps = new CommonPropsHolder();
    mComponentContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void testSetPropsAndBuild() {
    mCommonProps.layoutDirection(YogaDirection.INHERIT);
    mCommonProps.alignSelf(YogaAlign.AUTO);
    mCommonProps.positionType(YogaPositionType.ABSOLUTE);
    mCommonProps.flex(2);
    mCommonProps.flexGrow(3);
    mCommonProps.flexShrink(4);
    mCommonProps.flexBasisPx(5);
    mCommonProps.flexBasisPercent(6);

    mCommonProps.importantForAccessibility(
        ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    mCommonProps.duplicateParentState(false);

    mCommonProps.marginPx(YogaEdge.ALL, 5);
    mCommonProps.marginPx(YogaEdge.RIGHT, 6);
    mCommonProps.marginPx(YogaEdge.LEFT, 4);
    mCommonProps.marginPercent(YogaEdge.ALL, 10);
    mCommonProps.marginPercent(YogaEdge.VERTICAL, 12);
    mCommonProps.marginPercent(YogaEdge.RIGHT, 5);
    mCommonProps.marginAuto(YogaEdge.LEFT);
    mCommonProps.marginAuto(YogaEdge.TOP);
    mCommonProps.marginAuto(YogaEdge.RIGHT);
    mCommonProps.marginAuto(YogaEdge.BOTTOM);

    mCommonProps.paddingPx(YogaEdge.ALL, 1);
    mCommonProps.paddingPx(YogaEdge.RIGHT, 2);
    mCommonProps.paddingPx(YogaEdge.LEFT, 3);
    mCommonProps.paddingPercent(YogaEdge.VERTICAL, 7);
    mCommonProps.paddingPercent(YogaEdge.RIGHT, 6);
    mCommonProps.paddingPercent(YogaEdge.ALL, 5);

    mCommonProps.border(Border.create(mComponentContext).build());

    mCommonProps.positionPx(YogaEdge.ALL, 11);
    mCommonProps.positionPx(YogaEdge.RIGHT, 12);
    mCommonProps.positionPx(YogaEdge.LEFT, 13);
    mCommonProps.positionPercent(YogaEdge.VERTICAL, 17);
    mCommonProps.positionPercent(YogaEdge.RIGHT, 16);
    mCommonProps.positionPercent(YogaEdge.ALL, 15);

    mCommonProps.widthPx(5);
    mCommonProps.widthPercent(50);
    mCommonProps.minWidthPx(15);
    mCommonProps.minWidthPercent(100);
    mCommonProps.maxWidthPx(25);
    mCommonProps.maxWidthPercent(26);

    mCommonProps.heightPx(30);
    mCommonProps.heightPercent(31);
    mCommonProps.minHeightPx(32);
    mCommonProps.minHeightPercent(33);
    mCommonProps.maxHeightPx(34);
    mCommonProps.maxHeightPercent(35);

    mCommonProps.aspectRatio(20);

    mCommonProps.touchExpansionPx(YogaEdge.RIGHT, 22);
    mCommonProps.touchExpansionPx(YogaEdge.LEFT, 23);
    mCommonProps.touchExpansionPx(YogaEdge.ALL, 21);
    ComparableDrawable background = ComparableColorDrawable.create(Color.RED);
    Reference<ComparableDrawable> bgRef = DrawableReference.create(background);
    mCommonProps.background(bgRef);
    ComparableDrawable foreground = ComparableColorDrawable.create(Color.BLACK);
    mCommonProps.foreground(foreground);

    mCommonProps.wrapInView();

    final EventHandler<ClickEvent> clickHandler = mock(EventHandler.class);
    final EventHandler<LongClickEvent> longClickHandler = mock(EventHandler.class);
    final EventHandler<TouchEvent> touchHandler = mock(EventHandler.class);
    final EventHandler<InterceptTouchEvent> interceptTouchHandler = mock(EventHandler.class);
    final EventHandler<FocusChangedEvent> focusChangedHandler = mock(EventHandler.class);
    mCommonProps.clickHandler(clickHandler);
    mCommonProps.focusChangeHandler(focusChangedHandler);
    mCommonProps.longClickHandler(longClickHandler);
    mCommonProps.touchHandler(touchHandler);
    mCommonProps.interceptTouchHandler(interceptTouchHandler);

    mCommonProps.focusable(true);
    mCommonProps.selected(false);
    mCommonProps.enabled(false);
    mCommonProps.visibleHeightRatio(55);
    mCommonProps.visibleWidthRatio(56);

    final EventHandler<VisibleEvent> visibleHandler = mock(EventHandler.class);
    final EventHandler<FocusedVisibleEvent> focusedHandler = mock(EventHandler.class);
    final EventHandler<UnfocusedVisibleEvent> unfocusedHandler = mock(EventHandler.class);
    final EventHandler<FullImpressionVisibleEvent> fullImpressionHandler = mock(EventHandler.class);
    final EventHandler<InvisibleEvent> invisibleHandler = mock(EventHandler.class);
    final EventHandler<VisibilityChangedEvent> visibleRectChangedHandler = mock(EventHandler.class);
    mCommonProps.visibleHandler(visibleHandler);
    mCommonProps.focusedHandler(focusedHandler);
    mCommonProps.unfocusedHandler(unfocusedHandler);
    mCommonProps.fullImpressionHandler(fullImpressionHandler);
    mCommonProps.invisibleHandler(invisibleHandler);
    mCommonProps.visibilityChangedHandler(visibleRectChangedHandler);

    mCommonProps.contentDescription("test");

    Object viewTag = new Object();
    SparseArray<Object> viewTags = new SparseArray<>();
    mCommonProps.viewTag(viewTag);
    mCommonProps.viewTags(viewTags);

    mCommonProps.shadowElevationPx(60);

    mCommonProps.clipToOutline(false);
    mCommonProps.transitionKey("transitionKey");
    mCommonProps.testKey("testKey");

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
    mCommonProps.accessibilityRole(AccessibilityRole.BUTTON);
    mCommonProps.dispatchPopulateAccessibilityEventHandler(
        dispatchPopulateAccessibilityEventHandler);
    mCommonProps.onInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    mCommonProps.onInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    mCommonProps.onPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    mCommonProps.onRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    mCommonProps.performAccessibilityActionHandler(performAccessibilityActionHandler);
    mCommonProps.sendAccessibilityEventHandler(sendAccessibilityEventHandler);
    mCommonProps.sendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);

    final StateListAnimator stateListAnimator = mock(StateListAnimator.class);
    mCommonProps.stateListAnimator(stateListAnimator);

    mCommonProps.copyInto(mComponentContext, mNode);

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

    verify(mNode).background(bgRef);
    verify(mNode).foreground(foreground);

    verify(mNode).wrapInView();

    verify(mNode).clickHandler(clickHandler);
    verify(mNode).focusChangeHandler(focusChangedHandler);
    verify(mNode).longClickHandler(longClickHandler);
    verify(mNode).touchHandler(touchHandler);
    verify(mNode).interceptTouchHandler(interceptTouchHandler);

    verify(mNode).focusable(true);
    verify(mNode).selected(false);
    verify(mNode).enabled(false);
    verify(mNode).visibleHeightRatio(55);
    verify(mNode).visibleWidthRatio(56);

    verify(mNode).visibleHandler(visibleHandler);
    verify(mNode).focusedHandler(focusedHandler);
    verify(mNode).unfocusedHandler(unfocusedHandler);
    verify(mNode).fullImpressionHandler(fullImpressionHandler);
    verify(mNode).invisibleHandler(invisibleHandler);
    verify(mNode).visibilityChangedHandler(visibleRectChangedHandler);

    verify(mNode).contentDescription("test");

    verify(mNode).viewTag(viewTag);
    verify(mNode).viewTags(viewTags);

    verify(mNode).shadowElevationPx(60);

    verify(mNode).clipToOutline(false);
    verify(mNode).transitionKey("transitionKey");
    verify(mNode).testKey("testKey");

    verify(mNode).accessibilityRole(AccessibilityRole.BUTTON);
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

    verify(mNode).stateListAnimator(stateListAnimator);
  }
}
