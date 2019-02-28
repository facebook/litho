/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.reference;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.StateSet;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pools;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A cache that holds Drawables retreived from Android {@link android.content.res.Resources} for
 * each resId this class keeps a {@link androidx.core.util.Pools.SynchronizedPool} of
 * DRAWABLES_POOL_MAX_ITEMS. The cache has a maximum capacity of DRAWABLES_MAX_ENTRIES. When the
 * cache is full it starts clearing memory deleting the less recently used pool of resources
 */
class DrawableResourcesCache {

  private static final int DRAWABLES_MAX_ENTRIES = 200;
  private static final int DRAWABLES_POOL_MAX_ITEMS = 10;

  private final LruCache<Integer, SimplePoolWithCount<Drawable>> mDrawableCache;

  DrawableResourcesCache() {
    mDrawableCache = new LruCache<Integer, SimplePoolWithCount<Drawable>>(DRAWABLES_MAX_ENTRIES) {
      @Override
      protected int sizeOf(Integer key, SimplePoolWithCount<Drawable> value) {
        return value.getPoolSize();
      }
    };
  }

  /**
   * @deprecated use {@link #get(int, Resources, Resources.Theme)}
   */
  @Nullable
  @Deprecated
  public Drawable get(int resId, Resources resources) {
    return get(resId, resources, null);
  }

  @Nullable
  public Drawable get(int resId, Resources resources, @Nullable Resources.Theme theme) {
    SimplePoolWithCount<Drawable> drawablesPool = mDrawableCache.get(resId);

    if (drawablesPool == null) {
      drawablesPool = new SimplePoolWithCount<>(DRAWABLES_POOL_MAX_ITEMS);
      mDrawableCache.put(resId, drawablesPool);
    }

    Drawable drawable = drawablesPool.acquire();

    if (drawable == null) {
      drawable = ResourcesCompat.getDrawable(resources, resId, theme);
    }

    // We never want this pool to remain empty otherwise we would risk to resolve a new drawable
    // when get is called again. So if the pool is about to drain we just put a new Drawable in it
    // to keep it warm.
    if (drawable != null && drawablesPool.getPoolSize() == 0) {
      drawablesPool.release(drawable.getConstantState().newDrawable());
    }

    return drawable;
  }

  public void release(Drawable drawable, int resId) {
    SimplePoolWithCount<Drawable> drawablesPool = mDrawableCache.get(resId);
    if (drawablesPool == null) {
      drawablesPool = new SimplePoolWithCount<>(DRAWABLES_POOL_MAX_ITEMS);
      mDrawableCache.put(resId, drawablesPool);
    }

    // Reset a stateful drawable, and its animations, before being released.
    if (drawable.isStateful()) {
      drawable.setState(StateSet.WILD_CARD);

      if (SDK_INT >= HONEYCOMB) {
        drawable.jumpToCurrentState();
      }
    }

    drawablesPool.release(drawable);
  }

  private static class SimplePoolWithCount<T> extends Pools.SynchronizedPool<T> {

    private final AtomicInteger mPoolSize;

    public SimplePoolWithCount(int maxPoolSize) {
      super(maxPoolSize);
      mPoolSize = new AtomicInteger(0);
    }

    @Override
    public T acquire() {
      T item = super.acquire();
      if (item != null) {
        mPoolSize.decrementAndGet();
      }

      return item;
    }

    @Override
    public boolean release(T instance) {
      boolean added = super.release(instance);
      if (added) {
        mPoolSize.incrementAndGet();
      }

      return added;
    }

    public int getPoolSize() {
      return mPoolSize.get();
    }
  }
}
