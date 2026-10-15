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

package com.facebook.litho.animation

import com.facebook.rendercore.Dimen
import com.facebook.rendercore.ResourceResolver
import com.facebook.rendercore.graphics.Color
import com.facebook.rendercore.px
import kotlin.math.roundToInt

/**
 * Converts a value of type [T] to and from the [AnimationVector] the animation system runs on. Each
 * animated type decomposes into a fixed set of float components — 1 for a scalar like [Float], 2
 * for a size, 4 for a color — and the spring/tween machinery drives each component on its own, so a
 * multi-component value carries a real per-component value *and* per-component velocity rather than
 * being collapsed to one lossy scalar.
 *
 * [newVector] mints an empty vector of the right shape (via [AnimationVector.ofSize]); [toVector]
 * writes a value's components into a caller-owned vector so a per-frame animation allocates
 * nothing.
 *
 * A custom animated type implements all three methods, e.g. for a 3-component value:
 * ```kotlin
 * object PointConverter : TypeConverter<Point> {
 *   override fun newVector(): AnimationVector = AnimationVector.ofSize(3)
 *   override fun toVector(value: Point, vector: AnimationVector) {
 *     vector[0] = value.x
 *     vector[1] = value.y
 *     vector[2] = value.z
 *   }
 *   override fun fromVector(vector: AnimationVector): Point =
 *       Point(vector[0], vector[1], vector[2])
 * }
 * ```
 *
 * ## Built-in Converters
 * - [FloatTypeConverter] / [IntTypeConverter] / [DimenTypeConverter] — 1 component
 * - [ColorTypeConverter] — 4 components (alpha, red, green, blue)
 * - `IntSizeTypeConverter` (see `AnimatedSize`) — 2 components (width, height)
 *
 * @see AnimationVector
 * @see AnimationSpec
 * @see Animator
 */
interface TypeConverter<T> {

  /** Creates a zeroed [AnimationVector] shaped for [T], typically via [AnimationVector.ofSize]. */
  fun newVector(): AnimationVector

  /** Writes [value]'s components into [vector], which is shaped by this converter's [newVector]. */
  fun toVector(value: T, vector: AnimationVector)

  /** Reconstructs a [T] from a [vector] produced by this converter. */
  fun fromVector(vector: AnimationVector): T
}

/** Shorthand for [TypeConverter.newVector] followed by [TypeConverter.toVector] for [value]. */
internal fun <T> TypeConverter<T>.newVectorFromValue(value: T): AnimationVector =
    newVector().also { toVector(value, it) }

/** Identity [TypeConverter] for [Float] values. */
object FloatTypeConverter : TypeConverter<Float> {
  override fun newVector(): AnimationVector = AnimationVector1D()

  override fun toVector(value: Float, vector: AnimationVector) {
    vector[0] = value
  }

  override fun fromVector(vector: AnimationVector): Float = vector[0]
}

/** [TypeConverter] for [Int] values. Rounds the component back to the nearest integer. */
object IntTypeConverter : TypeConverter<Int> {
  override fun newVector(): AnimationVector = AnimationVector1D()

  override fun toVector(value: Int, vector: AnimationVector) {
    vector[0] = value.toFloat()
  }

  override fun fromVector(vector: AnimationVector): Int = vector[0].roundToInt()
}

/**
 * [TypeConverter] for [Color] values: 4 components (alpha, red, green, blue) blended independently,
 * so two colors that would collide under a single-float projection still animate correctly.
 */
object ColorTypeConverter : TypeConverter<Color> {
  override fun newVector(): AnimationVector = AnimationVector4D()

  override fun toVector(value: Color, vector: AnimationVector) {
    vector[0] = value.alpha
    vector[1] = value.red.toFloat()
    vector[2] = value.green.toFloat()
    vector[3] = value.blue.toFloat()
  }

  override fun fromVector(vector: AnimationVector): Color =
      Color.fromRgba(
          red = vector[1].roundToInt().coerceIn(0, 255),
          green = vector[2].roundToInt().coerceIn(0, 255),
          blue = vector[3].roundToInt().coerceIn(0, 255),
          alpha = vector[0].coerceIn(0f, 1f),
      )
}

/**
 * [TypeConverter] for [Dimen] values, animating in resolved pixels.
 *
 * @param resourceResolver Used to resolve dp/sp values to pixels.
 */
class DimenTypeConverter(private val resourceResolver: ResourceResolver) : TypeConverter<Dimen> {
  override fun newVector(): AnimationVector = AnimationVector1D()

  override fun toVector(value: Dimen, vector: AnimationVector) {
    vector[0] = value.toFloatPixels(resourceResolver)
  }

  override fun fromVector(vector: AnimationVector): Dimen = vector[0].px
}

/**
 * Evaluates an intermediate [Color] between two colors, per channel. Kept as a standalone helper
 * for call sites that blend colors directly.
 */
fun evaluateColor(fraction: Float, startColor: Color, endColor: Color): Color {
  val a = lerp(startColor.alpha, endColor.alpha, fraction).coerceIn(0f, 1f)
  val r = lerp(startColor.red, endColor.red, fraction).coerceIn(0, 255)
  val g = lerp(startColor.green, endColor.green, fraction).coerceIn(0, 255)
  val b = lerp(startColor.blue, endColor.blue, fraction).coerceIn(0, 255)

  return Color.fromRgba(red = r, green = g, blue = b, alpha = a)
}
