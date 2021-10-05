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

package com.facebook.litho;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.MountItemsPool;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/**
 * Callbacks that must be invoked to avoid leaking memory if using Components below ICS (API level
 * 14).
 */
public class ComponentsLifecycles {
  private static class LeakDetector {
    private Context mContext;

    LeakDetector(Context context) {
      mContext = context;
    }

    void clear() {
      mContext = null;
    }

    @Override
    public void finalize() {
      if (mContext != null) {
        final Context context = mContext;

        // Post the error to the main thread to bring down the process -
        // exceptions on finalizer threads are ignored by default.
        new Handler(Looper.getMainLooper())
            .post(
                new Runnable() {
                  @Override
                  public void run() {
                    throw new RuntimeException(
                        "onContextDestroyed method not called for: " + context);
                  }
                });
      }
    }
  }

  private static WeakHashMap<Context, LeakDetector> mTrackedContexts;

  public static void onActivityCreated(Activity activity, @Nullable Bundle savedInstanceState) {
    onContextCreated(activity);
  }

  public static void onActivityDestroyed(Activity activity) {
    onContextDestroyed(activity);
  }

  public static void onContextCreated(Activity activity) {
    setManualCallbacksEnabledForPool();
    onContextCreated((Context) activity);
  }

  public static void onContextCreated(Context context) {
    if (mTrackedContexts == null) {
      mTrackedContexts = new WeakHashMap<>();
    }

    LeakDetector old = mTrackedContexts.put(context, new LeakDetector(context));
    if (old != null) {
      throw new RuntimeException("Duplicate onContextCreated call for: " + context);
    }

    onContextCreatedForPool(context);
  }

  public static void onContextDestroyed(Context context) {
    if (mTrackedContexts == null) {
      throw new RuntimeException(
          "onContextDestroyed called without onContextCreated for: " + context);
    }

    LeakDetector removed = mTrackedContexts.remove(context);
    if (removed == null) {
      throw new RuntimeException(
          "onContextDestroyed called without onContextCreated for: " + context);
    } else {
      removed.clear();
    }

    onContextDestroyedForPool(context);
  }

  private static void setManualCallbacksEnabledForPool() {
    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      MountItemsPool.sIsManualCallbacks = true;
    } else {
      ComponentsPools.sIsManualCallbacks = true;
    }
  }

  private static void onContextCreatedForPool(Context context) {
    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      MountItemsPool.onContextCreated(context);
    } else {
      ComponentsPools.onContextCreated(context);
    }
  }

  private static void onContextDestroyedForPool(Context context) {
    if (ComponentsConfiguration.delegateToRenderCoreMount) {
      MountItemsPool.onContextDestroyed(context);
    } else {
      ComponentsPools.onContextDestroyed(context);
    }
  }
}
