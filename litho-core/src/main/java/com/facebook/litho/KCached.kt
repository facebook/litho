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
 *
 * ## Lifetime: a cached value can outlive the ComponentTree that created it
 *
 * Cached values are stored in the [TreeState] of the enclosing ComponentTree and have no pruning
 * path: unlike state, which `StateHandler.clearUnusedState` drops once a component leaves the tree,
 * cached values are copied forward verbatim into every subsequent `StateHandler`.
 *
 * Inside a collection this is compounded by the fact that a row's `TreeState` is deliberately
 * retained when the row leaves the prepared range, so that state can be restored on scroll-back
 * (`RecyclerBinder.Builder.acquireStateHandlerOnRelease` defaults to `true`, and
 * `LithoRenderer.cleanup` does not clear `treeState`). A cached value therefore survives release of
 * the row's ComponentTree and is only freed when the collection's data set is replaced: retention
 * grows with the number of rows ever scrolled past, not the number of rows on screen. The same
 * applies to a value cached by a throwaway component built only to measure, since
 * `Component.measure` resolves against the enclosing `TreeState`.
 *
 * Because of this, do not cache objects that are expensive to retain — inflated animation graphs,
 * bitmaps, view hierarchies, anything holding native memory — in a component that can appear in a
 * collection item. For those, either recompute per render, or hold the object in a bounded,
 * evicting cache owned outside the tree and cache only a cheap key here.
 */
@Hook
fun <T> ComponentScope.useCached(vararg inputs: Any?, calculator: () -> T): T {

  if (context.lithoConfiguration.componentsConfig.useStateForCachedValues) {
    // Store the cached value in the state container rather than the legacy cached-values map.
    return getUpdatedState(deps = inputs, initializer = calculator)
  } else {
    val globalKey = context.globalKey
    val cacheKey = HookGlobalKey(globalKey, requireCurrentHookKey(), RenderScope.Resolve)
    val cacheInputs = CachedInputs(inputs)
    val result =
        context.getCachedValue(cacheKey, cacheInputs)
            ?: calculator().also { context.putCachedValue(cacheKey, cacheInputs, it) }

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
