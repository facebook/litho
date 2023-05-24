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

@file:JvmName("SizeUtils")

package com.facebook.rendercore.utils

import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Returns a [Size] with width and height set based on the provided [SizeConstraints].
 *
 * This should only be used for Components which do not measure themselves - it's the parent that
 * has determined the exact size for this child.
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @return [Size] containing computed width and height if the [SizeConstraints] are exact, otherwise
 *   returns [Size.Invalid]
 */
fun Size.Companion.exact(sizeConstraints: SizeConstraints): Size {
  if (!sizeConstraints.hasExactWidth || !sizeConstraints.hasExactHeight) {
    return Size.Invalid
  }
  return Size(sizeConstraints.maxWidth, sizeConstraints.maxHeight)
}

/**
 * Returns a [Size] with width and height set based on the [SizeConstraints] if the constraints are
 * bounded, otherwise uses fallback values or min constraint values.
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @param fallbackWidth The pixel value for the width of the measured component when the provided
 *   maxWidth constraint is equal to Infinity.
 * @param fallbackHeight The pixel value for the height of the measured component when the provided
 *   maxHeight constraint is equal to Infinity.
 * @return [Size] containing computed width and height
 */
fun Size.Companion.fillSpace(
    sizeConstraints: SizeConstraints,
    fallbackWidth: Int,
    fallbackHeight: Int
): Size {
  if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
    return Size(
        max(sizeConstraints.minWidth, fallbackWidth),
        max(sizeConstraints.minHeight, fallbackHeight))
  }

  val width = if (sizeConstraints.hasBoundedWidth) sizeConstraints.maxWidth else fallbackWidth
  val height = if (sizeConstraints.hasBoundedHeight) sizeConstraints.maxHeight else fallbackHeight

  return Size(width, height)
}

/**
 * Returns a [Size] with width and height set according to an aspect ratio and [SizeConstraints]. It
 * will respect the intrinsic size of the component being measured if it fits within the provided
 * constraints.
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @param aspectRatio The aspect ratio for calculating size.
 * @param intrinsicWidth The pixel value for the intrinsic width of the measured component.
 * @param intrinsicHeight The pixel value for the intrinsic height of the measured component.
 * @return [Size] containing computed width and height if [aspectRatio] is > 0, otherwise returns
 *   [Size.Invalid]
 */
fun Size.Companion.withAspectRatio(
    sizeConstraints: SizeConstraints,
    aspectRatio: Float,
    intrinsicWidth: Int,
    intrinsicHeight: Int
): Size {
  var constraints = sizeConstraints
  if (sizeConstraints.hasBoundedWidth &&
      (intrinsicWidth in sizeConstraints.minWidth..sizeConstraints.maxWidth)) {
    constraints = constraints.copy(maxWidth = intrinsicWidth)
  }
  if (sizeConstraints.hasBoundedHeight &&
      (intrinsicHeight in sizeConstraints.minHeight..sizeConstraints.maxHeight)) {
    constraints = constraints.copy(maxHeight = intrinsicHeight)
  }

  return Size.withAspectRatio(constraints, aspectRatio)
}

/**
 * Returns a [Size] with width and height set according to an aspect ratio and [SizeConstraints]. It
 * will respect the intrinsic size of the component being measured if it fits within the provided
 * constraints.
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @param aspectRatio The aspect ratio for calculating size.
 * @param intrinsicWidth The pixel value for the intrinsic width of the measured component.
 * @param intrinsicHeight The pixel value for the intrinsic height of the measured component.
 * @return [Size] containing computed width and height if [aspectRatio] is > 0, otherwise returns
 *   [Size.Invalid]
 */
inline fun Size.Companion.withAspectRatio(
    sizeConstraints: SizeConstraints,
    aspectRatio: Double,
    intrinsicWidth: Int,
    intrinsicHeight: Int
): Size {
  return Size.withAspectRatio(
      sizeConstraints, aspectRatio.toFloat(), intrinsicWidth, intrinsicHeight)
}

/**
 * Returns a [Size] measured according to an aspect ratio and [SizeConstraints].
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @param aspectRatio The aspect ratio for calculating size.
 * @return [Size] containing computed width and height if [aspectRatio] is > 0, otherwise returns
 *   [Size.Invalid]
 */
