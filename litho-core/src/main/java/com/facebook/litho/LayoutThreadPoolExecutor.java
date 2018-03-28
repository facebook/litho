/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.os.Process;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread pool executor implementation used to calculate layout on multiple background threads. */
public class LayoutThreadPoolExecutor extends ThreadPoolExecutor {

  private static final AtomicInteger threadPoolId = new AtomicInteger(1);

  public LayoutThreadPoolExecutor(int corePoolSize, int maxPoolSize, int priority) {
    super(
        corePoolSize,
        maxPoolSize,
        1,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(),
        new LayoutThreadFactory(priority));
  }

  private static class LayoutThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final int mThreadPriority;
    private final int mThreadPoolId;

    public LayoutThreadFactory(int threadPriority) {
      mThreadPriority = threadPriority;
      mThreadPoolId = threadPoolId.getAndIncrement();
    }

    @Override
    public Thread newThread(final Runnable r) {

      final Runnable wrapperRunnable =
          new Runnable() {
            @Override
            public void run() {
              try {
                Process.setThreadPriority(mThreadPriority);
              } catch (SecurityException e) {
                /**
                 * From {@link Process#THREAD_PRIORITY_DISPLAY}, some applications can not change
                 * the thread priority to that of the main thread. This catches that potential error
                 * and tries to set a lower priority.
                 */
                Process.setThreadPriority(mThreadPriority + 1);
              }
              r.run();
            }
          };

      return new Thread(
          wrapperRunnable,
          "ComponentLayoutThread" + mThreadPoolId + "-" + threadNumber.getAndIncrement());
    }
  }
}
