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
import org.junit.Test

/**
 * What [KeyBasedRangeWorker] releases across a data change ([RangeManager.onDataSetChanged] resets
 * the worker before scheduling the new range), with and without `releaseOutOfRangeOnReset`.
 */
class KeyBasedRangeWorkerResetTest {

  private val items = (0 until 30).associate { "k$it" to TestStringKeyItem("k$it") }
  private val provider = TestStringKeyItemProvider().apply { setItems(items) }
  private var manager: RangeManager<Int, TestStringKeyRange>? = null

  @After
  fun tearDown() {
    manager?.destroy()
  }

  /** Pins the default, which existing callers such as Feed rely on until they opt in. */
  @Test
  fun `by default, rows that leave the range on a data change stay prepared`() {
    val manager = manager(releaseOutOfRangeOnReset = false)
    manager.recalculateRange(0)

    manager.onDataSetChanged(10)

    assertThat(unprepareCounts(0 until 5)).containsOnly(0)
  }

  /** A new message arrives, nothing is removed, and the list now shows other rows. */
  @Test
  fun `rows still in the list that leave the range on a data change are released once`() {
    val manager = manager(releaseOutOfRangeOnReset = true)
    manager.recalculateRange(0)

    manager.onDataSetChanged(10)

    assertThat(unprepareCounts(0 until 5)).containsOnly(1)
    assertThat(unprepareCounts(10 until 15)).containsOnly(0)
  }

  @Test
  fun `rows still in range after a data change are not released`() {
    val manager = manager(releaseOutOfRangeOnReset = true)
    manager.recalculateRange(0)

    manager.onDataSetChanged(2)

    assertThat(unprepareCounts(0 until 2)).containsOnly(1)
    assertThat(unprepareCounts(2 until 5)).containsOnly(0)
  }

  /** The provider releases rows the data no longer has; the worker must not release them again. */
  @Test
  fun `rows removed by a data change are released once`() {
    val manager = manager(releaseOutOfRangeOnReset = true)
    manager.recalculateRange(0)
    cacheAll(0 until 5)
    provider.setItems(items - setOf("k0", "k1"))

    manager.onDataSetChanged(10)

    assertThat(unprepareCounts(0 until 5)).containsOnly(1)
  }

  /** A caller can release a row itself, as Feed does for a row that is no longer single-owner. */
  @Test
  fun `a row the provider already released is not released again when it leaves the range`() {
    val manager = manager(releaseOutOfRangeOnReset = true)
    manager.recalculateRange(0)
    provider.invalidate("k2")

    manager.recalculateRange(10)

    assertThat(items.getValue("k2").unprepareCount).isEqualTo(1)
  }

  /** Same as without the option: a reset leaves no current range until the next schedule. */
  /** Stories uses a provider without a cache; the option must not stop its ordinary releases. */
  @Test
  fun `with no cache, a row that leaves the range is still released`() {
    val uncached = TestStringKeyItemProvider(NoCache()).apply { setItems(items) }
    val worker = KeyBasedRangeWorker(uncached, releaseOutOfRangeOnReset = true)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0", "k1")))

    worker.schedule(Unit, TestStringKeyRange(listOf("k5")))

    assertThat(unprepareCounts(0 until 2)).containsOnly(1)
    worker.destroy()
  }

  @Test
  fun `after a reset, a key is not prepared on demand until the next schedule`() {
    val worker = KeyBasedRangeWorker(provider, releaseOutOfRangeOnReset = true)
    worker.schedule(Unit, TestStringKeyRange(listOf("k0")))

    worker.reset()

    assertThat(worker.prepareKeyIfInRange("k0")).isFalse()
    worker.destroy()
  }

  private fun manager(releaseOutOfRangeOnReset: Boolean): RangeManager<Int, TestStringKeyRange> {
    val scope = PrepareScope()
    return RangeManager(
        calculator =
            RangeCalculator<Int, TestStringKeyRange> { first ->
              TestStringKeyRange((first until first + 5).map { "k$it" })
            },
        worker =
            with(RangeManager) {
              keyBasedWorker(
                  itemProvider = provider,
                  scope = scope,
                  releaseOutOfRangeOnReset = releaseOutOfRangeOnReset,
              )
            },
        itemProvider = provider,
        scope = scope,
    )
        .also { manager = it }
  }

  private fun cacheAll(indices: IntRange) = indices.forEach { provider["k$it"] }

  private fun unprepareCounts(indices: IntRange): List<Int> = indices.map {
    items.getValue("k$it").unprepareCount
  }
}
