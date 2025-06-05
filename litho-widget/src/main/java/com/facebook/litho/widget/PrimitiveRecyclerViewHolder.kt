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

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A ViewHolder implementation for RecyclerView that creates and holds a primitive View. This class
 * is used internally by the RecyclerView component to manage views.
 *
 * @param context The context used to create the view
 * @param viewAllocator A function that creates a View given a Context
 */
internal class PrimitiveRecyclerViewHolder(context: Context, viewAllocator: (Context) -> View) :
    RecyclerView.ViewHolder(viewAllocator(context)) {
  /** The data supports the view holder. */
  var data: Any? = null
}
