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
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.AccessibilityRole.AccessibilityRoleType;

/**
 * SparseNodeInfo holds props in a {@link SparseArray} to optimize for memory. This is under the
 * assumption that the number of props which will be set for a component is very less than half.
 */
@ThreadConfined(ThreadConfined.ANY)
class SparseNodeInfo implements NodeInfo {

  // Flags to track if properties have been set
  private static final int PFLAG_CONTENT_DESCRIPTION_IS_SET = 1;
  private static final int PFLAG_VIEW_TAG_IS_SET = 1 << 1;
  private static final int PFLAG_VIEW_TAGS_IS_SET = 1 << 2;
  private static final int PFLAG_CLICK_HANDLER_IS_SET = 1 << 3;
  private static final int PFLAG_LONG_CLICK_HANDLER_IS_SET = 1 << 4;
  private static final int PFLAG_TOUCH_HANDLER_IS_SET = 1 << 5;
  private static final int PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 6;
  private static final int PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 7;
  private static final int PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET = 1 << 8;
  private static final int PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 9;
  private static final int PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 10;
  private static final int PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET = 1 << 11;
  private static final int PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 12;
  private static final int PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET = 1 << 13;
  private static final int PFLAG_SHADOW_ELEVATION_IS_SET = 1 << 14;
  private static final int PFLAG_OUTINE_PROVIDER_IS_SET = 1 << 15;
  private static final int PFLAG_CLIP_TO_OUTLINE_IS_SET = 1 << 16;
  private static final int PFLAG_FOCUS_CHANGE_HANDLER_IS_SET = 1 << 17;
  private static final int PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET = 1 << 18;
  private static final int PFLAG_SCALE_IS_SET = 1 << 19;
  private static final int PFLAG_ALPHA_IS_SET = 1 << 20;
  private static final int PFLAG_ROTATION_IS_SET = 1 << 21;
  private static final int PFLAG_ACCESSIBILITY_ROLE_IS_SET = 1 << 22;
  private static final int PFLAG_CLIP_CHILDREN_IS_SET = 1 << 23;
  private static final int PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET = 1 << 24;
  private static final int PFLAG_ROTATION_X_IS_SET = 1 << 25;
  private static final int PFLAG_ROTATION_Y_IS_SET = 1 << 26;

  // Flags to track if properties were set to non null values
  private static final int OFLAG_HAS_FOCUS_CHANGE_HANDLER = 1;
  private static final int OFLAG_HAS_CLICK_HANDLER = 1 << 1;
  private static final int OFLAG_HAS_LONG_CLICK_HANDLER = 1 << 2;
  private static final int OFLAG_HAS_TOUCH_HANDLER = 1 << 3;
  private static final int OFLAG_HAS_INTERCEPT_TOUCH_HANDLER = 1 << 4;
  private static final int OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER = 1 << 5;
  private static final int OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER = 1 << 6;
  private static final int OFLAG_HAS_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER = 1 << 7;
  private static final int OFLAG_HAS_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER = 1 << 8;
  private static final int OFLAG_HAS_PERFORM_ACCESSIBILITY_ACTION_HANDLER = 1 << 9;
  private static final int OFLAG_HAS_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER = 1 << 10;
  private static final int OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_HANDLER = 1 << 11;
  private static final int OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER = 1 << 12;
  private static final int OFLAG_HAS_ACCESSIBILITY_ROLE = 1 << 13;
  private static final int OFLAG_HAS_ACCESSIBILITY_ROLE_DESCRIPTION = 1 << 14;

  // Index of the properties
  private static final int INDEX_ViewTag = 0;
  private static final int INDEX_ViewTags = 1;
  private static final int INDEX_OutlineProvider = 2;
  private static final int INDEX_ContentDescription = 3;
  private static final int INDEX_AccessibilityRoleDescription = 4;
  private static final int INDEX_AccessibilityRole = 5;
  private static final int INDEX_ClickHandler = 6;
  private static final int INDEX_FocusChangeHandler = 7;
  private static final int INDEX_LongClickHandler = 8;
  private static final int INDEX_TouchHandler = 9;
  private static final int INDEX_InterceptTouchHandler = 10;
  private static final int INDEX_DispatchPopulateAccessibilityEventHandler = 11;
  private static final int INDEX_OnInitializeAccessibilityEventHandler = 12;
  private static final int INDEX_OnPopulateAccessibilityEventHandler = 13;
  private static final int INDEX_OnInitializeAccessibilityNodeInfoHandler = 14;
  private static final int INDEX_OnRequestSendAccessibilityEventHandler = 15;
  private static final int INDEX_PerformAccessibilityActionHandler = 16;
  private static final int INDEX_SendAccessibilityEventHandler = 17;
  private static final int INDEX_SendAccessibilityEventUncheckedHandler = 18;

