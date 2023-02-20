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

import java.lang.IndexOutOfBoundsException
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SparseFloatArrayTest {
  @Test
  fun test_get() {
    val array = SparseFloatArray()
    Assertions.assertThat(array[1]).isEqualTo(0f)
    Assertions.assertThat(array[1, 1f]).isEqualTo(1f)
    Assertions.assertThat(array[1, 1f]).isEqualTo(1f)
  }

  @Test
  fun test_append() {
    val array = SparseFloatArray()
    array.append(4, 1f)
    Assertions.assertThat(array[4]).isEqualTo(1f)

    // override
    array.append(4, 2f)
    Assertions.assertThat(array[4]).isEqualTo(2f)

    // do not use default value if found
    array.append(6, 1f)
    Assertions.assertThat(array[6, -1f]).isEqualTo(1f)

    // use default value if NOT found
    array.append(1, 1f)
    array.append(2, 1f)
    array.append(3, 1f)
    array.append(5, 1f)
    array.append(6, 1f)
    array.append(8, 1f)
    Assertions.assertThat(array[7, -1f]).isEqualTo(-1f)
  }

  @Test
  fun test_insert() {
    val array = SparseFloatArray()
    array.put(4, 1f)
    Assertions.assertThat(array[4]).isEqualTo(1f)

    // override
    array.put(4, 2f)
    Assertions.assertThat(array[4]).isEqualTo(2f)

    // do not use default value if found
    array.put(6, 1f)
    Assertions.assertThat(array[6, -1f]).isEqualTo(1f)

    // use default value if NOT found
    array.put(1, 1f)
    array.put(2, 1f)
    array.put(3, 1f)
    array.put(5, 1f)
    array.put(6, 1f)
    array.put(8, 1f)
    Assertions.assertThat(array[7, -1f]).isEqualTo(-1f)
  }

  @Test
  fun test_delete() {
    val array = SparseFloatArray()
    array.put(1, 1f)
    array.put(2, 1f)
    array.put(3, 1f)
    array.put(4, 1f)
    array.put(5, 1f)
    array.delete(2)
    Assertions.assertThat(array[2, -1f]).isEqualTo(-1f)
    array.put(2, 1f)
    Assertions.assertThat(array[2, -1f]).isEqualTo(1f)
  }

  @Test
  fun test_removeAt() {
    val array = SparseFloatArray()
    array.put(1, 5f)
    array.put(2, 4f)
    array.put(3, 3f)
    array.put(4, 2f)
    array.put(5, 1f)
    Assertions.assertThat(array[2, -1f]).isEqualTo(4f)
    array.removeAt(1)
    Assertions.assertThat(array[2, -1f]).isEqualTo(-1f)
  }

  @Test
  fun test_keyAt_and_valueAt() {
    val array = SparseFloatArray()
    array.put(1, 5f)
    array.put(2, 4f)
    array.put(3, 3f)
    array.put(4, 2f)
    array.put(5, 1f)
    Assertions.assertThat(array.keyAt(0)).isEqualTo(1)
    Assertions.assertThat(array.valueAt(0)).isEqualTo(5f)
    Assertions.assertThat(array.keyAt(4)).isEqualTo(5)
    Assertions.assertThat(array.valueAt(4)).isEqualTo(1f)
  }

  @Test
  fun test_setValueAt() {
    val array = SparseFloatArray()
    array.put(1, 1f)
    array.put(2, 2f)
    array.put(3, 3f)
    array.put(4, 4f)
    array.put(5, 5f)
    array.setValueAt(2, 0f)
    Assertions.assertThat(array[3, -1f]).isEqualTo(0f)
    Assertions.assertThat(array.valueAt(2)).isEqualTo(0f)
  }

  @Test
  fun test_indexOfKey() {
    val array = SparseFloatArray()
    array.put(5, 1f)
    array.put(4, 2f)
    array.put(3, 3f)
    array.put(2, 4f)
    array.put(1, 5f)
    Assertions.assertThat(array.indexOfKey(1)).isEqualTo(0)
    Assertions.assertThat(array.indexOfKey(5)).isEqualTo(4)
    Assertions.assertThat(array.indexOfKey(3)).isEqualTo(2)
  }

  @Test(expected = IndexOutOfBoundsException::class)
  fun test_out_of_bounds_check_keyAt() {
    val array = SparseFloatArray()
    array.put(1, 5f)
    array.put(2, 4f)
    array.put(3, 3f)
    array.put(4, 2f)
    array.put(5, 1f)
    array.keyAt(8)
  }

  @Test(expected = IndexOutOfBoundsException::class)
  fun test_out_of_bounds_check_valueAt() {
    val array = SparseFloatArray()
    array.put(1, 5f)
    array.put(2, 4f)
    array.put(3, 3f)
    array.put(4, 2f)
    array.put(5, 1f)
    array.valueAt(8)
  }

  @Test
  fun test_growth_upto_growth() {
    val array = SparseFloatArray()
    array.put(1, 1f)
    Assertions.assertThat(array.valueAt(0)).isEqualTo(1f)
    Assertions.assertThat(array.valueAt(1)).isEqualTo(0f)
    array.put(2, 1f)
    Assertions.assertThat(array.valueAt(1)).isEqualTo(1f)
    array.put(3, 1f)
    Assertions.assertThat(array.valueAt(2)).isEqualTo(1f)
  }

  @Test(expected = IndexOutOfBoundsException::class)
  fun test_growth_at_limit() {
    val array = SparseFloatArray()
    array.put(1, 1f)
    Assertions.assertThat(array.valueAt(0)).isEqualTo(1f)

    // next should be zero
    Assertions.assertThat(array.valueAt(1)).isEqualTo(0f)
    array.put(2, 1f)
    Assertions.assertThat(array.valueAt(1)).isEqualTo(1f)

    // next should throw IndexOutOfBoundsException
    array.valueAt(2)
  }

  @Test(expected = IndexOutOfBoundsException::class)
  fun test_growth_greater_than() {
    val array = SparseFloatArray()
    array.put(1, 1f)
    array.put(2, 1f)
    array.put(3, 1f)
    array.put(4, 1f)
    Assertions.assertThat(array.valueAt(0)).isEqualTo(1f)
    Assertions.assertThat(array.valueAt(1)).isEqualTo(1f)
    Assertions.assertThat(array.valueAt(2)).isEqualTo(1f)
    Assertions.assertThat(array.valueAt(3)).isEqualTo(1f)
    array.put(5, 1f)
    Assertions.assertThat(array.valueAt(4)).isEqualTo(1f)

    // rest should be zero
    Assertions.assertThat(array.valueAt(5)).isEqualTo(0f)
    Assertions.assertThat(array.valueAt(6)).isEqualTo(0f)
    Assertions.assertThat(array.valueAt(7)).isEqualTo(0f)

    // next should throw IndexOutOfBoundsException
    array.valueAt(8)
  }
}
