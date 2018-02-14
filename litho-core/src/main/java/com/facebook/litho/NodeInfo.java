/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.infer.annotation.ThreadConfined;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NodeInfo holds information that are set to the {@link InternalNode} and needs to be used
 * while mounting a {@link MountItem} in {@link MountState}.
 */
@ThreadConfined(ThreadConfined.ANY)
class NodeInfo {

  static final short FOCUS_UNSET = 0;
  static final short FOCUS_SET_TRUE = 1;
  static final short FOCUS_SET_FALSE = 2;

  @IntDef({FOCUS_UNSET, FOCUS_SET_TRUE, FOCUS_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface FocusState {}

  static final short ENABLED_UNSET = 0;
  static final short ENABLED_SET_TRUE = 1;
  static final short ENABLED_SET_FALSE = 2;

  @IntDef({ENABLED_UNSET, ENABLED_SET_TRUE, ENABLED_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface EnabledState {}

  static final short SELECTED_UNSET = 0;
  static final short SELECTED_SET_TRUE = 1;
  static final short SELECTED_SET_FALSE = 2;

  @IntDef({SELECTED_UNSET, SELECTED_SET_TRUE, SELECTED_SET_FALSE})
  @Retention(RetentionPolicy.SOURCE)
  @interface SelectedState {}

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
  private static final int PFLAG_ACCESSIBILITY_ROLE_IS_SET = 1 << 21;

  private final AtomicInteger mReferenceCount = new AtomicInteger(0);

  private CharSequence mContentDescription;
  private Object mViewTag;
  @Nullable private SparseArray<Object> mViewTags;
  private float mShadowElevation;
  private ViewOutlineProvider mOutlineProvider;
  private boolean mClipToOutline;
  private float mScale = 1;
  private float mAlpha = 1;
  private EventHandler<ClickEvent> mClickHandler;
  private EventHandler<FocusChangedEvent> mFocusChangeHandler;
  private EventHandler<LongClickEvent> mLongClickHandler;
  private EventHandler<TouchEvent> mTouchHandler;
  private EventHandler<InterceptTouchEvent> mInterceptTouchHandler;
  @AccessibilityRole.AccessibilityRoleType private String mAccessibilityRole;
  private EventHandler<DispatchPopulateAccessibilityEventEvent>
      mDispatchPopulateAccessibilityEventHandler;
  private EventHandler<OnInitializeAccessibilityEventEvent>
      mOnInitializeAccessibilityEventHandler;
  private EventHandler<OnPopulateAccessibilityEventEvent> mOnPopulateAccessibilityEventHandler;
  private EventHandler<OnInitializeAccessibilityNodeInfoEvent>
      mOnInitializeAccessibilityNodeInfoHandler;
  private EventHandler<OnRequestSendAccessibilityEventEvent>
      mOnRequestSendAccessibilityEventHandler;
  private EventHandler<PerformAccessibilityActionEvent> mPerformAccessibilityActionHandler;
  private EventHandler<SendAccessibilityEventEvent> mSendAccessibilityEventHandler;
  private EventHandler<SendAccessibilityEventUncheckedEvent>
      mSendAccessibilityEventUncheckedHandler;
  @FocusState
  private short mFocusState = FOCUS_UNSET;
  @EnabledState
  private short mEnabledState = ENABLED_UNSET;
  @SelectedState private short mSelectedState = SELECTED_UNSET;

  private int mPrivateFlags;

  void setContentDescription(CharSequence contentDescription) {
    mPrivateFlags |= PFLAG_CONTENT_DESCRIPTION_IS_SET;
    mContentDescription = contentDescription;
  }

  CharSequence getContentDescription() {
    return mContentDescription;
  }

  void setViewTag(Object viewTag) {
    mPrivateFlags |= PFLAG_VIEW_TAG_IS_SET;
    mViewTag = viewTag;
  }

  Object getViewTag() {
    return mViewTag;
  }

  void setViewTags(SparseArray<Object> viewTags) {
    mPrivateFlags |= PFLAG_VIEW_TAGS_IS_SET;
    mViewTags = viewTags;
  }

  float getShadowElevation() {
    return mShadowElevation;
  }

  public void setShadowElevation(float shadowElevation) {
    mPrivateFlags |= PFLAG_SHADOW_ELEVATION_IS_SET;
    mShadowElevation = shadowElevation;
  }

  ViewOutlineProvider getOutlineProvider() {
    return mOutlineProvider;
  }

  public void setOutlineProvider(ViewOutlineProvider outlineProvider) {
    mPrivateFlags |= PFLAG_OUTINE_PROVIDER_IS_SET;
    mOutlineProvider = outlineProvider;
  }

  public boolean getClipToOutline() {
    return mClipToOutline;
  }

  public void setClipToOutline(boolean clipToOutline) {
    mPrivateFlags |= PFLAG_CLIP_TO_OUTLINE_IS_SET;
    mClipToOutline = clipToOutline;
  }

  @Nullable
  SparseArray<Object> getViewTags() {
    return mViewTags;
  }

  void setClickHandler(EventHandler<ClickEvent> clickHandler) {
    mPrivateFlags |= PFLAG_CLICK_HANDLER_IS_SET;
    mClickHandler = clickHandler;
  }

  EventHandler<ClickEvent> getClickHandler() {
    return mClickHandler;
  }

  void setLongClickHandler(EventHandler<LongClickEvent> longClickHandler) {
    mPrivateFlags |= PFLAG_LONG_CLICK_HANDLER_IS_SET;
    mLongClickHandler = longClickHandler;
  }

  EventHandler<LongClickEvent> getLongClickHandler() {
    return mLongClickHandler;
  }

  void setFocusChangeHandler(EventHandler<FocusChangedEvent> focusChangedHandler) {
    mPrivateFlags |= PFLAG_FOCUS_CHANGE_HANDLER_IS_SET;
    mFocusChangeHandler = focusChangedHandler;
  }

  EventHandler<FocusChangedEvent> getFocusChangeHandler() {
    return mFocusChangeHandler;
  }

  boolean hasFocusChangeHandler() {
    return mFocusChangeHandler != null;
  }

  void setTouchHandler(EventHandler<TouchEvent> touchHandler) {
    mPrivateFlags |= PFLAG_TOUCH_HANDLER_IS_SET;
    mTouchHandler = touchHandler;
  }

  EventHandler<TouchEvent> getTouchHandler() {
    return mTouchHandler;
  }

  void setInterceptTouchHandler(EventHandler<InterceptTouchEvent> interceptTouchHandler) {
    mPrivateFlags |= PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET;
    mInterceptTouchHandler = interceptTouchHandler;
  }

  EventHandler<InterceptTouchEvent> getInterceptTouchHandler() {
    return mInterceptTouchHandler;
  }

  boolean hasTouchEventHandlers() {
    return mClickHandler != null
        || mLongClickHandler != null
        || mTouchHandler != null
        || mInterceptTouchHandler != null;
  }

  void setAccessibilityRole(@AccessibilityRole.AccessibilityRoleType String role) {
    mPrivateFlags |= PFLAG_ACCESSIBILITY_ROLE_IS_SET;
    mAccessibilityRole = role;
  }

  @AccessibilityRole.AccessibilityRoleType
  String getAccessibilityRole() {
    return mAccessibilityRole;
  }

  void setDispatchPopulateAccessibilityEventHandler(
      EventHandler<DispatchPopulateAccessibilityEventEvent>
          dispatchPopulateAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mDispatchPopulateAccessibilityEventHandler = dispatchPopulateAccessibilityEventHandler;
  }

  EventHandler<DispatchPopulateAccessibilityEventEvent>
  getDispatchPopulateAccessibilityEventHandler() {
    return mDispatchPopulateAccessibilityEventHandler;
  }

  void setOnInitializeAccessibilityEventHandler(
      EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mOnInitializeAccessibilityEventHandler = onInitializeAccessibilityEventHandler;
  }

  EventHandler<OnInitializeAccessibilityEventEvent>
  getOnInitializeAccessibilityEventHandler() {
    return mOnInitializeAccessibilityEventHandler;
  }

  void setOnInitializeAccessibilityNodeInfoHandler(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent>
          onInitializeAccessibilityNodeInfoHandler) {
    mPrivateFlags |= PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET;
    mOnInitializeAccessibilityNodeInfoHandler = onInitializeAccessibilityNodeInfoHandler;
  }

  EventHandler<OnInitializeAccessibilityNodeInfoEvent>
  getOnInitializeAccessibilityNodeInfoHandler() {
    return mOnInitializeAccessibilityNodeInfoHandler;
  }

  void setOnPopulateAccessibilityEventHandler(
      EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mOnPopulateAccessibilityEventHandler = onPopulateAccessibilityEventHandler;
  }

  EventHandler<OnPopulateAccessibilityEventEvent> getOnPopulateAccessibilityEventHandler() {
    return mOnPopulateAccessibilityEventHandler;
  }

  void setOnRequestSendAccessibilityEventHandler(
      EventHandler<OnRequestSendAccessibilityEventEvent> onRequestSendAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mOnRequestSendAccessibilityEventHandler = onRequestSendAccessibilityEventHandler;
  }

  EventHandler<OnRequestSendAccessibilityEventEvent>
  getOnRequestSendAccessibilityEventHandler() {
    return mOnRequestSendAccessibilityEventHandler;
  }

  void setPerformAccessibilityActionHandler(
      EventHandler<PerformAccessibilityActionEvent> performAccessibilityActionHandler) {
    mPrivateFlags |= PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET;
    mPerformAccessibilityActionHandler = performAccessibilityActionHandler;
  }

  EventHandler<PerformAccessibilityActionEvent> getPerformAccessibilityActionHandler() {
    return mPerformAccessibilityActionHandler;
  }

  void setSendAccessibilityEventHandler(
      EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler) {
    mPrivateFlags |= PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET;
    mSendAccessibilityEventHandler = sendAccessibilityEventHandler;
  }

  EventHandler<SendAccessibilityEventEvent> getSendAccessibilityEventHandler() {
    return mSendAccessibilityEventHandler;
  }

  void setSendAccessibilityEventUncheckedHandler(
      EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler) {
    mPrivateFlags |= PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET;
    mSendAccessibilityEventUncheckedHandler = sendAccessibilityEventUncheckedHandler;
  }

  EventHandler<SendAccessibilityEventUncheckedEvent> getSendAccessibilityEventUncheckedHandler() {
    return mSendAccessibilityEventUncheckedHandler;
  }

  boolean needsAccessibilityDelegate() {
    return mOnInitializeAccessibilityEventHandler != null
        || mOnInitializeAccessibilityNodeInfoHandler != null
        || mOnPopulateAccessibilityEventHandler != null
        || mOnRequestSendAccessibilityEventHandler != null
        || mPerformAccessibilityActionHandler != null
        || mDispatchPopulateAccessibilityEventHandler != null
        || mSendAccessibilityEventHandler != null
        || mSendAccessibilityEventUncheckedHandler != null
        || mAccessibilityRole != null;
  }

  void setFocusable(boolean isFocusable) {
    if (isFocusable) {
      mFocusState = FOCUS_SET_TRUE;
    } else {
      mFocusState = FOCUS_SET_FALSE;
    }
  }

  @NodeInfo.FocusState
  short getFocusState() {
    return mFocusState;
  }

  void setEnabled(boolean isEnabled) {
    if (isEnabled) {
      mEnabledState = ENABLED_SET_TRUE;
    } else {
      mEnabledState = ENABLED_SET_FALSE;
    }
  }

  @EnabledState
  short getEnabledState() {
    return mEnabledState;
  }

  void setSelected(boolean isSelected) {
    if (isSelected) {
      mSelectedState = SELECTED_SET_TRUE;
    } else {
      mSelectedState = SELECTED_SET_FALSE;
    }
  }

  @SelectedState
  short getSelectedState() {
    return mSelectedState;
  }

  float getScale() {
    return mScale;
  }

  void setScale(float scale) {
    mScale = scale;
    mPrivateFlags |= PFLAG_SCALE_IS_SET;
  }

  boolean isScaleSet() {
    return (mPrivateFlags & PFLAG_SCALE_IS_SET) != 0;
  }

  float getAlpha() {
    return mAlpha;
  }

  void setAlpha(float alpha) {
    mAlpha = alpha;
    mPrivateFlags |= PFLAG_ALPHA_IS_SET;
  }

  boolean isAlphaSet() {
    return (mPrivateFlags & PFLAG_ALPHA_IS_SET) != 0;
  }

  void updateWith(NodeInfo newInfo) {
    if ((newInfo.mPrivateFlags & PFLAG_CLICK_HANDLER_IS_SET) != 0) {
      mClickHandler = newInfo.mClickHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_LONG_CLICK_HANDLER_IS_SET) != 0) {
      mLongClickHandler = newInfo.mLongClickHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_FOCUS_CHANGE_HANDLER_IS_SET) != 0) {
      mFocusChangeHandler = newInfo.mFocusChangeHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_TOUCH_HANDLER_IS_SET) != 0) {
      mTouchHandler = newInfo.mTouchHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET) != 0) {
      mInterceptTouchHandler = newInfo.mInterceptTouchHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_ACCESSIBILITY_ROLE_IS_SET) != 0) {
      mAccessibilityRole = newInfo.mAccessibilityRole;
    }
    if ((newInfo.mPrivateFlags & PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      mDispatchPopulateAccessibilityEventHandler =
          newInfo.mDispatchPopulateAccessibilityEventHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      mOnInitializeAccessibilityEventHandler = newInfo.mOnInitializeAccessibilityEventHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET) != 0) {
      mOnInitializeAccessibilityNodeInfoHandler = newInfo.mOnInitializeAccessibilityNodeInfoHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      mOnPopulateAccessibilityEventHandler = newInfo.mOnPopulateAccessibilityEventHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      mOnRequestSendAccessibilityEventHandler = newInfo.mOnRequestSendAccessibilityEventHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET) != 0) {
      mPerformAccessibilityActionHandler = newInfo.mPerformAccessibilityActionHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      mSendAccessibilityEventHandler = newInfo.mSendAccessibilityEventHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET) != 0) {
      mSendAccessibilityEventUncheckedHandler = newInfo.mSendAccessibilityEventUncheckedHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_CONTENT_DESCRIPTION_IS_SET) != 0) {
      mContentDescription = newInfo.mContentDescription;
    }
    if ((newInfo.mPrivateFlags & PFLAG_SHADOW_ELEVATION_IS_SET) != 0) {
      mShadowElevation = newInfo.mShadowElevation;
    }
    if ((newInfo.mPrivateFlags & PFLAG_OUTINE_PROVIDER_IS_SET) != 0) {
      mOutlineProvider = newInfo.mOutlineProvider;
    }
    if ((newInfo.mPrivateFlags & PFLAG_CLIP_TO_OUTLINE_IS_SET) != 0) {
      mClipToOutline = newInfo.mClipToOutline;
    }
    if (newInfo.mViewTag != null) {
      mViewTag = newInfo.mViewTag;
    }
    if (newInfo.mViewTags != null ) {
      mViewTags = newInfo.mViewTags;
    }
    if (newInfo.getFocusState() != FOCUS_UNSET) {
      mFocusState = newInfo.getFocusState();
    }
    if (newInfo.getEnabledState() != ENABLED_UNSET) {
      mEnabledState = newInfo.getEnabledState();
    }
    if (newInfo.getSelectedState() != SELECTED_UNSET) {
      mSelectedState = newInfo.getSelectedState();
    }
    if ((newInfo.mPrivateFlags & PFLAG_SCALE_IS_SET) != 0) {
      mScale = newInfo.mScale;
    }
    if ((newInfo.mPrivateFlags & PFLAG_ALPHA_IS_SET) != 0) {
      mAlpha = newInfo.mAlpha;
    }
  }

  void copyInto(InternalNode layout) {
    if ((mPrivateFlags & PFLAG_CLICK_HANDLER_IS_SET) != 0) {
      layout.clickHandler(mClickHandler);
    }
    if ((mPrivateFlags & PFLAG_LONG_CLICK_HANDLER_IS_SET) != 0) {
      layout.longClickHandler(mLongClickHandler);
    }
    if ((mPrivateFlags & PFLAG_FOCUS_CHANGE_HANDLER_IS_SET) != 0) {
      layout.focusChangeHandler(mFocusChangeHandler);
    }
    if ((mPrivateFlags & PFLAG_TOUCH_HANDLER_IS_SET) != 0) {
      layout.touchHandler(mTouchHandler);
    }
    if ((mPrivateFlags & PFLAG_INTERCEPT_TOUCH_HANDLER_IS_SET) != 0) {
      layout.interceptTouchHandler(mInterceptTouchHandler);
    }
    if ((mPrivateFlags & PFLAG_ACCESSIBILITY_ROLE_IS_SET) != 0) {
      layout.accessibilityRole(mAccessibilityRole);
    }
    if ((mPrivateFlags & PFLAG_DISPATCH_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      layout.dispatchPopulateAccessibilityEventHandler(mDispatchPopulateAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      layout.onInitializeAccessibilityEventHandler(mOnInitializeAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_INITIALIZE_ACCESSIBILITY_NODE_INFO_HANDLER_IS_SET) != 0) {
      layout.onInitializeAccessibilityNodeInfoHandler(mOnInitializeAccessibilityNodeInfoHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_POPULATE_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      layout.onPopulateAccessibilityEventHandler(mOnPopulateAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_ON_REQUEST_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      layout.onRequestSendAccessibilityEventHandler(mOnRequestSendAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_PERFORM_ACCESSIBILITY_ACTION_HANDLER_IS_SET) != 0) {
      layout.performAccessibilityActionHandler(mPerformAccessibilityActionHandler);
    }
    if ((mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_HANDLER_IS_SET) != 0) {
      layout.sendAccessibilityEventHandler(mSendAccessibilityEventHandler);
    }
    if ((mPrivateFlags & PFLAG_SEND_ACCESSIBILITY_EVENT_UNCHECKED_HANDLER_IS_SET) != 0) {
      layout.sendAccessibilityEventUncheckedHandler(mSendAccessibilityEventUncheckedHandler);
    }
    if ((mPrivateFlags & PFLAG_CONTENT_DESCRIPTION_IS_SET) != 0) {
      layout.contentDescription(mContentDescription);
    }
    if ((mPrivateFlags & PFLAG_SHADOW_ELEVATION_IS_SET) != 0) {
      layout.shadowElevationPx(mShadowElevation);
    }
    if ((mPrivateFlags & PFLAG_OUTINE_PROVIDER_IS_SET) != 0) {
      layout.outlineProvider(mOutlineProvider);
    }
    if ((mPrivateFlags & PFLAG_CLIP_TO_OUTLINE_IS_SET) != 0) {
      layout.clipToOutline(mClipToOutline);
    }
    if (mViewTag != null) {
      layout.viewTag(mViewTag);
    }
    if (mViewTags != null) {
      layout.viewTags(mViewTags);
    }
    if (getFocusState() != FOCUS_UNSET) {
      layout.focusable(getFocusState() == FOCUS_SET_TRUE);
    }
    if (getEnabledState() != ENABLED_UNSET) {
      layout.enabled(getEnabledState() == ENABLED_SET_TRUE);
    }
    if (getSelectedState() != SELECTED_UNSET) {
      layout.selected(getSelectedState() == SELECTED_SET_TRUE);
    }
    if ((mPrivateFlags & PFLAG_SCALE_IS_SET) != 0) {
      layout.scale(mScale);
    }
    if ((mPrivateFlags & PFLAG_ALPHA_IS_SET) != 0) {
      layout.alpha(mAlpha);
    }
  }

  static NodeInfo acquire() {
    final NodeInfo nodeInfo = ComponentsPools.acquireNodeInfo();

    if (nodeInfo.mReferenceCount.getAndSet(1) != 0) {
      throw new IllegalStateException("The NodeInfo reference acquired from the pool " +
          " wasn't correctly released.");
    }

    return nodeInfo;
  }

  NodeInfo acquireRef() {
    if (mReferenceCount.getAndIncrement() < 1) {
      throw new IllegalStateException("The NodeInfo being acquired wasn't correctly initialized.");
    }

    return this;
  }

  void release() {
    final int count = mReferenceCount.decrementAndGet();
    if (count < 0) {
      throw new IllegalStateException("Trying to release a recycled NodeInfo.");
    } else if (count > 0) {
      return;
    }

    mContentDescription = null;
    mViewTag = null;
    mViewTags = null;
    mClickHandler = null;
    mLongClickHandler = null;
    mFocusChangeHandler = null;
    mTouchHandler = null;
    mInterceptTouchHandler = null;
    mAccessibilityRole = null;
    mDispatchPopulateAccessibilityEventHandler = null;
    mOnInitializeAccessibilityEventHandler = null;
    mOnPopulateAccessibilityEventHandler = null;
    mOnInitializeAccessibilityNodeInfoHandler = null;
    mOnRequestSendAccessibilityEventHandler = null;
    mPerformAccessibilityActionHandler = null;
    mSendAccessibilityEventHandler = null;
    mSendAccessibilityEventUncheckedHandler = null;
    mFocusState = FOCUS_UNSET;
    mEnabledState = ENABLED_UNSET;
    mSelectedState = SELECTED_UNSET;
    mPrivateFlags = 0;
    mShadowElevation = 0;
    mOutlineProvider = null;
    mClipToOutline = false;
    mScale = 1;
    mAlpha = 1;

    ComponentsPools.release(this);
  }
}
