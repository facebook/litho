/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Looper;
import android.support.v4.util.Pools;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.infer.annotation.ThreadSafe;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.ThreadUtils.assertDoesntHoldLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;

public abstract class BaseBinder<
    V extends ViewGroup,
    R extends WorkingRangeController> implements Binder<V>, BinderOperations<V> {

  private static final Pools.SynchronizedPool<List> sListPool =
      new Pools.SynchronizedPool<>(8);

  protected static final int URFLAG_REFRESH_IN_RANGE = 0x1;
  protected static final int URFLAG_RELEASE_OUTSIDE_RANGE = 0x2;

  protected interface Listener {
    void onDataSetChanged();

    void onItemInserted(int position);
    void onItemRangeInserted(int positionStart, int itemCount);

    void onItemChanged(int position);
    void onItemRangeChanged(int positionStart, int itemCount);

    void onItemMoved(int fromPosition, int toPosition);

    void onItemRemoved(int position);
    void onItemRangeRemoved(int positionStart, int itemCount);
  }

  private final ComponentContext mContext;
  private final BinderTreeCollection mComponentTrees;
  private final Looper mLayoutLooper;

  private int mContentWidthSpec = SizeSpec.makeSizeSpec(0, UNSPECIFIED);
  private int mContentHeightSpec = SizeSpec.makeSizeSpec(0, UNSPECIFIED);
  private Listener mListener;
  private R mRangeController;
  private V mView;

  public BaseBinder(Context context, R rangeController) {
    this(context, null, rangeController);
  }

  public BaseBinder(Context context, Looper layoutLooper, R rangeController) {
    mContext = new ComponentContext(context);
    mComponentTrees = new BinderTreeCollection();

    setRangeController(rangeController);

    mLayoutLooper = layoutLooper;

    if (mLayoutLooper != null && mLayoutLooper == Looper.getMainLooper()) {
      throw new IllegalStateException("If you want to compute the layout of the " +
          "Binder's elements in the Main Thread you shouldn't set the MainLooper here but" +
          "override isAsyncLayoutEnabled() and return false.");
    }
  }

  @Override
  public void measure(Size outSize, int widthSpec, int heightSpec) {
    throw new IllegalStateException("Recycler must have sizes spec set " +
        "when using the old binder implementation.");
  }

  @Override
  public final void notifyDataSetChanged() {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    // TODO(12986103): Once the experiment proved to be ok, remove the updateRange code.
    if (ComponentsConfiguration.bootstrapBinderItems) {
      initializeRange(0);
    } else {
      updateRange(0, getCount(), URFLAG_REFRESH_IN_RANGE | URFLAG_RELEASE_OUTSIDE_RANGE);
    }

    if (mListener != null) {
      mListener.onDataSetChanged();
    }
  }

  @Override
  public final void notifyItemChanged(int position) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    if (isInRange(position)) {
      updateRange(position, 1, URFLAG_REFRESH_IN_RANGE);
    }

    if (mListener != null) {
      mListener.onItemChanged(position);
    }
  }

  @Override
  public void notifyItemRangeChanged(int positionStart, int itemCount) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    if (isInRange(positionStart, itemCount)) {
      updateRange(positionStart, itemCount, URFLAG_REFRESH_IN_RANGE);
    }

    if (mListener != null) {
      mListener.onItemRangeChanged(positionStart, itemCount);
    }
  }

  @Override
  public void notifyItemInserted(int position) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    boolean shouldInsertItem = true;

    synchronized (this) {
      // Check if the inserted item is inside the range or adjacent to it.
      if (!isInRange(position) &&
          position != mComponentTrees.getFirstPosition() + mComponentTrees.size()) {
        // If the new item is inserted before the current range, than shift right the range.
        if (position < mComponentTrees.getFirstPosition()) {
          mComponentTrees.shiftAllRight(1);
        }

        shouldInsertItem = false;
      }
    }

    if (shouldInsertItem) {
      Component component = createComponent(mContext, position);

      synchronized (this) {
        // We need to check again because we exited the critical section and since the last check
        // the result might have changed.
        if (isInRange(position) ||
            position == mComponentTrees.getFirstPosition() + mComponentTrees.size()) {

          final ComponentTree componentTree = buildComponentTree(component);

          if (isAsyncLayoutEnabled()) {
            componentTree.setSizeSpecAsync(getWidthSpec(position), getHeightSpec(position));
          } else {
            componentTree.setSizeSpec(getWidthSpec(position), getHeightSpec(position));
          }

          // Move right the successive ComponentTree positions and insert the new item.
          mComponentTrees.insert(position, componentTree);
        }
      }
    }

    // There might be a componentTree outside the range now but for simplicity we just keep it,
    // it will be released in a successive updateRange call.

    if (mListener != null) {
      mListener.onItemInserted(position);
    }
  }

