/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.Typeface
import android.os.Build
import android.util.LongSparseArray
import android.util.SparseArray
import androidx.core.util.forEach
import java.util.concurrent.atomic.AtomicBoolean

object SynchronizedTypefaceHelper {

  private val isInitialized = AtomicBoolean(false)

  /**
   * Android doesn't expect typeface operations to occur off of the UI thread. To partially
   * alleviate this issue, we override the typeface cache with a synchronized version.
   */
  @SuppressLint("PrivateApi")
  @JvmStatic
  fun setupSynchronizedTypeface() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      // sTypefaceCache was introduced in API level 16.
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      // sTypefaceCache was removed and Typeface is thread-safe since Android 9.
      return
    }
    if (isInitialized.getAndSet(true)) {
      return
    }
    try {
      val typefaceCacheField = Typeface::class.java.getDeclaredField("sTypefaceCache")
      typefaceCacheField.isAccessible = true
      val lock = Any()
      // This is nasty, but otherwise we have another race condition between
      // typefaceCacheField.set and newCache.append if Typeface.create(...) is called elsewhere.
      synchronized(lock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          val oldCache = typefaceCacheField[null] as LongSparseArray<SparseArray<Typeface?>>
          val newCache = SynchronizedLongSparseArray(lock, oldCache.size())
          typefaceCacheField[null] = newCache
          oldCache.forEach { key, value ->
            newCache.append(key, SynchronizedTypefaceSparseArray(value))
          }
        } else {
          val oldCache = typefaceCacheField[null] as SparseArray<SparseArray<Typeface?>>
          val newCache = SynchronizedSparseArray(lock, oldCache.size())
          typefaceCacheField[null] = newCache
          oldCache.forEach { key, value ->
            newCache.append(key, SynchronizedTypefaceSparseArray(value))
          }
        }
      }
    } catch (e: Exception) {
      // We'll probably hit some thread-safety issues as we haven't managed to set a synchronized
      // typefaceCache.
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  internal class SynchronizedLongSparseArray(private val lock: Any, initialCapacity: Int) :
      LongSparseArray<SparseArray<Typeface?>?>(initialCapacity) {

    override fun get(key: Long): SparseArray<Typeface?>? {
      synchronized(lock) {
        val sparseArray = super.get(key)
        if (sparseArray == null || sparseArray is SynchronizedTypefaceSparseArray) {
          return sparseArray
        }
        val synchronizedSparseArray = SynchronizedTypefaceSparseArray(sparseArray)
        put(key, synchronizedSparseArray)
        return synchronizedSparseArray
      }
    }

    override fun put(key: Long, value: SparseArray<Typeface?>?) {
      synchronized(lock) { super.put(key, value) }
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  internal class SynchronizedSparseArray(private val lock: Any, initialCapacity: Int) :
      SparseArray<SparseArray<Typeface?>?>(initialCapacity) {

    override fun get(key: Int): SparseArray<Typeface?>? {
      synchronized(lock) {
        val sparseArray = super.get(key)
        if (sparseArray == null || sparseArray is SynchronizedTypefaceSparseArray) {
          return sparseArray
        }
        val synchronizedSparseArray = SynchronizedTypefaceSparseArray(sparseArray)
        put(key, synchronizedSparseArray)
        return synchronizedSparseArray
      }
    }

    override fun put(key: Int, value: SparseArray<Typeface?>?) {
      synchronized(lock) { super.put(key, value) }
    }
  }

  internal class SynchronizedTypefaceSparseArray(delegateSparseArray: SparseArray<Typeface?>?) :
      SparseArray<Typeface?>() {
    private val lock = Any()
    private val delegateSparseArray: SparseArray<Typeface?> = delegateSparseArray ?: SparseArray()

    override fun get(key: Int): Typeface? {
      synchronized(lock) {
        return delegateSparseArray[key]
      }
    }

    override fun put(key: Int, value: Typeface?) {
      synchronized(lock) { delegateSparseArray.put(key, value) }
    }
  }
}
