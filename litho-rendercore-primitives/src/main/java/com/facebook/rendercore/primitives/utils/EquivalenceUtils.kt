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

@file:JvmName("EquivalenceUtils")

package com.facebook.rendercore.primitives.utils

import android.util.SparseArray
import com.facebook.rendercore.primitives.Equivalence

/** Checks if two objects are equal. */
fun equals(a: Any?, b: Any?): Boolean {
  return a == b
}

/** Checks if two [SparseArray] objects are equal. */
fun equals(a: SparseArray<*>?, b: SparseArray<*>?): Boolean {
  if (a === b) {
    return true
  }

  if (a == null || b == null) {
    return false
  }

  val size = a.size()
  if (size != b.size()) {
    return false
  }

  for (index in 0 until size) {
    val key: Int = a.keyAt(index)
    if (!equals(a.valueAt(index), b.get(key))) {
      return false
    }
  }

  return true
}

/** Checks if two [Equivalence] objects are equivalent. */
fun <T : Equivalence<T>> isEquivalentTo(a: T?, b: T?): Boolean {
  return if (a === b) {
    true
  } else if (a == null || b == null) {
    false
  } else {
    a.isEquivalentTo(b)
  }
}

/**
 * Checks if two objects are equivalent if they implement [Equivalence] or compares the objects
 * field by field.
 */
@Suppress("UNCHECKED_CAST")
fun <T> isEqualOrEquivalentTo(a: T?, b: T?): Boolean {
  return if (a === b) {
    true
  } else if (a == null || b == null) {
    false
  } else if (a is Equivalence<*> && b is Equivalence<*>) {
    isEquivalentTo(a as Equivalence<Any>, b as Equivalence<Any>)
  } else {
    hasEquivalentFields(a, b)
  }
}

/** Checks if all private final fields in two objects are equivalent. */
fun hasEquivalentFields(a: Any?, b: Any?): Boolean {
  if (a === b) {
    return true
  }

  if (a == null || b == null) {
    return false
  }

  if (a.javaClass != b.javaClass) {
    return false
  }

  for (field in a.javaClass.declaredFields) {
    val val1: Any?
    val val2: Any?
    try {
      val wasAccessible = field.isAccessible
      if (!wasAccessible) {
        field.isAccessible = true
      }
      val1 = field[a]
      val2 = field[b]
      if (!wasAccessible) {
        field.isAccessible = false
      }
    } catch (e: IllegalAccessException) {
      throw IllegalStateException("Unable to get fields by reflection.", e)
    }

    if (!areObjectsEquivalent(val1, val2)) {
      return false
    }
  }

  return true
}

/** Checks if two objects are equivalent. */
@Suppress("UNCHECKED_CAST")
fun areObjectsEquivalent(val1: Any?, val2: Any?): Boolean {
  if (val1 === val2) {
    return true
  }

  if (val1 == null || val2 == null || val1.javaClass != val2.javaClass) {
    return false
  }

  return when {
    val1 is Float -> val1.compareTo(val2 as Float) == 0
    val1 is Double -> val1.compareTo(val2 as Double) == 0
    val1 is Equivalence<*> -> (val1 as Equivalence<Any>).isEquivalentTo(val2)
    val1.javaClass.isArray -> areArraysEquivalent(val1, val2)
    val1 is List<*> && val1 is RandomAccess -> areRandomAccessListsEquivalent(val1, val2 as List<*>)
    val1 is Collection<*> -> areCollectionsEquivalent(val1, val2 as Collection<*>)
    else -> val1 == val2
  }
}

/** Checks if two arrays are equivalent. */
@Suppress("UNCHECKED_CAST")
private fun areArraysEquivalent(val1: Any, val2: Any): Boolean {
  return when (val1) {
    is ByteArray -> val1.contentEquals(val2 as ByteArray)
    is ShortArray -> val1.contentEquals(val2 as ShortArray)
    is CharArray -> val1.contentEquals(val2 as CharArray)
    is IntArray -> val1.contentEquals(val2 as IntArray)
    is LongArray -> val1.contentEquals(val2 as LongArray)
    is FloatArray -> val1.contentEquals(val2 as FloatArray)
    is DoubleArray -> val1.contentEquals(val2 as DoubleArray)
    is BooleanArray -> val1.contentEquals(val2 as BooleanArray)
    else -> {
      val array1 = val1 as Array<Any>
      val array2 = val2 as Array<Any>

      if (array1.size != array2.size) {
        return false
      }

      val size = array1.size
      for (i in 0 until size) {
        if (!areObjectsEquivalent(array1[i], array2[i])) {
          return false
        }
      }

      return true
    }
  }
}

/** Checks if two random access lists are equivalent. */
private fun areRandomAccessListsEquivalent(val1: List<*>, val2: List<*>): Boolean {
  if (val1.size != val2.size) {
    return false
  }

  for (index in 0 until val1.size) {
    if (!areObjectsEquivalent(val1[index], val2[index])) {
      return false
    }
  }

  return true
}

/** Checks if two collections are equivalent. */
private fun areCollectionsEquivalent(val1: Collection<*>, val2: Collection<*>): Boolean {
  if (val1.size != val2.size) {
    return false
  }

  val iterator1 = val1.iterator()
  val iterator2 = val2.iterator()

  while (iterator1.hasNext()) {
    val elem1 = iterator1.next()
    val elem2 = iterator2.next()
    if (!areObjectsEquivalent(elem1, elem2)) {
      return false
    }
  }

  return true
}
