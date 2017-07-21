/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.lang.reflect.Field;

import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.os.Build;
import android.util.LongSparseArray;
import android.util.SparseArray;

public class SynchronizedTypefaceHelper {

  static void setupSynchronizedTypeface() {
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
