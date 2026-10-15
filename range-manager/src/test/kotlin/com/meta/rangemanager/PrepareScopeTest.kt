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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Unit tests for [PrepareScope] state save/restore. */
class PrepareScopeTest {

  /** A test item that saves [state] during unprepare and reads it back during prepare. */
  private class StatefulItem(var state: Any? = null) : PreparableCollectionItem {
    var restoredState: Any? = null

    override fun PrepareScope.prepare() {
      restoredState = getSavedState()
    }

    override fun PrepareScope.unprepare() {
      saveState(state)
    }
  }

  @Test
  fun `prepare restores null when no state saved`() {
    val scope = PrepareScope()
    val item = StatefulItem()

    scope.prepare("key1", item)

    assertThat(item.restoredState).isNull()
  }

  @Test
  fun `unprepare saves state and prepare restores it`() {
    val scope = PrepareScope()
    val state = "saved-state"
    val item = StatefulItem(state = state)

    scope.unprepare("key1", item)

    val reader = StatefulItem()
    scope.prepare("key1", reader)
    assertThat(reader.restoredState).isSameAs(state)
  }

  @Test
  fun `unprepare overwrites existing state`() {
    val scope = PrepareScope()
    val item1 = StatefulItem(state = "state1")
    val item2 = StatefulItem(state = "state2")

    scope.unprepare("key1", item1)
    scope.unprepare("key1", item2)

    val reader = StatefulItem()
    scope.prepare("key1", reader)
    assertThat(reader.restoredState).isSameAs("state2")
  }

  @Test
  fun `unprepare with null state clears saved state`() {
    val scope = PrepareScope()
    val saver = StatefulItem(state = "state")
    scope.unprepare("key1", saver)

    val clearer = StatefulItem(state = null)
    scope.unprepare("key1", clearer)

    val reader = StatefulItem()
    scope.prepare("key1", reader)
    assertThat(reader.restoredState).isNull()
  }

  @Test
  fun `state is keyed independently`() {
    val scope = PrepareScope()

    scope.unprepare("key1", StatefulItem(state = "state-for-key1"))
    scope.unprepare("key2", StatefulItem(state = "state-for-key2"))

    val reader1 = StatefulItem()
    scope.prepare("key1", reader1)
    val reader2 = StatefulItem()
    scope.prepare("key2", reader2)

    assertThat(reader1.restoredState).isEqualTo("state-for-key1")
    assertThat(reader2.restoredState).isEqualTo("state-for-key2")
  }

  @Test
  fun `clearSavedState removes state for specified key only`() {
    val scope = PrepareScope()
    scope.unprepare("key1", StatefulItem(state = "state1"))
    scope.unprepare("key2", StatefulItem(state = "state2"))

    scope.clearSavedState("key1")

    val reader1 = StatefulItem()
    scope.prepare("key1", reader1)
    val reader2 = StatefulItem()
    scope.prepare("key2", reader2)
    assertThat(reader1.restoredState).isNull()
    assertThat(reader2.restoredState).isEqualTo("state2")
  }

  @Test
  fun `clearAll removes all saved states`() {
    val scope = PrepareScope()
    scope.unprepare("key1", StatefulItem(state = "state1"))
    scope.unprepare("key2", StatefulItem(state = "state2"))

    scope.clearAll()

    val reader1 = StatefulItem()
    scope.prepare("key1", reader1)
    val reader2 = StatefulItem()
    scope.prepare("key2", reader2)
    assertThat(reader1.restoredState).isNull()
    assertThat(reader2.restoredState).isNull()
  }

  @Test
  fun `uses key equality not identity`() {
    data class DataKey(val id: String)

    val scope = PrepareScope()
    val state = "saved-state"

    scope.unprepare(DataKey("a"), StatefulItem(state = state))

    // Different DataKey instance with same value should retrieve the same state
    val reader = StatefulItem()
    scope.prepare(DataKey("a"), reader)
    assertThat(reader.restoredState).isSameAs(state)
  }

  @Test
  fun `composite children get independent state via CompoundKey`() {
    val scope = PrepareScope()
    val childA = StatefulItem(state = "stateA")
    val childB = StatefulItem(state = "stateB")
    val composite = CompositeCollectionItem("childA" to childA, "childB" to childB)

    // Unprepare the composite — each child saves state under its own CompoundKey
    scope.unprepare("parent", composite)

    // Prepare with fresh children — each should restore its own state
    val readerA = StatefulItem()
    val readerB = StatefulItem()
    val freshComposite = CompositeCollectionItem("childA" to readerA, "childB" to readerB)
    scope.prepare("parent", freshComposite)

    assertThat(readerA.restoredState).isEqualTo("stateA")
    assertThat(readerB.restoredState).isEqualTo("stateB")
  }

