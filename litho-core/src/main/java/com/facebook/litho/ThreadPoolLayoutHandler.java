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

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/** LithoHandler implementation that uses a thread pool to calculate the layout. */
public class ThreadPoolLayoutHandler implements LithoHandler {

  public static final LayoutThreadPoolConfiguration DEFAULT_LAYOUT_THREAD_POOL_CONFIGURATION =
      new LayoutThreadPoolConfigurationImpl(
          2, 2, ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY);
  private static ThreadPoolLayoutHandler sInstance;
  private final ThreadPoolExecutor mLayoutThreadPoolExecutor;

  private ThreadPoolLayoutHandler(LayoutThreadPoolConfiguration configuration) {
    mLayoutThreadPoolExecutor =
        new LayoutThreadPoolExecutor(
            configuration.getCorePoolSize(),
            configuration.getMaxPoolSize(),
            configuration.getThreadPriority());
  }

  /**
   * Gets the default static singleton reference to {@link ThreadPoolLayoutHandler}. It is preferred
   * to use the default thread pool in order prevent the app creating too many threads.
   *
   * @return default {@code ThreadPoolLayoutHandler}.
   */
  public static ThreadPoolLayoutHandler getDefaultInstance() {
    if (sInstance == null) {
      synchronized (ThreadPoolLayoutHandler.class) {
        if (sInstance == null) {
          sInstance = new ThreadPoolLayoutHandler(DEFAULT_LAYOUT_THREAD_POOL_CONFIGURATION);
        }
      }
    }
    return sInstance;
  }

  /**
   * Creates a new {@link ThreadPoolLayoutHandler} with the provided configuration. This method will
   * create a new {@code ThreadPoolExecutor} which can negatively affect the performance of the app.
   *
   * @param configuration {@link com.facebook.litho.config.LayoutThreadPoolConfiguration} specifying
   *     core and max pool size, and thread priority
   * @return new instance with a separate {@code ThreadPoolExecutor} with specified configuration.
   */
  public static ThreadPoolLayoutHandler getNewInstance(
      LayoutThreadPoolConfiguration configuration) {
    return new ThreadPoolLayoutHandler(configuration);
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
}