  private @Nullable SparseArray<Object> mObjectProps;

  private float mScale = 1;
  private float mAlpha = 1;
  private float mRotation = 0;
  private float mRotationX = 0;
  private float mRotationY = 0;
  private float mShadowElevation = 0;

  private @FocusState int mFocusState = FOCUS_UNSET;
  private @ClickableState int mClickableState = CLICKABLE_UNSET;
  private @EnabledState int mEnabledState = ENABLED_UNSET;
  private @SelectedState int mSelectedState = SELECTED_UNSET;

  private boolean mClipToOutline;
  private boolean mClipChildren = true;

  private int mPrivateFlags;
  private int mOtherFlags;

  @Override
  public void setContentDescription(@Nullable CharSequence contentDescription) {
    mPrivateFlags |= PFLAG_CONTENT_DESCRIPTION_IS_SET;
    getOrCreateObjectProps().append(INDEX_ContentDescription, contentDescription);
  }

  @Override
  public @Nullable CharSequence getContentDescription() {
    return (mPrivateFlags & PFLAG_CONTENT_DESCRIPTION_IS_SET) != 0
        ? (CharSequence) getOrCreateObjectProps().get(INDEX_ContentDescription)
        : null;
  }

  @Override
  public void setViewTag(@Nullable Object viewTag) {
    mPrivateFlags |= PFLAG_VIEW_TAG_IS_SET;
    getOrCreateObjectProps().append(INDEX_ViewTag, viewTag);
  }

  @Override
  public @Nullable Object getViewTag() {
    return (mPrivateFlags & PFLAG_VIEW_TAG_IS_SET) != 0
        ? getOrCreateObjectProps().get(INDEX_ViewTag)
        : null;
  }

  @Override
  public void setViewTags(@Nullable SparseArray<Object> viewTags) {
    mPrivateFlags |= PFLAG_VIEW_TAGS_IS_SET;
    getOrCreateObjectProps().append(INDEX_ViewTags, viewTags);
  }

  @Override
  public @Nullable SparseArray<Object> getViewTags() {
    return (mPrivateFlags & PFLAG_VIEW_TAGS_IS_SET) != 0
        ? (SparseArray<Object>) getOrCreateObjectProps().get(INDEX_ViewTags)
        : null;
  }

  @Override
  public void setShadowElevation(float shadowElevation) {
    mPrivateFlags |= PFLAG_SHADOW_ELEVATION_IS_SET;
    mShadowElevation = shadowElevation;
  }

  @Override
  public float getShadowElevation() {
    return mShadowElevation;
  }

  @Override
  public void setOutlineProvider(@Nullable ViewOutlineProvider outlineProvider) {
    mPrivateFlags |= PFLAG_OUTINE_PROVIDER_IS_SET;
    getOrCreateObjectProps().append(INDEX_OutlineProvider, outlineProvider);
  }

  @Override
  public @Nullable ViewOutlineProvider getOutlineProvider() {
    return (mPrivateFlags & PFLAG_OUTINE_PROVIDER_IS_SET) != 0
        ? (ViewOutlineProvider) getOrCreateObjectProps().get(INDEX_OutlineProvider)
        : null;
  }

  @Override
  public void setClipToOutline(boolean clipToOutline) {
    mPrivateFlags |= PFLAG_CLIP_TO_OUTLINE_IS_SET;
    mClipToOutline = clipToOutline;
  }

  @Override
  public boolean getClipToOutline() {
    return mClipToOutline;
  }

  @Override
  public void setClipChildren(boolean clipChildren) {
    mPrivateFlags |= PFLAG_CLIP_CHILDREN_IS_SET;
    mClipChildren = clipChildren;
  }

  @Override
  public boolean getClipChildren() {
    return mClipChildren;
  }

