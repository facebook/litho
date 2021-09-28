/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.sections.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s pagination prop */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class CollectionTailPaginationManagerTest {

  @Test
  fun `test tail pagination manager calls fetchNextPage correctly`() {
    var fetchNextPageWasCalled = false
    val tailPaginationManager = Collection.tailPagination { fetchNextPageWasCalled = true }

    // Final item is not visible
    fetchNextPageWasCalled = false
    tailPaginationManager(0, 2)
    assertThat(fetchNextPageWasCalled).isFalse

    // Final item is visible
    fetchNextPageWasCalled = false
    tailPaginationManager(0, 1)
    assertThat(fetchNextPageWasCalled).isTrue

    // Over scrolled
    fetchNextPageWasCalled = false
    tailPaginationManager(0, 0)
    assertThat(fetchNextPageWasCalled).isTrue
  }

  @Test
  fun `test tail pagination manager respects offsetBeforeTailFetch`() {
    var fetchNextPageWasCalled = false
    val tailPaginationManager =
        Collection.tailPagination(offsetBeforeTailFetch = 10) { fetchNextPageWasCalled = true }

    // Haven't scrolled to offset
    fetchNextPageWasCalled = false
    tailPaginationManager(0, 20)
    assertThat(fetchNextPageWasCalled).isFalse

    // Scrolled to last item before offset
    fetchNextPageWasCalled = false
    tailPaginationManager(8, 20)
    assertThat(fetchNextPageWasCalled).isFalse

    // Scrolled to offset
    fetchNextPageWasCalled = false
    tailPaginationManager(9, 20)
    assertThat(fetchNextPageWasCalled).isTrue

    // Scrolled past offset
    fetchNextPageWasCalled = false
    tailPaginationManager(10, 20)
    assertThat(fetchNextPageWasCalled).isTrue
  }
}
