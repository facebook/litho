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

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Abstract base class for holders that manage root host views in collection items. This class
 * serves as a foundation for view holders that need to provide access to a root view and maintain
 * associated data for collection items.
 *
 * @param V The type of View that serves as the root container, must extend Android's View class
 * @param T The type of CollectionItem data associated with this holder, must be a CollectionItem of
 *   type V
 */
abstract class CollectionItemRootHostHolder<V : View, T : CollectionItem<V>> {

  /** The root View that this holder manages and provides access to */
  abstract val view: V

  /** The data supports the view holder. */
  internal var data: T? = null
}

/**
 * A ViewHolder implementation for RecyclerView that manages primitive views through delegation.
 * This class wraps a CollectionItemRootHostHolder delegate to provide RecyclerView compatibility
 * while maintaining access to the underlying view and associated data.
 *
 * @param delegate The CollectionItemRootHostHolder that provides the root view for this ViewHolder
 */
class PrimitiveRecyclerViewHolder(
    val delegate: CollectionItemRootHostHolder<View, CollectionItem<View>>
) : RecyclerView.ViewHolder(delegate.view)
