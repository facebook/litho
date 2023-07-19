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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Pools of recycled resources.
 *
 * <p>FUTURE: Consider customizing the pool implementation such that we can match buffer sizes.
 * Without this we will tend to expand all buffers to the largest size needed.
 */
public class MountItemsPool {

  /** A factory used to create {@link MountItemsPool.ItemPool}s. */
  public interface Factory {

    /** Creates an ItemPool for the mountable content. */
    MountItemsPool.ItemPool createMountContentPool();
  }

  private static final int DEFAULT_POOL_SIZE = 3;

  private MountItemsPool() {}

  private static final Object sMountContentLock = new Object();

  @GuardedBy("sMountContentLock")
  private static final Map<Context, Map<Object, ItemPool>> sMountContentPoolsByContext =
      new HashMap<>(4);

  @GuardedBy("sMountContentLock")
  private static final Map<Context, WeakHashMap<IBinder, ItemPool>> sHostPoolsByWindowAndContext =
      new HashMap<>(4);

  // This Map is used as a set and the values are ignored.
  @GuardedBy("sMountContentLock")
  private static final WeakHashMap<Context, Boolean> sDestroyedRootContexts = new WeakHashMap<>();

  private static PoolsActivityCallback sActivityCallbacks;

  /**
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API doesn't exist), we
   * allow apps to explicitly invoke activity callbacks. If this is enabled we'll throw if we are
   * passed a context for which we have no record.
   */
  public static boolean sIsManualCallbacks;

  /** Should be used to disable pooling entirely for debugging, testing, and other use cases. */
  public static boolean isPoolingDisabled;

  /** Can be used to return a custom Pool implementation for testing. */
  private static final ThreadLocal<MountItemsPool.Factory> sMountContentPoolFactory =
      new ThreadLocal<>();

  private static boolean sHasMountContentPoolFactory = false;

  public static Object acquireMountContent(Context context, ContentAllocator poolableMountContent) {

    final ItemPool pool = getMountContentPool(context, poolableMountContent);
    if (pool == null) {
      return poolableMountContent.createPoolableContent(context);
    }

    final Object content = pool.acquire(context, poolableMountContent);
    if (content != null) {
      return content;
    }

    return poolableMountContent.createPoolableContent(context);
  }

  public static void release(
      Context context, ContentAllocator poolableMountContent, Object mountContent) {
    final ItemPool pool = getMountContentPool(context, poolableMountContent);
    if (pool != null) {
      pool.release(mountContent);
    }
  }

  public static void maybePreallocateContent(
      Context context, ContentAllocator poolableMountContent) {
    final ItemPool pool = getMountContentPool(context, poolableMountContent);
    if (pool != null) {
      pool.maybePreallocateContent(context, poolableMountContent);
    }
  }

  /**
   * Can be called to fill up a mount content pool for the specified MountContent types. If a pool
   * doesn't exist for a Mount Content type, a default one will be created with the specified size.
   * PoolSize will only be respected if the RenderUnit does not provide a custom Pool
   * implementation.
   */
  public static void prefillMountContentPool(
      Context context, int poolSize, ContentAllocator poolableMountContent) {
    if (poolSize == 0) {
      return;
    }

    final ItemPool pool = getMountContentPool(context, poolableMountContent, poolSize);
    if (pool != null) {
      for (int i = 0; i < poolSize; i++) {
        pool.release(poolableMountContent.createPoolableContent(context));
      }
    }
  }

  private static @Nullable ItemPool getMountContentPool(
      Context context, ContentAllocator poolableMountContent) {
    return getMountContentPool(context, poolableMountContent, DEFAULT_POOL_SIZE);
  }

