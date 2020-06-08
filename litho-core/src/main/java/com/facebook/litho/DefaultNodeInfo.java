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

import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.AccessibilityRole.AccessibilityRoleType;

/**
 * NodeInfo holds information that are set to the {@link InternalNode} and needs to be used while
 * mounting a {@link MountItem} in {@link MountState}.
 */
@ThreadConfined(ThreadConfined.ANY)
class DefaultNodeInfo implements NodeInfo {

  // When this flag is set, contentDescription was explicitly set on this node.
  private static final int PFLAG_CONTENT_DESCRIPTION_IS_SET = 1 << 0;
  // When this flag is set, viewTag was explicitly set on this node.
  private static final int PFLAG_VIEW_TAG_IS_SET = 1 << 1;
  // When this flag is set, viewTags was explicitly set on this node.
  private static final int PFLAG_VIEW_TAGS_IS_SET = 1 << 2;
  // When this flag is set, clickHandler was explicitly set on this node.
  private static final int PFLAG_CLICK_HANDLER_IS_SET = 1 << 3;
  // When this flag is set, longClickHandler was explicitly set on this node.
  private static final int PFLAG_LONG_CLICK_HANDLER_IS_SET = 1 << 4;
  // When this flag is set, touchHandler was explicitly set on this node.
  private static final int PFLAG_TOUCH_HANDLER_IS_SET = 1 << 5;
  // When this flag is set, dispatchPopulateAccessibilityEventHandler
  // was explicitly set on this node.
  private static final int PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 6;
  // When this flag is set, onInitializeAccessibilityEventHandler was explicitly set on this node.
  private static final int PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 7;
  // When this flag is set, onInitializeAccessibilityNodeInfo was explicitly set on this node.
  private static final int PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET = 1 << 8;
  // When this flag is set, onPopulateAccessibilityEventHandler was explicitly set on this node
  private static final int PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 9;
  // When this flag is set, onRequestSendAccessibilityEventHandler was explicitly set on this node.
  private static final int PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 10;
  // When this flag is set, performAccessibilityActionHandler was explicitly set on this node.
  private static final int PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET = 1 << 11;
  // When this flag is set, sendAccessibilityEventHandler was explicitly set on this node.
  private static final int PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET = 1 << 12;
  // When this flag is set, sendAccessibilityEventUncheckedHandler was explicitly set on this node.
  private static final int PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET = 1 << 13;
  // When this flag is set, shadowElevation was explicitly set on this node.
  private static final int PFLAG_SHADOW_ELEVATION_IS_SET = 1 << 14;
  // When this flag is set, outlineProvider was explicitly set on this node.
  private static final int PFLAG_OUTINE_PROVIDER_IS_SET = 1 << 15;
  // When this flag is set, clipToOutline was explicitly set on this node.
  private static final int PFLAG_CLIP_TO_OUTLINE_IS_SET = 1 << 16;
  // When this flag is set, focusChangeHandler was explicitly set on this code.
  private static final int PFLAG_FOCUS_CHANGE_HANDLER_IS_SET = 1 << 17;
  // When this flag is set, interceptTouchHandler was explicitly set on this node.
  private static final int PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET = 1 << 18;
  private static final int PFLAG_SCALE_IS_SET = 1 << 19;
  private static final int PFLAG_ALPHA_IS_SET = 1 << 20;
  private static final int PFLAG_ROTATION_IS_SET = 1 << 21;
  private static final int PFLAG_ACCESSIBILITY_ROLE_IS_SET = 1 << 22;
  // When this flag is set, clipChildren was explicitly set on this node.
  private static final int PFLAG_CLIP_CHILDREN_IS_SET = 1 << 23;
  private static final int PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET = 1 << 24;
  private static final int PFLAG_ROTATION_X_IS_SET = 1 << 25;
  private static final int PFLAG_ROTATION_Y_IS_SET = 1 << 26;

