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

package com.facebook.rendercore

import android.graphics.Rect
import com.facebook.proguard.annotations.DoNotStrip
import java.util.Locale

@DoNotStrip
class RenderTreeNode(
    val parent: RenderTreeNode?,
    val renderUnit: RenderUnit<*>,
    val layoutData: Any?,
    val bounds: Rect,
    val resolvedPadding: Rect?,
    val positionInParent: Int
) {
  val absoluteX: Int = (parent?.absoluteX ?: 0) + bounds.left
  val absoluteY: Int = (parent?.absoluteY ?: 0) + bounds.top
  private val children: MutableList<RenderTreeNode> by
      lazy(LazyThreadSafetyMode.NONE) { ArrayList(DEFAULT_SIZE) }

  fun child(renderTreeNode: RenderTreeNode) {
    children.add(renderTreeNode)
  }

  /**
   * Sets the absolutes bounds of this render tree node in [outRect]; i.e. returns the bounds of
   * this render tree node within its [RootHost].
   *
   * @param outRect the calculated absolute bounds.
   */
  fun getAbsoluteBounds(outRect: Rect): Rect {
    outRect.left = absoluteX
    outRect.top = absoluteY
    outRect.right = absoluteX + bounds.width()
    outRect.bottom = absoluteY + bounds.height()
    return outRect
  }

  val childrenCount: Int
    get() = children.size

  fun getChildAt(idx: Int): RenderTreeNode {
    return children.get(idx)
  }

  fun generateDebugString(tree: RenderTree?): String {
    val id = renderUnit.id
    val contentType = renderUnit.description
    val index = tree?.getRenderTreeNodeIndex(renderUnit.id) ?: -1
    val bounds = bounds.toShortString()
    val childCount = childrenCount
    val parentId = parent?.renderUnit?.id ?: -1
    return String.format(
        Locale.US,
        "Id=%d; renderUnit='%s'; indexInTree=%d; posInParent=%d; bounds=%s; absPosition=[%d, %d]; childCount=%d; parentId=%d;",
        id,
        contentType,
        index,
        positionInParent,
        bounds,
        absoluteX,
        absoluteY,
        childCount,
        parentId)
  }

  companion object {
    private const val DEFAULT_SIZE = 4
  }
}
