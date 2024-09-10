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

package com.facebook.litho

import android.util.SparseArray
import androidx.collection.SparseArrayCompat
import androidx.core.util.forEach
import com.facebook.infer.annotation.FalseOnNull
import com.facebook.infer.annotation.TrueOnNull

/** Provides util methods for common collection patterns. */
object CollectionsUtils {

  @TrueOnNull
  @JvmStatic
  fun isNullOrEmpty(collection: Collection<*>?): Boolean {
    return collection.isNullOrEmpty()
  }

  @FalseOnNull
  @JvmStatic
  fun isNotNullOrEmpty(collection: Collection<*>?): Boolean {
    return !isNullOrEmpty(collection)
  }

  @FalseOnNull
  @JvmStatic
  fun isEmpty(collection: Collection<*>?): Boolean {
    return collection != null && collection.isEmpty()
  }

  @TrueOnNull
  @JvmStatic
  fun isNullOrEmpty(map: Map<*, *>?): Boolean {
    return map.isNullOrEmpty()
  }

  @FalseOnNull
  @JvmStatic
  fun isNotNullOrEmpty(map: Map<*, *>?): Boolean {
    return !isNullOrEmpty(map)
  }

  @FalseOnNull
  @JvmStatic
  fun isEmpty(map: Map<*, *>?): Boolean {
    return map != null && map.isEmpty()
  }

  @TrueOnNull
  @JvmStatic
  fun isNullOrEmpty(sparseArrayCompat: SparseArrayCompat<*>?): Boolean {
    return sparseArrayCompat == null || sparseArrayCompat.isEmpty
  }

  @FalseOnNull
  @JvmStatic
  fun isNotNullOrEmpty(sparseArrayCompat: SparseArrayCompat<*>?): Boolean {
    return !isNullOrEmpty(sparseArrayCompat)
  }

  @FalseOnNull
  @JvmStatic
  fun isEmpty(sparseArrayCompat: SparseArrayCompat<*>?): Boolean {
    return sparseArrayCompat != null && sparseArrayCompat.isEmpty
  }

  @TrueOnNull
  @JvmStatic
  fun isNullOrEmpty(sparseArray: SparseArray<*>?): Boolean {
    return sparseArray == null || sparseArray.size() == 0
  }

  @FalseOnNull
  @JvmStatic
  fun isNotNullOrEmpty(sparseArray: SparseArray<*>?): Boolean {
    return !isNullOrEmpty(sparseArray)
  }

  @FalseOnNull
  @JvmStatic
  fun isEmpty(sparseArray: SparseArray<*>?): Boolean {
    return sparseArray != null && sparseArray.size() == 0
  }

  fun mergeSparseArrays(array1: SparseArray<Any>?, array2: SparseArray<Any>?): SparseArray<Any> {
    return when {
      array1 == null || array1.size() == 0 -> array2 ?: SparseArray()
      array2 == null || array2.size() == 0 -> array1
      else -> {
        val mergedArray = SparseArray<Any>(array1.size() + array2.size())
        array1.forEach { key, value -> mergedArray.put(key, value) }
        array2.forEach { key, value -> mergedArray.put(key, value) }
        mergedArray
      }
    }
  }
}
