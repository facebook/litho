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

import android.graphics.ComposePathEffect
import android.graphics.DashPathEffect
import android.graphics.DiscretePathEffect
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.PathEffect
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.Dimension
import androidx.annotation.IntDef
import androidx.annotation.Px
import com.facebook.rendercore.Equivalence
import com.facebook.rendercore.ResourceResolver
import com.facebook.yoga.YogaEdge

/**
 * Represents a collection of attributes that describe how a border should be applied to a layout
 */
class Border private constructor() : Equivalence<Border> {

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(
      flag = true,
      value = [Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT, Corner.BOTTOM_LEFT])
  annotation class Corner {
    companion object {
      const val TOP_LEFT: Int = 0
      const val TOP_RIGHT: Int = 1
      const val BOTTOM_RIGHT: Int = 2
      const val BOTTOM_LEFT: Int = 3
    }
  }

  @JvmField val radius: FloatArray = FloatArray(RADIUS_COUNT)
  @JvmField val edgeWidths: IntArray = IntArray(EDGE_COUNT)
  @JvmField val edgeColors: IntArray = IntArray(EDGE_COUNT)
  @JvmField var pathEffect: PathEffect? = null

  fun setEdgeWidth(edge: YogaEdge, width: Int) {
    require(width >= 0) { "Given negative border width value: ${width} for edge ${edge.name}" }
    setEdgeValue(edgeWidths, edge, width)
  }

  fun setEdgeColor(edge: YogaEdge, @ColorInt color: Int) {
    setEdgeValue(edgeColors, edge, color)
  }

  class Builder internal constructor(context: ComponentContext) {
    private val border: Border = Border()
    private var resourceResolver: ResourceResolver? = context.resourceResolver
    private val pathEffects = arrayOfNulls<PathEffect>(MAX_PATH_EFFECTS)
    private var numPathEffects = 0

