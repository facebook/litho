/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import android.view.ViewGroup;
import com.facebook.litho.ComponentTree;
import javax.annotation.Nullable;

/**
 * A delegation that is used to customize the adapter behaviour for the RecyclerView that the
 * RecyclerBinder uses.
 */
public interface RecyclerBinderAdapterDelegate<T extends RecyclerBinderViewHolder> {

  /**
   * The same function as {@link
   * androidx.recyclerview.widget.RecyclerView.Adapter#onCreateViewHolder(ViewGroup, int)}.
   */
  T onCreateViewHolder(ViewGroup parent, int viewType);

  /**
   * The same function as {@link
   * androidx.recyclerview.widget.RecyclerView.Adapter#onBindViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder,
   * int)} but with two additional parameters, {@param componentTree} and {@param renderInfo}, which
   * are useful to the LithoView. It will be called after the LithoView has been attached to the
   * screen.
   */
  void onBindViewHolder(
      T viewHolder, int position, @Nullable ComponentTree componentTree, RenderInfo renderInfo);

  /**
   * The same function as {@link
   * androidx.recyclerview.widget.RecyclerView.Adapter#onViewRecycled(androidx.recyclerview.widget.RecyclerView.ViewHolder)}.
   */
  void onViewRecycled(T viewHolder);

  /**
   * The same function as {@link androidx.recyclerview.widget.RecyclerView.Adapter#hasStableIds()}.
   */
  boolean hasStableIds();

  /**
   * The same function as {@link androidx.recyclerview.widget.RecyclerView.Adapter#getItemId(int)}.
   */
  long getItemId(int position);
}
