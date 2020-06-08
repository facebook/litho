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

import static com.facebook.litho.LayoutOutput.getLayoutOutput;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SparseArrayCompat;
import androidx.core.util.Pools;
import com.facebook.rendercore.MountItem;

/** Compound touch delegate that forward touch events to recyclable inner touch delegates. */
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
   * Registers an inner touch delegate for the given view with the specified expansion. It assumes
   * the given view has its final bounds set.
   *
   * @param index The drawing order index of the given view.
   * @param view The view to which touch expansion should be applied.
   * @param item The mount item which requires touch expansion.
   */
  void registerTouchExpansion(int index, View view, MountItem item) {
    mDelegates.put(index, InnerTouchDelegate.acquire(view, item));
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
    if (valueIndex >= 0) {
      final InnerTouchDelegate touchDelegate = mDelegates.valueAt(valueIndex);
      mDelegates.removeAt(valueIndex);
      if (touchDelegate != null) {
        touchDelegate.release();
      }
    }
  }

  private boolean maybeUnregisterFromScrap(int index) {
    if (mScrapDelegates != null) {
      final int valueIndex = mScrapDelegates.indexOfKey(index);
      if (valueIndex >= 0) {
        final InnerTouchDelegate touchDelegate = mScrapDelegates.valueAt(valueIndex);
        mScrapDelegates.removeAt(valueIndex);
        if (touchDelegate != null) {
          touchDelegate.release();
        }

        return true;
      }
    }

    return false;
  }

  void draw(Canvas canvas, Paint paint) {
    for (int i = mDelegates.size() - 1; i >= 0; i--) {
      final InnerTouchDelegate delegate = mDelegates.valueAt(i);
      if (delegate != null) {
        final Rect bounds = delegate.getDelegateBounds();
        if (bounds != null) {
          canvas.drawRect(bounds, paint);
        }
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    for (int i = mDelegates.size() - 1; i >= 0; i--) {
      final InnerTouchDelegate touchDelegate = mDelegates.valueAt(i);
      if (touchDelegate != null && touchDelegate.onTouchEvent(event)) {
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

  @VisibleForTesting
  public int size() {
    return mDelegates.size();
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

    private static final Pools.SimplePool<InnerTouchDelegate> sPool = new Pools.SimplePool<>(4);

    private View mDelegateView;
    private MountItem mItem;
    private boolean mIsHandlingTouch;

    void init(View delegateView, MountItem item) {
      mDelegateView = delegateView;
      mItem = item;
    }

    @Nullable
    Rect getDelegateBounds() {
      final ViewNodeInfo info = getLayoutOutput(mItem).getViewNodeInfo();
      if (info == null) {
        return null;
      }

      return info.getExpandedTouchBounds();
    }

    boolean onTouchEvent(MotionEvent event) {
      final int x = (int) event.getX();
      final int y = (int) event.getY();
      final Rect delegateBounds = getDelegateBounds();
      if (delegateBounds == null) {
        return false;
      }

      final int slop = ViewConfiguration.get(mDelegateView.getContext()).getScaledTouchSlop();
      final Rect delegateSlopBounds = new Rect();

      delegateSlopBounds.set(delegateBounds);
      delegateSlopBounds.inset(-slop, -slop);

      boolean shouldDelegateTouchEvent = false;
      boolean touchWithinViewBounds = true;
      boolean handled = false;

      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          mIsHandlingTouch = delegateBounds.contains(x, y);
          shouldDelegateTouchEvent = mIsHandlingTouch;

          break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_MOVE:
          shouldDelegateTouchEvent = mIsHandlingTouch;
          if (mIsHandlingTouch) {
            if (!delegateSlopBounds.contains(x, y)) {
              touchWithinViewBounds = false;
            }
          }
          if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsHandlingTouch = false;
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
          event.setLocation(-(slop * 2), -(slop * 2));
        }

        handled = mDelegateView.dispatchTouchEvent(event);
      }

      return handled;
    }

    static InnerTouchDelegate acquire(View delegateView, MountItem item) {
      InnerTouchDelegate touchDelegate = sPool.acquire();
      if (touchDelegate == null) {
        touchDelegate = new InnerTouchDelegate();
      }

      touchDelegate.init(delegateView, item);

      return touchDelegate;
    }

    void release() {
      mDelegateView = null;
      mItem = null;
      mIsHandlingTouch = false;
      sPool.release(this);
    }
  }
}
