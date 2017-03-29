/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import android.support.annotation.IntDef;
import android.util.SparseArray;

import com.facebook.infer.annotation.ThreadConfined;

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

  private final AtomicInteger mReferenceCount = new AtomicInteger(0);

  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;
  private EventHandler mClickHandler;
  private EventHandler mLongClickHandler;
  private EventHandler mTouchHandler;
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
  private @NodeInfo.FocusState short mFocusState = FOCUS_UNSET;

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

  SparseArray<Object> getViewTags() {
    return mViewTags;
  }

  void setClickHandler(EventHandler clickHandler) {
    mPrivateFlags |= PFLAG_CLICK_HANDLER_IS_SET;
    mClickHandler = clickHandler;
  }

  EventHandler getClickHandler() {
    return mClickHandler;
  }

  boolean isClickable() {
    return (mClickHandler != null);
  }

  void setLongClickHandler(EventHandler longClickHandler) {
    mPrivateFlags |= PFLAG_LONG_CLICK_HANDLER_IS_SET;
    mLongClickHandler = longClickHandler;
  }

  EventHandler getLongClickHandler() {
    mPrivateFlags |= PFLAG_TOUCH_HANDLER_IS_SET;
    return mLongClickHandler;
  }

  boolean isLongClickable() {
    return (mLongClickHandler != null);
  }

  void setTouchHandler(EventHandler touchHandler) {
    mTouchHandler = touchHandler;
  }

  EventHandler getTouchHandler() {
    return mTouchHandler;
  }

  boolean isTouchable() {
    return (mTouchHandler != null);
  }

  boolean hasTouchEventHandlers() {
    return mClickHandler != null
        || mLongClickHandler != null
        || mTouchHandler != null;
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

  boolean hasAccessibilityHandlers() {
    return mOnInitializeAccessibilityEventHandler != null
        || mOnInitializeAccessibilityNodeInfoHandler != null
        || mOnPopulateAccessibilityEventHandler != null
        || mOnRequestSendAccessibilityEventHandler != null
        || mPerformAccessibilityActionHandler != null
        || mDispatchPopulateAccessibilityEventHandler != null
        || mSendAccessibilityEventHandler != null
        || mSendAccessibilityEventUncheckedHandler != null;
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

  void updateWith(NodeInfo newInfo) {
    if ((newInfo.mPrivateFlags & PFLAG_CLICK_HANDLER_IS_SET) != 0) {
      mClickHandler = newInfo.mClickHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_LONG_CLICK_HANDLER_IS_SET) != 0) {
      mLongClickHandler = newInfo.mLongClickHandler;
    }
    if ((newInfo.mPrivateFlags & PFLAG_TOUCH_HANDLER_IS_SET) != 0) {
      mTouchHandler = newInfo.mTouchHandler;
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
    if (newInfo.mViewTag != null) {
      mViewTag = newInfo.mViewTag;
    }
    if (newInfo.mViewTags != null ) {
      mViewTags = newInfo.mViewTags;
    }
    if (newInfo.getFocusState() != FOCUS_UNSET) {
      mFocusState = newInfo.getFocusState();
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
    mTouchHandler = null;
    mDispatchPopulateAccessibilityEventHandler = null;
    mOnInitializeAccessibilityEventHandler = null;
    mOnPopulateAccessibilityEventHandler = null;
    mOnInitializeAccessibilityNodeInfoHandler = null;
    mOnRequestSendAccessibilityEventHandler = null;
    mPerformAccessibilityActionHandler = null;
    mSendAccessibilityEventHandler = null;
    mSendAccessibilityEventUncheckedHandler = null;
    mFocusState = FOCUS_UNSET;
    mPrivateFlags = 0;

    ComponentsPools.release(this);
  }
}
