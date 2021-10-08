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
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.DeviceInfoUtils;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;
import com.facebook.rendercore.RunnableHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/** LithoHandler implementation that uses a thread pool to calculate the layout. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ThreadPoolLayoutHandler implements RunnableHandler {

  private static final int CPU_CORES = DeviceInfoUtils.getNumberOfCPUCores();

  private static final int CPU_CORES_MULTIPLIER =
      ComponentsConfiguration.layoutCalculationThreadPoolCpuCoresMultiplier;

  public static final LayoutThreadPoolConfiguration DEFAULT_LAYOUT_THREAD_POOL_CONFIGURATION =
      new LayoutThreadPoolConfigurationImpl(
          2, 2, ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY);

  private static class DefaultThreadPoolHolder {
    static final ThreadPoolLayoutHandler INSTANCE =
        new ThreadPoolLayoutHandler(DEFAULT_LAYOUT_THREAD_POOL_CONFIGURATION);
  }

  private static class CpuCoresThreadPoolHolder {
    static final ThreadPoolLayoutHandler INSTANCE =
        new ThreadPoolLayoutHandler(
            new LayoutThreadPoolConfigurationImpl(
                Math.max(
                    CPU_CORES * CPU_CORES_MULTIPLIER
                        + ComponentsConfiguration.layoutCalculationThreadPoolCpuCoresSubtractor,
                    1),
                Math.max(
                    CPU_CORES * CPU_CORES_MULTIPLIER
                        + ComponentsConfiguration.layoutCalculationThreadPoolCpuCoresSubtractor,
                    1),
                ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY));
  }

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
  public static RunnableHandler getDefaultInstance() {
    // currently there are clients calling getDefaultInstance() directly, so we need to override
    // here too, not only in getNewInstance()
    // at most one of the configuration parameters should be true, otherwise the experiment has been
    // misconfigured
    if (ComponentsConfiguration.layoutCalculationAlwaysUseSingleThread) {
      return new DefaultHandler(ComponentTree.getDefaultLayoutThreadLooper());
    }
    if (CPU_CORES_MULTIPLIER > 0) {
      return CpuCoresThreadPoolHolder.INSTANCE;
    }

    return DefaultThreadPoolHolder.INSTANCE;
  }

  /**
   * Creates a new {@link ThreadPoolLayoutHandler} with the provided configuration. This method will
   * create a new {@code ThreadPoolExecutor} which can negatively affect the performance of the app.
   *
   * @param configuration {@link com.facebook.litho.config.LayoutThreadPoolConfiguration} specifying
   *     core and max pool size, and thread priority
   * @return new instance with a separate {@code ThreadPoolExecutor} with specified configuration.
   */
  public static RunnableHandler getNewInstance(LayoutThreadPoolConfiguration configuration) {
    // at most one of the configuration parameters should be true, otherwise the experiment has been
    // misconfigured
    if (ComponentsConfiguration.layoutCalculationAlwaysUseSingleThread) {
      return new DefaultHandler(ComponentTree.getDefaultLayoutThreadLooper());
    } else if (ComponentsConfiguration.layoutCalculationAlwaysUseDefaultThreadPool) {
      return DefaultThreadPoolHolder.INSTANCE;
    } else if (CPU_CORES_MULTIPLIER > 0) {
      return CpuCoresThreadPoolHolder.INSTANCE;
    } else {
      return new ThreadPoolLayoutHandler(configuration);
    }
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
