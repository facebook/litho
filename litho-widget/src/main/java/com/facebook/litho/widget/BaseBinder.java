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

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;
import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.components.Size;
import com.facebook.components.SizeSpec;
import com.facebook.infer.annotation.ThreadSafe;

import static com.facebook.components.SizeSpec.EXACTLY;
import static com.facebook.components.SizeSpec.UNSPECIFIED;
import static com.facebook.components.ThreadUtils.assertDoesntHoldLock;
import static com.facebook.components.ThreadUtils.assertMainThread;

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
