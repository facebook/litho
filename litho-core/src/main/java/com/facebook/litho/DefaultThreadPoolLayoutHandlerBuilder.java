/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

/**
 * Default provider for a ThreadPoolLayoutHandler. If no values are configured for the pool sizes,
 * it will create a pool with a size of 1.
 *
 * <p>If a fixed size pool is enabled, a ThreadPoolLayoutHandler with a fixed core and max pool size
 * will be provided. Otherwise, it uses the multiplier and increment values passed to the builder to
 * define the core pool size and max pool size as (numProcessors * multiplier + increment).
 */
public class DefaultThreadPoolLayoutHandlerBuilder {

  private boolean mHasFixedSizePool;
  private int mCorePoolSize = 1;
  private int mMaxPoolSize = 1;
  private double mCorePoolSizeMultiplier = 1;
  private int mCorePoolSizeIncrement = 0;
  private double mMaxPoolSizeMultiplier = 1;
  private int mMaxPoolSizeIncrement = 0;

  public DefaultThreadPoolLayoutHandlerBuilder() {}

  public DefaultThreadPoolLayoutHandlerBuilder hasFixedSizePool(boolean hasFixedSizePool) {
    mHasFixedSizePool = hasFixedSizePool;
    return this;
  }

  public DefaultThreadPoolLayoutHandlerBuilder fixedSizePoolConfiguration(
      int corePoolSize, int maxPoolSize) {
    mCorePoolSize = corePoolSize;
    mMaxPoolSize = maxPoolSize;
    return this;
  }

  public DefaultThreadPoolLayoutHandlerBuilder coreDependentPoolConfiguration(
      double corePoolSizeMultiplier,
      int corePoolSizeIncrement,
      double maxPoolSizeMultiplier,
      int maxPoolSizeIncrement) {
    mCorePoolSizeMultiplier = corePoolSizeMultiplier;
    mCorePoolSizeIncrement = corePoolSizeIncrement;
    mMaxPoolSizeMultiplier = maxPoolSizeMultiplier;
    mMaxPoolSizeIncrement = maxPoolSizeIncrement;
    return this;
  }

  /**
   * Creates a ThreadPoolLayoutHandler based on the Builder configuration. If no
   *
   * @return
   */
  public ThreadPoolLayoutHandler build() {
    if (mHasFixedSizePool) {
      return new ThreadPoolLayoutHandler(mCorePoolSize, mMaxPoolSize);
    }

    return new ThreadPoolLayoutHandler(
        new ThreadPoolLayoutHandler.PoolSizeCalculator() {
          @Override
          public int getCorePoolSize(int numProcessors) {
            return (int) (numProcessors * mCorePoolSizeMultiplier + mCorePoolSizeIncrement);
          }

          @Override
          public int getMaxPoolSize(int numProcessors) {
            return (int) (numProcessors * mMaxPoolSizeMultiplier + mMaxPoolSizeIncrement);
          }
        });
  }
}
