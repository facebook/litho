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
 * node. Values put in the LayoutCache (WriteCache) will be available for read in the next layout
 * pass as ReadCache.
 */
public final class LayoutCache {
  static final class CachedData {
    private final Map<Node<?>, Node.LayoutResult> mCacheByNode = new HashMap<>();
    private final LongSparseArray<Object> mCacheById = new LongSparseArray<>();
  }

  private final CachedData mWriteCache = new CachedData();
  private final @Nullable CachedData mReadCache;

  public LayoutCache() {
    this(null);
  }

  LayoutCache(@Nullable CachedData oldWriteCache) {
    mReadCache = oldWriteCache;
  }

  @Nullable
  public Node.LayoutResult get(Node<?> node) {
    return mReadCache == null ? null : mReadCache.mCacheByNode.get(node);
  }

  public void put(Node<?> node, Node.LayoutResult layout) {
    mWriteCache.mCacheByNode.put(node, layout);
  }

  @Nullable
  public <T> T get(long uniqueId) {
    //noinspection unchecked
    return (T) (mReadCache == null ? null : mReadCache.mCacheById.get(uniqueId));
  }

  public void put(long uniqueId, Object value) {
    mWriteCache.mCacheById.put(uniqueId, value);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public CachedData getWriteCacheData() {
    return mWriteCache;
  }
}
