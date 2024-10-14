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
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.ArrayList
import java.util.HashMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [CollectionsUtils] */
@RunWith(LithoTestRunner::class)
class CollectionsUtilsTest {

  @Test
  fun verifyList() {
    var list: MutableList<String>? = null
    assertThat(CollectionsUtils.isNullOrEmpty(list)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(list)).isFalse
    assertThat(CollectionsUtils.isEmpty(list)).isFalse
    list = ArrayList()
    assertThat(CollectionsUtils.isNullOrEmpty(list)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(list)).isFalse
    assertThat(CollectionsUtils.isEmpty(list)).isTrue
    list.add("1")
    assertThat(CollectionsUtils.isNullOrEmpty(list)).isFalse
    assertThat(CollectionsUtils.isNotNullOrEmpty(list)).isTrue
    assertThat(CollectionsUtils.isEmpty(list)).isFalse
  }

  @Test
  fun verifyMap() {
    var map: MutableMap<String, String>? = null
    assertThat(CollectionsUtils.isNullOrEmpty(map)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(map)).isFalse
    assertThat(CollectionsUtils.isEmpty(map)).isFalse
    map = HashMap()
    assertThat(CollectionsUtils.isNullOrEmpty(map)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(map)).isFalse
    assertThat(CollectionsUtils.isEmpty(map)).isTrue
    map["key"] = "value"
    assertThat(CollectionsUtils.isNullOrEmpty(map)).isFalse
    assertThat(CollectionsUtils.isNotNullOrEmpty(map)).isTrue
    assertThat(CollectionsUtils.isEmpty(map)).isFalse
  }

  @Test
  fun verifySparseArrayCompat() {
    var sparseArrayCompat: SparseArrayCompat<String>? = null
    assertThat(CollectionsUtils.isNullOrEmpty(sparseArrayCompat)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(sparseArrayCompat)).isFalse
    assertThat(CollectionsUtils.isEmpty(sparseArrayCompat)).isFalse
    sparseArrayCompat = SparseArrayCompat()
    assertThat(CollectionsUtils.isNullOrEmpty(sparseArrayCompat)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(sparseArrayCompat)).isFalse
    assertThat(CollectionsUtils.isEmpty(sparseArrayCompat)).isTrue
    sparseArrayCompat.put(1, "value")
    assertThat(CollectionsUtils.isNullOrEmpty(sparseArrayCompat)).isFalse
    assertThat(CollectionsUtils.isNotNullOrEmpty(sparseArrayCompat)).isTrue
    assertThat(CollectionsUtils.isEmpty(sparseArrayCompat)).isFalse
  }

  @Test
  fun verifySparseArray() {
    var sparseArray: SparseArray<String>? = null
    assertThat(CollectionsUtils.isNullOrEmpty(sparseArray)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(sparseArray)).isFalse
    assertThat(CollectionsUtils.isEmpty(sparseArray)).isFalse
    sparseArray = SparseArray()
    assertThat(CollectionsUtils.isNullOrEmpty(sparseArray)).isTrue
    assertThat(CollectionsUtils.isNotNullOrEmpty(sparseArray)).isFalse
    assertThat(CollectionsUtils.isEmpty(sparseArray)).isTrue
    sparseArray.put(1, "value")
    assertThat(CollectionsUtils.isNullOrEmpty(sparseArray)).isFalse
    assertThat(CollectionsUtils.isNotNullOrEmpty(sparseArray)).isTrue
    assertThat(CollectionsUtils.isEmpty(sparseArray)).isFalse
  }
}
