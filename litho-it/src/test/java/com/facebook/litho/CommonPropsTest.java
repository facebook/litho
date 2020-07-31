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
import static com.facebook.litho.it.R.drawable.background_with_padding;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseArray;
import androidx.core.content.ContextCompat;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

@RunWith(LithoTestRunner.class)
public class CommonPropsTest {

  private InternalNode mNode;
  private NodeInfo mNodeInfo;
  private CommonProps mCommonProps;
  private ComponentContext mComponentContext;

  @Before
  public void setup() {
    mNode = mock(InternalNode.class);
    mNodeInfo = mock(NodeInfo.class);
    when(mNode.getOrCreateNodeInfo()).thenReturn(mNodeInfo);
    mCommonProps = new CommonPropsHolder();
    mComponentContext = new ComponentContext(getApplicationContext());
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
    Drawable background = ComparableColorDrawable.create(Color.RED);
    mCommonProps.background(background);
    Drawable foreground = ComparableColorDrawable.create(Color.BLACK);
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
    mCommonProps.clickable(true);
    mCommonProps.selected(false);
    mCommonProps.enabled(false);
    mCommonProps.visibleHeightRatio(55);
    mCommonProps.visibleWidthRatio(56);
    mCommonProps.accessibilityHeading(false);

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
    mCommonProps.transitionKey("transitionKey", "");
    mCommonProps.testKey("testKey");

    final Object componentTag = new Object();
    mCommonProps.componentTag(componentTag);

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
    mCommonProps.accessibilityRoleDescription("Test Role Description");
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

    verify(mNode).border((Border) any());

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

    verify(mNode).background(background);
    verify(mNode).foreground(foreground);

    verify(mNode).wrapInView();

    verify(mNodeInfo).setClickHandler(clickHandler);
    verify(mNodeInfo).setFocusChangeHandler(focusChangedHandler);
    verify(mNodeInfo).setLongClickHandler(longClickHandler);
    verify(mNodeInfo).setTouchHandler(touchHandler);
    verify(mNodeInfo).setInterceptTouchHandler(interceptTouchHandler);

    verify(mNodeInfo).setFocusable(true);
    verify(mNodeInfo).setClickable(true);
    verify(mNodeInfo).setSelected(false);
    verify(mNodeInfo).setEnabled(false);
    verify(mNode).visibleHeightRatio(55);
    verify(mNode).visibleWidthRatio(56);
    verify(mNodeInfo).setAccessibilityHeading(false);

    verify(mNode).visibleHandler(visibleHandler);
    verify(mNode).focusedHandler(focusedHandler);
    verify(mNode).unfocusedHandler(unfocusedHandler);
    verify(mNode).fullImpressionHandler(fullImpressionHandler);
    verify(mNode).invisibleHandler(invisibleHandler);
    verify(mNode).visibilityChangedHandler(visibleRectChangedHandler);

    verify(mNodeInfo).setContentDescription("test");

    verify(mNodeInfo).setViewTag(viewTag);
    verify(mNodeInfo).setViewTags(viewTags);

    verify(mNodeInfo).setShadowElevation(60);

    verify(mNodeInfo).setClipToOutline(false);
    verify(mNode).transitionKey(eq("transitionKey"), anyString());
    verify(mNode).testKey("testKey");
    verify(mNode).componentTag(componentTag);

    verify(mNodeInfo).setAccessibilityRole(AccessibilityRole.BUTTON);
    verify(mNodeInfo).setAccessibilityRoleDescription("Test Role Description");
    verify(mNodeInfo)
        .setDispatchPopulateAccessibilityEventHandler(dispatchPopulateAccessibilityEventHandler);
    verify(mNodeInfo)
        .setOnInitializeAccessibilityEventHandler(onInitializeAccessibilityEventHandler);
    verify(mNodeInfo)
        .setOnInitializeAccessibilityNodeInfoHandler(onInitializeAccessibilityNodeInfoHandler);
    verify(mNodeInfo).setOnPopulateAccessibilityEventHandler(onPopulateAccessibilityEventHandler);
    verify(mNodeInfo)
        .setOnRequestSendAccessibilityEventHandler(onRequestSendAccessibilityEventHandler);
    verify(mNodeInfo).setPerformAccessibilityActionHandler(performAccessibilityActionHandler);
    verify(mNodeInfo).setSendAccessibilityEventHandler(sendAccessibilityEventHandler);
    verify(mNodeInfo)
        .setSendAccessibilityEventUncheckedHandler(sendAccessibilityEventUncheckedHandler);

    verify(mNode).stateListAnimator(stateListAnimator);
  }

