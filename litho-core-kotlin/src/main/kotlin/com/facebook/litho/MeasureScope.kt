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

import android.content.Context
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.ResourceCache
import com.facebook.rendercore.ResourceResolver
import com.facebook.rendercore.utils.MeasureSpecUtils

/**
 * The scope for the [SimpleMountable] measure method. Provides access to [androidContext],
 * [previousLayoutData] and utility methods that help return the appropriate [MeasureResult].
 */
class MeasureScope(val layoutContext: LayoutContext<*>, val previousLayoutData: Any?) {

  val androidContext: Context
    get() = layoutContext.androidContext

  val resourceResolver: ResourceResolver by lazy {
    ResourceResolver(
        androidContext, ResourceCache.getLatest(androidContext.resources.configuration))
  }

  /**
   * Measure according to an aspect ratio an width and height constraints. This version of
   * forAspectRatio will respect the intrinsic size of the component being measured.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param intrinsicWidth A pixel value for the intrinsic width of the measured component
   * @param intrinsicHeight A pixel value for the intrinsic height of the measured component
   * @param aspectRatio The aspect ration size against
   */
  fun forAspectRatio(
      widthSpec: Int,
      heightSpec: Int,
      intrinsicWidth: Int,
      intrinsicHeight: Int,
      aspectRatio: Float
  ): MeasureResult =
      MeasureResult.forAspectRatio(
          widthSpec, heightSpec, intrinsicWidth, intrinsicHeight, aspectRatio)

  /**
   * Returns a {@link MeasureResult} to respect both size specs and try to keep both width and
   * height equal. This will only not guarantee equal width and height if these specs use modes and
   * sizes which prevent it.
   */
  fun withEqualSize(widthSpec: Int, heightSpec: Int): MeasureResult =
      MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)

  /**
   * Returns a {@link MeasureResult} with sizes set based on the provided {@param widthSpec} and
   * {@param heightSpec}.
   *
   * <p>This method should only be used for Mountable Components which do not measure themselves -
   * it's the parent that has determined the exact size for this child.
   *
   * @throws IllegalArgumentException if the widthSpec or heightSpec is not exact
   */
  fun fromSpecs(widthSpec: Int, heightSpec: Int): MeasureResult =
      MeasureResult.fromSpecs(widthSpec, heightSpec)

  /**
   * Returns a {@link MeasureResult} that respects both specs and the desired width and height. The
   * desired size is usually the necessary pixels to render the inner content.
   */
  fun withExactSize(
      widthSpec: Int,
      heightSpec: Int,
      desiredWidthPx: Int,
      desiredHeightPx: Int
  ): MeasureResult =
      MeasureResult.withDesiredPx(widthSpec, heightSpec, desiredWidthPx, desiredHeightPx)
}

/**
 * Extracts the mode from the supplied size specification.
 *
 * @param spec the size specification to extract the mode from.
 * @return [android.view.View.MeasureSpec.UNSPECIFIED], [android.view.View.MeasureSpec.AT_MOST] or
 *   [android.view.View.MeasureSpec.EXACTLY]
 */
inline fun MeasureScope.getMode(spec: Int): Int {
  return MeasureSpecUtils.getMode(spec)
}

/**
 * Extracts the size from the supplied size specification.
 *
 * @param spec the size specification to extract the size from.
 * @return the size in pixels defined in the supplied size specification.
 */
inline fun MeasureScope.getSize(spec: Int): Int {
  return MeasureSpecUtils.getSize(spec)
}
