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

package com.facebook.litho.widget

import android.view.ViewGroup
import com.facebook.litho.ComponentTree

/**
 * A delegation that is used to customize the adapter behaviour for the RecyclerView that the
 * RecyclerBinder uses.
 */
interface RecyclerBinderAdapterDelegate<T : RecyclerBinderViewHolder> {
  /**
   * The same function as [androidx.recyclerview.widget.RecyclerView.Adapter.onCreateViewHolder].
   */
  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

  /**
   * The same function as [androidx.recyclerview.widget.RecyclerView.Adapter.onBindViewHolder] but
   * with two additional parameters, [componentTree] and [renderInfo], which are useful to the
   * LithoView. It will be called after the LithoView has been attached to the screen.
   */
  fun onBindViewHolder(
      viewHolder: T,
      position: Int,
      componentTree: ComponentTree?,
      renderInfo: RenderInfo?
  )

  /** The same function as [androidx.recyclerview.widget.RecyclerView.Adapter.onViewRecycled]. */
  fun onViewRecycled(viewHolder: T)

  /** The same function as [androidx.recyclerview.widget.RecyclerView.Adapter.hasStableIds]. */
  fun hasStableIds(): Boolean

  /** The same function as [androidx.recyclerview.widget.RecyclerView.Adapter.getItemId]. */
  fun getItemId(position: Int): Long
}
