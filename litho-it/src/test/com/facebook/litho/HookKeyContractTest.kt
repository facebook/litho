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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `equals`/`hashCode` contract for [HookKey], across nesting depths.
 *
 * [HookKey.hashCode] is precomputed at construction rather than walked on every call, so these pin
 * that the memoised value still agrees with structural equality -- both between interned and
 * non-interned instances, and up a parent chain.
 */
@RunWith(LithoTestRunner::class)
class HookKeyContractTest {

  private fun chain(depth: Int, intern: Boolean, callSiteIds: List<Int>): HookKey {
    var key: HookKey? = null
    for (i in 0 until depth) {
      key = HookKey.create(callSiteId = callSiteIds[i], index = 0, parent = key, intern = intern)
    }
    return checkNotNull(key)
  }

  @Test
  fun `structurally equal keys have equal hash codes at every depth`() {
    for (depth in 1..4) {
      val ids = (1..depth).toList()
      // intern = false so these are genuinely distinct instances, not the same cached object.
      val a = chain(depth, intern = false, callSiteIds = ids)
      val b = chain(depth, intern = false, callSiteIds = ids)

      assertThat(a).describedAs("depth %d: structurally equal", depth).isEqualTo(b)
      assertThat(a).describedAs("depth %d: distinct instances", depth).isNotSameAs(b)
      assertThat(a.hashCode())
          .describedAs("depth %d: equal keys must hash equally", depth)
          .isEqualTo(b.hashCode())
    }
  }

  @Test
  fun `keys differing anywhere in the chain are not equal`() {
    for (depth in 2..4) {
      val base = (1..depth).toList()
      for (position in 0 until depth) {
        val mutated = base.toMutableList().also { it[position] = it[position] + 100 }
        val a = chain(depth, intern = false, callSiteIds = base)
        val b = chain(depth, intern = false, callSiteIds = mutated)
        assertThat(a)
            .describedAs("depth %d: differing at position %d must not be equal", depth, position)
            .isNotEqualTo(b)
      }
    }
  }

  @Test
  fun `runtime index participates in identity`() {
    val parent = HookKey.create(callSiteId = 1, index = 0, parent = null, intern = false)
    val first = HookKey.create(callSiteId = 2, index = 0, parent = parent, intern = false)
    val second = HookKey.create(callSiteId = 2, index = 1, parent = parent, intern = false)

    assertThat(first).isNotEqualTo(second)
  }

  @Test
  fun `custom key identity includes call site value occurrence and parent in a disjoint domain`() {
    val parent = HookKey.create(callSiteId = 1, index = 0, parent = null, intern = false)
    val equivalentParent = HookKey.create(callSiteId = 1, index = 0, parent = null, intern = false)
    val key = HookKey.createCustom(callSiteId = 2, key = "item", parent = parent)
    val equivalent = HookKey.createCustom(callSiteId = 2, key = "item", parent = equivalentParent)

    assertThat(key).isEqualTo(equivalent)
    assertThat(key.hashCode()).isEqualTo(equivalent.hashCode())
    assertThat(key).isNotSameAs(equivalent)
    assertThat(key)
        .isNotEqualTo(HookKey.createCustom(callSiteId = 3, key = "item", parent = parent))
    assertThat(key)
        .isNotEqualTo(HookKey.createCustom(callSiteId = 2, key = "other", parent = parent))
    assertThat(key)
        .isNotEqualTo(
            HookKey.createCustom(
                callSiteId = 2,
                key = "item",
                parent = parent,
                duplicateIndex = 1,
            ),
        )
    val ordinary = HookKey.create(callSiteId = 2, index = 0, parent = parent, intern = false)
    assertThat(key).isNotEqualTo(ordinary)
    assertThat(ordinary).isNotEqualTo(key)
  }

  @Test
  fun `hash code survives the only post-construction mutation a HookKey undergoes`() {
    // `children` is populated lazily by interning and is deliberately excluded from identity. It is
    // the one piece of a HookKey that changes after construction, so it is the only thing that
    // could invalidate a memoised hash. Reading `hashCode()` repeatedly would prove nothing --
    // `hash` is a final field, so that holds under any implementation, including the recursive one
    // this replaced.
    val key = HookKey.create(callSiteId = 1, index = 0, parent = null, intern = true)
    val before = key.hashCode()

    // Mutates key.children.
    HookKey.create(callSiteId = 99, index = 0, parent = key, intern = true)
    HookKey.create(callSiteId = 98, index = 0, parent = key, intern = true)

    assertThat(key.hashCode())
        .describedAs("acquiring children must not change a key's hash")
        .isEqualTo(before)
    assertThat(key)
        .describedAs("acquiring children must not change a key's identity")
        .isEqualTo(HookKey.create(callSiteId = 1, index = 0, parent = null, intern = false))
  }

  @Test
  fun `a value stored under an interned key is retrievable with an equal non-interned key`() {
    // The point of the memoisation: HookKey is used as a HashMap key (InitialState's per-hook seed
    // map), so hashCode is called on every get and put. This is the round-trip that has to hold.
    val ids = listOf(21, 22, 23)
    val map = HashMap<HookKey, String>()
    map[chain(depth = 3, intern = true, callSiteIds = ids)] = "seeded"

    assertThat(map[chain(depth = 3, intern = false, callSiteIds = ids)])
        .describedAs("structurally equal key must hit the same bucket and compare equal")
        .isEqualTo("seeded")
    assertThat(map[chain(depth = 3, intern = false, callSiteIds = listOf(21, 22, 24))])
        .describedAs("a different chain must miss")
        .isNull()
  }

  @Test
  fun `interned and non-interned keys with the same shape agree on equality and hash`() {
    val ids = listOf(3, 4, 5)
    val interned = chain(depth = 3, intern = true, callSiteIds = ids)
    val plain = chain(depth = 3, intern = false, callSiteIds = ids)

    assertThat(interned).isEqualTo(plain)
    assertThat(interned.hashCode()).isEqualTo(plain.hashCode())
  }

  @Test
  fun `interning returns the same instance for the same shape`() {
    val ids = listOf(11, 12)
    assertThat(chain(depth = 2, intern = true, callSiteIds = ids))
        .isSameAs(chain(depth = 2, intern = true, callSiteIds = ids))
  }

  @Test
  fun `concurrent first calls to hashCode all observe the same value`() {
    // The cache is written without synchronisation, on the same reasoning as GlobalKey and
    // java.lang.String: racing threads compute the same value and an Int write cannot tear. This
    // pins that, by racing the *first* call -- the only one that writes.
    repeat(50) { round ->
      val key = chain(depth = 4, intern = false, callSiteIds = listOf(round, 2, 3, 4))
      val threads = 8
      val start = CountDownLatch(1)
      val done = CountDownLatch(threads)
      val observed = IntArray(threads)

      (0 until threads)
          .map { t ->
            Thread {
              start.await()
              observed[t] = key.hashCode()
              done.countDown()
            }
          }
          .forEach { it.start() }

      start.countDown()
      assertThat(done.await(30, TimeUnit.SECONDS)).isTrue

      val expected = 31 * key.id.hashCode() + (key.parent?.hashCode() ?: 0)
      for (t in 0 until threads) {
        assertThat(observed[t])
            .describedAs("round %d: thread %d must observe the canonical hash", round, t)
            .isEqualTo(expected)
      }
    }
  }
}
