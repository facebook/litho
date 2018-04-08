/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default {@link MountContentPool} used for mount content unless your MountSpec
 * implements @OnCreateMountContentPool.
 */
public class DefaultMountContentPool extends RecyclePool implements MountContentPool {

  private final AtomicInteger mAllocationCount = new AtomicInteger(0);
  private final int mPoolSize;

  public DefaultMountContentPool(String name, int maxSize, boolean sync) {
    super(name, maxSize, sync);
    mPoolSize = maxSize;
  }

  @Override
  public Object acquire(ComponentContext c, ComponentLifecycle lifecycle) {
    final Object fromPool = super.acquire();
    if (fromPool != null) {
      return fromPool;
    }

    mAllocationCount.incrementAndGet();
    return lifecycle.createMountContent(c);
  }

  @Override
  public final Object acquire() {
    throw new UnsupportedOperationException("Call acquire(ComponentContext, ComponentLifecycle)");
  }

  /**
   * Pre-allocates one item for the given ComponentLifecycle if the preallocation count is less than
   * the pool size, otherwise does nothing.
   */
  @Override
  public void maybePreallocateContent(ComponentContext c, ComponentLifecycle lifecycle) {
    // There's a slight race between checking isFull and the actual release() but this shouldn't
    // happen much and when it does it isn't that bad.
    if (!isFull() && mAllocationCount.getAndIncrement() < mPoolSize) {
      release(lifecycle.createMountContent(c));
    }
  }
}