  @Override
  public boolean isClipChildrenSet() {
    return (mPrivateFlags & PFLAG_CLIP_CHILDREN_IS_SET) != 0;
  }

  @Override
  public void setClickHandler(@Nullable EventHandler<ClickEvent> clickHandler) {
    mPrivateFlags |= PFLAG_CLICK_HANDLER_IS_SET;
    if (clickHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_CLICK_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_CLICK_HANDLER;
    }
    getOrCreateObjectProps().append(INDEX_ClickHandler, clickHandler);
  }

  @Override
  public @Nullable EventHandler<ClickEvent> getClickHandler() {
    return (mOtherFlags & OFLAG_HAS_CLICK_HANDLER) != 0
        ? (EventHandler<ClickEvent>) getOrCreateObjectProps().get(INDEX_ClickHandler)
        : null;
  }

  @Override
  public void setLongClickHandler(@Nullable EventHandler<LongClickEvent> longClickHandler) {
    mPrivateFlags |= PFLAG_LONG_CLICK_HANDLER_IS_SET;
    if (longClickHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_LONG_CLICK_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_LONG_CLICK_HANDLER;
    }
    getOrCreateObjectProps().append(INDEX_LongClickHandler, longClickHandler);
  }

  @Override
  public @Nullable EventHandler<LongClickEvent> getLongClickHandler() {
    return (mOtherFlags & OFLAG_HAS_LONG_CLICK_HANDLER) != 0
        ? (EventHandler<LongClickEvent>) getOrCreateObjectProps().get(INDEX_LongClickHandler)
        : null;
  }

  @Override
  public void setFocusChangeHandler(@Nullable EventHandler<FocusChangedEvent> focusChangedHandler) {
    mPrivateFlags |= PFLAG_FOCUS_CHANGE_HANDLER_IS_SET;
    if (focusChangedHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_FOCUS_CHANGE_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_FOCUS_CHANGE_HANDLER;
    }
    getOrCreateObjectProps().append(INDEX_FocusChangeHandler, focusChangedHandler);
  }

  @Override
  public @Nullable EventHandler<FocusChangedEvent> getFocusChangeHandler() {
    return hasFocusChangeHandler()
        ? (EventHandler<FocusChangedEvent>) getOrCreateObjectProps().get(INDEX_FocusChangeHandler)
        : null;
  }

  @Override
  public boolean hasFocusChangeHandler() {
    return (mOtherFlags & OFLAG_HAS_FOCUS_CHANGE_HANDLER) != 0;
  }

  @Override
  public void setTouchHandler(@Nullable EventHandler<TouchEvent> touchHandler) {
    mPrivateFlags |= PFLAG_TOUCH_HANDLER_IS_SET;
    if (touchHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_TOUCH_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_TOUCH_HANDLER;
    }
    getOrCreateObjectProps().append(INDEX_TouchHandler, touchHandler);
  }

  @Override
  public @Nullable EventHandler<TouchEvent> getTouchHandler() {
    return (mOtherFlags & OFLAG_HAS_TOUCH_HANDLER) != 0
        ? (EventHandler<TouchEvent>) getOrCreateObjectProps().get(INDEX_TouchHandler)
        : null;
  }

  @Override
  public void setInterceptTouchHandler(
      @Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    mPrivateFlags |= PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET;
    if (interceptTouchHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_INTERCEPT_TOUCH_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_INTERCEPT_TOUCH_HANDLER;
    }
    getOrCreateObjectProps().append(INDEX_InterceptTouchHandler, interceptTouchHandler);
  }

  @Override
  public @Nullable EventHandler<InterceptTouchEvent> getInterceptTouchHandler() {
    return (mOtherFlags & OFLAG_HAS_INTERCEPT_TOUCH_HANDLER) != 0
        ? (EventHandler<InterceptTouchEvent>)
            getOrCreateObjectProps().get(INDEX_InterceptTouchHandler)
        : null;
  }

  @Override
  public boolean hasTouchEventHandlers() {
    return (mOtherFlags
            & (OFLAG_HAS_CLICK_HANDLER
                | OFLAG_HAS_LONG_CLICK_HANDLER
                | OFLAG_HAS_TOUCH_HANDLER
                | OFLAG_HAS_INTERCEPT_TOUCH_HANDLER))
        != 0;
  }

