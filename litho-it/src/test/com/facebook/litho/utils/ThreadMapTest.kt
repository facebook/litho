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

package com.facebook.litho.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Test the internal ThreadMap */
class ThreadMapTest {
  @Test
  fun canCreateAMap() {
    val map = emptyThreadMap()
    assertThat(map).isNotNull()
  }

  @Test
  fun setOfEmptyFails() {
    val map = emptyThreadMap()
    val added = map.trySet(1, 1)
    assertThat(added).isFalse()
  }

  @Test
  fun canAddOneToEmpty() {
    val map = emptyThreadMap()
    val newMap = map.newWith(1, 1)
    assertThat(newMap).isNotEqualTo(map)
    assertThat(newMap.get(1)).isEqualTo(1)
  }

  @Test
  fun canCreateForward() {
    val map = testMap(0 until 100)
    assertThat(map).isNotNull()
    for (i in 0 until 100) {
      assertThat(map.get(i.toLong())).isEqualTo(i)
    }
    for (i in -100 until 0) {
      assertThat(map.get(i.toLong())).isNull()
    }
    for (i in 100 until 200) {
      assertThat(map.get(i.toLong())).isNull()
    }
  }

  @Test
  fun canCreateBackward() {
    val map = testMap((0 until 100).reversed())
    assertThat(map).isNotNull()
    for (i in 0 until 100) {
      assertThat(map.get(i.toLong())).isEqualTo(i)
    }
    for (i in -100 until 0) {
      assertThat(map.get(i.toLong())).isNull()
    }
    for (i in 100 until 200) {
      assertThat(map.get(i.toLong())).isNull()
    }
  }

  @Test
  fun canCreateRandom() {
    val list = Array(100) { it.toLong() }
    list.shuffle()
    var map = emptyThreadMap()
    for (item in list) {
      map = map.newWith(item, item)
    }
    for (i in 0 until 100) {
      assertThat(map.get(i.toLong())).isEqualTo(i.toLong())
    }
    for (i in -100 until 0) {
      assertThat(map.get(i.toLong())).isNull()
    }
    for (i in 100 until 200) {
      assertThat(map.get(i.toLong())).isNull()
    }
  }

  @Test
  fun canRemoveOne() {
    val map = testMap(1..10)
    val set = map.trySet(5, null)
    assertThat(set).isTrue()
    for (i in 1..10) {
      if (i == 5) {
        assertThat(map.get(i.toLong())).isNull()
      } else {
        assertThat(map.get(i.toLong())).isEqualTo(i)
      }
    }
  }

  @Test
  fun canRemoveOneThenAddOne() {
    val map = testMap(1..10)
    val set = map.trySet(5, null)
    assertThat(set).isTrue()
    val newMap = map.newWith(11, 11)
    assertThat(newMap.get(5)).isNull()
    assertThat(newMap.get(11)).isEqualTo(11)
  }

  private fun emptyThreadMap() = ThreadMap(0, LongArray(0), arrayOfNulls(0))

  private fun testMap(intProgression: IntProgression): ThreadMap {
    var result = emptyThreadMap()
    for (i in intProgression) {
      result = result.newWith(i.toLong(), i)
    }
    return result
  }
}
