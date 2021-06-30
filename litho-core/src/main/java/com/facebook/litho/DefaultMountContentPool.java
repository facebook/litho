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

import android.content.Context;
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
  public Object acquire(Context c, Component component) {
    final Object fromPool = super.acquire();
    if (fromPool != null) {
      return fromPool;
    }

    mAllocationCount.incrementAndGet();
    return component.createMountContent(c);
  }

  @Override
  public final Object acquire() {
    throw new UnsupportedOperationException("Call acquire(ComponentContext, Component)");
  }

  /**
   * Pre-allocates one item for the given Component if the preallocation count is less than the pool
   * size, otherwise does nothing.
   */
  @Override
  public void maybePreallocateContent(Context c, Component component) {
    // There's a slight race between checking isFull and the actual release() but this shouldn't
    // happen much and when it does it isn't that bad.
    if (!isFull() && mAllocationCount.getAndIncrement() < mPoolSize) {
      release(component.createMountContent(c));
    }
  }
}
