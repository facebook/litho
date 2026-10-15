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

/**
 * A small, mutable, fixed-size holder for the float components an animated value decomposes into —
 * the currency the spring/tween machinery computes on. A scalar animates as one component, a size
 * as two, a color as four.
 *
 * The [get]/[set] operators let callers treat it like an indexed buffer (`vector[i]`). Concrete
 * shapes are an implementation detail; obtain one via [ofSize], which a [TypeConverter] uses to
 * implement [TypeConverter.newVector]. The small fixed sizes are backed by plain fields (no
 * [FloatArray] indirection); larger sizes fall back to an array.
 *
 * @see TypeConverter
 * @see Animator
 */
sealed class AnimationVector {

  /** Number of float components this vector holds. */
  abstract val size: Int

  /** Reads component [index]; out-of-range indices read as `0f`. */
  abstract operator fun get(index: Int): Float

  /** Writes component [index]; out-of-range indices are ignored. */
  abstract operator fun set(index: Int, value: Float)

  /** Zeros every component. */
  internal fun reset() {
    for (i in 0 until size) this[i] = 0f
  }

  companion object {
    /**
     * Creates a zeroed [AnimationVector] with [size] components. This is the entry point a custom
     * [TypeConverter] returns from [TypeConverter.newVector].
     *
     * @param size Number of components; must be at least 1.
     */
    fun ofSize(size: Int): AnimationVector {
      require(size >= 1) { "AnimationVector size must be >= 1, but was $size" }
      return when (size) {
        1 -> AnimationVector1D()
        2 -> AnimationVector2D()
        3 -> AnimationVector3D()
        4 -> AnimationVector4D()
        else -> AnimationVectorND(size)
      }
    }
  }
}

/** A one-component [AnimationVector] for scalar types such as [Float], [Int], and [Dimen]. */
internal class AnimationVector1D(var value: Float = 0f) : AnimationVector() {
  override val size: Int
    get() = 1

  override fun get(index: Int): Float = if (index == 0) value else 0f

  override fun set(index: Int, value: Float) {
    if (index == 0) this.value = value
  }

  override fun toString(): String = "AnimationVector1D($value)"
}

/** A two-component [AnimationVector], e.g. the width and height of a size. */
internal class AnimationVector2D(var v1: Float = 0f, var v2: Float = 0f) : AnimationVector() {
  override val size: Int
    get() = 2

  override fun get(index: Int): Float =
      when (index) {
        0 -> v1
        1 -> v2
        else -> 0f
      }

  override fun set(index: Int, value: Float) {
    when (index) {
      0 -> v1 = value
      1 -> v2 = value
    }
  }

  override fun toString(): String = "AnimationVector2D($v1, $v2)"
}

/** A three-component [AnimationVector], e.g. a point or translation in 3-space. */
internal class AnimationVector3D(
    var v1: Float = 0f,
    var v2: Float = 0f,
    var v3: Float = 0f,
) : AnimationVector() {
  override val size: Int
    get() = 3

  override fun get(index: Int): Float =
      when (index) {
        0 -> v1
        1 -> v2
        2 -> v3
        else -> 0f
      }

  override fun set(index: Int, value: Float) {
    when (index) {
      0 -> v1 = value
      1 -> v2 = value
      2 -> v3 = value
    }
  }

  override fun toString(): String = "AnimationVector3D($v1, $v2, $v3)"
}

/** A four-component [AnimationVector], e.g. the alpha, red, green, and blue of a color. */
internal class AnimationVector4D(
    var v1: Float = 0f,
    var v2: Float = 0f,
    var v3: Float = 0f,
    var v4: Float = 0f,
) : AnimationVector() {
  override val size: Int
    get() = 4

  override fun get(index: Int): Float =
      when (index) {
        0 -> v1
        1 -> v2
        2 -> v3
        3 -> v4
        else -> 0f
      }

  override fun set(index: Int, value: Float) {
    when (index) {
      0 -> v1 = value
      1 -> v2 = value
      2 -> v3 = value
      3 -> v4 = value
    }
  }

  override fun toString(): String = "AnimationVector4D($v1, $v2, $v3, $v4)"
}

/**
 * An [AnimationVector] of arbitrary length, backed by a [FloatArray]. Used for the uncommon case of
 * five or more components; the smaller shapes use field-backed variants instead.
 */
internal class AnimationVectorND(size: Int) : AnimationVector() {
  private val components = FloatArray(size)

  override val size: Int
    get() = components.size

  override fun get(index: Int): Float = if (index in components.indices) components[index] else 0f

  override fun set(index: Int, value: Float) {
    if (index in components.indices) components[index] = value
  }

  override fun toString(): String = "AnimationVectorND(${components.joinToString()})"
}
