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

package com.facebook.rendercore;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Cache that can be used to reuse LayoutResults or parts of them across layout calculations. It's
 * responsibility of the implementer of the Layout function to put values in the cache for a given
 * node. Values put in the LayoutCache (WriteCache) will be available for read in the next layout
 * pass as ReadCache.
 */
public final class LayoutCache {

  public static final class CacheItem {
    private final LayoutResult mLayoutResult;
    private final int mWidthSpec;
    private final int mHeightSpec;

    public CacheItem(LayoutResult layoutResult, int widthSpec, int heightSpec) {
      mLayoutResult = layoutResult;
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
    }

    public LayoutResult getLayoutResult() {
      return mLayoutResult;
    }

    public int getWidthSpec() {
      return mWidthSpec;
    }

    public int getHeightSpec() {
      return mHeightSpec;
    }
  }

  public static final class CachedData {
    private final Map<Node<?>, CacheItem> mCacheByNode = new HashMap<>();
    private final LongSparseArray<Object> mCacheById = new LongSparseArray<>();

    public Map<Node<?>, CacheItem> getCacheByNode() {
      return Collections.unmodifiableMap(mCacheByNode);
    }
  }

  private final CachedData mWriteCache = new CachedData();
  private final @Nullable CachedData mReadCache;

  public LayoutCache() {
    this(null);
  }

  public LayoutCache(@Nullable CachedData oldWriteCache) {
    mReadCache = oldWriteCache;
  }

  @Nullable
  public CacheItem get(Node<?> node) {
    return mReadCache == null ? null : mReadCache.mCacheByNode.get(node);
  }

  public void put(Node<?> node, CacheItem cacheItem) {
    mWriteCache.mCacheByNode.put(node, cacheItem);
  }

  @Nullable
  public <T> T get(long uniqueId) {
    //noinspection unchecked
    return (T) (mReadCache == null ? null : mReadCache.mCacheById.get(uniqueId));
  }

  public void put(long uniqueId, Object value) {
    mWriteCache.mCacheById.put(uniqueId, value);
  }

  public CachedData getWriteCacheData() {
    return mWriteCache;
  }
}