  private static @Nullable ItemPool getMountContentPool(
      Context context, ContentAllocator poolableMountContent, int size) {
    if (poolableMountContent.isRecyclingDisabled()
        || isPoolingDisabled
        || poolableMountContent.poolSize() == 0) {
      return null;
    }

    synchronized (sMountContentLock) {
      Map<Object, ItemPool> poolsMap = sMountContentPoolsByContext.get(context);
      if (poolsMap == null) {
        final Context rootContext = getRootContext(context);
        if (sDestroyedRootContexts.containsKey(rootContext)) {
          return null;
        }

        ensureActivityCallbacks(context);
        poolsMap = new HashMap<>();
        sMountContentPoolsByContext.put(context, poolsMap);
      }
      final Object lifecycle = poolableMountContent.getPoolableContentType();

      ItemPool pool = poolsMap.get(lifecycle);
      if (pool == null) {
        pool = createRecyclingPool(poolableMountContent);

        // PoolableMountContent might produce a null pool. In this case, just create a default one.
        if (pool == null) {
          pool = new DefaultItemPool(lifecycle, size);
        }

        poolsMap.put(lifecycle, pool);
      }

      return pool;
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static void clear() {
    synchronized (sMountContentLock) {
      sMountContentPoolsByContext.clear();
      sHostPoolsByWindowAndContext.clear();
      sDestroyedRootContexts.clear();
    }
  }

  @VisibleForTesting
  public static void setMountContentPoolFactory(@Nullable final MountItemsPool.Factory factory) {
    sMountContentPoolFactory.set(factory);
    sHasMountContentPoolFactory = factory != null;
  }

  public static Object acquireHostMountContent(
      Context context, @Nullable IBinder windowToken, ContentAllocator hostContentProvider) {
    final ItemPool pool = getHostMountContentPool(context, windowToken, hostContentProvider);
    if (pool == null) {
      return hostContentProvider.createPoolableContent(context);
    }

    return pool.acquire(context, hostContentProvider);
  }

  public static void releaseHostMountContent(
      Context context,
      @Nullable IBinder windowToken,
      ContentAllocator hostContentProvider,
      Object content) {
    final ItemPool pool = getHostMountContentPool(context, windowToken, hostContentProvider);
    if (pool == null) {
      return;
    }

    pool.release(content);
  }

  private static @Nullable ItemPool getHostMountContentPool(
      Context context, @Nullable IBinder windowToken, ContentAllocator hostContentProvider) {
    // Currently, this seems most likely to occur when we call unmountAllItems from onViewRecycled
    // in an RV as the RootHost will have already been detached.
    if (windowToken == null) {
      return null;
    }

    synchronized (sMountContentLock) {
      WeakHashMap<IBinder, ItemPool> poolsByWindowToken = sHostPoolsByWindowAndContext.get(context);
      if (poolsByWindowToken == null) {
        final Context rootContext = getRootContext(context);
        if (sDestroyedRootContexts.containsKey(rootContext)) {
          return null;
        }

        ensureActivityCallbacks(context);
        poolsByWindowToken = new WeakHashMap<>();
        sHostPoolsByWindowAndContext.put(context, poolsByWindowToken);
      }

      ItemPool pool = poolsByWindowToken.get(windowToken);
      if (pool == null) {
        pool = createRecyclingPool(hostContentProvider);
        poolsByWindowToken.put(windowToken, pool);
      }

      return pool;
    }
  }

  @Nullable
  private static ItemPool createRecyclingPool(ContentAllocator poolableMountContent) {
    if (sHasMountContentPoolFactory) {
      final MountItemsPool.Factory factory = sMountContentPoolFactory.get();
      if (factory != null) {
        return factory.createMountContentPool();
      }
    }

    return poolableMountContent.createRecyclingPool();
  }

  /**
   * @return the "most base" Context of this Context, i.e. the Activity, Application, or Service
   *     backing this Context and all its ContextWrappers. In some cases, e.g. instrumentation tests
   *     or other places we don't wrap a standard Context, this root Context may instead be a raw
   *     ContextImpl.
   */
  private static Context getRootContext(Context context) {
    Context currentContext = context;

    while (currentContext instanceof ContextWrapper
        && !(currentContext instanceof Activity)
        && !(currentContext instanceof Application)
        && !(currentContext instanceof Service)) {
      currentContext = ((ContextWrapper) currentContext).getBaseContext();
    }

    return currentContext;
  }

  private static void ensureActivityCallbacks(Context context) {
    if (sActivityCallbacks == null && !sIsManualCallbacks) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        throw new RuntimeException(
            "Activity callbacks must be invoked manually below ICS (API level 14)");
      }
      sActivityCallbacks = new PoolsActivityCallback();
      ((Application) context.getApplicationContext())
          .registerActivityLifecycleCallbacks(sActivityCallbacks);
    }
  }