  @Test
  public void testSetScalePropsWrapsInView() {
    mCommonProps.scale(5);
    mCommonProps.copyInto(mComponentContext, mNode);

    verify(mNodeInfo).setScale(5);
    verify(mNode).wrapInView();
  }

  @Test
  public void testSetFullScalePropsDoesNotWrapInView() {
    mCommonProps.scale(0.5f);
    mCommonProps.scale(1f);
    mCommonProps.copyInto(mComponentContext, mNode);

    verify(mNodeInfo, never()).setScale(1f);
    verify(mNode, never()).wrapInView();
  }

  @Test
  public void testSetAlphaPropsWrapsInView() {
    mCommonProps.alpha(5);
    mCommonProps.copyInto(mComponentContext, mNode);

    verify(mNodeInfo).setAlpha(5);
    verify(mNode).wrapInView();
  }

  @Test
  public void testSetFullAlphaPropsDoesNotWrapInView() {
    mCommonProps.alpha(5);
    mCommonProps.alpha(1f);
    mCommonProps.copyInto(mComponentContext, mNode);

    verify(mNodeInfo, never()).setAlpha(anyFloat());
    verify(mNode, never()).wrapInView();
  }

  @Test
  public void testSetRotationPropsWrapsInView() {
    mCommonProps.rotation(5);
    mCommonProps.copyInto(mComponentContext, mNode);

    verify(mNodeInfo).setRotation(5);
    verify(mNode).wrapInView();
  }

  @Test
  public void testSetZeroRotationPropsDoesNotWrapInView() {
    mCommonProps.rotation(1f);
    mCommonProps.rotation(0f);
    mCommonProps.copyInto(mComponentContext, mNode);

    verify(mNodeInfo, never()).setRotation(anyFloat());
    verify(mNode, never()).wrapInView();
  }

  @Test
  public void testPaddingFromDrawable() {
    final InternalNode node = spy(new DefaultInternalNode(mComponentContext));

    mCommonProps.background(
        ContextCompat.getDrawable(mComponentContext.getAndroidContext(), background_with_padding));

    mCommonProps.copyInto(mComponentContext, node);

    verify(node).paddingPx(YogaEdge.LEFT, 48);
    verify(node).paddingPx(YogaEdge.TOP, 0);
    verify(node).paddingPx(YogaEdge.RIGHT, 0);
    verify(node).paddingPx(YogaEdge.BOTTOM, 0);
  }

  @Test
  public void testPaddingFromDrawableIsOverwritten() {
    final InternalNode node = spy(new DefaultInternalNode(mComponentContext));

    mCommonProps.background(
        ContextCompat.getDrawable(mComponentContext.getAndroidContext(), background_with_padding));
    mCommonProps.paddingPx(YogaEdge.LEFT, 0);
    mCommonProps.paddingPx(YogaEdge.TOP, 0);
    mCommonProps.paddingPx(YogaEdge.RIGHT, 0);
    mCommonProps.paddingPx(YogaEdge.BOTTOM, 0);

    mCommonProps.copyInto(mComponentContext, node);

    InOrder inOrder = Mockito.inOrder(node);
    inOrder.verify(node).paddingPx(YogaEdge.LEFT, 48);
    inOrder.verify(node).paddingPx(YogaEdge.LEFT, 0);

    verify(node, times(2)).paddingPx(YogaEdge.TOP, 0);
    verify(node, times(2)).paddingPx(YogaEdge.RIGHT, 0);
    verify(node, times(2)).paddingPx(YogaEdge.BOTTOM, 0);
  }
}
