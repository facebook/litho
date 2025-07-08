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

package com.facebook.litho.widget.collection

import androidx.recyclerview.widget.ListUpdateCallback

/**
 * This class aims to generate the changeset between two lists of data and create the corresponding
 * RenderInfo for later use.
 */
class CollectionUpdateOperation<T>(val prevData: List<T>?, val nextData: List<T>?) :
    ListUpdateCallback {

  internal val operations: MutableList<CollectionOperation> = mutableListOf()

  internal val hasChanges: Boolean
    get() = operations.isNotEmpty()

  override fun onInserted(position: Int, count: Int) {
    operations.add(
        CollectionOperation(
            type = CollectionOperation.Type.INSERT, index = position, count = count))
  }

  override fun onRemoved(position: Int, count: Int) {
    operations.add(
        CollectionOperation(
            type = CollectionOperation.Type.DELETE, index = position, count = count))
  }

  override fun onMoved(fromPosition: Int, toPosition: Int) {
    operations.add(
        CollectionOperation(
            type = CollectionOperation.Type.MOVE, index = fromPosition, toIndex = toPosition))
  }

  override fun onChanged(position: Int, count: Int, payload: Any?) {
    // We cannot generate RenderInfo at this point because the index might change after the insert
    // or remove operation, which ends up generating a ComponentInfo with an incorrect data model.
    operations.add(
        CollectionOperation(
            type = CollectionOperation.Type.UPDATE, index = position, count = count))
  }
}
