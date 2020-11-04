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

package com.facebook.rendercore;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import java.util.HashMap;
import java.util.Map;

/**
 * A Cache that can be used to reuse LayoutResults or parts of them across layout calculations. It's
 * responsibility of the implementer of the Layout function to put values in the cache for a given
 * node. Values put in the LayoutCache will only be available in the next layout pass.
 */
public class LayoutCache {
  private final Map<Node<?>, Node.LayoutResult<?>> mWriteCacheByNode = new HashMap<>();
  private final LongSparseArray<Object> mWriteCacheById = new LongSparseArray<>();

  private final Map<Node<?>, Node.LayoutResult<?>> mReadCacheByNode;
  private final LongSparseArray<Object> mReadCacheById;

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public LayoutCache() {
    this(null);
  }

  LayoutCache(@Nullable LayoutCache oldCache) {
    if (oldCache == null) {
      mReadCacheByNode = new HashMap<>();
      mReadCacheById = new LongSparseArray<>();
    } else {
      mReadCacheByNode = oldCache.mWriteCacheByNode;
      mReadCacheById = oldCache.mWriteCacheById;
    }
  }

  @Nullable
  public Node.LayoutResult<?> get(Node<?> node) {
    return mReadCacheByNode.get(node);
  }

  public void put(Node<?> node, Node.LayoutResult<?> layout) {
    mWriteCacheByNode.put(node, layout);
  }

  @Nullable
  public <T> T get(long uniqueId) {
    //noinspection unchecked
    return (T) mReadCacheById.get(uniqueId);
  }

  public void put(long uniqueId, Object value) {
    mWriteCacheById.put(uniqueId, value);
  }
}
