/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

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
}
