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

package com.facebook.litho.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** A test {@link LithoRecyclerView} class used for unit testing. */
public class TestLithoRecyclerView extends LithoRecyclerView {

  @Nullable private RecyclerView.ItemAnimator itemAnimator;
  @Nullable private TouchInterceptor touchInterceptor;
  private final List<OnScrollListener> removeOnScrollListeners = new ArrayList<>();
  private final List<OnScrollListener> addOnScrollListeners = new ArrayList<>();
  private boolean layoutRequested;

  public TestLithoRecyclerView(Context context) {
    super(context);
  }

  @Override
  @Nullable
  public RecyclerView.ItemAnimator getItemAnimator() {
    return itemAnimator;
  }

  @Override
  public void setItemAnimator(@Nullable RecyclerView.ItemAnimator animator) {
    this.itemAnimator = animator;
  }

  @Override
  public void removeOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
    removeOnScrollListeners.add(onScrollListener);
    super.removeOnScrollListener(onScrollListener);
  }

  public int getRemoveOnScrollListenersCount() {
    return removeOnScrollListeners.size();
  }

  @Override
  public void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
    addOnScrollListeners.add(onScrollListener);
    super.addOnScrollListener(onScrollListener);
  }

  public int getAddOnScrollListenersCount() {
    return addOnScrollListeners.size();
  }

  @Override
  public void setTouchInterceptor(@Nullable TouchInterceptor touchInterceptor) {
    super.setTouchInterceptor(touchInterceptor);
    this.touchInterceptor = touchInterceptor;
  }

  @Nullable
  public TouchInterceptor getTouchInterceptor() {
    return touchInterceptor;
  }

  @Override
  public void requestLayout() {
    super.requestLayout();
    layoutRequested = true;
  }

  @Override
  public boolean isLayoutRequested() {
    return layoutRequested;
  }
}