  @Override
  public void setAccessibilityRole(@Nullable @AccessibilityRoleType String role) {
    mPrivateFlags |= PFLAG_ACCESSIBILITY_ROLE_IS_SET;
    if (role == null) {
      mOtherFlags &= ~OFLAG_HAS_ACCESSIBILITY_ROLE;
    } else {
      mOtherFlags |= OFLAG_HAS_ACCESSIBILITY_ROLE;
    }
    getOrCreateObjectProps().append(INDEX_AccessibilityRole, role);
  }

  @Override
  public @Nullable @AccessibilityRoleType String getAccessibilityRole() {
    return (mOtherFlags & OFLAG_HAS_ACCESSIBILITY_ROLE) != 0
        ? (String) getOrCreateObjectProps().get(INDEX_AccessibilityRole)
        : null;
  }

  @Override
  public void setAccessibilityRoleDescription(@Nullable CharSequence roleDescription) {
    mPrivateFlags |= PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET;
    if (roleDescription == null) {
      mOtherFlags &= ~OFLAG_HAS_ACCESSIBILITY_ROLE_DESCRIPTION;
    } else {
      mOtherFlags |= OFLAG_HAS_ACCESSIBILITY_ROLE_DESCRIPTION;
    }
    getOrCreateObjectProps().append(INDEX_AccessibilityRoleDescription, roleDescription);
  }

  @Override
  public @Nullable CharSequence getAccessibilityRoleDescription() {
    return (mOtherFlags & OFLAG_HAS_ACCESSIBILITY_ROLE_DESCRIPTION) != 0
        ? (CharSequence) getOrCreateObjectProps().get(INDEX_AccessibilityRoleDescription)
        : null;
  }

