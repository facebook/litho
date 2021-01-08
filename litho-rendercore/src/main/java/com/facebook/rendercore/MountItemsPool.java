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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pools;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Pools of recycled resources.
 *
 * <p>FUTURE: Consider customizing the pool implementation such that we can match buffer sizes.
 * Without this we will tend to expand all buffers to the largest size needed.
 */
public class MountItemsPool {

  private static final int DEFAULT_POOL_SIZE = 3;

  private MountItemsPool() {}

  private static final Map<Context, Map<Object, Pools.SimplePool>> sMountContentPoolsByContext =
      new HashMap<>(4);

  // This Map is used as a set and the values are ignored.
  private static final WeakHashMap<Context, Boolean> sDestroyedRootContexts = new WeakHashMap<>();

  private static PoolsActivityCallback sActivityCallbacks;

  /**
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API doesn't exist), we
   * allow apps to explicitly invoke activity callbacks. If this is enabled we'll throw if we are
   * passed a context for which we have no record.
   */
  static boolean sIsManualCallbacks;

  static Object acquireMountContent(Context context, RenderUnit renderUnit) {
    final Pools.SimplePool pool = getMountContentPool(context, renderUnit.getRenderContentType());
    Object content = null;
    if (pool == null) {
      return renderUnit.createContent(context);
    } else {
      content = pool.acquire();
    }

    if (content == null) {
      content = renderUnit.createContent(context);
    }

    return content;
  }

  static void release(Context context, RenderUnit renderUnit, Object mountContent) {
    final Pools.SimplePool pool = getMountContentPool(context, renderUnit.getRenderContentType());
    if (pool != null) {
      pool.release(mountContent);
    }
  }

  private static @Nullable Pools.SimplePool getMountContentPool(Context context, Object lifecycle) {

    Map<Object, Pools.SimplePool> poolsMap = sMountContentPoolsByContext.get(context);
    if (poolsMap == null) {
      final Context rootContext = getRootContext(context);
      if (sDestroyedRootContexts.containsKey(rootContext)) {
        return null;
      }

      ensureActivityCallbacks(context);
      poolsMap = new HashMap<Object, Pools.SimplePool>();
      sMountContentPoolsByContext.put(context, poolsMap);
    }

    Pools.SimplePool pool = poolsMap.get(lifecycle);
    if (pool == null) {
      pool = new Pools.SimplePool(DEFAULT_POOL_SIZE);
      poolsMap.put(lifecycle, pool);
    }

    return pool;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static void clear() {
    sMountContentPoolsByContext.clear();
    sDestroyedRootContexts.clear();
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

  static void onContextCreated(Context context) {
    if (sMountContentPoolsByContext.containsKey(context)) {
      throw new IllegalStateException(
          "The MountContentPools has a reference to an activity that has just been created");
    }
  }

  static void onContextDestroyed(Context context) {
    sMountContentPoolsByContext.remove(context);

    // Clear any context wrappers holding a reference to this activity.
    final Iterator<Map.Entry<Context, Map<Object, Pools.SimplePool>>> it =
        sMountContentPoolsByContext.entrySet().iterator();

    while (it.hasNext()) {
      final Context contextKey = it.next().getKey();
      if (isContextWrapper(contextKey, context)) {
        it.remove();
      }
    }

    sDestroyedRootContexts.put(getRootContext(context), true);
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
}
