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
import static com.facebook.litho.config.ComponentsConfiguration.splitLayoutBackgroundThreadPoolConfiguration;
import static com.facebook.litho.config.ComponentsConfiguration.splitLayoutMainThreadPoolConfiguration;

import android.os.Looper;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Creates tasks for calculating the layout of a component's children on different threads and
 * commits the results to the parent's internal node when they are finished.
 */
public class SplitLayoutResolver {

  private static final ExecutorCompletionService mainService;
  private static final ExecutorCompletionService bgService;

  static {
    // TODO mihaelao T27032479 Set proper pool sizes when configuring the experiment.
    final LayoutThreadPoolExecutor mainExecutor =
        new LayoutThreadPoolExecutor(
            splitLayoutMainThreadPoolConfiguration.getCorePoolSize(),
            splitLayoutMainThreadPoolConfiguration.getMaxPoolSize(),
            splitLayoutMainThreadPoolConfiguration.getThreadPriority());
    final LayoutThreadPoolExecutor bgExecutor =
        new LayoutThreadPoolExecutor(
            splitLayoutBackgroundThreadPoolConfiguration.getCorePoolSize(),
            splitLayoutBackgroundThreadPoolConfiguration.getMaxPoolSize(),
            splitLayoutBackgroundThreadPoolConfiguration.getThreadPriority());
    mainService = new ExecutorCompletionService(mainExecutor);
    bgService = new ExecutorCompletionService(bgExecutor);
  }

  /**
   * Execute each child layout on a thread from one of the pools, depending which thread layout was
   * called from.
   */
  static void resolveLayouts(List<Component> children, final InternalNode node) {
    final ExecutorCompletionService service = ThreadUtils.isMainThread() ? mainService : bgService;

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
  }

  private static InternalNode getChildLayout(ComponentContext c, Component child) {
    return child != null ? Layout.create(c, child) : NULL_LAYOUT;
  }
}
