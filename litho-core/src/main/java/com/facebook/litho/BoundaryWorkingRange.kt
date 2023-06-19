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

package com.facebook.litho

/**
 * An implementation of working range that uses offset to define the upper bound and the lower
 * boundary. If the position is inside the boundary, it's in the range, otherwise it's out of the
 * range.
 */
class BoundaryWorkingRange @JvmOverloads constructor(private val offset: Int = OFFSET) :
    WorkingRange {

  override fun shouldEnterRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = isInRange(position, firstVisibleIndex, lastVisibleIndex, offset)

  override fun shouldExitRange(
      position: Int,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ): Boolean = !isInRange(position, firstVisibleIndex, lastVisibleIndex, offset)

  companion object {
    private const val OFFSET = 1

    private fun isInRange(
        position: Int,
        firstVisiblePosition: Int,
        lastVisiblePosition: Int,
        offset: Int
    ): Boolean {
      val lowerBound = firstVisiblePosition - offset
      val upperBound = lastVisiblePosition + offset
      return position in lowerBound..upperBound
    }
  }
}
