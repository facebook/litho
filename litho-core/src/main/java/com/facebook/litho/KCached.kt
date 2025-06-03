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

import com.facebook.litho.annotations.Hook
import com.facebook.rendercore.utils.areObjectsEquivalent

/**
 * Create a CachedValue variable within a Component. The [calculator] will provide the calculated
 * value if it hasn't already been calculated or if the inputs have changed since the previous
 * calculation.
 */
@Hook
fun <T> ComponentScope.useCached(vararg inputs: Any?, calculator: () -> T): T {

  if (context.lithoConfiguration.componentsConfig.useStateForCachedValues) {
    return getUpdatedState(deps = inputs, initializer = calculator)
  } else {
    val globalKey = context.globalKey
    val hookIndex = useCachedIndex++
    val cacheInputs = CachedInputs(inputs)
    val result =
        context.getCachedValue(globalKey, hookIndex, cacheInputs)
            ?: calculator().also { context.putCachedValue(globalKey, hookIndex, cacheInputs, it) }

    @Suppress("UNCHECKED_CAST")
    return result as T
  }
}

/**
 * Wrapper class for the inputs to a [useCached].
 *
 * This class implements [equals] and [hashCode] that delegates to the appropriate implementations,
 * to ensure correctness
 */
internal class CachedInputs(private val inputs: Array<*>) {
  override fun equals(other: Any?): Boolean {
    return other is CachedInputs && areObjectsEquivalent(inputs, other.inputs)
  }

  override fun hashCode(): Int = inputs.contentHashCode()
}
