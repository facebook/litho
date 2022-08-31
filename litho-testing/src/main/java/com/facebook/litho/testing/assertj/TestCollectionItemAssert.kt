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

package com.facebook.litho.testing.assertj

import com.facebook.litho.testing.TestCollectionItem
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat

class TestCollectionItemAssert(testCollectionItem: TestCollectionItem) :
    AbstractAssert<TestCollectionItemAssert, TestCollectionItem>(
        testCollectionItem, TestCollectionItemAssert::class.java) {

  /** Assert the item is visible. */
  fun isVisible(): TestCollectionItemAssert {
    assertThat(actual.isVisible).overridingErrorMessage("Expected item to be visible").isTrue
    return this
  }

  /** Assert the item is not visible */
  fun isNotVisible(): TestCollectionItemAssert {
    assertThat(actual.isVisible).overridingErrorMessage("Expected item to not be visible").isFalse
    return this
  }

  /** Assert the item is fully visible. */
  fun isFullyVisible(): TestCollectionItemAssert {
    assertThat(actual.isFullyVisible)
        .overridingErrorMessage(
            "Expected item to be fully visible but is not.  The item ${if (actual.isVisible) "is" else "also is not "} partially visible")
        .isTrue
    return this
  }

  /** Assert that item is not fully visible. */
  fun isNotFullyVisible(): TestCollectionItemAssert {
    assertThat(actual.isFullyVisible)
        .overridingErrorMessage("Expected item to not be fully visible but is.")
        .isFalse
    return this
  }

  /** Assert that item is at index [index] */
  fun hasIndex(index: Int): TestCollectionItemAssert {
    assertThat(actual.index)
        .overridingErrorMessage("Expected item to be at index $index but was ${actual.index}")
        .isEqualTo(index)
    return this
  }

  companion object {
    @JvmStatic
    fun assertThat(actual: TestCollectionItem): TestCollectionItemAssert =
        TestCollectionItemAssert(actual)
  }
}
