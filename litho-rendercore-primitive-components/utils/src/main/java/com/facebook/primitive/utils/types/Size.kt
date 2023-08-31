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

package com.facebook.primitive.utils.types

import android.annotation.SuppressLint

@SuppressLint("HexColorValueUsage")
@JvmInline
value class Size internal constructor(@PublishedApi internal val packedValue: Long) {
  val width: Float
    get() {
      return Float.fromBits(packedValue.shr(32).toInt())
    }

  val height: Float
    get() {
      return Float.fromBits(packedValue.and(0xFFFFFFFF).toInt())
    }

  operator fun times(operand: Float): Size = Size(width * operand, height * operand)

  operator fun div(operand: Float): Size = Size(width / operand, height / operand)
}

@SuppressLint("HexColorValueUsage")
fun Size(width: Float, height: Float): Size {
  // pack two Float values into one Long value
  val v1 = width.toBits().toLong()
  val v2 = height.toBits().toLong()
  val packed = v1.shl(32) or (v2 and 0xFFFFFFFF)
  return Size(packed)
}

val Size.center: Point
  get() {
    return Point(width / 2f, height / 2f)
  }
