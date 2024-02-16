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

import android.graphics.Typeface
import android.util.SparseArray
import com.facebook.rendercore.utils.SynchronizedTypefaceHelper.SynchronizedLongSparseArray
import com.facebook.rendercore.utils.SynchronizedTypefaceHelper.SynchronizedSparseArray
import com.facebook.rendercore.utils.SynchronizedTypefaceHelper.SynchronizedTypefaceSparseArray
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SynchronizedTypefaceHelperTest {

  @Test
  fun testSynchronizedTypefaceSparseArray() {
    val sparseArray = SparseArray<Typeface?>()
    sparseArray.put(1, Typeface.DEFAULT)
    val synchronizedSparseArray = SynchronizedTypefaceSparseArray(sparseArray)
    synchronizedSparseArray.put(2, Typeface.DEFAULT_BOLD)
    Assertions.assertThat(synchronizedSparseArray[1]).isSameAs(Typeface.DEFAULT)
    Assertions.assertThat(synchronizedSparseArray[2]).isSameAs(Typeface.DEFAULT_BOLD)
  }

  @Test
  fun testSynchronizedLongSparseArray() {
    val synchronizedLongSparseArray = SynchronizedLongSparseArray(Any(), 2)
    val sparseArray = SparseArray<Typeface?>()
    sparseArray.put(1, Typeface.DEFAULT)
    synchronizedLongSparseArray.put(2, sparseArray)
    val gotSparseArray = synchronizedLongSparseArray[2]
    Assertions.assertThat(gotSparseArray).isInstanceOf(SynchronizedTypefaceSparseArray::class.java)
    Assertions.assertThat(gotSparseArray!![1]).isSameAs(Typeface.DEFAULT)
  }

  @Test
  fun testSynchronizedSparseArray() {
    val synchronizedSparseArray = SynchronizedSparseArray(Any(), 2)
    val sparseArray = SparseArray<Typeface?>()
    sparseArray.put(1, Typeface.DEFAULT)
    synchronizedSparseArray.put(2, sparseArray)
    val gotSparseArray = synchronizedSparseArray[2]
    Assertions.assertThat(gotSparseArray).isInstanceOf(SynchronizedTypefaceSparseArray::class.java)
    Assertions.assertThat(gotSparseArray!![1]).isSameAs(Typeface.DEFAULT)
  }
}
