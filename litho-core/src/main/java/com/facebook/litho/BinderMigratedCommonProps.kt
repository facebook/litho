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

import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.facebook.litho.drawable.DrawableUtils
import com.facebook.rendercore.Equivalence

/**
 * Contains the data and flags for view attributes that have been migrated to the
 * `onBindViewWithDescription` binder pattern behind the `useViewStyleBinders` config flag.
 *
 * This class holds the `background`, `paddingFromBackground` data and the `shouldWrapInView` flags
 * (`PFLAG_SCALE_KEY_IS_SET`, `PFLAG_ALPHA_KEY_IS_SET`, `PFLAG_ROTATION_KEY_IS_SET`) that were
 * previously owned directly by [CommonProps].
 *
 * When the binder migration is fully rolled out and `useViewStyleBinders` is removed, this entire
 * class can be deleted.
 */
class BinderMigratedCommonProps : Equivalence<BinderMigratedCommonProps?> {

  private var privateFlags: Int = 0

  var background: Drawable? = null
    private set

  var paddingFromBackground: Rect? = null
    private set

  fun background(background: Drawable?) {
    background(background, background?.let(DrawableUtils::getPaddingOrNull))
  }

  internal fun background(background: Drawable?, paddingFromBackground: Rect?) {
    privateFlags = privateFlags or PFLAG_BACKGROUND_IS_SET
    this.background = background
    if (paddingFromBackground != null) {
      this.paddingFromBackground = paddingFromBackground
    }
  }

  val isBackgroundSet: Boolean
    get() = privateFlags and PFLAG_BACKGROUND_IS_SET != 0

  fun onScaleSet(scale: Float) {
    privateFlags =
        if (scale == 1f) {
          privateFlags and PFLAG_SCALE_KEY_IS_SET.inv()
        } else {
          privateFlags or PFLAG_SCALE_KEY_IS_SET
        }
  }

  fun onAlphaSet(alpha: Float) {
    privateFlags =
        if (alpha == 1f) {
          privateFlags and PFLAG_ALPHA_KEY_IS_SET.inv()
        } else {
          privateFlags or PFLAG_ALPHA_KEY_IS_SET
        }
  }

  fun onRotationSet(rotation: Float) {
    privateFlags =
        if (rotation == 0f) {
          privateFlags and PFLAG_ROTATION_KEY_IS_SET.inv()
        } else {
          privateFlags or PFLAG_ROTATION_KEY_IS_SET
        }
  }

  fun shouldWrapInView(): Boolean {
    return privateFlags and
        (PFLAG_SCALE_KEY_IS_SET or PFLAG_ALPHA_KEY_IS_SET or PFLAG_ROTATION_KEY_IS_SET) != 0
  }

  fun copyInto(node: LithoNode) {
    if (isBackgroundSet) {
      node.background(background)
      node.paddingFromBackground = paddingFromBackground
    }
  }

  override fun isEquivalentTo(other: BinderMigratedCommonProps?): Boolean {
    if (this === other) return true
    if (other == null) return false
    return privateFlags == other.privateFlags &&
        DrawableUtils.isEquivalentTo(background, other.background)
  }

  companion object {
    private const val PFLAG_BACKGROUND_IS_SET = 1 shl 0
    private const val PFLAG_SCALE_KEY_IS_SET = 1 shl 1
    private const val PFLAG_ALPHA_KEY_IS_SET = 1 shl 2
    private const val PFLAG_ROTATION_KEY_IS_SET = 1 shl 3
  }
}
