// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.app.Activity;
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
    private Activity mActivity;

    LeakDetector(Activity activity) {
      mActivity = activity;
    }

    void clear() {
      mActivity = null;
    }

    @Override
    public void finalize() {
      if (mActivity != null) {
        final Activity activity = mActivity;

        // Post the error to the main thread to bring down the process -
        // exceptions on finalizer threads are ignored by default.
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            throw new RuntimeException("onActivityDestroyed not called for: " + activity);
          }
        });
      }
    }
  }

  private static WeakHashMap<Activity, LeakDetector> mTrackedActivities;

  public static void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    if (mTrackedActivities == null) {
      mTrackedActivities = new WeakHashMap<>();
      ComponentsPools.sIsManualCallbacks = true;
    }

    LeakDetector old = mTrackedActivities.put(activity, new LeakDetector(activity));
    if (old != null) {
      throw new RuntimeException("Duplicate onActivityCreated call for: " + activity);
    }

    ComponentsPools.onActivityCreated(activity, savedInstanceState);
  }

  public static void onActivityDestroyed(Activity activity) {
    if (mTrackedActivities == null) {
      throw new RuntimeException(
          "onActivityDestroyed called without onActivityCreated for: " + activity);
    }

    LeakDetector removed = mTrackedActivities.remove(activity);
    if (removed == null) {
      throw new RuntimeException(
          "onActivityDestroyed called without onActivityCreated for: " + activity);
    } else {
      removed.clear();
    }

    ComponentsPools.onActivityDestroyed(activity);
  }
}
