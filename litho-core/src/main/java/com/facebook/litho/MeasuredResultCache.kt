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

import androidx.collection.SparseArrayCompat
import java.util.HashMap
import kotlin.jvm.JvmField

/**
 * Read & write layout result cache used during render phase for caching measured results that
 * happen during component-measure. This cache can be accessed via components or litho-nodes.
 */
class MeasuredResultCache
/**
 * Create a new MeasureResultCache with a delegate cache. When reading caches, if they are not found
 * in this cache instance, the delegate cache is checked.
 */
@JvmOverloads
constructor(private val delegateCache: MeasuredResultCache? = null) {
  private val componentIdToNodeCache = SparseArrayCompat<LithoNode>()
  private val nodeToResultCache: MutableMap<LithoNode, LithoLayoutResult> = HashMap()
  private var isFrozen = false

  /**
   * Marks this cache as frozen, meaning it is illegal to write new values into it, and should be
   * used as read-only. Attempting to write to a frozen cache will produce an illegal state
   * exception.
   */
  fun freezeCache() {
    isFrozen = true
  }

  /**
   * Add a cached result to the cache.
   *
   * @param component The component from which the result was generated
   * @param node The node generated from the component
   * @param layoutResult The layout result
   */
  fun addCachedResult(component: Component, node: LithoNode, layoutResult: LithoLayoutResult) {
    addCachedResult(component.id, node, layoutResult)
  }

  /**
   * Add a cached result to the cache.
   *
   * @param componentId The component ID from which the result was generated
   * @param node The node generated from the component
   * @param layoutResult The layout result
   */
  fun addCachedResult(componentId: Int, node: LithoNode, layoutResult: LithoLayoutResult) {
    check(!isFrozen) { "Cannot write into a frozen cache." }
    componentIdToNodeCache.put(componentId, node)
    nodeToResultCache[node] = layoutResult
  }

  /** Return true if there exists a cached layout result for the given component. */
  fun hasCachedNode(component: Component): Boolean = hasCachedNode(component.id)

  /** Return true if there exists a cached layout result for the given component ID. */
  fun hasCachedNode(componentId: Int): Boolean =
      componentIdToNodeCache.containsKey(componentId) ||
          delegateCache?.hasCachedNode(componentId) == true

  /** Return true if there exists a cached layout result for the given LithoNode. */
  fun hasCachedNode(node: LithoNode): Boolean =
      nodeToResultCache.containsKey(node) || delegateCache?.hasCachedNode(node) == true

  /** Returns the cached LithoNode from a given component. */
  fun getCachedNode(component: Component): LithoNode? = getCachedNode(component.id)

  /** Returns the cached LithoNode from a given component ID. */
  fun getCachedNode(componentId: Int): LithoNode? {
    val currentCacheNode = componentIdToNodeCache[componentId]
    return currentCacheNode ?: delegateCache?.getCachedNode(componentId)
  }

  /** Returns the cached layout result for the given component, or null if it does not exist. */
  fun getCachedResult(component: Component): LithoLayoutResult? = getCachedResult(component.id)

  /** Returns the cached layout result for the given component ID, or null if it does not exist. */
  fun getCachedResult(componentId: Int): LithoLayoutResult? {
    val node =
        componentIdToNodeCache[componentId] ?: return delegateCache?.getCachedResult(componentId)
    return getCachedResult(node)
  }

  /** Returns the cached layout result for the given node, or null if it does not exist. */
  fun getCachedResult(node: LithoNode): LithoLayoutResult? {
    val currentLayoutResult = nodeToResultCache[node]
    return currentLayoutResult ?: delegateCache?.getCachedResult(node)
  }

  /** Remove the cached layout result for the given Node. */
  fun removeCachedResult(node: LithoNode) {
    // Should we also check for freezing here?
    nodeToResultCache.remove(node)
    delegateCache?.removeCachedResult(node)
  }

  /** Cleares the cache generated for the given component. */
  fun clearCache(component: Component) {
    clearCache(component.id)
  }

  /** Cleares the cache generated for the given component ID. */
  fun clearCache(componentId: Int) {
    check(!isFrozen) { "Cannot delete from a frozen cache" }
    val node = componentIdToNodeCache[componentId] ?: return
    nodeToResultCache.remove(node)
    componentIdToNodeCache.remove(componentId)
  }

  companion object {
    @JvmField val EMPTY = MeasuredResultCache().apply { freezeCache() }
  }
}
