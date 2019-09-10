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

import static com.facebook.litho.config.ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY;

import com.facebook.litho.config.DeviceInfoUtils;
import com.facebook.litho.config.LayoutThreadPoolConfiguration;

/** Configures a thread pool used for layout calculations. */
public class LayoutThreadPoolConfigurationImpl implements LayoutThreadPoolConfiguration {

  private int mCorePoolSize;
  private int mMaxPoolSize;
  private int mThreadPriority;

  public LayoutThreadPoolConfigurationImpl(int corePoolSize, int maxPoolSize, int threadPriority) {
    mCorePoolSize = corePoolSize;
    mMaxPoolSize = maxPoolSize;
    mThreadPriority = threadPriority;
  }

  @Override
  public int getCorePoolSize() {
    return mCorePoolSize;
  }

  @Override
  public int getMaxPoolSize() {
    return mMaxPoolSize;
  }

  @Override
  public int getThreadPriority() {
    return mThreadPriority;
  }

  public static class Builder {
    private boolean hasFixedSizePool;
    private int corePoolSize = 1;
    private int maxPoolSize = 1;
    private double corePoolSizeMultiplier = 1;
    private int corePoolSizeIncrement = 0;
    private double maxPoolSizeMultiplier = 1;
    private int maxPoolSizeIncrement = 0;
    private int threadPriority = DEFAULT_BACKGROUND_THREAD_PRIORITY;

    public Builder hasFixedSizePool(boolean hasFixedSizePool) {
      this.hasFixedSizePool = hasFixedSizePool;
      return this;
    }

    public Builder fixedSizePoolConfiguration(int corePoolSize, int maxPoolSize) {
      this.corePoolSize = corePoolSize;
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    public Builder coreDependentPoolConfiguration(
        double corePoolSizeMultiplier,
        int corePoolSizeIncrement,
        double maxPoolSizeMultiplier,
        int maxPoolSizeIncrement) {
      this.corePoolSizeMultiplier = corePoolSizeMultiplier;
      this.corePoolSizeIncrement = corePoolSizeIncrement;
      this.maxPoolSizeMultiplier = maxPoolSizeMultiplier;
      this.maxPoolSizeIncrement = maxPoolSizeIncrement;
      return this;
    }

    public Builder threadPriority(int threadPriority) {
      this.threadPriority = threadPriority;
      return this;
    }

    public LayoutThreadPoolConfigurationImpl build() {
      if (hasFixedSizePool) {
        return new LayoutThreadPoolConfigurationImpl(corePoolSize, maxPoolSize, threadPriority);
      }

      int numProcessors = DeviceInfoUtils.getNumberOfCPUCores();

      if (numProcessors == DeviceInfoUtils.DEVICEINFO_UNKNOWN || numProcessors == 0) {
        numProcessors = 1;
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.WARNING, "Could not read number of cores from device");
      }

      int procDepCorePoolSize =
          (int) Math.ceil(numProcessors * corePoolSizeMultiplier + corePoolSizeIncrement);
      int procDepMaxPoolSize =
          (int) Math.ceil(numProcessors * maxPoolSizeMultiplier + maxPoolSizeIncrement);

      // Safety net if we somehow try to set an invalid pool size, which can happen if we set the
      // size to 0 or the max is smaller than the core size.
      if (procDepCorePoolSize == 0) {
        procDepCorePoolSize = 1;
      }

      if (procDepMaxPoolSize < procDepCorePoolSize) {
        procDepMaxPoolSize = procDepCorePoolSize;
      }

      return new LayoutThreadPoolConfigurationImpl(
          procDepCorePoolSize, procDepMaxPoolSize, threadPriority);
    }
  }
}
