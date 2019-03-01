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
package com.facebook.litho;

import static com.facebook.litho.ComponentContext.NULL_LAYOUT;

import android.os.Looper;
import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import javax.annotation.Nullable;

/**
 * Creates tasks for calculating the layout of a component's children on different threads and
 * commits the results to the parent's internal node when they are finished.
 */
@ThreadSafe
public class SplitLayoutResolver {

  @GuardedBy("SplitLayoutResolver.class")
  private static final Map<String, SplitLayoutResolver> sSplitLayoutResolvers = new HashMap<>();

  private final Set<String> mEnabledComponents = new LinkedHashSet<>();
  private @Nullable ExecutorCompletionService mainService;
  private @Nullable ExecutorCompletionService bgService;

  /**
   * Create a SplitLayoutResolver that will be used to split layout where possible in ComponentTrees
   * with the given split tag. If a configuration already exists for the same split tag, it uses
   * that one.
   *
   * @param tag split tag
   * @param mainThreadPoolConfig configuration for splitting main thread layouts
   * @param bgThreadPoolConfig configuration for splitting background thread layouts
   */
  public static synchronized void createForTag(
      String tag,
      @Nullable LayoutThreadPoolConfiguration mainThreadPoolConfig,
      @Nullable LayoutThreadPoolConfiguration bgThreadPoolConfig,
      Set<String> enabledComponents) {
    if (sSplitLayoutResolvers.containsKey(tag)) {
      return;
    }

    sSplitLayoutResolvers.put(
        tag, new SplitLayoutResolver(mainThreadPoolConfig, bgThreadPoolConfig, enabledComponents));
  }

  private SplitLayoutResolver(
      @Nullable LayoutThreadPoolConfiguration mainThreadPoolConfig,
      @Nullable LayoutThreadPoolConfiguration bgThreadPoolConfig,
      Set<String> enabledComponents) {
    if (mainThreadPoolConfig != null) {
      final LayoutThreadPoolExecutor mainExecutor =
          new LayoutThreadPoolExecutor(
              mainThreadPoolConfig.getCorePoolSize(),
              mainThreadPoolConfig.getMaxPoolSize(),
              mainThreadPoolConfig.getThreadPriority());
      mainService = new ExecutorCompletionService(mainExecutor);
    }

    if (bgThreadPoolConfig != null) {
      final LayoutThreadPoolExecutor bgExecutor =
          new LayoutThreadPoolExecutor(
              bgThreadPoolConfig.getCorePoolSize(),
              bgThreadPoolConfig.getMaxPoolSize(),
              bgThreadPoolConfig.getThreadPriority());
      bgService = new ExecutorCompletionService(bgExecutor);
    }

    if (enabledComponents != null) {
      mEnabledComponents.addAll(enabledComponents);
    }
  }

  static boolean isComponentEnabledForSplitting(ComponentContext c, Component component) {
    final SplitLayoutResolver resolver = getResolver(c);
    return resolver != null
        && resolver.mEnabledComponents.contains(component.getClass().getSimpleName());
  }

  /**
   * Execute each child layout on a thread from one of the pools, depending which thread layout was
   * called from. Returns false if the configuration does not allow splitting layout on the caller
   * thread.
   */
  static boolean resolveLayouts(
      ComponentContext c, List<Component> children, final InternalNode node) {
    final SplitLayoutResolver resolver = getResolver(c);
    if (resolver == null || !resolver.canSplitLayoutOnCurrentThread()) {
      return false;
    }

    final ExecutorCompletionService service =
        ThreadUtils.isMainThread() ? resolver.mainService : resolver.bgService;

    final InternalNode[] results = new InternalNode[children.size()];
    int size = children.size();

    for (int i = 1; i < size; i++) {
      final Component child = children.get(i);
      final int finalI = i;

      final Runnable runnable =
          new Runnable() {
            @Override
            public void run() {
              if (Looper.myLooper() == null) {
                Looper.prepare();
              }
              results[finalI] = getChildLayout(node.getContext(), child);
            }
          };
      service.submit(runnable, i - 1);
    }

    // Schedule one layout on the caller thread too so we're not idle while waiting for other tasks.
    // We can manually decide which one should go here when configuring.
    results[0] = getChildLayout(node.getContext(), children.get(0));

    for (int i = 0; i < size - 1; i++) {
      try {
        service.take();
      } catch (InterruptedException e) {
        throw new RuntimeException("Could not execute split layout task", e);
      }
    }

    // After all tasks have been executed, add the children layouts to the InternalNode.
    for (int i = 0; i < results.length; i++) {
      node.child(results[i]);
    }

    return true;
  }

  private static InternalNode getChildLayout(ComponentContext c, Component child) {
    return child != null ? Layout.create(c, child) : NULL_LAYOUT;
  }

  private boolean canSplitLayoutOnCurrentThread() {
    return ThreadUtils.isMainThread() ? mainService != null : bgService != null;
  }

  private static @Nullable SplitLayoutResolver getResolver(ComponentContext c) {
    final String splitTag = c.getSplitLayoutTag();
    if (splitTag == null) {
      return null;
    }

    synchronized ("SplitLayoutResolver.class") {
      return sSplitLayoutResolvers.get(splitTag);
    }
  }

  @VisibleForTesting
  static void clearTag(String tag) {
    sSplitLayoutResolvers.remove(tag);
  }

  @VisibleForTesting
  static SplitLayoutResolver getForTag(String tag) {
    return sSplitLayoutResolvers.get(tag);
  }
}
