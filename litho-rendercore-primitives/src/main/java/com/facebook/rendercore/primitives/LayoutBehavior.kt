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

import com.facebook.rendercore.Dimen
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.utils.hasEquivalentFields
import com.facebook.rendercore.px
import com.facebook.rendercore.utils.exact
import com.facebook.rendercore.utils.fillSpace
import com.facebook.rendercore.utils.withAspectRatio
import com.facebook.rendercore.utils.withEqualDimensions

/**
 * LayoutBehavior defines how large a primitive is and, if it has children, how it lays out and
 * places those children. Implementations should take exactly what data they need for layout -
 * LayoutBehaviors are considered equivalent via field-wise comparison (i.e. are all fields also
 * equivalent?). This will be used to determine whether re-layouts need to happen.
 */
interface LayoutBehavior : Equivalence<LayoutBehavior> {
  fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult

  // By default, LayoutBehavior have field-wise equivalence, like Components
  override fun isEquivalentTo(other: LayoutBehavior): Boolean = hasEquivalentFields(this, other)
}

/**
 * Returns a [LayoutBehavior] with sizes set based on the provided [SizeConstraints].
 *
 * This method should only be used for Primitive Components which do not measure themselves - it's
 * the parent that has determined the exact size for this child.
 *
 * @throws IllegalArgumentException if the [SizeConstraints] are not exact
 */
object ExactSizeConstraintsLayoutBehavior : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(size = Size.exact(sizeConstraints))
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set based on the [SizeConstraints] if the constraints are
 * bounded, otherwise uses default values.
 *
 * @param defaultWidth The [Dimen] value for the width of the measured component when the provided
 *   maxWidth constraint is equal to Infinity.
 * @param defaultHeight The [Dimen] value for the height of the measured component when the provided
 *   maxHeight constraint is equal to Infinity.
 */
class FillLayoutBehavior(
    private val defaultWidth: Dimen,
    private val defaultHeight: Dimen,
) : LayoutBehavior {

  constructor(defaultWidth: Int, defaultHeight: Int) : this(defaultWidth.px, defaultHeight.px)

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size = Size.fillSpace(sizeConstraints, defaultWidth.toPixels(), defaultHeight.toPixels()))
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set based provided values.
 *
 * @param width The [Dimen] value for the width of the measured component.
 * @param height The [Dimen] value for the height of the measured component.
 */
class FixedSizeLayoutBehavior(
    private val width: Dimen,
    private val height: Dimen,
) : LayoutBehavior {

  constructor(width: Int, height: Int) : this(width.px, height.px)

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(width = width.toPixels(), height = height.toPixels())
  }
}

/**
 * Returns a [LayoutBehavior] with sizes set according to an aspect ratio and [SizeConstraints]. It
 * will respect the intrinsic size of the component being measured.
 *
 * @param aspectRatio The aspect ratio for calculating size.
 * @param intrinsicWidth The [Dimen] value for the intrinsic width of the measured component.
 * @param intrinsicHeight The [Dimen] value for the intrinsic height of the measured component.
 */
class AspectRatioLayoutBehavior(
    private val aspectRatio: Float,
    private val intrinsicWidth: Dimen,
    private val intrinsicHeight: Dimen,
) : LayoutBehavior {

  constructor(
      aspectRatio: Float,
      intrinsicWidth: Int,
      intrinsicHeight: Int
  ) : this(aspectRatio, intrinsicWidth.px, intrinsicHeight.px)

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(
        size =
            Size.withAspectRatio(
                sizeConstraints,
                aspectRatio,
                intrinsicWidth.toPixels(),
                intrinsicHeight.toPixels()))
  }
}

/**
 * Returns a [LayoutBehavior] that respects both [SizeConstraints] and tries to keep both width and
 * height equal. This will only not guarantee equal width and height if the constraints don't allow
 * for it.
 */
object EqualDimensionsLayoutBehavior : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    return PrimitiveLayoutResult(size = Size.withEqualDimensions(sizeConstraints))
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

  constructor(
      size: Size,
      paddingTop: Int = 0,
      paddingRight: Int = 0,
      paddingBottom: Int = 0,
      paddingLeft: Int = 0,
      layoutData: Any? = null
  ) : this(
      size.width, size.height, paddingTop, paddingRight, paddingBottom, paddingLeft, layoutData)

  init {
    if (width < 0) {
      throw IllegalArgumentException("width must be >= 0, but was: $width")
    }
    if (height < 0) {
      throw IllegalArgumentException("height must be >= 0, but was: $height")
    }
    if (width >= SizeConstraints.MaxValue) {
      throw IllegalArgumentException(
          "width must be < ${SizeConstraints.MaxValue}, but was: $width. Components this big may affect performance and lead to out of memory errors.")
    }
    if (height >= SizeConstraints.MaxValue) {
      throw IllegalArgumentException(
          "height must be < ${SizeConstraints.MaxValue}, but was: $height. Components this big may affect performance and lead to out of memory errors.")
    }
  }

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