  private @Nullable CharSequence mContentDescription;
  private @Nullable Object mViewTag;
  private @Nullable String mTransitionName;
  private @Nullable SparseArray<Object> mViewTags;
  private float mShadowElevation;
  private @Nullable ViewOutlineProvider mOutlineProvider;
  private boolean mClipToOutline;
  // Default value for ViewGroup
  private boolean mClipChildren = true;
  private float mScale = 1;
  private float mAlpha = 1;
  private float mRotation = 0;
  private float mRotationX = 0;
  private float mRotationY = 0;
  private @Nullable EventHandler<ClickEvent> mClickHandler;
  private @Nullable EventHandler<FocusChangedEvent> mFocusChangeHandler;
  private @Nullable EventHandler<LongClickEvent> mLongClickHandler;
  private @Nullable EventHandler<TouchEvent> mTouchHandler;
  private @Nullable EventHandler<InterceptTouchEvent> mInterceptTouchHandler;
  private @Nullable @AccessibilityRoleType String mAccessibilityRole;
  private @Nullable CharSequence mAccessibilityRoleDescription;
  private @Nullable EventHandler<DispatchPopulateAccessibilityEventEvent>
      mDispatchPopulateAccessibilityEventHandler;
  private @Nullable EventHandler<OnInitializeAccessibilityEventEvent>
      mOnInitializeAccessibilityEventHandler;
  private @Nullable EventHandler<OnPopulateAccessibilityEventEvent>
      mOnPopulateAccessibilityEventHandler;
  private @Nullable EventHandler<OnInitializeAccessibilityNodeInfoEvent>
      mOnInitializeAccessibilityNodeInfoHandler;
  private @Nullable EventHandler<OnRequestSendAccessibilityEventEvent>
      mOnRequestSendAccessibilityEventHandler;
  private @Nullable EventHandler<PerformAccessibilityActionEvent>
      mPerformAccessibilityActionHandler;
  private @Nullable EventHandler<SendAccessibilityEventEvent> mSendAccessibilityEventHandler;
  private @Nullable EventHandler<SendAccessibilityEventUncheckedEvent>
      mSendAccessibilityEventUncheckedHandler;
  private @FocusState int mFocusState = FOCUS_UNSET;
  private @ClickableState int mClickableState = CLICKABLE_UNSET;
  private @EnabledState int mEnabledState = ENABLED_UNSET;
  private @SelectedState int mSelectedState = SELECTED_UNSET;
  private @AccessibilityHeadingState int mAccessibilityHeadingState = ACCESSIBILITY_HEADING_UNSET;

  private int mPrivateFlags;

  @Override
  public void setContentDescription(@Nullable CharSequence contentDescription) {
    mPrivateFlags |= PFLAG_CONTENT_DESCRIPTION_IS_SET;
    mContentDescription = contentDescription;
  }

  @Override
  public @Nullable CharSequence getContentDescription() {
    return mContentDescription;
  }

  @Override
  public void setViewTag(@Nullable Object viewTag) {
    mPrivateFlags |= PFLAG_VIEW_TAG_IS_SET;
    mViewTag = viewTag;
  }

  @Override
  public void setTransitionName(@Nullable String transitionName) {
    mTransitionName = transitionName;
  }

  @Override
  public @Nullable String getTransitionName() {
    return mTransitionName;
  }

  @Override
  public @Nullable Object getViewTag() {
    return mViewTag;
  }

  @Override
  public void setViewTags(@Nullable SparseArray<Object> viewTags) {
    mPrivateFlags |= PFLAG_VIEW_TAGS_IS_SET;
    mViewTags = viewTags;
  }

  @Override
  public float getShadowElevation() {
    return mShadowElevation;
  }

  @Override
  public void setShadowElevation(float shadowElevation) {
    mPrivateFlags |= PFLAG_SHADOW_ELEVATION_IS_SET;
    mShadowElevation = shadowElevation;
  }

  @Override
  public @Nullable ViewOutlineProvider getOutlineProvider() {
    return mOutlineProvider;
  }

  @Override
  public void setOutlineProvider(@Nullable ViewOutlineProvider outlineProvider) {
    mPrivateFlags |= PFLAG_OUTINE_PROVIDER_IS_SET;
    mOutlineProvider = outlineProvider;
  }

  @Override
  public boolean getClipToOutline() {
    return mClipToOutline;
  }

