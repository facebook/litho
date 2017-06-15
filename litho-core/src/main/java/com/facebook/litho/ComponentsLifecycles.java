/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.WeakHashMap;

/**
 * Callbacks that must be invoked to avoid leaking memory if using Components
 * below ICS (API level 14).
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
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            throw new RuntimeException("onContextDestroyed method not called for: " + context);
          }
        });
      }
    }
  }

  private static WeakHashMap<Context, LeakDetector> mTrackedContexts;

  public static void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    onContextCreated(activity);
  }

  public static void onActivityDestroyed(Activity activity) {
    onContextDestroyed(activity);
  }

  public static void onContextCreated(Activity activity) {
    ComponentsPools.sIsManualCallbacks = true;
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

    ComponentsPools.onContextCreated(context);
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

    ComponentsPools.onContextDestroyed(context);
  }
}
