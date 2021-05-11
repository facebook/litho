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

import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** A ThreadPoolExecutor that schedules tasks based on priority. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LayoutPriorityThreadPoolExecutor extends ThreadPoolExecutor {

  public static class ComparableFutureTask<T> extends FutureTask<T>
      implements Comparable<ComparableFutureTask<T>> {
    private final int mPriority;

    public ComparableFutureTask(Callable<T> callable, int taskPriority) {
      super(callable);

      mPriority = taskPriority;
    }

    @Override
    public int compareTo(ComparableFutureTask<T> o) {
      return mPriority - o.mPriority;
    }
  }

  /**
   * @param corePoolSize core thread pool size
   * @param maxPoolSize max thread pool size
   * @param threadPriority priority of threads in this pool
   */
  public LayoutPriorityThreadPoolExecutor(int corePoolSize, int maxPoolSize, int threadPriority) {
    super(
        corePoolSize,
        maxPoolSize,
        1,
        TimeUnit.SECONDS,
        new PriorityBlockingQueue<Runnable>(),
        new LayoutThreadFactory(threadPriority));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    throw new IllegalArgumentException(
        "You need to set a priority to use this executor, see submit(Callable<T> task, int priority) instead");
  }

  /**
   * Submit a task to the thread pool; the task will be scheduled to run based on the given
   * priority.
   */
  public <T> Future<T> submit(Callable<T> task, int priority) {
    if (task == null) {
      throw new NullPointerException();
    }
    RunnableFuture<T> ftask = newTaskFor(task, priority);
    execute(ftask);
    return ftask;
  }

  private static <T> RunnableFuture<T> newTaskFor(Callable<T> callable, int priority) {
    return new ComparableFutureTask<>(callable, priority);
  }
}
