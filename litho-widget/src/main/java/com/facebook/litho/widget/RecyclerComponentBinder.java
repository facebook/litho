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

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentView;

import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static com.facebook.components.SizeSpec.getMode;
import static com.facebook.components.SizeSpec.UNSPECIFIED;

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
