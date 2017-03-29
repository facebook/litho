/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v4.util.SparseArrayCompat;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Compound touch delegate that forward touch events to recyclable
 * inner touch delegates.
 */
class TouchExpansionDelegate extends TouchDelegate {

  private static final Rect IGNORED_RECT = new Rect();
  private static final Pools.SimplePool<SparseArrayCompat<InnerTouchDelegate>>
      sInnerTouchDelegateScrapArrayPool = new Pools.SimplePool<>(4);

  private final SparseArrayCompat<InnerTouchDelegate> mDelegates = new SparseArrayCompat<>();
  private SparseArrayCompat<InnerTouchDelegate> mScrapDelegates;

  TouchExpansionDelegate(ComponentHost host) {
    super(IGNORED_RECT, host);
  }

  /**
   * Registers an inner touch delegate for the given view with the specified
   * expansion. It assumes the given view has its final bounds set.
   *
   * @param index The drawing order index of the given view.
   * @param view The view to which touch expansion should be applied.
   * @param touchExpansion The expansion to be applied to each edge of the given view.
   */
  void registerTouchExpansion(int index, View view, Rect touchExpansion) {
    mDelegates.put(index, InnerTouchDelegate.acquire(view, touchExpansion));
  }

  /**
   * Unregisters an inner touch delegate with the given index.
   *
   * @param index The drawing order index of the given view.
   */
  void unregisterTouchExpansion(int index) {
    if (maybeUnregisterFromScrap(index)) {
      return;
    }

    final int valueIndex = mDelegates.indexOfKey(index);
    final InnerTouchDelegate touchDelegate = mDelegates.valueAt(valueIndex);

    mDelegates.removeAt(valueIndex);
    touchDelegate.release();
  }

  private boolean maybeUnregisterFromScrap(int index) {
    if (mScrapDelegates != null) {
      final int valueIndex = mScrapDelegates.indexOfKey(index);
      if (valueIndex >= 0) {
        final InnerTouchDelegate touchDelegate = mScrapDelegates.valueAt(valueIndex);
        mScrapDelegates.removeAt(valueIndex);
        touchDelegate.release();

        return true;
      }
    }

    return false;
  }

  void draw(Canvas canvas, Paint paint) {
    for (int i = mDelegates.size() - 1; i >= 0; i--) {
      canvas.drawRect(mDelegates.valueAt(i).mDelegateBounds, paint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    for (int i = mDelegates.size() - 1; i >= 0; i--) {
      final InnerTouchDelegate touchDelegate = mDelegates.valueAt(i);
      if (touchDelegate.onTouchEvent(event)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Called when the MountItem this Delegate is referred to is moved to another position to also
   * update the indexes of the TouchExpansionDelegate.
   */
  void moveTouchExpansionIndexes(int oldIndex, int newIndex) {
    if (mDelegates.get(newIndex) != null) {
      ensureScrapDelegates();
      ComponentHostUtils.scrapItemAt(newIndex, mDelegates, mScrapDelegates);
    }

    ComponentHostUtils.moveItem(oldIndex, newIndex, mDelegates, mScrapDelegates);

    releaseScrapDelegatesIfNeeded();
  }

  private void ensureScrapDelegates() {
    if (mScrapDelegates == null) {
      mScrapDelegates = acquireScrapTouchDelegatesArray();
    }
  }

  private static SparseArrayCompat<InnerTouchDelegate> acquireScrapTouchDelegatesArray() {
    SparseArrayCompat<InnerTouchDelegate> sparseArray = sInnerTouchDelegateScrapArrayPool.acquire();
    if (sparseArray == null) {
      sparseArray = new SparseArrayCompat<>(4);
    }

    return sparseArray;
  }

  private void releaseScrapDelegatesIfNeeded() {
    if (mScrapDelegates != null && mScrapDelegates.size() == 0) {
      releaseScrapTouchDelegatesArray(mScrapDelegates);
      mScrapDelegates = null;
    }
  }

  private static void releaseScrapTouchDelegatesArray(
      SparseArrayCompat<InnerTouchDelegate> sparseArray) {
    sInnerTouchDelegateScrapArrayPool.release(sparseArray);
  }

  private static class InnerTouchDelegate {

    private static final Pools.SimplePool<InnerTouchDelegate> sPool =
        new Pools.SimplePool<>(4);

    private View mDelegateView;
    private boolean mIsHandlingTouch;
    private int mSlop;

    private final Rect mDelegateBounds = new Rect();
    private final Rect mDelegateSlopBounds = new Rect();

    void init(View delegateView, Rect delegateBounds) {
      mDelegateView = delegateView;
      mSlop = ViewConfiguration.get(delegateView.getContext()).getScaledTouchSlop();

      mDelegateBounds.set(delegateBounds);

      mDelegateSlopBounds.set(delegateBounds);
      mDelegateSlopBounds.inset(-mSlop, -mSlop);
    }

    boolean onTouchEvent(MotionEvent event) {
      final int x = (int) event.getX();
      final int y = (int) event.getY();

      boolean shouldDelegateTouchEvent = false;
      boolean touchWithinViewBounds = true;
      boolean handled = false;

      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          if (mDelegateBounds.contains(x, y)) {
            mIsHandlingTouch = true;
            shouldDelegateTouchEvent = true;
          }
          break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_MOVE:
          shouldDelegateTouchEvent = mIsHandlingTouch;
          if (mIsHandlingTouch) {
            if (!mDelegateSlopBounds.contains(x, y)) {
              touchWithinViewBounds = false;
            }
          }
          break;

        case MotionEvent.ACTION_CANCEL:
          shouldDelegateTouchEvent = mIsHandlingTouch;
          mIsHandlingTouch = false;
          break;
      }

      if (shouldDelegateTouchEvent) {
        if (touchWithinViewBounds) {
          // Offset event coordinates to be inside the target view.
          event.setLocation(mDelegateView.getWidth() / 2, mDelegateView.getHeight() / 2);
        } else {
          // Offset event coordinates to be outside the target view (in case it does
          // something like tracking pressed state).
          event.setLocation(-(mSlop * 2), -(mSlop * 2));
        }

        handled = mDelegateView.dispatchTouchEvent(event);
      }

      return handled;
    }

    static InnerTouchDelegate acquire(View delegateView, Rect bounds) {
      InnerTouchDelegate touchDelegate = sPool.acquire();
      if (touchDelegate == null) {
        touchDelegate = new InnerTouchDelegate();
      }

      touchDelegate.init(delegateView, bounds);

      return touchDelegate;
    }

    void release() {
      mDelegateView = null;
      mDelegateBounds.setEmpty();
      mDelegateSlopBounds.setEmpty();
      mIsHandlingTouch = false;
      mSlop = 0;

      sPool.release(this);
    }
  }
}
