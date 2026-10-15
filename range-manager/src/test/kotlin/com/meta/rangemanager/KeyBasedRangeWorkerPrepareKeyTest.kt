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

package com.meta.rangemanager

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for [KeyBasedRangeWorker.prepareKeyIfInRange].
 *
 * This method enables preparing items immediately during registration if they fall within the
 * current range, distributing prepare work between registration and scroll callbacks.
 */
class KeyBasedRangeWorkerPrepareKeyTest {

  private lateinit var worker: KeyBasedRangeWorker<String>
  private lateinit var itemProvider: TestStringKeyItemProvider
  private lateinit var scope: PrepareScope

  @Before
  fun setup() {
    itemProvider = TestStringKeyItemProvider()
    scope = PrepareScope()
    worker = KeyBasedRangeWorker(itemProvider, scope)
  }

  @After
  fun tearDown() {
    worker.destroy()
  }

  @Test
  fun `prepareKeyIfInRange returns false when no range is set`() {
    val item = TestStringKeyItem("A")
    itemProvider.setItems(mapOf("A" to item))

    // No schedule() called yet - no range established
    val result = worker.prepareKeyIfInRange("A")

    assertThat(result).isFalse()
    assertThat(item.prepareCount).isEqualTo(0)
  }

  @Test
  fun `prepareKeyIfInRange prepares key when in current range`() {
    val itemA = TestStringKeyItem("A")
    val itemB = TestStringKeyItem("B")
    itemProvider.setItems(mapOf("A" to itemA, "B" to itemB))

    // Establish a range via schedule
    worker.schedule(Unit, TestStringKeyRange(listOf("A", "B")))
    itemA.prepareCount = 0 // Reset after schedule
    itemB.prepareCount = 0

    // Now prepareKeyIfInRange should work
    val result = worker.prepareKeyIfInRange("A")

    assertThat(result).isTrue()
    assertThat(itemA.prepareCount).isEqualTo(1)
    assertThat(itemB.prepareCount).isEqualTo(0) // Only A was prepared
  }

  @Test
  fun `prepareKeyIfInRange returns false when key not in current range`() {
    val itemA = TestStringKeyItem("A")
    val itemC = TestStringKeyItem("C")
    itemProvider.setItems(mapOf("A" to itemA, "C" to itemC))

    // Establish a range that includes only A
    worker.schedule(Unit, TestStringKeyRange(listOf("A")))
    itemA.prepareCount = 0

    // C is not in the range
    val result = worker.prepareKeyIfInRange("C")

    assertThat(result).isFalse()
    assertThat(itemC.prepareCount).isEqualTo(0)
  }

  @Test
  fun `prepareKeyIfInRange can be called multiple times - item handles idempotency`() {
    val item = TestStringKeyItem("A")
    itemProvider.setItems(mapOf("A" to item))

    // Establish a range
    worker.schedule(Unit, TestStringKeyRange(listOf("A")))
    item.prepareCount = 0

    // Call twice
    worker.prepareKeyIfInRange("A")
    worker.prepareKeyIfInRange("A")

    // Item's prepare() was called twice - item is responsible for idempotency
    assertThat(item.prepareCount).isEqualTo(2)
  }

  @Test
  fun `prepareKeyIfInRange uses current range not original range after schedule changes`() {
    val itemA = TestStringKeyItem("A")
    val itemB = TestStringKeyItem("B")
    itemProvider.setItems(mapOf("A" to itemA, "B" to itemB))

    // First range includes A
    worker.schedule(Unit, TestStringKeyRange(listOf("A")))
    itemA.prepareCount = 0

    // Second range includes only B
    worker.schedule(Unit, TestStringKeyRange(listOf("B")))
    itemA.prepareCount = 0
    itemB.prepareCount = 0

    // A is no longer in range
    val resultA = worker.prepareKeyIfInRange("A")
    assertThat(resultA).isFalse()
    assertThat(itemA.prepareCount).isEqualTo(0)

    // B is now in range
    val resultB = worker.prepareKeyIfInRange("B")
    assertThat(resultB).isTrue()
    assertThat(itemB.prepareCount).isEqualTo(1)
  }
}

// ===================================================================================
// Test helpers
// ===================================================================================

data class TestStringKeyRange(override val keys: List<String>) : KeyBasedRange<String>

class TestStringKeyItem(
    val key: String,
    private val onPrepare: (() -> Unit)? = null,
    private val onUnprepare: (() -> Unit)? = null,
) : PreparableCollectionItem {
  var prepareCount: Int = 0
  var unprepareCount: Int = 0

  override fun PrepareScope.prepare() {
    prepareCount++
    onPrepare?.invoke()
  }

  override fun PrepareScope.unprepare() {
    unprepareCount++
    onUnprepare?.invoke()
  }
}

class TestStringKeyItemProvider(
    cache: ItemCache<String> = SynchronizedItemCache(),
) : CollectionItemProvider<String>(cache) {
  private var items = mutableMapOf<String, TestStringKeyItem>()

  /** Keys passed to [create], in call order. Lets tests assert *when* an item was resolved. */
  val createdKeys: MutableList<String> = mutableListOf()

  fun setItems(items: Map<String, TestStringKeyItem>) {
    this.items = items.toMutableMap()
  }

  override fun create(key: String): PreparableCollectionItem {
    createdKeys.add(key)
    return items[key] ?: TestStringKeyItem(key)
  }

  override fun contains(key: String): Boolean {
    return items.containsKey(key)
  }
}
