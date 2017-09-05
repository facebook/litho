/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The {@link ServiceRegistry} is used by the {@link SectionTree} to register every Service
 * used by {@link Section}s. Whenever the Services are not used by any {@link Section}s in the
 * current {@link SectionTree}, the {@link ServiceRegistry} will invoke the
 * {@link com.facebook.litho.sections.annotations.OnDestroyService} callback on the
 * {@link SectionLifecycle}.
 */
@ThreadSafe
class ServiceRegistry {

  private static final Object sLock = new Object();

  // Service can be of any Object type
  @GuardedBy("sLock")
  private static final Map<Context, Map<Object, Set<Section>>> sContextsToServicesMap =
      new HashMap<>(2);

  // Services that are no longer used in any section within the current SectionTree will be here
  @GuardedBy("sLock")
  private static final Map<Object, Section> sUnusedServicesToSections = new HashMap<>(2);

  private static ActivityCallback sActivityCallback;

  static void registerService(Section section) {
    final Context context = section.getScopedContext().getBaseContext();
    final Object service = section.getLifecycle().getService(section);

    if (service == null || !section.getLifecycle().hasDestroyService()) {
      return;
    }

    synchronized (sLock) {
      if (sActivityCallback == null) {
        sActivityCallback = new ActivityCallback();
        ((Application) context.getApplicationContext())
            .registerActivityLifecycleCallbacks(sActivityCallback);
      }

      Map<Object, Set<Section>> sectionCountByService = sContextsToServicesMap.remove(context);
      if (sectionCountByService == null) {
        sectionCountByService = new HashMap<>(2);
      }

      Set<Section> sections = sectionCountByService.remove(service);
      if (sections == null) {
        sections = new HashSet<>();
      }

      sections.add(section);
      sectionCountByService.put(service, sections);
      sContextsToServicesMap.put(context, sectionCountByService);

      if (sUnusedServicesToSections.containsKey(service)) {
        sUnusedServicesToSections.remove(service);
      }
    }
  }

  static void unregisterService(Section section) {
    final Context context = section.getScopedContext().getBaseContext();
    final Object service = section.getLifecycle().getService(section);

    if (service == null || !section.getLifecycle().hasDestroyService()) {
      return;
    }

    synchronized (sLock) {
      if (!sContextsToServicesMap.containsKey(context)) {
        // The context is destroyed and the call onActivityDestroyed()
        // would have cleaned up sContextsToServiceMap
        return;
      }

      Map<Object, Set<Section>> sectionCountByService = sContextsToServicesMap.remove(context);
      Set<Section> sections = sectionCountByService.remove(service);
      if (sections == null) {
        // TODO t19173243: Investigate the reason for Set to be null
        return;
      }

      sections.remove(section);

      if (sections.isEmpty()) {
        sUnusedServicesToSections.put(service, section);
      } else {
        sectionCountByService.put(service, sections);
      }

      sContextsToServicesMap.put(context, sectionCountByService);
    }
  }

  static void cleanUnusedServices() {
    synchronized (sLock) {
      if (sUnusedServicesToSections.isEmpty()) {
        return;
      }

      final Set<Object> services = sUnusedServicesToSections.keySet();
      for (Object service : services) {
        Section section = sUnusedServicesToSections.get(service);
        section.getLifecycle().destroyService(section.getScopedContext(), service);
      }

      sUnusedServicesToSections.clear();
    }
  }

  static void onContextCreated(Context context) {
    synchronized (sLock) {
      if (sContextsToServicesMap.containsKey(context)) {
        throw new IllegalStateException("ServicePools has a reference to activity "
            + context.toString() + " that has just been created");
      }
    }
  }

  static void onActivityDestroyed(Activity activity) {
    Map<Object, Set<Section>> serviceCount = removeServicesForActivity(activity);

    if (serviceCount == null || serviceCount.isEmpty()) {
      return;
    }

    final Set<Object> services = serviceCount.keySet();
    for (Object service : services) {
      final Set<Section> sections = serviceCount.get(service);
      for (Section section : sections) {
        section.getLifecycle().unbindService(section.getScopedContext(), section);
        section.getLifecycle().destroyService(section.getScopedContext(), service);
      }

      sections.clear();
    }

    serviceCount.clear();
  }

  /**
   * Remove entries in map associated with the Activity
   */
  static Map<Object, Set<Section>> removeServicesForActivity(Activity activity) {
    synchronized (sLock) {
      // It is possible that the key in the map is a ContextWrapper around the Activity. Iterate
      // through the map and check to see if any of the keys is a wrapper of this activity
      Set<Context> contextsToRemove = new HashSet<>();
      for (Map.Entry<Context, Map<Object, Set<Section>>> entry : sContextsToServicesMap
          .entrySet()) {
        if (activity == unwrapContextToFindActivity(entry.getKey())) {
          contextsToRemove.add(entry.getKey());
        }
      }

      Map<Object, Set<Section>> serviceCounts = new HashMap<>();
      for (Context context : contextsToRemove) {
        serviceCounts.putAll(sContextsToServicesMap.remove(context));
      }
      return serviceCounts;
    }
  }

  /**
   * Recursively traverse through the chain of wrapped Contexts to find the underlying Activity
   */
  static @Nullable Activity unwrapContextToFindActivity(Context context) {
    if (context instanceof Activity) {
      return (Activity) context;
    } else if (context instanceof ContextWrapper) {
      ContextWrapper contextWrapper = (ContextWrapper) context;
      return unwrapContextToFindActivity(contextWrapper.getBaseContext());
    } else {
      return null;
    }
  }

  private static class ActivityCallback implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      ServiceRegistry.onContextCreated(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
      ServiceRegistry.onActivityDestroyed(activity);
    }
  }

  @VisibleForTesting
  static Map<Context, Map<Object, Set<Section>>> getContextsToServicesMap() {
    return sContextsToServicesMap;
  }

  @VisibleForTesting
  static Map<Object, Section> getUnusedServicesToSections() {
    return sUnusedServicesToSections;
  }
}
