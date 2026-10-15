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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentKeyUtilsTest {

  @Test
  fun formatKeySegment_manualKey_returnsKeyUnchanged() {
    val idToTypeMap = mapOf<Int, Any>()
    val result = ComponentKeyUtils.formatKeySegment("\$myManualKey", idToTypeMap)
    assertThat(result).isEqualTo("\$myManualKey")
  }

  @Test
  fun formatKeySegment_knownId_returnsClassName() {
    val idToTypeMap = mapOf<Int, Any>(42 to String::class.java)
    val result = ComponentKeyUtils.formatKeySegment("42", idToTypeMap)
    assertThat(result).isEqualTo("<cls>java.lang.String</cls>")
  }

  @Test
  fun formatKeySegment_unknownId_returnsUnresolved() {
    val idToTypeMap = mapOf<Int, Any>(1 to String::class.java)
    val result = ComponentKeyUtils.formatKeySegment("99", idToTypeMap)
    assertThat(result).isEqualTo("id(99)")
  }

  @Test
  fun formatKeySegment_nonNumericKey_returnsUnresolved() {
    val idToTypeMap = mapOf<Int, Any>(1 to String::class.java)
    val result = ComponentKeyUtils.formatKeySegment("abc", idToTypeMap)
    assertThat(result).isEqualTo("id(abc)")
  }

  @Test
  fun formatKeySegment_nonClassType_returnsToString() {
    val idToTypeMap = mapOf<Int, Any>(7 to "MyComponent")
    val result = ComponentKeyUtils.formatKeySegment("7", idToTypeMap)
    assertThat(result).isEqualTo("MyComponent")
  }

  @Test
  fun formatKeySegment_keyWithSuffix_extractsId() {
    val idToTypeMap = mapOf<Int, Any>(5 to String::class.java)
    val result = ComponentKeyUtils.formatKeySegment("5!2", idToTypeMap)
    assertThat(result).isEqualTo("<cls>java.lang.String</cls>")
  }

  @Test
  fun formatKeySegment_emptyMap_returnsUnresolved() {
    val idToTypeMap = mapOf<Int, Any>()
    val result = ComponentKeyUtils.formatKeySegment("1", idToTypeMap)
    assertThat(result).isEqualTo("id(1)")
  }

  @Test
  fun getOrCreateId_populatesReverseMap() {
    val id = Component.getOrCreateId(ComponentKeyUtilsTest::class.java)
    val hierarchy = Component.generateHierarchy(id.toString())
    assertThat(hierarchy).hasSize(1)
    assertThat(hierarchy.first).contains("ComponentKeyUtilsTest")
  }

  @Test
  fun getOrCreateId_sameType_returnsSameId() {
    val id1 = Component.getOrCreateId(String::class.java)
    val id2 = Component.getOrCreateId(String::class.java)
    assertThat(id1).isEqualTo(id2)
  }

  @Test
  fun getOrCreateId_differentTypes_returnDifferentIds() {
    val id1 = Component.getOrCreateId(Int::class.java)
    val id2 = Component.getOrCreateId(Long::class.java)
    assertThat(id1).isNotEqualTo(id2)
  }

  // --- parentNameFromKey tests ---

  @Test
  fun parentNameFromKey_null_returnsNull() {
    assertThat(ComponentKeyUtils.parentNameFromKey(null, emptyMap())).isNull()
  }

  @Test
  fun parentNameFromKey_defaultGlobalKey_noParent_returnsNull() {
    val key = DefaultGlobalKey(key = 1, position = 0, parent = null)
    assertThat(ComponentKeyUtils.parentNameFromKey(key, mapOf(1 to String::class.java))).isNull()
  }

  @Test
  fun parentNameFromKey_defaultGlobalKey_resolvesParentType() {
    val parentKey = DefaultGlobalKey(key = 10, position = 0, parent = null)
    val childKey = DefaultGlobalKey(key = 20, position = 0, parent = parentKey)
    val idToType = mapOf<Int, Any>(10 to String::class.java, 20 to Int::class.java)
    assertThat(ComponentKeyUtils.parentNameFromKey(childKey, idToType))
        .isEqualTo("java.lang.String")
  }

  @Test
  fun parentNameFromKey_defaultGlobalKey_unknownParentType_returnsNull() {
    val parentKey = DefaultGlobalKey(key = 10, position = 0, parent = null)
    val childKey = DefaultGlobalKey(key = 20, position = 0, parent = parentKey)
    val idToType = mapOf<Int, Any>(20 to Int::class.java)
    assertThat(ComponentKeyUtils.parentNameFromKey(childKey, idToType)).isNull()
  }

  @Test
  fun parentNameFromKey_defaultGlobalKey_nonClassType_returnsToString() {
    val parentKey = DefaultGlobalKey(key = 10, position = 0, parent = null)
    val childKey = DefaultGlobalKey(key = 20, position = 0, parent = parentKey)
    val idToType = mapOf<Int, Any>(10 to "MyParent")
    assertThat(ComponentKeyUtils.parentNameFromKey(childKey, idToType)).isEqualTo("MyParent")
  }

  @Test
  fun parentNameFromKey_stringGlobalKey_singleSegment_returnsNull() {
    val key = StringGlobalKey("42")
    assertThat(ComponentKeyUtils.parentNameFromKey(key, mapOf(42 to String::class.java))).isNull()
  }

  @Test
  fun parentNameFromKey_stringGlobalKey_resolvesParentSegment() {
    val key = StringGlobalKey("10,20")
    val idToType = mapOf<Int, Any>(10 to String::class.java, 20 to Int::class.java)
    assertThat(ComponentKeyUtils.parentNameFromKey(key, idToType)).isEqualTo("java.lang.String")
  }

  @Test
  fun parentNameFromKey_stringGlobalKey_threeSegments_resolvesMiddle() {
    val key = StringGlobalKey("5,10,20")
    val idToType = mapOf<Int, Any>(5 to Long::class.java, 10 to String::class.java)
    assertThat(ComponentKeyUtils.parentNameFromKey(key, idToType)).isEqualTo("java.lang.String")
  }

  @Test
  fun parentNameFromKey_stringGlobalKey_manualKeyParent_returnsRawSegment() {
    val key = StringGlobalKey("10,\$myKey,20")
    assertThat(ComponentKeyUtils.parentNameFromKey(key, emptyMap())).isEqualTo("\$myKey")
  }

  @Test
  fun parentNameFromKey_stringGlobalKey_unknownParentId_returnsNull() {
    val key = StringGlobalKey("99,20")
    val idToType = mapOf<Int, Any>(20 to Int::class.java)
    assertThat(ComponentKeyUtils.parentNameFromKey(key, idToType)).isNull()
  }

  @Test
  fun getOrCreateId_concurrentAccess_allIdsResolvable() {
    val numThreads = 8
    // Use unique marker classes to avoid collisions with other tests
    val types =
        (0 until numThreads).map {
          object : Any() {
            override fun toString(): String = "ConcurrentType$it"
          }
        }
    val barrier = CyclicBarrier(numThreads)
    val latch = CountDownLatch(numThreads)
    val ids = IntArray(numThreads)
    val errors = mutableListOf<Throwable>()

    val executor = Executors.newFixedThreadPool(numThreads)
    types.forEachIndexed { index, type ->
      executor.submit {
        try {
          barrier.await()
          ids[index] = Component.getOrCreateId(type)
        } catch (e: Throwable) {
          synchronized(errors) { errors.add(e) }
        } finally {
          latch.countDown()
        }
      }
    }
    latch.await()
    executor.shutdown()

    assertThat(errors).isEmpty()
    assertThat(ids.toSet()).hasSize(numThreads)
    for (i in 0 until numThreads) {
      val hierarchy = Component.generateHierarchy(ids[i].toString())
      assertThat(hierarchy).hasSize(1)
      assertThat(hierarchy.first).doesNotStartWith("id(")
    }
  }
}