  @Override
  public void setClipToOutline(boolean clipToOutline) {
    mPrivateFlags |= PFLAG_CLIP_TO_OUTLINE_IS_SET;
    mClipToOutline = clipToOutline;
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
  public @Nullable SparseArray<Object> getViewTags() {
    return mViewTags;
  }

  @Override
  public void setClickHandler(@Nullable EventHandler<ClickEvent> clickHandler) {
    mPrivateFlags |= PFLAG_CLICK_HANDLER_IS_SET;
    mClickHandler = clickHandler;
  }

  @Override
  public @Nullable EventHandler<ClickEvent> getClickHandler() {
    return mClickHandler;
  }

  @Override
  public void setLongClickHandler(@Nullable EventHandler<LongClickEvent> longClickHandler) {
    mPrivateFlags |= PFLAG_LONG_CLICK_HANDLER_IS_SET;
    mLongClickHandler = longClickHandler;
  }

  @Override
  public @Nullable EventHandler<LongClickEvent> getLongClickHandler() {
    return mLongClickHandler;
  }

  @Override
  public void setFocusChangeHandler(@Nullable EventHandler<FocusChangedEvent> focusChangedHandler) {
    mPrivateFlags |= PFLAG_FOCUS_CHANGE_HANDLER_IS_SET;
    mFocusChangeHandler = focusChangedHandler;
  }

  @Override
  public @Nullable EventHandler<FocusChangedEvent> getFocusChangeHandler() {
    return mFocusChangeHandler;
  }

  @Override
  public boolean hasFocusChangeHandler() {
    return mFocusChangeHandler != null;
  }

  @Override
  public void setTouchHandler(@Nullable EventHandler<TouchEvent> touchHandler) {
    mPrivateFlags |= PFLAG_TOUCH_HANDLER_IS_SET;
    mTouchHandler = touchHandler;
  }

  @Override
  public @Nullable EventHandler<TouchEvent> getTouchHandler() {
    return mTouchHandler;
  }

  @Override
  public void setInterceptTouchHandler(
      @Nullable EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    mPrivateFlags |= PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET;
    mInterceptTouchHandler = interceptTouchHandler;
  }

  @Override
  public @Nullable EventHandler<InterceptTouchEvent> getInterceptTouchHandler() {
    return mInterceptTouchHandler;
  }

  @Override
  public boolean hasTouchEventHandlers() {
    return mClickHandler != null
        || mLongClickHandler != null
        || mTouchHandler != null
        || mInterceptTouchHandler != null;
  }

  @Override
  public void setAccessibilityRole(@Nullable @AccessibilityRoleType String role) {
    mPrivateFlags |= PFLAG_ACCESSIBILITY_ROLE_IS_SET;
    mAccessibilityRole = role;
  }

  @Override
  public @Nullable @AccessibilityRoleType String getAccessibilityRole() {
    return mAccessibilityRole;
  }

  @Override
  public void setAccessibilityRoleDescription(@Nullable CharSequence roleDescription) {
    mPrivateFlags |= PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET;
    mAccessibilityRoleDescription = roleDescription;
  }

  @Override
  public @Nullable CharSequence getAccessibilityRoleDescription() {
    return mAccessibilityRoleDescription;
  }

  @Override
  public void setDispatchPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<DispatchPopulateAccessibilityEventEvent>
              dispatchPopulateAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mDispatchPopulateAccessibilityEventHandler = dispatchPopulateAccessibilityEventHandler;
  }

  @Override
  public @Nullable EventHandler<DispatchPopulateAccessibilityEventEvent>
      getDispatchPopulateAccessibilityEventHandler() {
    return mDispatchPopulateAccessibilityEventHandler;
  }

