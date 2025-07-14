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

package com.facebook.rendercore.visibility

import android.graphics.Rect
import com.facebook.rendercore.Function
import com.facebook.rendercore.LayoutResult

/**
 * Stores information about a node which has registered visibility handlers for. The information is
 * passed to the visibility extension which then dispatches the appropriate events.
 */
class VisibilityOutput(
    /**
     * An identifier unique to this visibility output. This needs to be unique for every output in a
     * given [com.facebook.rendercore.RenderTree].
     */
    val id: String,
    /** A pretty name of this visibility output; does not need to be unique. */
    val key: String,
    val bounds: Rect,
    @JvmField val hasMountableContent: Boolean,
    @JvmField val renderUnitId: Long,
    val visibleHeightRatio: Float,
    val visibleWidthRatio: Float,
    val tag: String?,
    val onVisible: VisibilityEventCallbackData?,
    val onInvisible: VisibilityEventCallbackData?,
    val onFocusedVisible: VisibilityEventCallbackData?,
    val onUnfocusedVisible: VisibilityEventCallbackData?,
    val onFullImpression: VisibilityEventCallbackData?,
    val onVisibilityChange: VisibilityEventCallbackData?
) {

  private var focusedRatio = 0f

  constructor(
      id: String,
      key: String,
      bounds: Rect,
      visibleHeightRatio: Float,
      visibleWidthRatio: Float,
      tag: String?,
      onVisible: VisibilityEventCallbackData?,
      onInvisible: VisibilityEventCallbackData?,
      onFocusedVisible: VisibilityEventCallbackData?,
      onUnfocusedVisible: VisibilityEventCallbackData?,
      onFullImpression: VisibilityEventCallbackData?,
      onVisibilityChange: VisibilityEventCallbackData?
  ) : this(
      id,
      key,
      bounds,
      false,
      0L,
      visibleHeightRatio,
      visibleWidthRatio,
      tag,
      onVisible,
      onInvisible,
      onFocusedVisible,
      onUnfocusedVisible,
      onFullImpression,
      onVisibilityChange)

  val visibilityTop: Float
    get() =
        if (visibleHeightRatio == 0f) {
          bounds.top.toFloat()
        } else {
          bounds.top + visibleHeightRatio * (bounds.bottom - bounds.top)
        }

  val visibilityBottom: Float
    get() =
        if (visibleHeightRatio == 0f) {
          bounds.bottom.toFloat()
        } else {
          bounds.bottom - visibleHeightRatio * (bounds.bottom - bounds.top)
        }

  val visibilityLeft: Float
    get() = bounds.left + visibleWidthRatio * (bounds.right - bounds.left)

  val visibilityRight: Float
    get() = bounds.right - visibleHeightRatio * (bounds.right - bounds.left)

  val fullImpressionTop: Float
    get() = bounds.bottom.toFloat()

  val fullImpressionBottom: Float
    get() = bounds.top.toFloat()

  val fullImpressionLeft: Float
    get() = bounds.right.toFloat()

  val fullImpressionRight: Float
    get() = bounds.left.toFloat()

  val focusedTop: Float
    get() = bounds.top + focusedRatio * (bounds.bottom - bounds.top)

  val focusedBottom: Float
    get() = bounds.bottom - focusedRatio * (bounds.bottom - bounds.top)

  val focusedLeft: Float
    get() = bounds.left + focusedRatio * (bounds.right - bounds.left)

  val focusedRight: Float
    get() = bounds.right - focusedRatio * (bounds.right - bounds.left)

  val componentArea: Int
    get() {
      val rect = bounds
      return if (rect.isEmpty) 0 else rect.width() * rect.height()
    }

  fun setFocusedRatio(focusedRatio: Float) {
    this.focusedRatio = focusedRatio
  }

  /**
   * The factory that client frameworks must implement to enable [VisibilityExtension] to create a
   * [VisibilityOutput] for every visited [LayoutResult] during the layout pass.
   */
  fun interface Factory<R : LayoutResult?> {
    /**
     * @param result The [LayoutResult] for which a [VisibilityOutput] is required
     * @param absoluteBounds The absolute bounds of {@param result}.
     * @return an output if the client framework needs visibility events for {@param result}.
     */
    fun createVisibilityOutput(result: R, absoluteBounds: Rect): VisibilityOutput?
  }
}

class VisibilityEventCallbackData(
    widthRatio: Float,
    heightRatio: Float,
    val callback: Function<Void?>,
)
