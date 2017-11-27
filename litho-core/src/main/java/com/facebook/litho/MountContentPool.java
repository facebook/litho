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

/** A RecyclePool specific to recycling MountContent. */
public class MountContentPool extends RecyclePool {

  private final AtomicInteger mPreallocationCount = new AtomicInteger(0);
  private final int mPoolSize;

  public MountContentPool(String name, int maxSize, boolean sync) {
    super(name, maxSize, sync);
    mPoolSize = maxSize;
  }

  /**
   * Pre-allocates one item for the given ComponentLifecycle if the preallocation count is less than
   * the pool size, otherwise does nothing.
   */
  void maybePreallocateContent(ComponentContext c, ComponentLifecycle lifecycle) {
    final int poolSize = lifecycle.poolSize();
    if (poolSize != mPoolSize) {
      throw new RuntimeException(
          "Expected lifecycle poolSize for "
              + lifecycle.getClass().getSimpleName()
              + " to match poolSize of recycle pool ("
              + poolSize
              + " != "
              + mPoolSize
              + ")");
    }

    // There's a slight race between checking isFull and the actual release() but this shouldn't
    // happen much and when it does it isn't that bad.
    if (!isFull() && mPreallocationCount.getAndIncrement() < lifecycle.poolSize()) {
      release(lifecycle.createMountContent(c));
    }
  }
}
