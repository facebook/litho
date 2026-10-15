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

import androidx.recyclerview.widget.DiffUtil

/**
 * A [DiffUtil.Callback] that compares two lists of items and returns whether two items are the
 * same.
 */
class CollectionDiffCallback<T>(
    private val previousData: List<T>?,
    private val nextData: List<T>?,
    private val sameItemComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
    private val sameContentComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
) : DiffUtil.Callback() {

  override fun getOldListSize(): Int = previousData?.size ?: 0

  override fun getNewListSize(): Int = nextData?.size ?: 0

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    if (previousData == null || nextData == null) {
      return false
    }
    val previous = previousData[oldItemPosition]
    val next = nextData[newItemPosition]

    if (previous === next) {
      return true
    }

    if (sameItemComparator != null) {
      return sameItemComparator.invoke(previous, next)
    }

    return (previous == next)
  }

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
    if (previousData == null || nextData == null) {
      return false
    }

    val previous = previousData[oldItemPosition]
    val next = nextData[newItemPosition]

    if (previous === next) {
      return true
    }

    if (sameContentComparator != null) {
      return sameContentComparator.invoke(previous, next)
    }

    return (previous == next)
  }
}
