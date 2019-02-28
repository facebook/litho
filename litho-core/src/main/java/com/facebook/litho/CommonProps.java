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

import android.animation.StateListAnimator;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.StyleRes;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

/** Common props that are accessible outside of the framework. */
public interface CommonProps extends CommonPropsCopyable {

  @Nullable
  EventHandler<ClickEvent> getClickHandler();

  @Nullable
  EventHandler<LongClickEvent> getLongClickHandler();

  @Nullable
  EventHandler<FocusChangedEvent> getFocusChangeHandler();

  @Nullable
  EventHandler<TouchEvent> getTouchHandler();

  @Nullable
  EventHandler<InterceptTouchEvent> getInterceptTouchHandler();

  boolean getFocusable();

  @Nullable
  String getTransitionKey();

  void setStyle(@AttrRes int defStyleAttr, @StyleRes int defStyleRes);

  void positionType(@Nullable YogaPositionType positionType);

  void positionPx(YogaEdge edge, @Px int position);

  void widthPx(@Px int width);

  void heightPx(@Px int height);

  void background(@Nullable Reference<? extends Drawable> background);

  void testKey(String testKey);

  void wrapInView();

  void layoutDirection(@Nullable YogaDirection direction);

  void alignSelf(@Nullable YogaAlign alignSelf);

  void flex(float flex);

  void flexGrow(float flexGrow);

  void flexShrink(float flexShrink);

  void flexBasisPx(@Px int flexBasis);

  void flexBasisPercent(float percent);

  void importantForAccessibility(int importantForAccessibility);

  void duplicateParentState(boolean duplicateParentState);

  void marginPx(YogaEdge edge, @Px int margin);

  void marginPercent(YogaEdge edge, float percent);

  void marginAuto(YogaEdge edge);

  void paddingPx(YogaEdge edge, @Px int padding);

  void paddingPercent(YogaEdge edge, float percent);

  void border(Border border);

  void stateListAnimator(@Nullable StateListAnimator stateListAnimator);

  void stateListAnimatorRes(@DrawableRes int resId);

  void positionPercent(@Nullable YogaEdge edge, float percent);

  void widthPercent(float percent);

  void minWidthPx(@Px int minWidth);

  void minWidthPercent(float percent);

  void maxWidthPx(@Px int maxWidth);

  void maxWidthPercent(float percent);

  void heightPercent(float percent);

  void minHeightPx(@Px int minHeight);

  void minHeightPercent(float percent);

  void maxHeightPx(@Px int maxHeight);

  void maxHeightPercent(float percent);

  void aspectRatio(float aspectRatio);

  void isReferenceBaseline(boolean isReferenceBaseline);

  void touchExpansionPx(@Nullable YogaEdge edge, @Px int touchExpansion);

  void foreground(@Nullable ComparableDrawable foreground);

  void clickHandler(EventHandler<ClickEvent> clickHandler);

  @Nullable
  Reference<? extends Drawable> getBackground();

  void longClickHandler(EventHandler<LongClickEvent> longClickHandler);

  void focusChangeHandler(EventHandler<FocusChangedEvent> focusChangeHandler);

  void touchHandler(EventHandler<TouchEvent> touchHandler);

  void interceptTouchHandler(EventHandler<InterceptTouchEvent> interceptTouchHandler);

  void focusable(boolean isFocusable);

  void enabled(boolean isEnabled);

  void selected(boolean isSelected);

  void visibleHeightRatio(float visibleHeightRatio);

  void visibleWidthRatio(float visibleWidthRatio);

  void visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler);

  void focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler);

  void unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler);

  void fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler);

  void invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler);

  void visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler);

  void contentDescription(@Nullable CharSequence contentDescription);

  void viewTag(@Nullable Object viewTag);

  void viewTags(@Nullable SparseArray<Object> viewTags);

  void shadowElevationPx(float shadowElevation);

  void outlineProvider(@Nullable ViewOutlineProvider outlineProvider);

  void clipToOutline(boolean clipToOutline);

  void clipChildren(boolean clipChildren);

  void accessibilityRole(@Nullable @AccessibilityRole.AccessibilityRoleType String role);

  void accessibilityRoleDescription(@Nullable CharSequence roleDescription);

  void dispatchPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<DispatchPopulateAccessibilityEventEvent>
              dispatchPopulateAccessibilityEventHandler);

  void onInitializeAccessibilityEventHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler);

  void onInitializeAccessibilityNodeInfoHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>
              onInitializeAccessibilityNodeInfoHandler);

  void onPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler);

  void onRequestSendAccessibilityEventHandler(
      @Nullable
          EventHandler<OnRequestSendAccessibilityEventEvent>
              onRequestSendAccessibilityEventHandler);

  void performAccessibilityActionHandler(
      @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler);

  void sendAccessibilityEventHandler(
      @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler);

  void sendAccessibilityEventUncheckedHandler(
      @Nullable
          EventHandler<SendAccessibilityEventUncheckedEvent>
              sendAccessibilityEventUncheckedHandler);

  void scale(float scale);

  void alpha(float alpha);

  void rotation(float rotation);

  void transitionKey(@Nullable String key);

  void transitionKeyType(@Nullable Transition.TransitionKeyType type);

  @Nullable
  public Transition.TransitionKeyType getTransitionKeyType();

  void useHeightAsBaseline(boolean useHeightAsBaseline);

  @Nullable
  NodeInfo getNullableNodeInfo();

  NodeInfo getOrCreateNodeInfo();
}
