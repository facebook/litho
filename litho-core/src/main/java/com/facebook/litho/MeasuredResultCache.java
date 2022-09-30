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

package com.facebook.litho;

import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.Map;

/**
 * Read & write layout result cache used during render phase for caching measured results that
 * happen during component-measure. This cache can be accessed via components or litho-nodes.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class MeasuredResultCache {
  private final SparseArrayCompat<LithoNode> mComponentIdToNodeCache = new SparseArrayCompat<>();
  private final Map<LithoNode, LithoLayoutResult> mNodeToResultCache = new HashMap<>();

  private @Nullable MeasuredResultCache mDelegateCache;

  private boolean mIsFrozen;

  public MeasuredResultCache() {}

  /**
   * Create a new MeasureResultCache with a delegate cache. When reading caches, if they are not
   * found in this cache instance, the delegate cache is checked.
   */
  public MeasuredResultCache(final MeasuredResultCache delegateCache) {
    mDelegateCache = delegateCache;
  }

  /**
   * Marks this cache as frozen, meaning it is illegal to write new values into it, and should be
   * used as read-only. Attempting to write to a frozen cache will produce an illegal state
   * exception.
   */
  public void freezeCache() {
    mIsFrozen = true;
  }

  /**
   * Add a cached result to the cache.
   *
   * @param component The component from which the result was generated
   * @param node The node generated from the component
   * @param layoutResult The layout result
   */
  public void addCachedResult(
      final Component component, final LithoNode node, final LithoLayoutResult layoutResult) {
    addCachedResult(component.getId(), node, layoutResult);
  }

  /**
   * Add a cached result to the cache.
   *
   * @param componentId The component ID from which the result was generated
   * @param node The node generated from the component
   * @param layoutResult The layout result
   */
  public void addCachedResult(
      final int componentId, final LithoNode node, final LithoLayoutResult layoutResult) {
    if (mIsFrozen) {
      throw new IllegalStateException("Cannot write into a frozen cache.");
    }

    mComponentIdToNodeCache.put(componentId, node);
    mNodeToResultCache.put(node, layoutResult);
  }

  /** Return true if there exists a cached layout result for the given component. */
  public boolean hasCachedNode(final Component component) {
    return hasCachedNode(component.getId());
  }

  /** Return true if there exists a cached layout result for the given component ID. */
  public boolean hasCachedNode(final int componentId) {
    return mComponentIdToNodeCache.containsKey(componentId)
        || (mDelegateCache != null && mDelegateCache.hasCachedNode(componentId));
  }

  /** Return true if there exists a cached layout result for the given LithoNode. */
  public boolean hasCachedNode(final LithoNode node) {
    return mNodeToResultCache.containsKey(node)
        || (mDelegateCache != null && mDelegateCache.hasCachedNode(node));
  }

  /** Returns the cached LithoNode from a given component. */
  @Nullable
  public LithoNode getCachedNode(final Component component) {
    return getCachedNode(component.getId());
  }

  /** Returns the cached LithoNode from a given component ID. */
  @Nullable
  public LithoNode getCachedNode(final int componentId) {
    final LithoNode currentCacheNode = mComponentIdToNodeCache.get(componentId);

    if (currentCacheNode == null && mDelegateCache != null) {
      return mDelegateCache.getCachedNode(componentId);
    }

    return currentCacheNode;
  }

  /** Returns the cached layout result for the given component, or null if it does not exist. */
  @Nullable
  public LithoLayoutResult getCachedResult(final Component component) {
    return getCachedResult(component.getId());
  }

  /** Returns the cached layout result for the given component ID, or null if it does not exist. */
  @Nullable
  public LithoLayoutResult getCachedResult(final int componentId) {
    final @Nullable LithoNode node = mComponentIdToNodeCache.get(componentId);

    if (node == null) {
      if (mDelegateCache != null) {
        return mDelegateCache.getCachedResult(componentId);
      }

      return null;
    }

    return getCachedResult(node);
  }

  /** Returns the cached layout result for the given node, or null if it does not exist. */
  @Nullable
  public LithoLayoutResult getCachedResult(final LithoNode node) {
    final LithoLayoutResult currentLayoutResult = mNodeToResultCache.get(node);

    if (currentLayoutResult == null && mDelegateCache != null) {
      return mDelegateCache.getCachedResult(node);
    }

    return currentLayoutResult;
  }

  /** Cleares the cache generated for the given component. */
  public void clearCache(final Component component) {
    clearCache(component.getId());
  }

  /** Cleares the cache generated for the given component ID. */
  public void clearCache(final int componentId) {
    if (mIsFrozen) {
      throw new IllegalStateException("Cannot delete from a frozen cache");
    }

    final @Nullable LithoNode node = mComponentIdToNodeCache.get(componentId);

    if (node == null) {
      return;
    }

    mNodeToResultCache.remove(node);
    mComponentIdToNodeCache.remove(componentId);
  }
}
