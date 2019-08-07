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

import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/** LithoHandler implementation that uses a thread pool to calculate the layout. */
public class ThreadPoolLayoutHandler implements LithoHandler {

  private static ThreadPoolExecutor sLayoutThreadPoolExecutor;

  public ThreadPoolLayoutHandler(LayoutThreadPoolConfiguration configuration) {
    if (sLayoutThreadPoolExecutor == null) {
      sLayoutThreadPoolExecutor =
          new LayoutThreadPoolExecutor(
              configuration.getCorePoolSize(),
              configuration.getMaxPoolSize(),
              configuration.getThreadPriority());
    }
  }

  @Override
  public boolean isTracing() {
    return false;
  }

  @Override
  public void post(Runnable runnable, String tag) {
    try {
      sLayoutThreadPoolExecutor.execute(runnable);
    } catch (RejectedExecutionException e) {
      throw new RuntimeException("Cannot execute layout calculation task; " + e);
    }
  }

  @Override
  public void remove(Runnable runnable) {
    sLayoutThreadPoolExecutor.remove(runnable);
  }
}
