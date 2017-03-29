/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentView;

import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static com.facebook.litho.SizeSpec.getMode;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * A component binder for {@link RecyclerView}.
 */
public abstract class RecyclerComponentBinder<L extends RecyclerView.LayoutManager,
    R extends RecyclerComponentBinder.RecyclerComponentWorkingRangeController>
    extends BaseBinder<RecyclerView, R> {

  private final L mLayoutManager;
  private final InternalAdapter mAdapter;
  private final RecyclerView.OnScrollListener mOnScrollListener;

  private RecyclerView mRecyclerView;

  public RecyclerComponentBinder(Context context, L layoutManager, R rangeController) {
    this(context, layoutManager, rangeController, null);
  }

  public RecyclerComponentBinder(
      Context context,
      L layoutManager,
      R rangeController,
      Looper layoutLooper) {
    super(context, layoutLooper, rangeController);

    mLayoutManager = layoutManager;
    mAdapter = new InternalAdapter(context, this);
    mOnScrollListener = new InternalOnScrollListener(this);

    setListener(mAdapter);
  }

  /**
   * Enables sticky header if the binder implements {@link HasStickyHeader} interface
   */
  private void maybeEnableStickyHeader() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      // Sticky header needs some APIs like 'setTranslationY()' which are not available Gingerbread
      // and below
      return;
    }
    if (mRecyclerView == null) {
      return;
    }
    RecyclerViewWrapper recyclerViewWrapper = RecyclerViewWrapper.getParentWrapper(mRecyclerView);
    if (recyclerViewWrapper == null) {
      return;
    }
    if (this instanceof HasStickyHeader) {
      RecyclerView.OnScrollListener onScrollListener =
          new StickyHeaderAwareScrollListener(
              new ComponentContext(mRecyclerView.getContext()),
              this,
              recyclerViewWrapper);
      mRecyclerView.addOnScrollListener(onScrollListener);
    }
  }

  /**
   * Return the stable ID for the component at <code>position</code>. If {@link #hasStableIds()}
   * would return false this method should return {@link RecyclerView#NO_ID}. The default
   * implementation of this method returns {@link RecyclerView#NO_ID}.
   *
   * @param position Binder position to query
   * @return the stable ID of the component at position
   */
  public long getComponentId(int position) {
    return RecyclerView.NO_ID;
  }

  /**
   * Indicates whether each component in the data set can be represented with a unique identifier of
   * type {@link java.lang.Long}.
   *
   * @param hasStableIds Whether components in data set have unique identifiers or not.
   * @see #hasStableIds()
   * @see #getComponentId(int)
   */
  public void setHasStableIds(boolean hasStableIds) {
    mAdapter.setHasStableIds(hasStableIds);
  }

  /**
   * Returns true if this binder publishes a unique <code>long</code> value that can act as a key
   * for the component at a given position in the data set. If that component is relocated in the
   * data set, the ID returned for that component should be the same.