  @Override
  public void setOnInitializeAccessibilityEventHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mOnInitializeAccessibilityEventHandler = onInitializeAccessibilityEventHandler;
  }

  @Override
  public @Nullable EventHandler<OnInitializeAccessibilityEventEvent>
      getOnInitializeAccessibilityEventHandler() {
    return mOnInitializeAccessibilityEventHandler;
  }

  @Override
  public void setOnInitializeAccessibilityNodeInfoHandler(
      @Nullable
          EventHandler<OnInitializeAccessibilityNodeInfoEvent>
              onInitializeAccessibilityNodeInfoHandler) {
    mPrivateFlags |= PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET;
    mOnInitializeAccessibilityNodeInfoHandler = onInitializeAccessibilityNodeInfoHandler;
  }

  @Override
  public @Nullable EventHandler<OnInitializeAccessibilityNodeInfoEvent>
      getOnInitializeAccessibilityNodeInfoHandler() {
    return mOnInitializeAccessibilityNodeInfoHandler;
  }

  @Override
  public void setOnPopulateAccessibilityEventHandler(
      @Nullable
          EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mOnPopulateAccessibilityEventHandler = onPopulateAccessibilityEventHandler;
  }

  @Override
  public @Nullable EventHandler<OnPopulateAccessibilityEventEvent>
      getOnPopulateAccessibilityEventHandler() {
    return mOnPopulateAccessibilityEventHandler;
  }

  @Override
  public void setOnRequestSendAccessibilityEventHandler(
      @Nullable
          EventHandler<OnRequestSendAccessibilityEventEvent>
              onRequestSendAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mOnRequestSendAccessibilityEventHandler = onRequestSendAccessibilityEventHandler;
  }

  @Override
  public @Nullable EventHandler<OnRequestSendAccessibilityEventEvent>
      getOnRequestSendAccessibilityEventHandler() {
    return mOnRequestSendAccessibilityEventHandler;
  }

  @Override
  public void setPerformAccessibilityActionHandler(
      @Nullable EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    mPrivateFlags |= PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET;
    mPerformAccessibilityActionHandler = performAccessibilityActionHandler;
  }

  @Override
  public @Nullable EventHandler<PerformAccessibilityActionEvent>
      getPerformAccessibilityActionHandler() {
    return mPerformAccessibilityActionHandler;
  }

  @Override
  public void setSendAccessibilityEventHandler(
      @Nullable EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mSendAccessibilityEventHandler = sendAccessibilityEventHandler;
  }

  @Override
  public @Nullable EventHandler<SendAccessibilityEventEvent> getSendAccessibilityEventHandler() {
    return mSendAccessibilityEventHandler;
  }

  @Override
  public void setSendAccessibilityEventUncheckedHandler(
      @Nullable
          EventHandler<SendAccessibilityEventUncheckedEvent>
              sendAccessibilityEventUncheckedHandler) {
    mPrivateFlags |= PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET;
    mSendAccessibilityEventUncheckedHandler = sendAccessibilityEventUncheckedHandler;
  }

  @Override
  public @Nullable EventHandler<SendAccessibilityEventUncheckedEvent>
      getSendAccessibilityEventUncheckedHandler() {
    return mSendAccessibilityEventUncheckedHandler;
  }

  @Override
  public boolean needsAccessibilityDelegate() {
    return mOnInitializeAccessibilityEventHandler != null
        || mOnInitializeAccessibilityNodeInfoHandler != null
        || mOnPopulateAccessibilityEventHandler != null
        || mOnRequestSendAccessibilityEventHandler != null
        || mPerformAccessibilityActionHandler != null
        || mDispatchPopulateAccessibilityEventHandler != null
        || mSendAccessibilityEventHandler != null
        || mSendAccessibilityEventUncheckedHandler != null
        || mAccessibilityRole != null
        || mAccessibilityRoleDescription != null;
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
  public void setAccessibilityHeading(boolean isHeading) {
    if (isHeading) {
      mAccessibilityHeadingState = ACCESSIBILITY_HEADING_SET_TRUE;
    } else {
      mAccessibilityHeadingState = ACCESSIBILITY_HEADING_SET_FALSE;
    }
  }

  @Override
  public int getAccessibilityHeadingState() {
    return mAccessibilityHeadingState;
  }

  @Override
  public float getScale() {
    return mScale;
  }

  @Override
  public void setScale(float scale) {
    mScale = scale;
    if (scale == 1) {
      mPrivateFlags &= ~PFLAG_SCALE_IS_SET;
    } else {
      mPrivateFlags |= PFLAG_SCALE_IS_SET;
    }
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
    if (alpha == 1) {
      mPrivateFlags &= ~PFLAG_ALPHA_IS_SET;
    } else {
      mPrivateFlags |= PFLAG_ALPHA_IS_SET;
    }
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
    if (rotation == 0) {
      mPrivateFlags &= ~PFLAG_ROTATION_IS_SET;
    } else {
      mPrivateFlags |= PFLAG_ROTATION_IS_SET;
    }
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
      target.setClickHandler(mClickHandler);
    }
    if ((mPrivateFlags & PFLAG_LONG_CLICK_HANDLER_IS_SET) != 0) {
      target.setLongClickHandler(mLongClickHandler);
    }
    if ((mPrivateFlags & PFLAG_FOCUS_CHANGE_HANDLER_IS_SET) != 0) {
      target.setFocusChangeHandler(mFocusChangeHandler);
    }
    if ((mPrivateFlags & PFLAG_TOUCH_HANDLER_IS_SET) != 0) {
      target.setTouchHandler(mTouchHandler);
    }
    if ((mPrivateFlags & PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET) != 0) {
      target.setInterceptTouchHandler(mInterceptTouchHandler);
    }
    if ((mPrivateFlags & PFLAG_ACCESSIBILITY_ROLE_IS_SET) != 0) {
      target.setAccessibilityRole(mAccessibilityRole);
    }
    if ((mPrivateFlags & PFLAG_ACCESSIBILITY_ROLE_DESCRIPTION_IS_SET) != 0) {
      target.setAccessibilityRoleDescription(mAccessibilityRoleDescription);
    }
    if ((mPrivateFlags & PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setDispatchPopulateAccessibilityEventHandler(
          mDispatchPopulateAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setOnInitializeAccessibilityEventHandler(mOnInitializeAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET) != 0) {
      target.setOnInitializeAccessibilityNodeInfoHandler(mOnInitializeAccessibilityNodeInfoHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setOnPopulateAccessibilityEventHandler(mOnPopulateAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setOnRequestSendAccessibilityEventHandler(mOnRequestSendAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET) != 0) {
      target.setPerformAccessibilityActionHandler(mPerformAccessibilityActionHandler);
    }
    if ((mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      target.setSendAccessibilityEventHandler(mSendAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET) != 0) {
      target.setSendAccessibilityEventUncheckedHandler(mSendAccessibilityEventUncheckedHandler);
    }
    if ((mPrivateFlags & PFLAG_CONTENT_DESCRIPTION_IS_SET) != 0) {
      target.setContentDescription(mContentDescription);
    }
    if ((mPrivateFlags & PFLAG_SHADOW_ELEVATION_IS_SET) != 0) {
      target.setShadowElevation(mShadowElevation);
    }
    if ((mPrivateFlags & PFLAG_OUTINE_PROVIDER_IS_SET) != 0) {
      target.setOutlineProvider(mOutlineProvider);
    }
    if ((mPrivateFlags & PFLAG_CLIP_TO_OUTLINE_IS_SET) != 0) {
      target.setClipToOutline(mClipToOutline);
    }
    if ((mPrivateFlags & PFLAG_CLIP_CHILDREN_IS_SET) != 0) {
      target.setClipChildren(mClipChildren);
    }
    if (mViewTag != null) {
      target.setViewTag(mViewTag);
    }
    if (mViewTags != null) {
      target.setViewTags(mViewTags);
    }
    if (mTransitionName != null) {
      target.setTransitionName(mTransitionName);
    }
    if (getFocusState() != FOCUS_UNSET) {
      target.setFocusable(getFocusState() == FOCUS_SET_TRUE);
    }
    if (getClickableState() != CLICKABLE_UNSET) {
      target.setClickable(getClickableState() == CLICKABLE_SET_TRUE);
    }
    if (getEnabledState() != ENABLED_UNSET) {
      target.setEnabled(getEnabledState() == ENABLED_SET_TRUE);
    }
    if (getSelectedState() != SELECTED_UNSET) {
      target.setSelected(getSelectedState() == SELECTED_SET_TRUE);
    }
    if (getAccessibilityHeadingState() != ACCESSIBILITY_HEADING_UNSET) {
      target.setAccessibilityHeading(
          getAccessibilityHeadingState() == ACCESSIBILITY_HEADING_SET_TRUE);
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
}
