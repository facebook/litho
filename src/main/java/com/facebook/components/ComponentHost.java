/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.facebook.R;

import static com.facebook.components.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.components.ComponentHostUtils.maybeInvalidateAccessibilityState;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}. This is used
 * by {@link MountState} to wrap mounted drawables to handle click events and update drawable
 * states accordingly.
 */
public class ComponentHost extends ViewGroup {

  private final SparseArrayCompat<MountItem> mMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapMountItemsArray;

  private final SparseArrayCompat<MountItem> mViewMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapViewMountItemsArray;

  private final SparseArrayCompat<MountItem> mDrawableMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapDrawableMountItems;

  private final SparseArrayCompat<Touchable> mTouchables = new SparseArrayCompat<>();
  private SparseArrayCompat<Touchable> mScrapTouchables;

  private final SparseArrayCompat<MountItem> mDisappearingItems = new SparseArrayCompat<>();

  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;

  private boolean mWasInvalidatedWhileSuppressed;
  private boolean mWasInvalidatedForAccessibilityWhileSuppressed;
  private boolean mSuppressInvalidations;

  private final InterleavedDispatchDraw mDispatchDraw = new InterleavedDispatchDraw();

  private final List<ComponentHost> mScrapHosts = new ArrayList<>(3);
  private final ComponentsLogger mLogger;

  private int[] mChildDrawingOrder = new int[0];
  private boolean mIsChildDrawingOrderDirty;

  private long mParentHostMarker;
  private boolean mInLayout;

  private ComponentAccessibilityDelegate mComponentAccessibilityDelegate;
  private boolean mIsComponentAccessibilityDelegateSet = false;

  private ComponentClickListener mOnClickListener;
  private ComponentLongClickListener mOnLongClickListener;
  private ComponentTouchListener mOnTouchListener;

  private TouchExpansionDelegate mTouchExpansionDelegate;

  public ComponentHost(Context context) {
    this(context, null);
  }

  public ComponentHost(Context context, AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public ComponentHost(ComponentContext context) {
    this(context, null);
  }

  public ComponentHost(ComponentContext context, AttributeSet attrs) {
    super(context, attrs);
    setWillNotDraw(false);
    setChildrenDrawingOrderEnabled(true);

    mLogger = context.getLogger();
    mComponentAccessibilityDelegate = new ComponentAccessibilityDelegate(this);
    refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(context));
  }

  /**
   * Sets the parent host marker for this host.
   * @param parentHostMarker marker that indicates which {@link ComponentHost} hosts this host.
   */
  void setParentHostMarker(long parentHostMarker) {
    mParentHostMarker = parentHostMarker;
  }

  /**
   * @return an id indicating which {@link ComponentHost} hosts this host.
   */
  long getParentHostMarker() {
    return mParentHostMarker;
  }

  /**
   * Mounts the given {@link MountItem} with unique index.
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as is passed for
   * the corresponding {@code unmount(index, mountItem)} call.
   * @param mountItem item to be mounted into the host.
   * @param bounds the bounds of the item that is to be mounted into the host
   */
  public void mount(int index, MountItem mountItem, Rect bounds) {
    final Object content = mountItem.getContent();
    if (content instanceof Drawable) {
      mountDrawable(index, mountItem, bounds);
    } else if (content instanceof View) {
      mViewMountItems.put(index, mountItem);
      mountView((View) content, mountItem.getFlags());
      maybeRegisterTouchExpansion(index, mountItem);
    }

    mMountItems.put(index, mountItem);

    maybeInvalidateAccessibilityState(mountItem);
  }

  void unmount(MountItem item) {
    final int index = mMountItems.keyAt(mMountItems.indexOfValue(item));
    unmount(index, item);
  }

  /**
   * Unmounts the given {@link MountItem} with unique index.
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as was passed for
   * the corresponding {@code mount(index, mountItem)} call.
   * @param mountItem item to be unmounted from the host.
   */
  public void unmount(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();
    if (content instanceof Drawable) {
      unmountDrawable(index, mountItem);
    } else if (content instanceof View) {
      unmountView((View) content);
      ComponentHostUtils.removeItem(index, mViewMountItems, mScrapViewMountItemsArray);
      maybeUnregisterTouchExpansion(index, mountItem);
    }

    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
    maybeInvalidateAccessibilityState(mountItem);
  }