fun Size.Companion.withAspectRatio(sizeConstraints: SizeConstraints, aspectRatio: Float): Size {
  if (aspectRatio <= 0f) {
    return Size.Invalid
  }

  if (!sizeConstraints.hasBoundedWidth && !sizeConstraints.hasBoundedHeight) {
    // default to size [minWidth, minHeight] because both width and height are unbounded
    return Size(sizeConstraints.minWidth, sizeConstraints.minHeight)
  }

  val widthBasedHeight = ceil((sizeConstraints.maxWidth / aspectRatio).toDouble()).toInt()
  val heightBasedWidth = ceil((sizeConstraints.maxHeight * aspectRatio).toDouble()).toInt()

  var outputWidth = 0
  var outputHeight = 0

  if (!sizeConstraints.hasExactWidth &&
      sizeConstraints.hasBoundedWidth &&
      !sizeConstraints.hasExactHeight &&
      sizeConstraints.hasBoundedHeight) {
    // Both width and height are bounded but not exact. Find the largest possible size which
    // respects both constraints.
    if (widthBasedHeight > sizeConstraints.maxHeight) {
      outputWidth = heightBasedWidth
      outputHeight = sizeConstraints.maxHeight
    } else {
      outputWidth = sizeConstraints.maxWidth
      outputHeight = widthBasedHeight
    }
  } else if (sizeConstraints.hasExactWidth) {
    // Width is exact and the height is either unbounded or is allowed to be large enough to
    // accommodate the given aspect ratio.
    outputWidth = sizeConstraints.maxWidth
    outputHeight =
        if (!sizeConstraints.hasBoundedHeight || widthBasedHeight <= sizeConstraints.maxHeight) {
          widthBasedHeight
        } else {
          sizeConstraints.maxHeight
        }
  } else if (sizeConstraints.hasExactHeight) {
    // Height is exact and the width is either unbounded or is allowed to be large enough to
    // accommodate the given aspect ratio.
    outputHeight = sizeConstraints.maxHeight
    outputWidth =
        if (!sizeConstraints.hasBoundedWidth || heightBasedWidth <= sizeConstraints.maxWidth) {
          heightBasedWidth
        } else {
          sizeConstraints.maxWidth
        }
  } else if (sizeConstraints.hasBoundedWidth) {
    // Width is bounded. If that is the case then height must be unbounded.
    outputWidth = sizeConstraints.maxWidth
    outputHeight = widthBasedHeight
  } else if (sizeConstraints.hasBoundedHeight) {
    // Height is bounded. If that is the case then width must be unbounded.
    outputWidth = heightBasedWidth
    outputHeight = sizeConstraints.maxHeight
  }

  // Ensure that the resulting width and height are in appropriate ranges.
  outputWidth =
      if (outputWidth == SizeConstraints.Infinity) {
        SizeConstraints.Infinity
      } else {
        outputWidth.coerceIn(
            sizeConstraints.minWidth, min(sizeConstraints.maxWidth, SizeConstraints.MaxValue - 1))
      }
  outputHeight =
      if (outputHeight == SizeConstraints.Infinity) {
        SizeConstraints.Infinity
      } else {
        outputHeight.coerceIn(
            sizeConstraints.minHeight, min(sizeConstraints.maxHeight, SizeConstraints.MaxValue - 1))
      }

  return Size(outputWidth, outputHeight)
}

/**
 * Returns a [Size] measured according to an aspect ratio and [SizeConstraints].
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @param aspectRatio The aspect ratio for calculating size.
 * @return [Size] containing computed width and height if [aspectRatio] is > 0, otherwise returns
 *   [Size.Invalid]
 */
inline fun Size.Companion.withAspectRatio(
    sizeConstraints: SizeConstraints,
    aspectRatio: Double
): Size {
  return Size.withAspectRatio(sizeConstraints, aspectRatio.toFloat())
}

/**
 * Returns a [Size] that respects both [SizeConstraints] and tries to keep both width and height
 * equal. This will not guarantee equal width and height only if the constraints don't allow for it.
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @return [Size] containing computed width and height
 */
fun Size.Companion.withEqualDimensions(sizeConstraints: SizeConstraints): Size {
  return Size.withAspectRatio(sizeConstraints, 1f)
}

/**
 * Returns [Size] that respects both [SizeConstraints] and the preferred width and height. The
 * preferred size is usually the necessary pixels to render the inner content.
 *
 * @param sizeConstraints The size constraints that will be used during measurement.
 * @param preferredWidth The desired width in pixels.
 * @param preferredHeight The desired height in pixels.
 * @return [Size] containing computed width and height
 */
fun Size.Companion.withPreferredSize(
    sizeConstraints: SizeConstraints,
    preferredWidth: Int,
    preferredHeight: Int
): Size {
  val width =
      if (sizeConstraints.hasExactWidth) {
        sizeConstraints.maxWidth
      } else if (sizeConstraints.hasBoundedWidth) {
        min(sizeConstraints.maxWidth, preferredWidth)
      } else {
        preferredWidth
      }

  val height =
      if (sizeConstraints.hasExactHeight) {
        sizeConstraints.maxHeight
      } else if (sizeConstraints.hasBoundedHeight) {
        min(sizeConstraints.maxHeight, preferredHeight)
      } else {
        preferredHeight
      }

  return Size(width, height)
}
