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

import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.AccessibilityRole.AccessibilityRoleType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * NodeInfo holds information that are set to the {@link InternalNode} and needs to be used while
 * mounting a {@link MountItem} in {@link MountState}.
 */
@ThreadConfined(ThreadConfined.ANY)
public interface NodeInfo {

  int FOCUS_UNSET = 0;
  int FOCUS_SET_TRUE = 1;
  int FOCUS_SET_FALSE = 2;

  @IntDef({FOCUS_UNSET, FOCUS_SET_TRUE, FOCUS_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface FocusState {}

  int CLICKABLE_UNSET = 0;
  int CLICKABLE_SET_TRUE = 1;
  int CLICKABLE_SET_FALSE = 2;

  @IntDef({CLICKABLE_UNSET, CLICKABLE_SET_TRUE, CLICKABLE_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface ClickableState {}

  int ENABLED_UNSET = 0;
  int ENABLED_SET_TRUE = 1;
  int ENABLED_SET_FALSE = 2;

  @IntDef({ENABLED_UNSET, ENABLED_SET_TRUE, ENABLED_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface EnabledState {}

  int SELECTED_UNSET = 0;
  int SELECTED_SET_TRUE = 1;
  int SELECTED_SET_FALSE = 2;

  @IntDef({SELECTED_UNSET, SELECTED_SET_TRUE, SELECTED_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface SelectedState {}

  static final int ACCESSIBILITY_HEADING_UNSET = 0;
  static final int ACCESSIBILITY_HEADING_SET_TRUE = 1;
  static final int ACCESSIBILITY_HEADING_SET_FALSE = 2;

  @IntDef({
    ACCESSIBILITY_HEADING_UNSET,
    ACCESSIBILITY_HEADING_SET_TRUE,
    ACCESSIBILITY_HEADING_SET_FALSE
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface AccessibilityHeadingState {}

  void setContentDescription(@Nullable CharSequence contentDescription);

  @Nullable
  CharSequence getContentDescription();

  void setViewTag(@Nullable Object viewTag);

  @Nullable
  Object getViewTag();

  void setViewTags(@Nullable SparseArray<Object> viewTags);

  float getShadowElevation();

  void setShadowElevation(float shadowElevation);

  @Nullable
  ViewOutlineProvider getOutlineProvider();

  void setOutlineProvider(@Nullable ViewOutlineProvider outlineProvider);

  boolean getClipToOutline();

  void setClipToOutline(boolean clipToOutline);

  void setClipChildren(boolean clipChildren);

  boolean getClipChildren();

  boolean isClipChildrenSet();

  @Nullable
  SparseArray<Object> getViewTags();

  void setClickHandler(@Nullable EventHandler<ClickEvent> clickHandler);

  @Nullable
  EventHandler<ClickEvent> getClickHandler();

  void setLongClickHandler(@Nullable EventHandler<LongClickEvent> longClickHandler);

  @Nullable
  EventHandler<LongClickEvent> getLongClickHandler();

  void setFocusChangeHandler(@Nullable EventHandler<FocusChangedEvent> focusChangedHandler);

  @Nullable
  EventHandler<FocusChangedEvent> getFocusChangeHandler();

  boolean hasFocusChangeHandler();

  void setTouchHandler(@Nullable EventHandler<TouchEvent> touchHandler);

  @Nullable
  EventHandler<TouchEvent> getTouchHandler();

  void setInterceptTouchHandler(@Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler);

  @Nullable
  EventHandler<InterceptTouchEvent> getInterceptTouchHandler();

  boolean hasTouchEventHandlers();

  void setAccessibilityRole(@Nullable @AccessibilityRoleType String role);

  @Nullable
  @AccessibilityRoleType
  String getAccessibilityRole();

  void setAccessibilityRoleDescription(@Nullable CharSequence roleDescription);

  @Nullable
  CharSequence getAccessibilityRoleDescription();

  void setDispatchPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<DispatchPopulateAccessibilityEventEvent>
              dispatchPopulateAccessibilityEventHandler);

  @Nullable
  EventHandler<DispatchPopulateAccessibilityEventEvent>
      getDispatchPopulateAccessibilityEventHandler();

  void setOnInitializeAccessibilityEventHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler);

  @Nullable
  EventHandler<OnInitializeAccessibilityEventEvent> getOnInitializeAccessibilityEventHandler();

  void setOnInitializeAccessibilityNodeInfoHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>
              onInitializeAccessibilityNodeInfoHandler);

  @Nullable
  EventHandler<OnInitializeAccessibilityNodeInfoEvent>
      getOnInitializeAccessibilityNodeInfoHandler();

  void setOnPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler);

  @Nullable
  EventHandler<OnPopulateAccessibilityEventEvent> getOnPopulateAccessibilityEventHandler();

  void setOnRequestSendAccessibilityEventHandler(
      @Nullable
          EventHandler<OnRequestSendAccessibilityEventEvent>
              onRequestSendAccessibilityEventHandler);

  @Nullable
  EventHandler<OnRequestSendAccessibilityEventEvent> getOnRequestSendAccessibilityEventHandler();

  void setPerformAccessibilityActionHandler(
      @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler);

  @Nullable
  EventHandler<PerformAccessibilityActionEvent> getPerformAccessibilityActionHandler();

  void setSendAccessibilityEventHandler(
      @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler);

  @Nullable
  EventHandler<SendAccessibilityEventEvent> getSendAccessibilityEventHandler();

  void setSendAccessibilityEventUncheckedHandler(
      @Nullable
          EventHandler<SendAccessibilityEventUncheckedEvent>
              sendAccessibilityEventUncheckedHandler);

  @Nullable
  EventHandler<SendAccessibilityEventUncheckedEvent> getSendAccessibilityEventUncheckedHandler();

  boolean needsAccessibilityDelegate();

  void setFocusable(boolean isFocusable);

  @FocusState
  int getFocusState();

  void setClickable(boolean isClickable);

  @ClickableState
  int getClickableState();

  void setEnabled(boolean isEnabled);

  @EnabledState
  int getEnabledState();

  void setSelected(boolean isSelected);

  @SelectedState
  int getSelectedState();

  void setAccessibilityHeading(boolean isHeading);

  @AccessibilityHeadingState
  int getAccessibilityHeadingState();

  float getScale();

  void setScale(float scale);

  boolean isScaleSet();

  float getAlpha();

  void setAlpha(float alpha);

  boolean isAlphaSet();

  float getRotation();

  void setRotation(float rotation);

  boolean isRotationSet();

  float getRotationX();

  void setRotationX(float rotationX);

  boolean isRotationXSet();

  float getRotationY();

  void setRotationY(float rotationY);

  boolean isRotationYSet();

  /**
   * Checks if this NodeInfo is equal to the {@param other}
   *
   * @param other the other NodeInfo
   * @return {@code true} iff this NodeInfo is equal to the {@param other}.
   */
  boolean isEquivalentTo(@Nullable NodeInfo other);

  void copyInto(NodeInfo target);

  int getFlags();
}
