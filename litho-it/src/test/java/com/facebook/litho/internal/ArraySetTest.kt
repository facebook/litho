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

package com.facebook.litho.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ArraySetTest {

  @Test
  fun testArraySet() {
    val set = ArraySet<String?>()
    val testElt1 = "test1"
    val testElt2 = "test2"
    val testElt3 = "test3"
    val testElt4 = "test4"
    val testElt5: String? = null
    assertThat(set).isEmpty()
    assertThat(set).isEmpty()
    set.add(testElt1)
    assertThat(set).contains(testElt1)
    // Can't add more than once
    assertThat(set.add(testElt1)).isFalse
    assertThat(set).hasSize(1)
    set.remove(testElt1)
    assertThat(set).doesNotContain(testElt1).isEmpty()
    assertThat(checkIterator(set)).isTrue
    assertThat(set.add(testElt1)).isTrue
    assertThat(set.add(testElt2)).isTrue
    assertThat(set.add(testElt3)).isTrue
    assertThat(set)
        .contains(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .doesNotContain(testElt5)
        .hasSize(3)
        .isNotEmpty
    assertThat(checkIterator(set)).isTrue
    assertThat(set.remove(testElt1)).isTrue
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .hasSize(2)
    assertThat(checkIterator(set)).isTrue
    assertThat(set.add(testElt4)).isTrue
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .contains(testElt4)
        .hasSize(3)
    assertThat(checkIterator(set)).isTrue
    assertThat(set.add(testElt5)).isTrue
    assertThat(set).contains(testElt5).hasSize(4)
    assertThat(checkIterator(set)).isTrue
    assertThat(set.remove(testElt5)).isTrue
    assertThat(set.remove(testElt1)).isFalse
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .contains(testElt4)
        .hasSize(3)
    assertThat(checkIterator(set)).isTrue
    assertThat(set.remove(testElt4)).isTrue
    assertThat(set)
        .doesNotContain(testElt1)
        .contains(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .hasSize(2)
    assertThat(checkIterator(set)).isTrue
    assertThat(set.remove(testElt2)).isTrue
    assertThat(set)
        .doesNotContain(testElt1)
        .doesNotContain(testElt2)
        .contains(testElt3)
        .doesNotContain(testElt4)
        .hasSize(1)
    assertThat(checkIterator(set)).isTrue
    assertThat(set.remove(testElt3)).isTrue
    assertThat(set)
        .doesNotContain(testElt1)
        .doesNotContain(testElt2)
        .doesNotContain(testElt3)
        .doesNotContain(testElt4)
        .isEmpty()
    assertThat(checkIterator(set)).isTrue
    assertThat(set.add(testElt1)).isTrue
    assertThat(set.add(testElt2)).isTrue
    assertThat(set.add(testElt3)).isTrue
    assertThat(checkIterator(set)).isTrue
    set.clear()
    assertThat(set).doesNotContain(testElt1)
    assertThat(set).doesNotContain(testElt2)
    assertThat(set).doesNotContain(testElt3)
    assertThat(set).isEmpty()
    assertThat(set).isEmpty()
    assertThat(checkIterator(set)).isTrue
    set.add(testElt1)
    set.add(testElt2)
    set.add(testElt3)
    val it = set.iterator()
    assertThat(it.hasNext()).isTrue
    while (it.hasNext()) {
      val value = it.next()
      assertThat(value).isNotNull
      if (value === testElt2) {
        it.remove()
      }
    }
    assertThat(set).contains(testElt1).doesNotContain(testElt2).contains(testElt3)
  }

  private fun <T> checkIterator(set: ArraySet<T>): Boolean {
    var index = 0
    for (value in set) {
      if (value !== set.valueAt(index)) {
        return false
      }
      if (set.indexOf(value) != index) {
        return false
      }
      ++index
    }
    return true
  }
}
