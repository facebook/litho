/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import com.facebook.litho.annotations.Hook

/**
 * Create a CachedValue variable within a Component. The [calculator] will provide the calculated
 * value if it hasn't already been calculated or if the inputs have changed since the previous
 * calculation.
 */
@Hook
fun <T> ComponentScope.useCached(vararg inputs: Any, calculator: () -> T): T {
  val globalKey = context.globalKey
  val hookIndex = useCachedIndex++
  val hookKey = "$globalKey:$hookIndex"
  val cacheInputs = CachedInputs(hookKey, inputs)
  val result =
      context.getCachedValue(cacheInputs)
          ?: calculator().also { context.putCachedValue(cacheInputs, it) }

  @Suppress("UNCHECKED_CAST") return result as T
}

internal class CachedInputs(val hookKey: String, val inputs: Array<out Any>) {
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (javaClass != other?.javaClass) {
      return false
    }

    other as CachedInputs

    return hookKey == other.hookKey && inputs.contentEquals(other.inputs)
  }

  override fun hashCode(): Int {
    var result = hookKey.hashCode()
    result = 31 * result + inputs.contentHashCode()
    return result
  }
}
