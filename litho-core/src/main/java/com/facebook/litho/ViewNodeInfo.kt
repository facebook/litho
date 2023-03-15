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

import android.animation.StateListAnimator
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.yoga.YogaDirection

/** Additional information used to set properties on Views during mounting. */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class ViewNodeInfo {

  var background: Drawable? = null
  var foreground: Drawable? = null

  var touchBoundsExpansion: Rect? = null
    private set

  lateinit var layoutDirection: YogaDirection

  var stateListAnimator: StateListAnimator? = null

  @get:DrawableRes @DrawableRes var stateListAnimatorRes: Int = 0

  @get:LayerType
  var layerType = LayerType.LAYER_TYPE_NOT_SET
    private set

  var layoutPaint: Paint? = null
    private set

  var padding: Rect? = null
    private set

  val paddingLeft: Int
    get() = padding?.left ?: 0

  val paddingTop: Int
    get() = padding?.top ?: 0

  val paddingRight: Int
    get() = padding?.right ?: 0

  val paddingBottom: Int
    get() = padding?.bottom ?: 0

  fun setPadding(l: Int, t: Int, r: Int, b: Int) {
    check(padding == null) { "Padding already initialized for this ViewNodeInfo." }
    padding = Rect(l, t, r, b)
  }

  fun hasPadding(): Boolean = padding != null

  fun setExpandedTouchBounds(result: LithoLayoutResult) {
    val left = result.touchExpansionLeft
    val top = result.touchExpansionTop
    val right = result.touchExpansionRight
    val bottom = result.touchExpansionBottom
    if (left == 0 && top == 0 && right == 0 && bottom == 0) {
      return
    }
    check(touchBoundsExpansion == null) {
      "ExpandedTouchBounds already initialized for this ViewNodeInfo."
    }
    touchBoundsExpansion = Rect(left, top, right, bottom)
  }

  fun setLayerType(@LayerType type: Int, paint: Paint?) {
    layerType = type
    layoutPaint = paint
  }

  /**
   * Checks if this ViewNodeInfo is equal to the {@param other}
   *
   * @param other the other ViewNodeInfo
   * @return `true` iff this NodeInfo is equal to the {@param other}.
   */
  fun isEquivalentTo(other: ViewNodeInfo?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null) {
      return false
    }
    if (!DrawableUtils.isEquivalentTo(background, other.background)) {
      return false
    }
    if (!DrawableUtils.isEquivalentTo(foreground, other.foreground)) {
      return false
    }
    if (padding != other.padding) {
      return false
    }
    if (touchBoundsExpansion != other.touchBoundsExpansion) {
      return false
    }
    if (layoutDirection != other.layoutDirection) {
      return false
    }
    if (stateListAnimatorRes != other.stateListAnimatorRes) {
      return false
    }

    // TODO: (T33421916) We need compare StateListAnimators more accurately
    return stateListAnimator == other.stateListAnimator
  }
}
