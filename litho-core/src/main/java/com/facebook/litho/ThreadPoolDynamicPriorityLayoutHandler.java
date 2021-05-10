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
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * LithoHandler implementation that uses a thread pool to calculate the layout. Enabled changing the
 * priority of the threads which execute the tasks.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ThreadPoolDynamicPriorityLayoutHandler implements LithoHandler {

  private final ThreadPoolExecutor mLayoutThreadPoolExecutor;
  private final LayoutThreadFactory mLayoutThreadFactory;

  public ThreadPoolDynamicPriorityLayoutHandler(LayoutThreadPoolConfiguration configuration) {
    mLayoutThreadFactory = new LayoutThreadFactory(configuration.getThreadPriority());

    mLayoutThreadPoolExecutor =
        new ThreadPoolExecutor(
            configuration.getCorePoolSize(),
            configuration.getMaxPoolSize(),
            1,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            mLayoutThreadFactory);
  }

  @Override
  public boolean isTracing() {
    return false;
  }

  @Override
  public void post(Runnable runnable, String tag) {
    try {
      mLayoutThreadPoolExecutor.execute(runnable);
    } catch (RejectedExecutionException e) {
      throw new RuntimeException("Cannot execute layout calculation task; " + e);
    }
  }

  @Override
  public void postAtFront(Runnable runnable, String tag) {
    throw new IllegalStateException("postAtFront is not supported for ThreadPoolLayoutHandler");
  }

  @Override
  public void remove(Runnable runnable) {
    mLayoutThreadPoolExecutor.remove(runnable);
  }

  /**
   * Changes the priority of any thread in the pool which will execute runnables starting from when
   * this is called. Does not change the priority of the threads executing runnables which were
   * posted before this was invoked.
   */
  public void setThreadPriority(int updatePriority) {
    mLayoutThreadFactory.setThreadPriority(updatePriority);
  }
}