  @Override
  public void setDispatchPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<DispatchPopulateAccessibilityEventEvent>
              dispatchPopulateAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    if (dispatchPopulateAccessibilityEventHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER;
    }
    getOrCreateObjectProps()
        .append(
            INDEX_DispatchPopulateAccessibilityEventHandler,
            dispatchPopulateAccessibilityEventHandler);
  }

  @Override
  public @Nullable EventHandler<DispatchPopulateAccessibilityEventEvent>
      getDispatchPopulateAccessibilityEventHandler() {
    return (mOtherFlags & OFLAG_HAS_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER) != 0
        ? (EventHandler<DispatchPopulateAccessibilityEventEvent>)
            getOrCreateObjectProps().get(INDEX_DispatchPopulateAccessibilityEventHandler)
        : null;
  }

  @Override
  public void setOnInitializeAccessibilityEventHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    if (onInitializeAccessibilityEventHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER;
    }
    getOrCreateObjectProps()
        .append(INDEX_OnInitializeAccessibilityEventHandler, onInitializeAccessibilityEventHandler);
  }

  @Override
  public @Nullable EventHandler<OnInitializeAccessibilityEventEvent>
      getOnInitializeAccessibilityEventHandler() {
    return (mOtherFlags & OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER) != 0
        ? (EventHandler<OnInitializeAccessibilityEventEvent>)
            getOrCreateObjectProps().get(INDEX_OnInitializeAccessibilityEventHandler)
        : null;
  }

  @Override
  public void setOnInitializeAccessibilityNodeInfoHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>
              onInitializeAccessibilityNodeInfoHandler) {
    mPrivateFlags |= PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET;
    if (onInitializeAccessibilityNodeInfoHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER;
    }
    getOrCreateObjectProps()
        .append(
            INDEX_OnInitializeAccessibilityNodeInfoHandler,
            onInitializeAccessibilityNodeInfoHandler);
  }

  @Override
  public @Nullable EventHandler<OnInitializeAccessibilityNodeInfoEvent>
      getOnInitializeAccessibilityNodeInfoHandler() {
    return (mOtherFlags & OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER) != 0
        ? (EventHandler<OnInitializeAccessibilityNodeInfoEvent>)
            getOrCreateObjectProps().get(INDEX_OnInitializeAccessibilityNodeInfoHandler)
        : null;
  }

  @Override
  public void setOnPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    if (onPopulateAccessibilityEventHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER;
    }
    getOrCreateObjectProps()
        .append(INDEX_OnPopulateAccessibilityEventHandler, onPopulateAccessibilityEventHandler);
  }

  @Override
  public @Nullable EventHandler<OnPopulateAccessibilityEventEvent>
      getOnPopulateAccessibilityEventHandler() {
    return (mOtherFlags & OFLAG_HAS_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER) != 0
        ? (EventHandler<OnPopulateAccessibilityEventEvent>)
            getOrCreateObjectProps().get(INDEX_OnPopulateAccessibilityEventHandler)
        : null;
  }

  @Override
  public void setOnRequestSendAccessibilityEventHandler(
      @Nullable
          EventHandler<OnRequestSendAccessibilityEventEvent>
              onRequestSendAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    if (onRequestSendAccessibilityEventHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER;
    }
    getOrCreateObjectProps()
        .append(
            INDEX_OnRequestSendAccessibilityEventHandler, onRequestSendAccessibilityEventHandler);
  }

  @Override
  public @Nullable EventHandler<OnRequestSendAccessibilityEventEvent>
      getOnRequestSendAccessibilityEventHandler() {
    return (mOtherFlags & OFLAG_HAS_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER) != 0
        ? (EventHandler<OnRequestSendAccessibilityEventEvent>)
            getOrCreateObjectProps().get(INDEX_OnRequestSendAccessibilityEventHandler)
        : null;
  }

  @Override
  public void setPerformAccessibilityActionHandler(
      @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    mPrivateFlags |= PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET;
    if (performAccessibilityActionHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_PERFORM_ACCESSIBILITY_ACTION_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_PERFORM_ACCESSIBILITY_ACTION_HANDLER;
    }
    getOrCreateObjectProps()
        .append(INDEX_PerformAccessibilityActionHandler, performAccessibilityActionHandler);
  }

  @Override
  public @Nullable EventHandler<PerformAccessibilityActionEvent>
      getPerformAccessibilityActionHandler() {
    return (mOtherFlags & OFLAG_HAS_PERFORM_ACCESSIBILITY_ACTION_HANDLER) != 0
        ? (EventHandler<PerformAccessibilityActionEvent>)
            getOrCreateObjectProps().get(INDEX_PerformAccessibilityActionHandler)
        : null;
  }

  @Override
  public void setSendAccessibilityEventHandler(
      @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    if (sendAccessibilityEventHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_HANDLER;
    }
    getOrCreateObjectProps()
        .append(INDEX_SendAccessibilityEventHandler, sendAccessibilityEventHandler);
  }

  @Override
  public @Nullable EventHandler<SendAccessibilityEventEvent> getSendAccessibilityEventHandler() {
    return (mOtherFlags & OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_HANDLER) != 0
        ? (EventHandler<SendAccessibilityEventEvent>)
            getOrCreateObjectProps().get(INDEX_SendAccessibilityEventHandler)
        : null;
  }

  @Override
  public void setSendAccessibilityEventUncheckedHandler(
      @Nullable
          EventHandler<SendAccessibilityEventUncheckedEvent>
              sendAccessibilityEventUncheckedHandler) {
    mPrivateFlags |= PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET;
    if (sendAccessibilityEventUncheckedHandler == null) {
      mOtherFlags &= ~OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER;
    } else {
      mOtherFlags |= OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER;
    }
    getOrCreateObjectProps()
        .append(
            INDEX_SendAccessibilityEventUncheckedHandler, sendAccessibilityEventUncheckedHandler);
  }

  @Override
  public @Nullable EventHandler<SendAccessibilityEventUncheckedEvent>
      getSendAccessibilityEventUncheckedHandler() {
    return (mOtherFlags & OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER) != 0
        ? (EventHandler<SendAccessibilityEventUncheckedEvent>)
            getOrCreateObjectProps().get(INDEX_SendAccessibilityEventUncheckedHandler)
        : null;
  }

  @Override
  public boolean needsAccessibilityDelegate() {
    return (mOtherFlags
            & (OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER
                | OFLAG_HAS_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER
                | OFLAG_HAS_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER
                | OFLAG_HAS_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER
                | OFLAG_HAS_PERFORM_ACCESSIBILITY_ACTION_HANDLER
                | OFLAG_HAS_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER
                | OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_HANDLER
                | OFLAG_HAS_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER
                | OFLAG_HAS_ACCESSIBILITY_ROLE
                | OFLAG_HAS_ACCESSIBILITY_ROLE_DESCRIPTION))
        != 0;
  }

  @Override
  public void setFocusable(boolean isFocusable) {
    if (isFocusable) {
      mFocusState = FOCUS_SET_TRUE;
    } else {
      mFocusState = FOCUS_SET_FALSE;
    }
  }

  @Override
  public @FocusState int getFocusState() {
    return mFocusState;
  }

  @Override
  public void setClickable(boolean isClickable) {
    if (isClickable) {
      mClickableState = ENABLED_SET_TRUE;
    } else {
      mClickableState = ENABLED_SET_FALSE;
    }
  }

  @Override
  public @ClickableState int getClickableState() {
    return mClickableState;
  }

  @Override
  public void setEnabled(boolean isEnabled) {
    if (isEnabled) {
      mEnabledState = ENABLED_SET_TRUE;
    } else {
      mEnabledState = ENABLED_SET_FALSE;
    }
  }

  @Override
  public @EnabledState int getEnabledState() {
    return mEnabledState;
  }

  @Override
  public void setSelected(boolean isSelected) {
    if (isSelected) {
      mSelectedState = SELECTED_SET_TRUE;
    } else {
      mSelectedState = SELECTED_SET_FALSE;
    }
  }

  @Override
  public @SelectedState int getSelectedState() {
    return mSelectedState;
  }

  @Override
  public float getScale() {
    return mScale;
  }

  @Override
  public void setScale(float scale) {
    mScale = scale;
    mPrivateFlags |= PFLAG_SCALE_IS_SET;
  }

  @Override
  public boolean isScaleSet() {
    return (mPrivateFlags & PFLAG_SCALE_IS_SET) != 0;
  }

  @Override
  public float getAlpha() {
    return mAlpha;
  }

  @Override
  public void setAlpha(float alpha) {
    mAlpha = alpha;
    mPrivateFlags |= PFLAG_ALPHA_IS_SET;
  }

  @Override
  public boolean isAlphaSet() {
    return (mPrivateFlags & PFLAG_ALPHA_IS_SET) != 0;
  }

  @Override
  public float getRotation() {
    return mRotation;
  }

  @Override
  public void setRotation(float rotation) {
    mRotation = rotation;
    mPrivateFlags |= PFLAG_ROTATION_IS_SET;
  }

  @Override
  public boolean isRotationSet() {
    return (mPrivateFlags & PFLAG_ROTATION_IS_SET) != 0;
  }

  @Override
  public float getRotationX() {
    return mRotationX;
  }

  @Override
  public void setRotationX(float rotationX) {
    mRotationX = rotationX;
    mPrivateFlags |= PFLAG_ROTATION_X_IS_SET;
  }

  @Override
  public boolean isRotationXSet() {
    return (mPrivateFlags & PFLAG_ROTATION_X_IS_SET) != 0;
  }

  @Override
  public float getRotationY() {
    return mRotationY;
  }

  @Override
  public void setRotationY(float rotationY) {
    mRotationY = rotationY;
    mPrivateFlags |= PFLAG_ROTATION_Y_IS_SET;
  }

  @Override
  public boolean isRotationYSet() {
    return (mPrivateFlags & PFLAG_ROTATION_Y_IS_SET) != 0;
  }

  /**
   * Checks if this NodeInfo is equal to the {@param other}
   *
   * @param other the other NodeInfo
   * @return {@code true} iff this NodeInfo is equal to the {@param other}.
   */
  @Override
  public boolean isEquivalentTo(@Nullable NodeInfo other) {
    return NodeInfoUtils.isEquivalentTo(this, other);
  }

  @Override
  public void copyInto(NodeInfo target) {
    if ((mPrivateFlags & PFLAG_CLICK_HANDLER_IS_SET) != 0) {
      target.setClickHandler(getClickHandler());
    }
    if ((mPrivateFlags & PFLAG_LONG_CLICK_HANDLER_IS_SET) != 0) {
      target.setLongClickHandler(getLongClickHandler());
    }
    if ((mPrivateFlags & PFLAG_FOCUS_CHANGE_HANDLER_IS_SET) != 0) {
      target.setFocusChangeHandler(getFocusChangeHandler());
    }
    if ((mPrivateFlags & PFLAG_TOUCH_HANDLER_IS_SET) != 0) {
      target.setTouchHandler(getTouchHandler());
    }
    if ((mPrivateFlags & PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET) != 0) {
      target.setInterceptTouchHandler(getInterceptTouchHandler());
    }
    if ((mPrivateFlags & PFLAG_ACCESSIBILITY_ROLE_IS_SET) != 0) {
      target.setAccessibilityRole(getAccessibilityRole());
    }
    if ((mPrivateFlags & PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET) != 0) {
      target.setAccessibilityRoleDescription(getAccessibilityRoleDescription());
    }
    if ((mPrivateFlags & PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setDispatchPopulateAccessibilityEventHandler(
          getDispatchPopulateAccessibilityEventHandler());
    }
    if ((mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setOnInitializeAccessibilityEventHandler(getOnInitializeAccessibilityEventHandler());
    }
    if ((mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET) != 0) {
      target.setOnInitializeAccessibilityNodeInfoHandler(
          getOnInitializeAccessibilityNodeInfoHandler());
    }
    if ((mPrivateFlags & PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setOnPopulateAccessibilityEventHandler(getOnPopulateAccessibilityEventHandler());
    }
    if ((mPrivateFlags & PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setOnRequestSendAccessibilityEventHandler(getOnRequestSendAccessibilityEventHandler());
    }
    if ((mPrivateFlags & PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET) != 0) {
      target.setPerformAccessibilityActionHandler(getPerformAccessibilityActionHandler());
    }
    if ((mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setSendAccessibilityEventHandler(getSendAccessibilityEventHandler());
    }
    if ((mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET) != 0) {
      target.setSendAccessibilityEventUncheckedHandler(getSendAccessibilityEventUncheckedHandler());
    }
    if ((mPrivateFlags & PFLAG_CONTENT_DESCRIPTION_IS_SET) != 0) {
      target.setContentDescription(getContentDescription());
    }
    if ((mPrivateFlags & PFLAG_SHADOW_ELEVATION_IS_SET) != 0) {
      target.setShadowElevation(mShadowElevation);
    }
    if ((mPrivateFlags & PFLAG_OUTINE_PROVIDER_IS_SET) != 0) {
      target.setOutlineProvider(getOutlineProvider());
    }
    if ((mPrivateFlags & PFLAG_CLIP_TO_OUTLINE_IS_SET) != 0) {
      target.setClipToOutline(mClipToOutline);
    }
    if ((mPrivateFlags & PFLAG_CLIP_CHILDREN_IS_SET) != 0) {
      target.setClipChildren(mClipChildren);
    }
    if ((mPrivateFlags & PFLAG_VIEW_TAG_IS_SET) != 0) {
      target.setViewTag(getViewTag());
    }
    if ((mPrivateFlags & PFLAG_VIEW_TAGS_IS_SET) != 0) {
      target.setViewTags(getViewTags());
    }
    if (getFocusState() != FOCUS_UNSET) {
      target.setFocusable(getFocusState() == FOCUS_SET_TRUE);
    }
    if (getEnabledState() != ENABLED_UNSET) {
      target.setEnabled(getEnabledState() == ENABLED_SET_TRUE);
    }
    if (getSelectedState() != SELECTED_UNSET) {
      target.setSelected(getSelectedState() == SELECTED_SET_TRUE);
    }
    if ((mPrivateFlags & PFLAG_SCALE_IS_SET) != 0) {
      target.setScale(mScale);
    }
    if ((mPrivateFlags & PFLAG_ALPHA_IS_SET) != 0) {
      target.setAlpha(mAlpha);
    }
    if ((mPrivateFlags & PFLAG_ROTATION_IS_SET) != 0) {
      target.setRotation(mRotation);
    }
    if ((mPrivateFlags & PFLAG_ROTATION_X_IS_SET) != 0) {
      target.setRotationX(mRotationX);
    }
    if ((mPrivateFlags & PFLAG_ROTATION_Y_IS_SET) != 0) {
      target.setRotationY(mRotationY);
    }
  }

  @Override
  public int getFlags() {
    return mPrivateFlags;
  }

  private SparseArray<Object> getOrCreateObjectProps() {
    if (mObjectProps == null) {
      mObjectProps = new SparseArray<>(2);
    }

    return mObjectProps;
  }
}
