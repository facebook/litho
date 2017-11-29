/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.Pools;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Used to recycle objects in Litho. Can be configured to be either syncronized or not. A {@link
 * RecyclePool} will keep track of its own size so that it can be queried to debug pool sizes.
 */
@ThreadSafe(enableChecks = false)
public class RecyclePool<T> implements PoolWithDebugInfo {
  private final String mName;
  private final int mMaxSize;
  private final boolean mIsSync;
  private final Pools.Pool<T> mPool;
  private int mCurrentSize = 0;

  public RecyclePool(String name, int maxSize, boolean sync) {
    mIsSync = sync;
    mName = name;
    mMaxSize = maxSize;
    mPool = sync ? new Pools.SynchronizedPool<T>(maxSize) : new Pools.SimplePool<T>(maxSize);
  }

  public T acquire() {
    T item;
    if (mIsSync) {
      synchronized (this) {
        item = mPool.acquire();
        mCurrentSize = Math.max(0, mCurrentSize - 1);
      }
    } else {
      item = mPool.acquire();
      mCurrentSize = Math.max(0, mCurrentSize - 1);
    }
    return item;
  }

  public void release(T item) {
    if (mIsSync) {
      synchronized (this) {
        mPool.release(item);
        mCurrentSize = Math.min(mMaxSize, mCurrentSize + 1);
      }
    } else {
      mPool.release(item);
      mCurrentSize = Math.min(mMaxSize, mCurrentSize + 1);
    }
  }

  @Override
  public String getName() {
    return mName;
  }

  @Override
  public int getMaxSize() {
    return mMaxSize;
  }

  @Override
  public int getCurrentSize() {
    return mCurrentSize;
  }

  public boolean isFull() {
    return mCurrentSize >= mMaxSize;
  }

  public void clear() {
    if (mIsSync) {
      synchronized (this) {
        while (acquire() != null) {
          // no-op.
        }
      }
    } else {
      while (acquire() != null) {
        // no-op.
      }
    }
  }
}
