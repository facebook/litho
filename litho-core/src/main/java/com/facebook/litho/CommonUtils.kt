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

@file:JvmName("CommonUtils")

package com.facebook.litho

/** Polyfill of Objects.hash that can be used on API<19. */
fun hash(vararg values: Any?): Int = values.contentHashCode()

fun <T> mergeLists(a: List<T>?, b: List<T>?): List<T>? {
  if (a.isNullOrEmpty()) return b
  if (b.isNullOrEmpty()) return a
  return a + b
}

/**
 * Util method that returns the instance if it's not `null` otherwise calls [initBlock] to create a
 * new instance.
 */
inline fun <T> T?.getOrCreate(initBlock: () -> T): T = this ?: initBlock()

/**
 * Util method that returns the result of [block] if both [var1] and [var2] are not `null`.
 * Otherwise returns `null`.
 *
 * ```
 * allNotNull(var1, var2) { v1, v2 -> ... }
 * ```
 */
inline fun <A, B, R> allNotNull(var1: A?, var2: B?, block: (A, B) -> R): R? {
  return if (var1 != null && var2 != null) {
    block(var1, var2)
  } else null
}

/**
 * Util method that returns the result of [block] if all three [var1], [var2] and [var3] are not
 * `null`. Otherwise returns `null`.
 *
 * ```
 * allNotNull(var1, var2, var3) { v1, v2, v3 -> ... }
 * ```
 */
inline fun <A, B, C, R> allNotNull(var1: A?, var2: B?, var3: C?, block: (A, B, C) -> R): R? {
  return if (var1 != null && var2 != null && var3 != null) {
    block(var1, var2, var3)
  } else null
}