  @Test
  fun `clearSavedState also clears compound keys for that parent`() {
    val scope = PrepareScope()

    // Save state for parent item
    scope.unprepare("parent", StatefulItem(state = "parent-state"))

    // Save state for composite children under "parent"
    val childA = StatefulItem(state = "stateA")
    val childB = StatefulItem(state = "stateB")
    val composite = CompositeCollectionItem("childA" to childA, "childB" to childB)
    scope.unprepare("parent", composite)

    // Save state for a different parent
    scope.unprepare("other", StatefulItem(state = "other-state"))

    // Clear the first parent — should clear parent + all its compound keys
    scope.clearSavedState("parent")

    // Parent's children are cleared
    val readerA = StatefulItem()
    val readerB = StatefulItem()
    val freshComposite = CompositeCollectionItem("childA" to readerA, "childB" to readerB)
    scope.prepare("parent", freshComposite)
    assertThat(readerA.restoredState).isNull()
    assertThat(readerB.restoredState).isNull()

    // Other parent is unaffected
    val otherReader = StatefulItem()
    scope.prepare("other", otherReader)
    assertThat(otherReader.restoredState).isEqualTo("other-state")
  }

  @Test
  fun `composite without parent key still calls prepare and unprepare`() {
    val scope = PrepareScope()
    var prepareCalled = false
    var unprepareCalled = false
    val child =
        object : PreparableCollectionItem {
          override fun PrepareScope.prepare() {
            prepareCalled = true
          }

          override fun PrepareScope.unprepare() {
            unprepareCalled = true
          }
        }
    val composite = CompositeCollectionItem("child" to child)

    // Call directly without wrapping in scope.prepare(key, ...)
    // This simulates usage with PrepareScope() where no key is set
    with(composite) { scope.prepare() }
    with(composite) { scope.unprepare() }

    assertThat(prepareCalled).isTrue()
    assertThat(unprepareCalled).isTrue()
  }

  @Test
  fun `clearSavedState on non-existent key does not throw`() {
    val scope = PrepareScope()

    // Should be a no-op, not throw
    scope.clearSavedState("non-existent")
  }

  @Test
  fun `key is restored even when prepare throws`() {
    val scope = PrepareScope()
    val throwing =
        object : PreparableCollectionItem {
          override fun PrepareScope.prepare() {
            throw RuntimeException("boom")
          }

          override fun PrepareScope.unprepare() {}
        }

    // Save state for a different key first
    scope.unprepare("other", StatefulItem(state = "safe"))

    try {
      scope.prepare("bad", throwing)
    } catch (_: RuntimeException) {}

    // The key should be restored — "other" state should still be accessible
    val reader = StatefulItem()
    scope.prepare("other", reader)
    assertThat(reader.restoredState).isEqualTo("safe")
  }

  @Test
  fun `empty composite does not throw`() {
    val scope = PrepareScope()
    val composite = CompositeCollectionItem()

    scope.prepare("key", composite)
    scope.unprepare("key", composite)
    // No exception = pass
  }

  @Test
  fun `concurrent prepare and unprepare operations are thread-safe`() {
    val scope = PrepareScope()
    val threads = 20
    val iterations = 100
    val startLatch = CountDownLatch(1)
    val doneLatch = CountDownLatch(threads)
    val errorCount = AtomicInteger(0)

    repeat(threads) { threadId ->
      Thread {
        startLatch.await()
        try {
          repeat(iterations) { iter ->
            val key = "key-$threadId-$iter"
            val state = "state-$threadId-$iter"

            // Unprepare to save state
            scope.unprepare(key, StatefulItem(state = state))

            // Prepare to read it back
            val reader = StatefulItem()
            scope.prepare(key, reader)
            if (reader.restoredState !== state) {
              errorCount.incrementAndGet()
            }

            scope.clearSavedState(key)
          }
        } catch (e: Exception) {
          errorCount.incrementAndGet()
        }
        doneLatch.countDown()
      }
          .start()
    }

    startLatch.countDown()
    doneLatch.await()

    assertThat(errorCount.get()).isEqualTo(0)
  }
}
