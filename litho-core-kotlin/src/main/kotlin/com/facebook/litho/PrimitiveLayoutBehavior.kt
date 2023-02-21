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

@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package com.facebook.litho

import com.facebook.rendercore.primitives.AspectRatioLayoutBehavior as PrimitiveAspectRatioLayoutBehavior
import com.facebook.rendercore.primitives.FillLayoutBehavior as PrimitiveFillLayoutBehavior
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior as PrimitiveFixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.LayoutBehavior

/**
 * Returns a [LayoutBehavior] with sizes set based on the widthSpec and heightSpec if the spec mode
 * is EXACTLY or AT_MOST, otherwise uses default values.
 *
 * @param defaultWidth The [Dimen] value for the width of the measured component using the
 *   UNSPECIFIED mode.
 * @param defaultHeight The [Dimen] value for the height of the measured component using the
 *   UNSPECIFIED mode.
 * @param layoutData The data to be returned from the layout pass.
 */
inline fun ResourcesScope.FillLayoutBehavior(
    defaultWidth: Dimen,
    defaultHeight: Dimen,
    layoutData: Any? = null
): LayoutBehavior {
  return PrimitiveFillLayoutBehavior(
      defaultWidth = defaultWidth.toPixels(), defaultHeight = defaultHeight.toPixels(), layoutData)
}

/**
 * Returns a [LayoutBehavior] with sizes set based provided values.
 *
 * @param width The [Dimen] value for the width of the measured component.
 * @param height The [Dimen] value for the height of the measured component.
 * @param layoutData The data to be returned from the layout pass.
 */
inline fun ResourcesScope.FixedSizeLayoutBehavior(
    width: Dimen,
    height: Dimen,
    layoutData: Any? = null,
): LayoutBehavior {
  return PrimitiveFixedSizeLayoutBehavior(
      width = width.toPixels(), height = height.toPixels(), layoutData)
}

/**
 * Returns a [LayoutBehavior] with sizes set according to an aspect ratio an width and height
 * constraints. It will respect the intrinsic size of the component being measured.
 *
 * @param aspectRatio The aspect ratio for calculating size.
 * @param intrinsicWidth The [Dimen] value for the intrinsic width of the measured component.
 * @param intrinsicHeight The [Dimen] value for the intrinsic height of the measured component.
 * @param layoutData The data to be returned from the layout pass.
 */
inline fun ResourcesScope.AspectRatioLayoutBehavior(
    aspectRatio: Float,
    intrinsicWidth: Dimen,
    intrinsicHeight: Dimen,
    layoutData: Any? = null
): LayoutBehavior {
  return PrimitiveAspectRatioLayoutBehavior(
      aspectRatio = aspectRatio,
      intrinsicWidth = intrinsicWidth.toPixels(),
      intrinsicHeight = intrinsicHeight.toPixels(),
      layoutData)
}
