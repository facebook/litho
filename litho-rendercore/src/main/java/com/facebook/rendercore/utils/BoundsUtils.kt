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

package com.facebook.rendercore.utils

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.MeasureSpec
import com.facebook.rendercore.Host
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.Systracer

object BoundsUtils {

  /**
   * Sets the bounds on the given content if the content doesn't already have those bounds (or if
   * 'force' is supplied).
   */
  @JvmStatic
  @JvmOverloads
  fun applyBoundsToMountContent(
      renderTreeNode: RenderTreeNode,
      content: Any,
      force: Boolean,
      tracer: Systracer? = null
  ) {
    val bounds = renderTreeNode.bounds
    val padding = renderTreeNode.resolvedPadding
    applyBoundsToMountContent(
        bounds.left, bounds.top, bounds.right, bounds.bottom, padding, content, force, tracer)
  }

  /**
   * Sets the bounds on the given content if the content doesn't already have those bounds (or if
   * 'force' is supplied).
   */
  @JvmStatic
  @JvmOverloads
  fun applyBoundsToMountContent(
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      padding: Rect?,
      content: Any,
      force: Boolean,
      tracer: Systracer? = null
  ) {
    val isTracing = tracer?.isTracing() == true
    if (isTracing) {
      tracer?.beginSection("applyBoundsToMountContent")
    }
    try {
      if (content is View) {
        applyBoundsToView(content, left, top, right, bottom, padding, force)
      } else if (content is Drawable) {
        var paddedLeft = left
        var paddedTop = top
        var paddedRight = right
        var paddedBottom = bottom
        if (padding != null) {
          paddedLeft += padding.left
          paddedTop += padding.top
          paddedRight -= padding.right
          paddedBottom -= padding.bottom
        }
        content.setBounds(paddedLeft, paddedTop, paddedRight, paddedBottom)
      } else {
        throw IllegalStateException("Unsupported mounted content $content")
      }
    } finally {
      if (isTracing) {
        tracer?.endSection()
      }
    }
  }

  /**
   * Sets the bounds on the given view if the view doesn't already have those bounds (or if 'force'
   * is supplied).
   */
  @JvmStatic
  private fun applyBoundsToView(
      view: View,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      padding: Rect?,
      force: Boolean
  ) {
    val width = right - left
    val height = bottom - top
    if (padding != null && view !is Host) {
      view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
    }
    if (force || view.measuredHeight != height || view.measuredWidth != width) {
      view.measure(
          MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
    if (force ||
        view.left != left ||
        view.top != top ||
        view.right != right ||
        view.bottom != bottom) {
      view.layout(left, top, right, bottom)
    }
  }
}
