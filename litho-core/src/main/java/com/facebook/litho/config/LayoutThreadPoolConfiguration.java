/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.config;

import android.os.Process;

/** Configures a thread pool used for layout calculations. */
public class LayoutThreadPoolConfiguration {

  private int mCorePoolSize;
  private int mMaxPoolSize;
  private int mThreadPriority;

  public LayoutThreadPoolConfiguration(int corePoolSize, int maxPoolSize, int threadPriority) {
    mCorePoolSize = corePoolSize;
    mMaxPoolSize = maxPoolSize;
    mThreadPriority = threadPriority;
  }

  public int getCorePoolSize() {
    return mCorePoolSize;
  }

  public int getMaxPoolSize() {
    return mMaxPoolSize;
  }

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
    private int threadPriority = Process.THREAD_PRIORITY_BACKGROUND;

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

    public LayoutThreadPoolConfiguration build() {
      if (hasFixedSizePool) {
        return new LayoutThreadPoolConfiguration(corePoolSize, maxPoolSize, threadPriority);
      }

      int numProcessors = DeviceInfoUtils.getNumberOfCPUCores();
      int procDepCorePoolSize =
          (int) (numProcessors * corePoolSizeMultiplier + corePoolSizeIncrement);
      int procDepMaxPoolSize = (int) (numProcessors * maxPoolSizeMultiplier + maxPoolSizeIncrement);

      return new LayoutThreadPoolConfiguration(
          procDepCorePoolSize, procDepMaxPoolSize, threadPriority);
    }
  }
}
