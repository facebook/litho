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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.concurrent.GuardedBy;

/**
 * A {@code LithoHandler} implementation that runs all layout computations against the provided
 * {@code Executor}. This {@code LithoHandler} can be used in apps that have a well established
 * threading model and need to run layout against one of their already available executors.
 */
public final class ExecutorLithoHandler implements LithoHandler {

  @GuardedBy("pendingTasks")
  private final Map<Runnable, Integer> pendingTasks = new HashMap<>();

  private final Executor executor;

  /**
   * Instantiates an {@code ExecutorLithoHandler} that uses the given {@code Executor} to run all
   * layout tasks against.
   */
  public ExecutorLithoHandler(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void post(final Runnable runnable, String tag) {
    synchronized (pendingTasks) {
      Integer runCount = pendingTasks.get(runnable);
      pendingTasks.put(runnable, runCount != null ? runCount + 1 : 1);
    }

    executor.execute(
        new Runnable() {
          @Override
          public void run() {
            boolean canRun = false;
            synchronized (pendingTasks) {
              Integer runCount = pendingTasks.get(runnable);
              if (runCount != null) {
                canRun = runCount > 0;
                runCount--;
                if (runCount > 0) {
                  pendingTasks.put(runnable, runCount);
                } else {
                  pendingTasks.remove(runnable);
                }
              }
            }
            if (canRun) {
              runnable.run();
            }
          }
        });
  }

  /**
   * {@inheritDoc}
   *
   * <p>In this implementation, postAtFront() is equivalent to post().
   */
  @Override
  public void postAtFront(Runnable runnable, String tag) {
    post(runnable, tag);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation removes all instances of the provided {@code Runnable} and prevents them
   * from running if they have not started yet.
   */
  @Override
  public void remove(Runnable runnable) {
    synchronized (pendingTasks) {
      pendingTasks.remove(runnable);
    }
  }

  @Override
  public boolean isTracing() {
    return false;
  }
}
