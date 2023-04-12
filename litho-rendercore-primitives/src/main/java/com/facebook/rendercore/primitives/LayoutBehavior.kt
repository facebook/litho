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

package com.facebook.rendercore.primitives

import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.primitives.utils.hasEquivalentFields

/**
 * LayoutBehavior defines how large a primitive is and, if it has children, how it lays out and
 * places those children. Implementations should take exactly what data they need for layout -
 * LayoutBehaviors are considered equivalent via field-wise comparison (i.e. are all fields also
 * equivalent?). This will be used to determine whether re-layouts need to happen.
 */
interface LayoutBehavior : Equivalence<LayoutBehavior> {
  fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult

  // By default, LayoutBehavior have field-wise equivalence, like Components
  override fun isEquivalentTo(other: LayoutBehavior): Boolean = hasEquivalentFields(this, other)
}

/**
 * Returns a [LayoutBehavior] with sizes set based on the provided widthSpec and heightSpec.
 *
 * This method should only be used for Primitive Components which do not measure themselves - it's
 * the parent that has determined the exact size for this child.
 *
 * @throws IllegalArgumentException if the widthSpec or heightSpec is not exact
 */
object FromSpecsLayoutBehavior : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val result = MeasureResult.fromSpecs(widthSpec, heightSpec)
    return PrimitiveLayoutResult(width = result.width, height = result.height)
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set based on the widthSpec and heightSpec if the spec mode
 * is EXACTLY or AT_MOST, otherwise uses default values.
 *
 * @param defaultWidth The pixel value for the width of the measured component using the UNSPECIFIED
 *   mode.
 * @param defaultHeight The pixel value for the height of the measured component using the
 *   UNSPECIFIED mode.
 * @param layoutData The data to be returned from the layout pass.
 */
class FillLayoutBehavior(
    private val defaultWidth: Int,
    private val defaultHeight: Int,
    private val layoutData: Any? = null
) : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val result = MeasureResult.fillSpace(widthSpec, heightSpec, defaultWidth, defaultHeight, null)
    return PrimitiveLayoutResult(
        width = result.width, height = result.height, layoutData = layoutData)
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set based on the widthSpec and heightSpec if the spec mode
 * is EXACTLY or AT_MOST, otherwise uses 0 size as a default.
 *
 * It's the same as [FillLayoutBehavior] but defaults to 0 for UNSPECIFIED mode.
 *
 * @param layoutData The data to be returned from the layout pass.
 */
class FillOrGoneLayoutBehavior(private val layoutData: Any? = null) : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val result = MeasureResult.fillSpaceOrGone(widthSpec, heightSpec, null)
    return PrimitiveLayoutResult(
        width = result.width, height = result.height, layoutData = layoutData)
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set based provided values.
 *
 * @param width The pixel value for the width of the measured component.
 * @param height The pixel value for the height of the measured component.
 * @param layoutData The data to be returned from the layout pass.
 */
class FixedSizeLayoutBehavior(
    private val width: Int,
    private val height: Int,
    private val layoutData: Any? = null,
) : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(width = width, height = height, layoutData = layoutData)
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set according to an aspect ratio an width and height
 * constraints. It will respect the intrinsic size of the component being measured.
 *
 * @param aspectRatio The aspect ratio for calculating size.
 * @param intrinsicWidth The pixel value for the intrinsic width of the measured component.
 * @param intrinsicHeight The pixel value for the intrinsic height of the measured component.
 * @param layoutData The data to be returned from the layout pass.
 */
class AspectRatioLayoutBehavior(
    private val aspectRatio: Float,
    private val intrinsicWidth: Int,
    private val intrinsicHeight: Int,
    private val layoutData: Any? = null
) : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val result =
        MeasureResult.forAspectRatio(
            widthSpec, heightSpec, intrinsicWidth, intrinsicHeight, aspectRatio)
    return PrimitiveLayoutResult(
        width = result.width, height = result.height, layoutData = layoutData)
  }
}

/**
 * Returns a [LayoutBehavior] that respects both size specs and try to keep both width and height
 * equal. This will only not guarantee equal width and height if these specs use modes and sizes
 * which prevent it.
 *
 * @param layoutData The data to be returned from the layout pass.
 */
class EqualDimensionsLayoutBehavior(private val layoutData: Any? = null) : LayoutBehavior {
  override fun LayoutScope.layout(widthSpec: Int, heightSpec: Int): PrimitiveLayoutResult {
    val result = MeasureResult.withEqualDimensions(widthSpec, heightSpec, null)
    return PrimitiveLayoutResult(
        width = result.width, height = result.height, layoutData = layoutData)
  }
}

class PrimitiveLayoutResult(
    val width: Int,
    val height: Int,
    private val paddingTop: Int = 0,
    private val paddingRight: Int = 0,
    private val paddingBottom: Int = 0,
    private val paddingLeft: Int = 0,
    private val layoutData: Any? = null,
) {
  internal fun toNodeLayoutResult(
      widthSpec: Int,
      heightSpec: Int,
      renderUnit: RenderUnit<*>?
  ): LayoutResult {
    return object : LayoutResult {
      override fun getRenderUnit(): RenderUnit<*>? = renderUnit
      override fun getLayoutData(): Any? = this@PrimitiveLayoutResult.layoutData
      override fun getChildrenCount(): Int = 0
      override fun getChildAt(index: Int): LayoutResult {
        throw UnsupportedOperationException("A PrimitiveLayoutResult has no children")
      }

      override fun getXForChildAtIndex(index: Int): Int {
        throw UnsupportedOperationException("A PrimitiveLayoutResult has no children")
      }

      override fun getYForChildAtIndex(index: Int): Int {
        throw UnsupportedOperationException("A PrimitiveLayoutResult has no children")
      }

      override fun getWidth(): Int = this@PrimitiveLayoutResult.width
      override fun getHeight(): Int = this@PrimitiveLayoutResult.height
      override fun getPaddingTop(): Int = this@PrimitiveLayoutResult.paddingTop
      override fun getPaddingRight(): Int = this@PrimitiveLayoutResult.paddingRight
      override fun getPaddingBottom(): Int = this@PrimitiveLayoutResult.paddingBottom
      override fun getPaddingLeft(): Int = this@PrimitiveLayoutResult.paddingLeft
      override fun getWidthSpec(): Int = widthSpec
      override fun getHeightSpec(): Int = heightSpec
    }
  }
}
