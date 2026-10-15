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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.state.ComponentState
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Concurrency contract for [InitialState]'s hook-identity seeding path.
 *
 * The guarantee under test is the reason [InitialState] exists at all: for a given `(GlobalKey,
 * HookKey)` the initializer runs **exactly once** within a drain epoch, and every concurrent caller
 * observes the *same* [CachedValue] instance.
 *
 * This is deliberately written against the public API rather than the storage shape, so it holds
 * across the flattening of the hook side (per-hook map entries) and will fail loudly if the locking
 * discipline regresses.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class InitialStateConcurrencyTest {

  private companion object {
    const val THREADS = 8
    const val HOOKS = 62
    const val TIMEOUT_SECONDS = 30L

    /**
     * Thread wake-up skew is comparable to the whole seeding loop, so a single round would usually
     * let thread 0 finish before thread 7 starts -- and would then pass with no locking at all.
     * Repeating with a fresh [InitialState] each time, on top of the per-hook barrier, is what
     * makes a locking regression actually reproduce.
     */
    const val ROUNDS = 25
  }

  /**
   * All deps arrays must be the *same* instance so the deps-equivalence branch of
   * `peekOrSeedInitialKStateByHookKey` is taken deterministically. A fresh `emptyArray()` per call
   * would still compare equivalent, but sharing removes the question from the test entirely.
   */
  private val deps = emptyArray<Any?>()

  private fun hookKeys(): List<HookKey> =
      (0 until HOOKS).map {
        HookKey.create(callSiteId = it, index = 0, parent = null, intern = true)
      }

  @Test
  fun `concurrent seeding of one component runs each initializer exactly once`() {
    repeat(ROUNDS) { round -> oneContendedRound(round) }
  }

  private fun oneContendedRound(round: Int) {
    val initialState = InitialState()
    val key = StringGlobalKey("concurrent-key-$round")
    val keys = hookKeys()

    val initializerRuns = ConcurrentHashMap<HookKey, AtomicInteger>()
    keys.forEach { initializerRuns[it] = AtomicInteger(0) }

    // results[t][h] = the CachedValue thread t observed for hook h
    val results = Array(THREADS) { arrayOfNulls<CachedValue>(HOOKS) }

    val start = CountDownLatch(1)
    val done = CountDownLatch(THREADS)
    val failures = ConcurrentHashMap<Int, Throwable>()
    // Rendezvous before every hook so all THREADS contend for the same (GlobalKey, HookKey) at the
    // same moment. Must be awaited OUTSIDE the seeding call: the initializer runs while holding
    // slot.lock, so a barrier reached from inside it would deadlock.
    val perHook = CyclicBarrier(THREADS)

    val threads =
        (0 until THREADS).map { t ->
          Thread {
            try {
              start.await()
              keys.forEachIndexed { h, hookKey ->
                perHook.await()
                results[t][h] =
                    initialState.peekOrSeedInitialKStateByHookKey(
                        key,
                        hookKey,
                        deps,
                        HookInitializer {
                          initializerRuns.getValue(hookKey).incrementAndGet()
                          "value-$h"
                        },
                        "ConcurrentTestComponent",
                    )
              }
            } catch (e: Throwable) {
              failures[t] = e
            } finally {
              done.countDown()
            }
          }
              .also { it.name = "initial-state-seed-$t" }
        }

    threads.forEach { it.start() }
    start.countDown()
    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue
    threads.forEach { it.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) }

    assertThat(failures).isEmpty()

    // Exactly one initializer invocation per hook, despite THREADS concurrent callers.
    keys.forEach { hookKey ->
      assertThat(initializerRuns.getValue(hookKey).get())
          .describedAs("round %d: initializer runs for %s", round, hookKey)
          .isEqualTo(1)
    }

    // Every thread observed the identical CachedValue instance for each hook.
    for (h in 0 until HOOKS) {
      val first = results[0][h]
      assertThat(first).describedAs("round %d: thread 0 result for hook %d", round, h).isNotNull
      assertThat(first?.value).isEqualTo("value-$h")
      for (t in 1 until THREADS) {
        assertThat(results[t][h])
            .describedAs(
                "round %d: thread %d must observe the same CachedValue as thread 0 for hook %d",
                round,
                t,
                h,
            )
            .isSameAs(first)
      }
    }
  }

  @Test
  fun `concurrent seeding across distinct global keys does not cross-contaminate`() {
    val initialState = InitialState()
    val hookKey = HookKey.create(callSiteId = 0, index = 0, parent = null, intern = true)
    val globalKeys = (0 until THREADS).map { StringGlobalKey("key-$it") }

    val start = CountDownLatch(1)
    val done = CountDownLatch(THREADS)
    val observed = arrayOfNulls<CachedValue>(THREADS)
    val failures = ConcurrentHashMap<Int, Throwable>()

    val threads =
        (0 until THREADS).map { t ->
          Thread {
            try {
              start.await()
              observed[t] =
                  initialState.peekOrSeedInitialKStateByHookKey(
                      globalKeys[t],
                      hookKey,
                      deps,
                      HookInitializer { "value-$t" },
                      "ConcurrentTestComponent",
                  )
            } catch (e: Throwable) {
              failures[t] = e
            } finally {
              done.countDown()
            }
          }
        }

    threads.forEach { it.start() }
    start.countDown()
    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue
    threads.forEach { it.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) }

    assertThat(failures).isEmpty()

    // The same HookKey under different GlobalKeys must resolve independently.
    for (t in 0 until THREADS) {
      assertThat(observed[t]?.value)
          .describedAs("global key %s must keep its own seeded value", globalKeys[t])
          .isEqualTo("value-$t")
    }
  }

  /**
   * The hazard [InitialState]'s locking discipline exists for: a drain (`unregisterStateHandler` ->
   * `slots = null`) racing threads that are mid-seed.
   *
   * Before the hook side was flattened, the map values were immutable and `states` was a
   * `synchronizedMap`, so a straggler could not corrupt anything. Now `Slot.hooks` is a plain
   * mutable `LinkedHashMap`, and the only thing standing between a drain and a torn map is
   * fetch-once (INV-2) plus co-drain (INV-3): a thread that vended its slot before the drain keeps
   * mutating that orphaned slot, and never sees the replacement one.
   *
   * The assertion is deliberately weak on outcomes -- during a drain, seeds legitimately get
   * discarded and initializers legitimately re-run -- and strong on safety: nothing may throw, and
   * in particular no `ConcurrentModificationException` may escape.
   */
  @Test
  fun `draining concurrently with seeding is safe`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val stateHandler = treeState.resolveState
    val keys = hookKeys()

    val failures = ConcurrentHashMap<String, Throwable>()
    val start = CountDownLatch(1)
    val done = CountDownLatch(THREADS + 1)
    val stop = AtomicBoolean(false)

    val seeders =
        (0 until THREADS).map { t ->
          Thread {
            try {
              start.await()
              repeat(ROUNDS) { round ->
                keys.forEachIndexed { h, hookKey ->
                  initialState.peekOrSeedInitialKStateByHookKey(
                      StringGlobalKey("drain-key-$round"),
                      hookKey,
                      deps,
                      HookInitializer { "value-$h" },
                      "DrainRaceComponent",
                  )
                }
              }
            } catch (e: Throwable) {
              failures["seeder-$t"] = e
            } finally {
              done.countDown()
            }
          }
        }

    val drainer = Thread {
      try {
        start.await()
        while (!stop.get()) {
          // Each unregister empties pendingStateHandlers and therefore drains.
          initialState.registerStateHandler(stateHandler)
          initialState.unregisterStateHandler(stateHandler)
        }
      } catch (e: Throwable) {
        failures["drainer"] = e
      } finally {
        done.countDown()
      }
    }

    (seeders + drainer).forEach { it.start() }
    start.countDown()
    seeders.forEach { it.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) }
    stop.set(true)
    drainer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue

    assertThat(failures)
        .describedAs("a drain concurrent with seeding must never throw; got %s", failures)
        .isEmpty()
  }

  /**
   * Minimal Spec component whose only job is to count how many times its initial state is created.
   * `StateUpdateTestComponent` keeps that count private.
   */
  private class CountingSpecComponent : SpecGeneratedComponent("CountingSpec") {
    val initialStateCreations = AtomicInteger(0)

    override fun isEquivalentProps(other: Component?): Boolean = this === other

    override fun hasState(): Boolean = true

    override fun createInitialState(c: ComponentContext, stateContainer: StateContainer) {
      initialStateCreations.incrementAndGet()
    }

    override fun createStateContainer(): StateContainer = CountingStateContainer()

    class CountingStateContainer : StateContainer() {
      override fun applyStateUpdate(stateUpdate: StateUpdate) = Unit
    }
  }

  /**
   * The Spec path, [InitialState.createOrGetState], has the same once-per-key contract as the hook
   * path and reaches it a different way -- `states.getOrPut(key)` under the per-key lock rather
   * than a [CachedValue] lookup. Contended, with no drain, it must still create exactly one initial
   * state per key.
   */
  @Test
  fun `concurrent createOrGetState runs the Spec initializer exactly once per key`() {
    repeat(ROUNDS) { round ->
      val initialState = InitialState()
      val context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
      val component = CountingSpecComponent()
      val key = StringGlobalKey("spec-key-$round")

      val start = CountDownLatch(1)
      val done = CountDownLatch(THREADS)
      val barrier = CyclicBarrier(THREADS)
      val failures = ConcurrentHashMap<Int, Throwable>()
      val observed = arrayOfNulls<ComponentState<out StateContainer>>(THREADS)

      val threads =
          (0 until THREADS).map { t ->
            Thread {
              try {
                start.await()
                barrier.await()
                observed[t] = initialState.createOrGetState(component, context, key)
              } catch (e: Throwable) {
                failures[t] = e
              } finally {
                done.countDown()
              }
            }
          }

      threads.forEach { it.start() }
      start.countDown()
      assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue
      threads.forEach { it.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) }

      assertThat(failures).isEmpty()
      assertThat(component.initialStateCreations.get())
          .describedAs("round %d: Spec initial state must be created exactly once", round)
          .isEqualTo(1)
      for (t in 0 until THREADS) {
        assertThat(observed[t])
            .describedAs("round %d: thread %d must observe the same ComponentState", round, t)
            .isSameAs(observed[0])
      }
    }
  }

  /**
   * The same Spec path with a drain racing it.
   *
   * This is the one place a post-drain window genuinely bites: `states.getOrPut(key) { ... }` is
   * get-then-put over a `synchronizedMap`, so two threads holding pre- and post-drain [Slot]s for
   * the same key are not mutually excluded and can both run `createInitialState`. Duplicate
   * creation is therefore *expected* here and is not asserted against -- production cannot reach
   * it, because a registered `StateHandler` keeps `pendingStateHandlers` non-empty and so blocks
   * the drain. What must hold is that the race stays *safe*: no exception, and every caller gets a
   * fully-constructed state rather than a torn one.
   */
  @Test
  fun `draining concurrently with Spec initial-state creation is safe`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val stateHandler = treeState.resolveState
    val context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())

    val failures = ConcurrentHashMap<String, Throwable>()
    val start = CountDownLatch(1)
    val done = CountDownLatch(THREADS + 1)
    val stop = AtomicBoolean(false)
    val torn = AtomicInteger(0)

    val creators =
        (0 until THREADS).map { t ->
          Thread {
            try {
              start.await()
              repeat(ROUNDS) { round ->
                val state =
                    initialState.createOrGetState(
                        CountingSpecComponent(),
                        context,
                        StringGlobalKey("spec-drain-key-$round"),
                    )
                if (state.value !is CountingSpecComponent.CountingStateContainer) {
                  torn.incrementAndGet()
                }
              }
            } catch (e: Throwable) {
              failures["creator-$t"] = e
            } finally {
              done.countDown()
            }
          }
        }

    val drainer = Thread {
      try {
        start.await()
        while (!stop.get()) {
          initialState.registerStateHandler(stateHandler)
          initialState.unregisterStateHandler(stateHandler)
        }
      } catch (e: Throwable) {
        failures["drainer"] = e
      } finally {
        done.countDown()
      }
    }

    (creators + drainer).forEach { it.start() }
    start.countDown()
    creators.forEach { it.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) }
    stop.set(true)
    drainer.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
    assertThat(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue

    assertThat(failures)
        .describedAs(
            "a drain concurrent with Spec initial-state creation must never throw; got %s",
            failures,
        )
        .isEmpty()
    assertThat(torn.get()).describedAs("every caller must observe a fully-constructed state").isZero
  }
}