  /** Empty implementation of the {@link Application.ActivityLifecycleCallbacks} interface */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class PoolsActivityCallback implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      MountItemsPool.onContextCreated(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityResumed(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityPaused(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityStopped(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      // Do nothing.
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      MountItemsPool.onContextDestroyed(activity);
    }
  }

  public static void onContextCreated(Context context) {
    synchronized (sMountContentLock) {
      if (sMountContentPoolsByContext.containsKey(context)) {
        throw new IllegalStateException(
            "The MountContentPools has a reference to an activity that has just been created");
      }
    }
  }

  public static void onContextDestroyed(Context context) {
    synchronized (sMountContentLock) {
      clearMatchingContexts(context, sMountContentPoolsByContext);
      clearMatchingContexts(context, sHostPoolsByWindowAndContext);

      sDestroyedRootContexts.put(getRootContext(context), true);
    }
  }

  @GuardedBy("sMountContentLock")
  private static <T> void clearMatchingContexts(Context context, Map<Context, T> poolsMap) {
    poolsMap.remove(context);

    // Clear any context wrappers holding a reference to this activity.
    final Iterator<Map.Entry<Context, T>> it = poolsMap.entrySet().iterator();

    while (it.hasNext()) {
      final Context contextKey = it.next().getKey();
      if (isContextWrapper(contextKey, context)) {
        it.remove();
      }
    }
  }

  @VisibleForTesting
  public static List<ItemPool> getMountItemPools() {
    final List<ItemPool> result = new ArrayList<>();

    for (Map<Object, ItemPool> poolMap : sMountContentPoolsByContext.values()) {
      for (ItemPool pool : poolMap.values()) {
        result.add(pool);
      }
    }

    return result;
  }

  /** Check whether contextWrapper is a wrapper of baseContext */
  private static boolean isContextWrapper(Context contextWrapper, Context baseContext) {
    while (baseContext instanceof ContextWrapper) {
      baseContext = ((ContextWrapper) baseContext).getBaseContext();
    }

    Context currentContext = contextWrapper;
    while (currentContext instanceof ContextWrapper) {
      currentContext = ((ContextWrapper) currentContext).getBaseContext();
    }

    return currentContext == baseContext;
  }

  /** Content item pools that RenderCore uses to recycle content (such as Views) */
  public interface ItemPool {

    /**
     * Acquire a pooled content item from the pool
     *
     * @param c the Android context
     * @param renderUnit the RenderUnit for the item
     * @return a pooled content item
     */
    Object acquire(Context c, ContentAllocator poolableMountContent);

    /**
     * Called when an item is released and can return to the pool
     *
     * @param item the item to release to the pool
     * @return {@code true} iff the {@param item} is released to the pool.
     */
    boolean release(Object item);

    /**
     * Called early in the lifecycle to allow the pool implementation to preallocate items in the
     * pool (as released items)
     *
     * @param c the android context
     * @param renderUnit the RenderUnit for the item
     */
    void maybePreallocateContent(Context c, ContentAllocator poolableMountContent);
  }

  public static class DefaultItemPool implements ItemPool {

    private final Pools.SimplePool<Object> mPool;
    private final Object mLifecycle;

    public DefaultItemPool(Object lifecycle, int size) {
      mLifecycle = lifecycle;
      mPool = new Pools.SimplePool<Object>(size);
    }

    @Override
    public @Nullable Object acquire(Context c, ContentAllocator poolableMountContent) {
      return mPool.acquire();
    }

    @Override
    public boolean release(Object item) {
      try {
        return mPool.release(item);
      } catch (IllegalStateException e) {
        String metadata =
            "Lifecycle: "
                + ((mLifecycle instanceof Class)
                    ? " <cls>" + ((Class) mLifecycle).getName() + "</cls>"
                    : mLifecycle.toString());
        throw new IllegalStateException(metadata, e);
      }
    }

    @Override
    public void maybePreallocateContent(Context c, ContentAllocator poolableMountContent) {
      // Do Nothing.
    }
  }
}
