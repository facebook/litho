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

package com.facebook.rendercore

import com.facebook.rendercore.Size.Companion.BitMask

/**
 * A class representing 2D size.
 *
 * In order to avoid runtime overhead [Size] is a Kotlin value class that wraps a Long value. Width
 * and height are encoded in a single Long value.
 */
@JvmInline
value class Size internal constructor(@PublishedApi internal val encodedValue: Long) {
  val width: Int
    get() = encodedValue.shr(32).and(BitMask).toInt()

  val height: Int
    get() {
      return encodedValue.and(BitMask).toInt()
    }

  operator fun component1(): Int = width

  operator fun component2(): Int = height

  val isValid: Boolean
    get() {
      return encodedValue != Invalid.encodedValue
    }

  fun copy(
      width: Int = this.width,
      height: Int = this.height,
  ): Size {
    return Size(width, height)
  }

  override fun toString(): String {
    return if (isValid) {
      "Size[width = $width, height = $height]"
    } else {
      "Size[Invalid]"
    }
  }

  companion object {
    /**
     * A value representing an invalid [Size]. Accessing width and height of an invalid [Size] is
     * not possible.
     */
    val Invalid: Size = Size(width = -1, height = -1)

    internal const val BitMask: Long = 0xFFFFFFFF
  }
}

/** Creates [Size] from the provided width and height. */
fun Size(width: Int, height: Int): Size {
  val v1 = width.toLong()
  val v2 = height.toLong()
  val packed = v1.shl(32) or (v2 and BitMask)
  return Size(packed)
}

/**
 * Returns the current size if it's valid. Otherwise executes [action] lambda and returns its
 * result.
 */
inline fun Size.getOrElse(action: () -> Size): Size {
  return if (isValid) {
    this
  } else {
    action()
  }
}
