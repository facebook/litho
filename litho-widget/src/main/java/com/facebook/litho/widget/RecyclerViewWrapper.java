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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;

/**
 * Wrapper that encapsulates all the features {@link RecyclerSpec} provides such as
 * sticky header and pull-to-refresh
 */
public class RecyclerViewWrapper extends SwipeRefreshLayout {

  private final ComponentView mStickyHeader;
  private final RecyclerView mRecyclerView;
  /**
   * Indicates whether {@link RecyclerView} has been detached. In such case we need to make sure
   * to relayout its children eventually.
   */
  private boolean mHasBeenDetachedFromWindow = false;

  public RecyclerViewWrapper(Context context, RecyclerView recyclerView) {
    super(context);

    mRecyclerView = recyclerView;

    // We need to draw first visible item on top of other children to support sticky headers
    mRecyclerView.setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {
      @Override
      public int onGetChildDrawingOrder(int childCount, int i) {
        return childCount - 1 - i;
      }
    });

    addView(mRecyclerView);
    mStickyHeader = new ComponentView(getContext());
    mStickyHeader.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(mStickyHeader);
  }

  public RecyclerView getRecyclerView() {
    return mRecyclerView;
  }

  public void setStickyComponent(ComponentTree component) {
    if (component.getComponentView() != null) {
      component.getComponentView().startTemporaryDetach();
    }
    mStickyHeader.setComponent(component);
  }

  public ComponentView getStickyHeader() {
    return mStickyHeader;
  }

