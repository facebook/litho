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

import androidx.recyclerview.widget.RecyclerView

/**
 * A value class that wraps an integer representing the orientation of a collection. This provides
 * type safety and convenience methods for working with RecyclerView orientations.
 *
 * @param value The integer value representing the orientation (RecyclerView.VERTICAL or
 *   RecyclerView.HORIZONTAL)
 */
@JvmInline
value class CollectionOrientation private constructor(private val value: Int) {

  /**
   * Checks if the collection orientation is vertical.
   *
   * @return true if the orientation is vertical, false otherwise
   */
  val isVertical: Boolean
    get() = (value == RecyclerView.VERTICAL)

  /**
   * Checks if the collection orientation is horizontal.
   *
   * @return true if the orientation is horizontal, false otherwise
   */
  val isHorizontal: Boolean
    get() = (value == RecyclerView.HORIZONTAL)

  companion object {
    /** Predefined horizontal orientation instance */
    val HORIZONTAL: CollectionOrientation = CollectionOrientation(RecyclerView.HORIZONTAL)

    /** Predefined vertical orientation instance */
    val VERTICAL: CollectionOrientation = CollectionOrientation(RecyclerView.VERTICAL)

    /**
     * Converts to a CollectionOrientation instance from RecyclerView's orientation.
     *
     * @param value The integer value representing the orientation (RecyclerView.HORIZONTAL or
     *   RecyclerView.VERTICAL)
     * @return A CollectionOrientation instance corresponding to the provided value
     * @throws IllegalArgumentException if the value is not a valid orientation constant
     */
    fun fromInt(value: Int): CollectionOrientation {
      return when (value) {
        RecyclerView.HORIZONTAL -> HORIZONTAL
        RecyclerView.VERTICAL -> VERTICAL
        else -> throw IllegalArgumentException("Invalid orientation value: $value")
      }
    }
  }
}