  void startUnmountDisappearingItem(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();
    if (!(content instanceof View)) {
      throw new RuntimeException("Cannot unmount non-view item");
    }
    mIsChildDrawingOrderDirty = true;

    ComponentHostUtils.removeItem(index, mViewMountItems, mScrapViewMountItemsArray);
    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
    mDisappearingItems.put(index, mountItem);
  }

  void unmountDisappearingItem(MountItem disappearingItem) {
    final int indexOfValue = mDisappearingItems.indexOfValue(disappearingItem);
    final int key = mDisappearingItems.keyAt(indexOfValue);
    mDisappearingItems.removeAt(indexOfValue);

    final View content = (View) disappearingItem.getContent();

    unmountView(content);
    maybeUnregisterTouchExpansion(key, disappearingItem);
    maybeInvalidateAccessibilityState(disappearingItem);
  }

  boolean hasDisappearingItems() {
    return mDisappearingItems.size() > 0;
  }

  List<String> getDisappearingItemKeys() {
    if (!hasDisappearingItems()) {
      return null;
    }
    final List<String> keys = new ArrayList<>();
    for (int i = 0, size = mDisappearingItems.size(); i < size; i++) {
      keys.add(mDisappearingItems.valueAt(i).getViewNodeInfo().getTransitionKey());
    }

    return keys;
  }

  private void maybeMoveTouchExpansionIndexes(MountItem item, int oldIndex, int newIndex) {
    final ViewNodeInfo viewNodeInfo = item.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    final Rect expandedTouchBounds = viewNodeInfo.getExpandedTouchBounds();
    if (expandedTouchBounds == null || mTouchExpansionDelegate == null) {
      return;
    }

    mTouchExpansionDelegate.moveTouchExpansionIndexes(
        oldIndex,
        newIndex);
  }

  private void maybeRegisterTouchExpansion(int index, MountItem mountItem) {
    final ViewNodeInfo viewNodeInfo = mountItem.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    final Rect expandedTouchBounds = viewNodeInfo.getExpandedTouchBounds();
    if (expandedTouchBounds == null) {
      return;
    }

    if (mTouchExpansionDelegate == null) {
      mTouchExpansionDelegate = new TouchExpansionDelegate(this);
      setTouchDelegate(mTouchExpansionDelegate);
    }

    mTouchExpansionDelegate.registerTouchExpansion(
        index,
        (View) mountItem.getContent(),
        expandedTouchBounds);
  }

  private void maybeUnregisterTouchExpansion(int index, MountItem mountItem) {
    final ViewNodeInfo viewNodeInfo = mountItem.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    if (mTouchExpansionDelegate == null || viewNodeInfo.getExpandedTouchBounds() == null) {
      return;
    }

    mTouchExpansionDelegate.unregisterTouchExpansion(index);
  }

  /**
   * Tries to recycle a scrap host attached to this host.
   * @return The host view to be recycled.
   */
  ComponentHost recycleHost() {
    if (mScrapHosts.size() > 0) {
      final ComponentHost host = mScrapHosts.remove(0);

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        // We are bringing the re-used host to the front because before API 17, Android doesn't
        // take into account the children drawing order when dispatching ViewGroup touch events,
        // but it just traverses its children list backwards.
        bringChildToFront(host);
      }

      // The recycled host is immediately re-mounted in mountView(), therefore setting
      // the flag here is redundant, but future proof.
      mIsChildDrawingOrderDirty = true;

      return host;
    }

    return null;
  }

  /**
   * @return number of {@link MountItem}s that are currently mounted in the host.
   */
  int getMountItemCount() {
    return mMountItems.size();
  }

  /**
   * @return the {@link MountItem} that was mounted with the given index.
   */
  MountItem getMountItemAt(int index) {
    return mMountItems.valueAt(index);
  }

  /**
   * Hosts are guaranteed to have only one accessible component
   * in them due to the way the view hierarchy is constructed in {@link LayoutState}.
   * There might be other non-accessible components in the same hosts such as
   * a background/foreground component though. This is why this method iterates over
   * all mount items in order to find the accessible one.
   */
  MountItem getAccessibleMountItem() {
    for (int i = 0; i < getMountItemCount(); i++) {
      MountItem item = getMountItemAt(i);
      if (item.isAccessible()) {
        return item;
      }
    }

    return null;
  }

  /**
   * @return list of drawables that are mounted on this host.
   */
