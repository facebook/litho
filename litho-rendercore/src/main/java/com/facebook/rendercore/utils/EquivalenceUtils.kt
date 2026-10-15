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

package com.facebook.rendercore.utils

import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import com.facebook.rendercore.Equivalence
import com.facebook.rendercore.RenderCoreConfig
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

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

/**
 * Ceiling on [instanceFieldsByClass], as a backstop against pathological class generation rather
 * than a working limit. A full `litho-it` run puts 180 distinct classes through
 * [hasEquivalentFields], and IG4A's entire static `KComponent` population including the shared
 * libraries it links is roughly 2.5k, so real hosts stay far below this.
 */
private const val DEFAULT_MAX_CACHED_CLASSES = 16384

@VisibleForTesting internal var maxCachedClasses: Int = DEFAULT_MAX_CACHED_CLASSES

private val instanceFieldsByClass = ConcurrentHashMap<Class<*>, Array<Field>>()

@VisibleForTesting internal fun cachedClassCountForTesting(): Int = instanceFieldsByClass.size

@VisibleForTesting
internal fun clearInstanceFieldCacheForTesting() {
  instanceFieldsByClass.clear()
}

/**
 * The declared instance fields of [cls], with accessibility already forced, or null if [cls] is not
 * cached and the cache is full.
 *
 * [Class.getDeclaredFields] clones the array and every [Field] in it on each call, so asking for
 * them per comparison allocates proportionally to the field count before a single value is read.
 * The clone also means the accessibility flag set here is only visible to this cache.
 *
 * Static fields are dropped: reading one off two instances of the same class yields the same value
 * by construction, so they can never make two objects inequivalent.
 *
 * Entries are never evicted; on Android the classes reaching them are loaded by the app's own class
 * loader and live for the process. Growth is bounded by [maxCachedClasses], past which callers
 * resolve the fields themselves. The size check races with concurrent inserts, so the map may
 * overshoot by the number of threads in flight — this is a ceiling, not an exact size.
 */
private fun instanceFieldsOf(cls: Class<*>): Array<Field>? {
  instanceFieldsByClass[cls]?.let {
    return it
  }
  if (instanceFieldsByClass.size >= maxCachedClasses) {
    return null
  }
  val fields =
      cls.declaredFields
          .filterNot { Modifier.isStatic(it.modifiers) }
          .onEach { it.isAccessible = true }
          .toTypedArray()
  return instanceFieldsByClass.putIfAbsent(cls, fields) ?: fields
}

/**
 * Checks if all private final fields in two objects are equivalent.
 *
 * [RenderCoreConfig.cacheEquivalenceFields] selects between the cached field list and resolving it
 * per comparison. Reading it here is a static boolean load; the MobileConfig it is wired from is
 * read once at app startup.
 */
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

  if (RenderCoreConfig.cacheEquivalenceFields) {
    instanceFieldsOf(a.javaClass)?.let {
      return hasEquivalentFieldsFrom(it, a, b)
    }
  }
  return hasEquivalentDeclaredFields(a, b)
}

private fun hasEquivalentFieldsFrom(fields: Array<Field>, a: Any, b: Any): Boolean {
  for (field in fields) {
    val val1: Any?
    val val2: Any?
    try {
      val1 = field[a]
      val2 = field[b]
    } catch (e: IllegalAccessException) {
      throw IllegalStateException("Unable to get fields by reflection.", e)
    }

    if (!areObjectsEquivalent(val1, val2)) {
      return false
    }
  }

  return true
}

private fun hasEquivalentDeclaredFields(a: Any, b: Any): Boolean {
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
    val1 is CharSequence -> safeCharSequenceEquals(val1, val2 as CharSequence)
    else -> val1 == val2
  }
}

/**
 * Safely compares two [CharSequence] objects for equality. Android's
 * [android.text.SpannableStringInternal.equals] can throw [NullPointerException] when the spans
 * array contains null elements (a known Android framework bug on API 34+). This method catches that
 * NPE and returns false (not equivalent), which triggers a safe re-render in Litho rather than
 * crashing.
 */
private fun safeCharSequenceEquals(val1: CharSequence, val2: CharSequence): Boolean {
  return try {
    val1 == val2
  } catch (e: NullPointerException) {
    false
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
