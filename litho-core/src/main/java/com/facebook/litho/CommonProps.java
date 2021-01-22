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

import android.animation.StateListAnimator;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.yoga.YogaEdge;

/** Common props that are accessible outside of the framework. */
@ThreadConfined(ThreadConfined.ANY)
public interface CommonProps extends CommonPropsCopyable, LayoutProps, Equivalence<CommonProps> {

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

  void background(@Nullable Drawable background);

  /**
   * Returns the test key associated with this Component or null if none is set. Note that this
   * method will not return the test key for Container Components that resolve into LayoutNodes or
   * the test keys for Components that are all resolved into a single InternalNode.
   */
  @Nullable
  String getTestKey();

  void testKey(String testKey);

  @Nullable
  Object getComponentTag();

  void componentTag(@Nullable Object componentTag);

  void wrapInView();

  void importantForAccessibility(int importantForAccessibility);

  void duplicateParentState(boolean duplicateParentState);

  void duplicateChildrenStates(boolean duplicateChildrenStates);

  void border(Border border);

  void stateListAnimator(@Nullable StateListAnimator stateListAnimator);

  void stateListAnimatorRes(@DrawableRes int resId);

  void touchExpansionPx(@Nullable YogaEdge edge, @Px int touchExpansion);

  void foreground(@Nullable Drawable foreground);

  void clickHandler(@Nullable EventHandler<ClickEvent> clickHandler);

  @Nullable
  Drawable getBackground();

  void longClickHandler(@Nullable EventHandler<LongClickEvent> longClickHandler);

  void focusChangeHandler(@Nullable EventHandler<FocusChangedEvent> focusChangeHandler);

  void touchHandler(@Nullable EventHandler<TouchEvent> touchHandler);

  void interceptTouchHandler(@Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler);

  void focusable(boolean isFocusable);

  void clickable(boolean isClickable);

  void enabled(boolean isEnabled);

  void selected(boolean isSelected);

  void accessibilityHeading(boolean isHeading);

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

  void transitionName(@Nullable String transitionName);

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

  void rotationX(float rotationX);

  void rotationY(float rotationY);

  void transitionKey(@Nullable String key, @Nullable String ownerKey);

  void transitionKeyType(@Nullable Transition.TransitionKeyType type);

  void layerType(@LayerType int type, Paint paint);

  @Nullable
  Transition.TransitionKeyType getTransitionKeyType();

  @Nullable
  NodeInfo getNullableNodeInfo();

  NodeInfo getOrCreateNodeInfo();
}