    /**
     * Specifies a width for a specific edge
     *
     * Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The [YogaEdge] that will have its width modified
     * @param width The desired width in raw pixels
     */
    fun widthPx(edge: YogaEdge, @Px width: Int): Builder {
      checkNotBuilt()
      border.setEdgeWidth(edge, width)
      return this
    }
    /**
     * Specifies a width for a specific edge
     *
     * Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The [YogaEdge] that will have its width modified
     * @param width The desired width in density independent pixels
     */
    fun widthDip(edge: YogaEdge, @Dimension(unit = Dimension.DP) width: Float): Builder {
      val resolver = checkNotBuilt()
      return widthPx(edge, resolver.dipsToPixels(width))
    }
    /**
     * Specifies a width for a specific edge
     *
     * Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The [YogaEdge] that will have its width modified
     * @param widthRes The desired width resource to resolve
     */
    fun widthRes(edge: YogaEdge, @DimenRes widthRes: Int): Builder {
      val resolver = checkNotBuilt()
      return widthPx(edge, resolver.resolveDimenSizeRes(widthRes))
    }
    /**
     * Specifies a width for a specific edge
     *
     * Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The [YogaEdge] that will have its width modified
     * @param attrId The attribute to resolve a width value from
     */
    fun widthAttr(edge: YogaEdge, @AttrRes attrId: Int): Builder {
      checkNotBuilt()
      return widthAttr(edge, attrId, 0)
    }
    /**
     * Specifies a width for a specific edge
     *
     * Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The [YogaEdge] that will have its width modified
     * @param attrId The attribute to resolve a width value from
     * @param defaultResId Default resource value to utilize if the attribute is not set
     */
    fun widthAttr(edge: YogaEdge, @AttrRes attrId: Int, @DimenRes defaultResId: Int): Builder {
      val resolver = checkNotBuilt()
      return widthPx(edge, resolver.resolveDimenSizeAttr(attrId, defaultResId))
    }
    /**
     * Specifies the border radius for all corners
     *
     * @param radius The desired border radius for all corners
     */
    fun radiusPx(@Px radius: Int): Builder {
      checkNotBuilt()
      border.radius.fill(radius.toFloat())
      return this
    }
    /**
     * Specifies the border radius for all corners
     *
     * @param radius The desired border radius for all corners
     */
    fun radiusDip(@Dimension(unit = Dimension.DP) radius: Float): Builder {
      val resolver = checkNotBuilt()
      return radiusPx(resolver.dipsToPixels(radius))
    }
    /**
     * Specifies the border radius for all corners
     *
     * @param radiusRes The resource id to retrieve the border radius value from
     */
    fun radiusRes(@DimenRes radiusRes: Int): Builder {
      val resolver = checkNotBuilt()
      return radiusPx(resolver.resolveDimenSizeRes(radiusRes))
    }
    /**
     * Specifies the border radius for all corners
     *
     * @param attrId The attribute id to retrieve the border radius value from
     * @param defaultResId Default resource to utilize if the attribute is not set
     */
    @JvmOverloads
    fun radiusAttr(@AttrRes attrId: Int, @DimenRes defaultResId: Int = 0): Builder {
      val resolver = checkNotBuilt()
      return radiusPx(resolver.resolveDimenSizeAttr(attrId, defaultResId))
    }
    /**
     * Specifies the border radius for the given corner
     *
     * @param corner The [Corner] to specify the radius of
     * @param radius The desired radius
     */
    fun radiusPx(@Corner corner: Int, @Px radius: Int): Builder {
      checkNotBuilt()
      require(corner in 0 until RADIUS_COUNT) { "Given invalid corner: $corner" }
      require(radius >= 0f) { "Can't have a negative radius value" }
      border.radius[corner] = radius.toFloat()
      return this
    }
    /**
     * Specifies the border radius for the given corner
     *
     * @param corner The [Corner] to specify the radius of
     * @param radius The desired radius
     */
    fun radiusDip(@Corner corner: Int, @Dimension(unit = Dimension.DP) radius: Float): Builder {
      val resolver = checkNotBuilt()
      return radiusPx(corner, resolver.dipsToPixels(radius))
    }
    /**
     * Specifies the border radius for the given corner
     *
     * @param corner The [Corner] to specify the radius of
     * @param res The desired dimension resource to use for the radius
     */
    fun radiusRes(@Corner corner: Int, @DimenRes res: Int): Builder {
      val resolver = checkNotBuilt()
      return radiusPx(corner, resolver.resolveDimenSizeRes(res))
    }
    /**
     * Specifies the border radius for the given corner
     *
     * @param corner The [Corner] to specify the radius of
     * @param attrId The attribute ID to retrieve the radius from
     * @param defaultResId Default resource ID to use if the attribute is not set
     */
    fun radiusAttr(
        @Corner corner: Int,
        @AttrRes attrId: Int,
        @DimenRes defaultResId: Int
    ): Builder {
      val resolver = checkNotBuilt()
      return radiusPx(corner, resolver.resolveDimenSizeAttr(attrId, defaultResId))
    }
    /**
     * Specifies a color for a specific edge
     *
     * @param edge The [YogaEdge] that will have its color modified
     * @param color The raw color value to use
     */
    fun color(edge: YogaEdge, @ColorInt color: Int): Builder {
      checkNotBuilt()
      border.setEdgeColor(edge, color)
      return this
    }
    /**
     * Specifies a color for a specific edge
     *
     * @param edge The [YogaEdge] that will have its color modified
     * @param colorRes The color resource to use
     */
    fun colorRes(edge: YogaEdge, @ColorRes colorRes: Int): Builder {
      val resolver = checkNotBuilt()
      return color(edge, resolver.resolveColorRes(colorRes))
    }
    /**
     * Applies a dash effect to the border
     *
     * Specifying two effects will compose them where the first specified effect acts as the outer
     * effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param intervals Must be even-sized >= 2. Even indices specify "on" intervals and odd indices
     *   specify "off" intervals
     * @param phase Offset into the given intervals
     */
    fun dashEffect(intervals: FloatArray, phase: Float): Builder {
      checkNotBuilt()
      checkEffectCount()
      pathEffects[numPathEffects++] = DashPathEffect(intervals, phase)
      return this
    }
    /**
     * Applies a discrete effect to the border
     *
     * Specifying two effects will compose them where the first specified effect acts as the outer
     * effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param segmentLength Length of line segments
     * @param deviation Maximum amount of deviation. Utilized value is random in the range
     *   [-deviation, deviation]
     */
    fun discreteEffect(segmentLength: Float, deviation: Float): Builder {
      checkNotBuilt()
      checkEffectCount()
      pathEffects[numPathEffects++] = DiscretePathEffect(segmentLength, deviation)
      return this
    }
    /**
     * Applies a path dash effect to the border
     *
     * Specifying two effects will compose them where the first specified effect acts as the outer
     * effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param shape The path to stamp along
     * @param advance The spacing between each stamp
     * @param phase Amount to offset before the first stamp
     * @param style How to transform the shape at each position
     */
    fun pathDashEffect(
        shape: Path?,
        advance: Float,
        phase: Float,
        style: PathDashPathEffect.Style?
    ): Builder {
      checkNotBuilt()
      checkEffectCount()
      pathEffects[numPathEffects++] = PathDashPathEffect(shape, advance, phase, style)
      return this
    }

