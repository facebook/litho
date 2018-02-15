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
import com.facebook.litho.config.ComponentsConfiguration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** LayoutHandler implementation that uses a thread pool to calculate the layout. */
public class ThreadPoolLayoutHandler implements LayoutHandler {

  /**
   * A PoolSizeCalculator instance can be passed to construct the ThreadPoolLayoutHandler for a
   * custom way of calculating the number of threads based on the number of cores available on the
   * device.
   */
  public interface PoolSizeCalculator {
    int getCorePoolSize(int numProcessors);

    int getMaxPoolSize(int numProcessors);
  }

  private final ThreadPoolExecutor mLayoutThreadPoolExecutor;

  public ThreadPoolLayoutHandler(int corePoolSize, int maxPoolSize) {
    mLayoutThreadPoolExecutor = new LayoutThreadPoolExecutor(corePoolSize, maxPoolSize);
  }

  public ThreadPoolLayoutHandler(PoolSizeCalculator poolSizeCalculator) {
    final int numProcessors = DeviceInfoUtils.getNumberOfCPUCores();
    mLayoutThreadPoolExecutor =
        new LayoutThreadPoolExecutor(
            poolSizeCalculator.getCorePoolSize(numProcessors),
            poolSizeCalculator.getMaxPoolSize(numProcessors));
  }

  @Override
  public boolean post(Runnable runnable) {
    try {
      mLayoutThreadPoolExecutor.execute(runnable);
      return true;
    } catch (RejectedExecutionException e) {
      throw new RuntimeException("Cannot execute layout calculation task; " + e);
    }
  }

  @Override
  public void removeCallbacks(Runnable runnable) {
    mLayoutThreadPoolExecutor.remove(runnable);
  }

  @Override
  public void removeCallbacksAndMessages(Object token) {
    throw new RuntimeException("Operation not supported");
  }

  private static class LayoutThreadPoolExecutor extends ThreadPoolExecutor {

    private static final ThreadFactory sThreadFactory =
        new ThreadFactory() {

          private final AtomicInteger threadNumber = new AtomicInteger(1);

          @Override
          public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "ComponentLayoutThread" + threadNumber.getAndIncrement());
            Process.setThreadPriority(ComponentsConfiguration.defaultBackgroundThreadPriority);
            return thread;
          }
        };

    public LayoutThreadPoolExecutor(int corePoolSize, int maxPoolSize) {
      super(
          corePoolSize,
          maxPoolSize,
          1,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<Runnable>(),
          sThreadFactory);
    }
  }
}
