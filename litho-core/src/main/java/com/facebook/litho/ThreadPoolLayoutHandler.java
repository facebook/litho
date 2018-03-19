/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/** LayoutHandler implementation that uses a thread pool to calculate the layout. */
public class ThreadPoolLayoutHandler implements LayoutHandler {

  private final ThreadPoolExecutor mLayoutThreadPoolExecutor;

  public ThreadPoolLayoutHandler(LayoutThreadPoolConfiguration configuration) {
    mLayoutThreadPoolExecutor =
        new LayoutThreadPoolExecutor(
            configuration.getCorePoolSize(),
            configuration.getMaxPoolSize(),
            configuration.getThreadPriority());
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