    fun build(): Border {
      checkNotBuilt()
      resourceResolver = null
      if (numPathEffects == MAX_PATH_EFFECTS) {
        border.pathEffect = ComposePathEffect(pathEffects[0], pathEffects[1])
      } else if (numPathEffects > 0) {
        border.pathEffect = pathEffects[0]
      }
      require(border.pathEffect == null || equalValues(border.edgeWidths)) {
        "Borders do not currently support different widths with a path effect"
      }
      return border
    }

    private fun checkNotBuilt(): ResourceResolver {
      return checkNotNull(resourceResolver) { "This builder has already been disposed / built!" }
    }

    private fun checkEffectCount() {
      require(numPathEffects < MAX_PATH_EFFECTS) {
        "You cannot specify more than 2 effects to compose"
      }
    }

    companion object {
      private const val MAX_PATH_EFFECTS = 2
    }
  }

  override fun isEquivalentTo(other: Border): Boolean {
    if (this === other) {
      return true
    }
    return radius.contentEquals(other.radius) &&
        edgeWidths.contentEquals(other.edgeWidths) &&
        edgeColors.contentEquals(other.edgeColors) &&
        pathEffect === other.pathEffect
  }

  companion object {
    const val EDGE_LEFT: Int = 0
    const val EDGE_TOP: Int = 1
    const val EDGE_RIGHT: Int = 2
    const val EDGE_BOTTOM: Int = 3
    const val EDGE_COUNT: Int = 4
    const val RADIUS_COUNT: Int = 4

    @JvmStatic fun create(context: ComponentContext): Builder = Builder(context)

    @JvmStatic
    fun getEdgeColor(colorArray: IntArray, edge: YogaEdge): Int {
      require(colorArray.size == EDGE_COUNT) { "Given wrongly sized array" }
      return colorArray[edgeIndex(edge)]
    }

    @JvmStatic
    fun edgeFromIndex(i: Int): YogaEdge {
      require(i in 0 until EDGE_COUNT) { "Given index out of range of acceptable edges: $i" }
      return when (i) {
        EDGE_LEFT -> YogaEdge.LEFT
        EDGE_TOP -> YogaEdge.TOP
        EDGE_RIGHT -> YogaEdge.RIGHT
        EDGE_BOTTOM -> YogaEdge.BOTTOM
        else -> throw IllegalArgumentException("Given unknown edge index: $i")
      }
    }
    /**
     * @param values values pertaining to [YogaEdge]s
     * @return whether the values are equal for each edge
     */
    @JvmStatic
    fun equalValues(values: IntArray): Boolean {
      require(values.size == EDGE_COUNT) { "Given wrongly sized array" }
      val lastValue = values[0]
      val length = values.size
      for (i in 1 until length) {
        if (values[i] != lastValue) return false
      }
      return true
    }

    @JvmStatic
    fun setEdgeValue(edges: IntArray, edge: YogaEdge, value: Int) {
      when (edge) {
        YogaEdge.ALL -> {
          for (i in 0 until EDGE_COUNT) edges[i] = value
        }
        YogaEdge.VERTICAL -> {
          edges[EDGE_TOP] = value
          edges[EDGE_BOTTOM] = value
        }
        YogaEdge.HORIZONTAL -> {
          edges[EDGE_LEFT] = value
          edges[EDGE_RIGHT] = value
        }
        YogaEdge.LEFT,
        YogaEdge.TOP,
        YogaEdge.RIGHT,
        YogaEdge.BOTTOM,
        YogaEdge.START,
        YogaEdge.END -> edges[edgeIndex(edge)] = value
      }
    }

    private fun edgeIndex(edge: YogaEdge): Int {
      return when (edge) {
        YogaEdge.START,
        YogaEdge.LEFT -> EDGE_LEFT
        YogaEdge.TOP -> EDGE_TOP
        YogaEdge.END,
        YogaEdge.RIGHT -> EDGE_RIGHT
        YogaEdge.BOTTOM -> EDGE_BOTTOM
        else -> throw IllegalArgumentException("Given unsupported edge ${edge.name}")
      }
    }
  }
}
