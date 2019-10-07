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

/** A test {@link SectionsRecyclerView} class used for unit testing. */
public class TestSectionsRecyclerView extends SectionsRecyclerView {

  @Nullable private OnRefreshListener listener;
  private int removeCallbackCount;
  private int postCount;
  private boolean lastRefreshingValue;
  private int setRefreshingValuesCount;

  public TestSectionsRecyclerView(Context context, RecyclerView view) {
    super(context, view);
  }

  public int getSetRefreshingValuesCount() {
    return setRefreshingValuesCount;
  }

  public int getRemoveCallbackCount() {
    return removeCallbackCount;
  }

  public int getPostCount() {
    return postCount;
  }

  @Override
  public void setRefreshing(boolean refreshing) {
    super.setRefreshing(refreshing);
    setRefreshingValuesCount++;
    lastRefreshingValue = refreshing;
  }

  @Override
  public boolean removeCallbacks(Runnable action) {
    removeCallbackCount++;
    return true;
  }

  @Override
  public boolean post(Runnable action) {
    postCount++;
    return true;
  }

  @Override
  public boolean isRefreshing() {
    return lastRefreshingValue;
  }

  @Override
  public void setOnRefreshListener(@Nullable OnRefreshListener listener) {
    this.listener = listener;
    super.setOnRefreshListener(listener);
  }

  @Nullable
  public OnRefreshListener getOnRefreshListener() {
    return listener;
  }

  /** Used for resetting the fields of {@link TestSectionsRecyclerView} */
  public void reset() {
    setRefreshingValuesCount = 0;
    removeCallbackCount = 0;
    postCount = 0;
  }
}
