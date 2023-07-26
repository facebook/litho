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

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.ResourcesScope
import com.facebook.rendercore.Dimen

/**
 * An `ItemDecoration` to add spacing around list items in a [Collection]. It is intended for use
 * with linear collections (which is the default layout, see [CollectionLayouts.Linear]), and wont
 * work with grids or staggered grids.
 *
 * Features include:
 * 1. equals() implementation (it's a data class) means it will pass prop isEquivalent checks
 *    avoiding unnecessary re-renders.
 * 2. Respects orientation and layout direction.
 * 3. Specify start/end/between spacing separately.
 *
 * Example usage:
 * ```
 * Collection(
 *   itemDecoration = LinearSpacing(all = 10.dp)
 * ) {}
 * ```
 *
 * @param all Spacing between all items and to the start and end item. If other values are set they
 *   will override this value.
 * @param between Spacing between all items
 * @param start Spacing at the front of the first item
 * @param end Spacing at the end of the last item
 */
fun ResourcesScope.LinearSpacing(
    all: Dimen? = null,
    between: Dimen? = null,
    start: Dimen? = null,
    end: Dimen? = null,
): RecyclerView.ItemDecoration =
    LinearSpacingItemDecoration(
        all?.toPixels(), between?.toPixels(), start?.toPixels(), end?.toPixels())

@DataClassGenerate
data class LinearSpacingItemDecoration(
    @Px val all: Int? = null,
    @Px val between: Int? = null,
    @Px val start: Int? = null,
    @Px val end: Int? = null,
) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State
  ) {
    val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
    val isHorizontal = layoutManager.orientation == RecyclerView.HORIZONTAL
    val isRTL = isHorizontal && layoutManager.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val isReverseLayout = layoutManager.reverseLayout
    val isReversed = isRTL xor isReverseLayout

    @Px val resolvedStart = start ?: all ?: 0
    @Px val resolvedEnd = end ?: all ?: 0
    @Px val resolvedBetween = between ?: all ?: 0

    val childAdapterPosition = parent.getChildAdapterPosition(view)
    if (childAdapterPosition == RecyclerView.NO_POSITION) return
    val isFirstItem = childAdapterPosition == 0
    val isLastItem = childAdapterPosition == parent.adapter?.itemCount?.minus(1) ?: -1
    with(outRect) {
      if (isFirstItem) {
        if (isReversed) {
          right = if (isHorizontal) resolvedStart else 0
          bottom = if (isHorizontal) 0 else resolvedStart
        } else {
          left = if (isHorizontal) resolvedStart else 0
          top = if (isHorizontal) 0 else resolvedStart
        }
      }
      if (isLastItem) {
        if (isReversed) {
          left = if (isHorizontal) resolvedEnd else 0
          top = if (isHorizontal) 0 else resolvedEnd
        } else {
          right = if (isHorizontal) resolvedEnd else 0
          bottom = if (isHorizontal) 0 else resolvedEnd
        }
      } else {
        if (isReversed) {
          left = if (isHorizontal) resolvedBetween else 0
          top = if (isHorizontal) 0 else resolvedBetween
        } else {
          right = if (isHorizontal) resolvedBetween else 0
          bottom = if (isHorizontal) 0 else resolvedBetween
        }
      }
    }
  }
}
