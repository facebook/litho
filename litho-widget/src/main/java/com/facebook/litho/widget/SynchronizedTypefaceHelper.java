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

package com.facebook.litho.widget;

import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.os.Build;
import android.util.LongSparseArray;
import android.util.SparseArray;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public class SynchronizedTypefaceHelper {
  private static final AtomicBoolean sIsInitialized = new AtomicBoolean(false);

  /**
   * Android doesn't expect typeface operations to occur off of the UI thread. To partially
   * alleviate this issue, we override the typeface cache with a synchronized version.
   */
  public static void setupSynchronizedTypeface() {
    if (sIsInitialized.getAndSet(true)) {
      return;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      // sTypefaceCache was introduced in API level 16.
      return;
    }

    try {
      final Field typefaceCacheField = Typeface.class.getDeclaredField("sTypefaceCache");
      typefaceCacheField.setAccessible(true);

      final Object lock = new Object();
      // This is nasty, but otherwise we have another race condition between
      // typefaceCacheField.set and newCache.append if Typeface.create(...) is called elsewhere.
      synchronized (lock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          final LongSparseArray<SparseArray<Typeface>> oldCache =
              (LongSparseArray<SparseArray<Typeface>>) typefaceCacheField.get(null);
          final SynchronizedLongSparseArray newCache =
              new SynchronizedLongSparseArray(lock, oldCache.size());
          typefaceCacheField.set(null, newCache);
          for (int i = 0, size = oldCache.size(); i < size; i++) {
            newCache.append(
                oldCache.keyAt(i), new SynchronizedTypefaceSparseArray(oldCache.valueAt(i)));
          }
        } else {
          final SparseArray<SparseArray<Typeface>> oldCache =
              (SparseArray<SparseArray<Typeface>>) typefaceCacheField.get(null);
          final SynchronizedSparseArray newCache =
              new SynchronizedSparseArray(lock, oldCache.size());
          typefaceCacheField.set(null, newCache);
          for (int i = 0, size = oldCache.size(); i < size; i++) {
            newCache.append(
                oldCache.keyAt(i), new SynchronizedTypefaceSparseArray(oldCache.valueAt(i)));
          }
        }
      }
    } catch (Exception e) {
      // We'll probably hit some thread-safety issues as we haven't managed to set a synchronized
      // typefaceCache.
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  static class SynchronizedLongSparseArray extends LongSparseArray<SparseArray<Typeface>> {
    private final Object mLock;

    SynchronizedLongSparseArray(Object lock, int initialCapacity) {
      super(initialCapacity);
      mLock = lock;
    }

    @Override
    public SparseArray<Typeface> get(long key) {
      synchronized (mLock) {
        final SparseArray<Typeface> sparseArray = super.get(key);
        if (sparseArray == null || sparseArray instanceof SynchronizedTypefaceSparseArray) {
          return sparseArray;
        }

        final SynchronizedTypefaceSparseArray synchronizedSparseArray =
            new SynchronizedTypefaceSparseArray(sparseArray);
        put(key, synchronizedSparseArray);

        return synchronizedSparseArray;
      }
    }

    @Override
    public void put(long key, SparseArray<Typeface> value) {
      synchronized (mLock) {
        super.put(key, value);
      }
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  static class SynchronizedSparseArray extends SparseArray<SparseArray<Typeface>> {
    private final Object mLock;

    SynchronizedSparseArray(Object lock, int initialCapacity) {
      super(initialCapacity);
      mLock = lock;
    }

    @Override
    public SparseArray<Typeface> get(int key) {
      synchronized (mLock) {
        final SparseArray<Typeface> sparseArray = super.get(key);
        if (sparseArray == null || sparseArray instanceof SynchronizedTypefaceSparseArray) {
          return sparseArray;
        }

        final SynchronizedTypefaceSparseArray synchronizedSparseArray =
            new SynchronizedTypefaceSparseArray(sparseArray);
        put(key, synchronizedSparseArray);

        return synchronizedSparseArray;
      }
    }

    @Override
    public void put(int key, SparseArray<Typeface> value) {
      synchronized (mLock) {
        super.put(key, value);
      }
    }
  }

  static class SynchronizedTypefaceSparseArray extends SparseArray<Typeface> {
    private final Object mLock = new Object();
    private final SparseArray<Typeface> mDelegateSparseArray;

    SynchronizedTypefaceSparseArray(SparseArray<Typeface> delegateSparseArray) {
      mDelegateSparseArray = delegateSparseArray;
    }

    @Override
    public Typeface get(int key) {
      synchronized (mLock) {
        return mDelegateSparseArray.get(key);
      }
    }

    @Override
    public void put(int key, Typeface value) {
      synchronized (mLock) {
        mDelegateSparseArray.put(key, value);
      }
    }
  }
}
