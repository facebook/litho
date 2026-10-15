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
import com.facebook.litho.state.StateId
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class TreeStateTests {

  private lateinit var context: ComponentContext
  private lateinit var testComponent: StateUpdateTestComponent
  private lateinit var testComponentKey: GlobalKey

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    testComponent = StateUpdateTestComponent()
    testComponentKey = StringGlobalKey(testComponent.key)
  }

  @Test
  fun testQueueStateUpdate() {
    val treeState = TreeState()
    val stateContainer = ComponentState(value = StateUpdateTestComponent.TestStateContainer())

    // Add state container
    treeState.addState(testComponentKey, stateContainer, false)

    // Queue state update
    treeState.queueStateUpdate(
        makeStateId(testComponentKey),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        false,
        false,
    )

    // Apply state updates
    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    val previousStateContainer =
        treeState.getState(testComponentKey, false)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(1)
  }

  private fun testRegisterUnregisterInitialStateContainer(isNestedTree: Boolean) {
    val componentContext: ComponentContext = mock()
    whenever(componentContext.isContextForLayout).thenReturn(isNestedTree)
    whenever(componentContext.scopedComponentInfo)
        .thenReturn(ScopedComponentInfo(testComponent, context, null))
    val treeState = TreeState()
    val localTreeState = TreeState(treeState)

    // Register local tree state to ISC via InitialStateScope
    val resolveScope = InitialStateScope.forResolve(localTreeState)
    val layoutScope = InitialStateScope.forLayout(localTreeState)

    // Add state containers in ISC
    localTreeState.createOrGetState(componentContext, testComponent, testComponentKey)
    val resolveState = treeState.resolveState
    val layoutStateHandler = treeState.layoutState
    if (isNestedTree) {
      assertThat(resolveState.initialState.isCacheEmpty()).isTrue
      assertThat(layoutStateHandler.initialState.isCacheEmpty()).isFalse
    } else {
      assertThat(resolveState.initialState.isCacheEmpty()).isFalse
      assertThat(layoutStateHandler.initialState.isCacheEmpty()).isTrue
    }

    // Unregister local tree state from ISC via scope close
    resolveScope.close()
    layoutScope.close()

    // State containers in ISC should be cleared
    assertThat(resolveState.initialState).isNotNull
    assertThat(resolveState.initialState.isCacheEmpty()).isTrue
    assertThat(layoutStateHandler.initialState).isNotNull
    assertThat(layoutStateHandler.initialState.isCacheEmpty()).isTrue
  }

  @Test
  fun testRegisterUnregisterInitialStateContainerRenderState() {
    testRegisterUnregisterInitialStateContainer(false)
  }

  @Test
  fun testRegisterUnregisterInitialStateContainerLayoutState() {
    testRegisterUnregisterInitialStateContainer(true)
  }

  @Test
  fun testCommitRenderState() {
    val previousTreeState = TreeState()
    previousTreeState.addState(
        testComponentKey,
        ComponentState(value = StateUpdateTestComponent.TestStateContainer()),
        false,
    )
    val treeState = TreeState()
    treeState.commitResolveState(previousTreeState)
    val previousStateContainer =
        treeState.getState(testComponentKey, false)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testCommitLayoutState() {
    val previousTreeState = TreeState()
    previousTreeState.addState(
        testComponentKey,
        ComponentState(value = StateUpdateTestComponent.TestStateContainer()),
        true,
    )
    val treeState = TreeState()
    treeState.commitLayoutState(previousTreeState)
    val previousStateContainer =
        treeState.getState(testComponentKey, true)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testCopyPreviousState() {
    val previousTreeState = TreeState()
    previousTreeState.addState(
        testComponentKey,
        ComponentState(value = StateUpdateTestComponent.TestStateContainer()),
        false,
    )
    val treeState = TreeState(previousTreeState)
    val previousStateContainer =
        treeState.getState(testComponentKey, false)?.value
            as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count).isEqualTo(0)
  }

  @Test
  fun testKeysForPendingUpdates() {
    val treeState = TreeState()
    val anotherTestComponentKey = StringGlobalKey("anotherTestComponentKey")

    // State Update on main tree component
    treeState.queueStateUpdate(
        makeStateId(testComponentKey),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        false,
        false,
    )

    // State Update on nested tree component
    treeState.queueStateUpdate(
        makeStateId(anotherTestComponentKey),
        StateUpdateTestComponent.createIncrementStateUpdate(),
        false,
        true,
    )
    val renderKeysForPendingUpdates =
        treeState.getStateIdsForPendingResolveStateUpdates().mapTo(hashSetOf()) { it.globalKey }
    assertThat(renderKeysForPendingUpdates).contains(testComponentKey)
    val layoutKeysForPendingUpdates =
        treeState.getStateIdsForPendingLayoutStateUpdates().mapTo(hashSetOf()) { it.globalKey }
    assertThat(layoutKeysForPendingUpdates).contains(anotherTestComponentKey)
  }

  @Test
  fun `queueDuplicateStateUpdates enabled - StateUpdate is enqueued if it is a duplicate`() {
    val treeState = TreeState(fromState = null)

    treeState.createOrGetState(context, testComponent, testComponentKey)

    treeState.assertThatTestStateContainerHasValue(4)

    // increment
    treeState.queueStateUpdate(
        key = makeStateId(testComponentKey),
        stateUpdate = StateUpdateTestComponent.createIncrementStateUpdate(),
        isLazyStateUpdate = false,
        isLayoutState = false,
    )

    assertThat(treeState.hasUncommittedUpdates()).isTrue

    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    treeState.assertThatTestStateContainerHasValue(5)

    // update with the same value (duplicate)
    val duplicateUpdateEnqueued =
        treeState.queueStateUpdate(
            key = makeStateId(testComponentKey),
            stateUpdate = StateUpdateTestComponent.createValueStateUpdate(5),
            isLazyStateUpdate = false,
            isLayoutState = false,
        )

    assertThat(treeState.hasUncommittedUpdates()).isTrue
    treeState.applyStateUpdatesEarly(context, testComponent, null, false)
    treeState.assertThatTestStateContainerHasValue(5)
  }

  private fun TreeState.assertThatTestStateContainerHasValue(value: Int) {
    assertThat(
        (getState(testComponentKey, false)?.value as StateUpdateTestComponent.TestStateContainer)
            .count,
    )
        .isEqualTo(value)
  }

  // ---------------------------------------------------------------------------
  // InitialState race condition tests
  // ---------------------------------------------------------------------------

  /** Helper to seed one hook's initial state and return the unwrapped value. */
  private fun InitialState.createKState(
      key: GlobalKey,
      hookKey: HookKey = HookKey.create(callSiteId = 0, index = 0, parent = null),
      initializer: () -> Any?,
  ): Any? {
    val result = getOrCreateInitialKStateByHookKey(
        key,
        hookKey,
        emptyArray<Any>(),
        HookInitializer { initializer() },
        "TestComponent",
    )
    return result.value.getByHookKey(hookKey)?.value
  }

  /**
   * Simulates the race condition that existed before the fix: two concurrent resolves share the
   * same InitialState. If the cache is cleared between them (via premature unregister), the second
   * resolve re-runs the initializer and can produce a different value.
   *
   * With the fix (register → compute → commit → unregister), both resolves find the cached value.
   */
  @Test
  fun `concurrent resolves with non-deterministic initializer produce same cached value`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState

    // Simulate two concurrent resolves: both create TreeState copies sharing the same InitialState
    val resolveA = TreeState(treeState)
    val resolveB = TreeState(treeState)

    // Both share the same InitialState
    assertThat(resolveA.resolveState.initialState).isSameAs(initialState)
    assertThat(resolveB.resolveState.initialState).isSameAs(initialState)

    // Register both (simulates withResolveScope entry for both concurrent resolves)
    val scopeA = InitialStateScope.forResolve(resolveA)
    val scopeB = InitialStateScope.forResolve(resolveB)

    var initializerCallCount = 0
    val key = StringGlobalKey("testKey")

    // Resolve A runs its initializer first
    val valueA =
        initialState.createKState(key) {
          initializerCallCount++
          42
        }
    assertThat(valueA).isEqualTo(42)
    assertThat(initializerCallCount).isEqualTo(1)

    // Resolve B finds the cached value — initializer does NOT run again
    val valueB =
        initialState.createKState(key) {
          initializerCallCount++
          99
        }
    assertThat(valueB).isEqualTo(42) // same as A, not 99
    assertThat(initializerCallCount).isEqualTo(1) // still 1, not 2

    // Unregister A — cache should NOT be cleared because B is still pending
    scopeA.close()
    assertThat(initialState.isCacheEmpty()).isFalse

    // Unregister B — now cache is cleared (both done)
    scopeB.close()
    assertThat(initialState.isCacheEmpty()).isTrue
  }

  /**
   * Verifies the race condition scenario: if unregister happens before commit (the old behavior), a
   * sequential resolve sees an empty cache and re-runs the initializer.
   *
   * This test demonstrates the bug that the fix prevents.
   */
  @Test
  fun `premature unregister causes re-initialization with different value`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState

    val resolveA = TreeState(treeState)
    var initializerCallCount = 0

    // Resolve A: register, compute, unregister (premature — before commit)
    val scopeA = InitialStateScope.forResolve(resolveA)
    initialState.createKState(StringGlobalKey("key")) {
      initializerCallCount++
      42
    }
    scopeA.close() // premature: cache cleared!

    assertThat(initialState.isCacheEmpty()).isTrue // cache wiped
    assertThat(initializerCallCount).isEqualTo(1)

    // Resolve B: starts after A unregistered but before A committed
    val resolveB = TreeState(treeState)
    val scopeB = InitialStateScope.forResolve(resolveB)

    // B's StateHandler was copied from treeState (which has no committed values yet)
    // B finds empty cache → re-runs initializer → gets different value!
    val valueB =
        initialState.createKState(StringGlobalKey("key")) {
          initializerCallCount++
          99
        }
    assertThat(valueB).isEqualTo(99) // different from A's 42!
    assertThat(initializerCallCount).isEqualTo(2) // initializer ran twice — the bug

    scopeB.close()
  }

  /** Verifies that register/unregister keeps the cache alive during the scope and clears after. */
  @Test
  fun `register protects cache during scope and unregister clears after`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val localTreeState = TreeState(treeState)

    val scope = InitialStateScope.forResolve(localTreeState)

    // Cache is protected — pending handler is registered
    assertThat(initialState.pendingStateHandlers).isNotEmpty

    initialState.createKState(StringGlobalKey("key")) { "hello" }
    assertThat(initialState.isCacheEmpty()).isFalse

    // Unregister clears the cache
    scope.close()

    assertThat(initialState.isCacheEmpty()).isTrue
    assertThat(initialState.pendingStateHandlers).isEmpty()
  }

  /**
   * Verifies that layout register/unregister works the same as resolve register/unregister but for
   * the layout StateHandler.
   */
  @Test
  fun `layout register protects cache during scope and unregister clears after`() {
    val treeState = TreeState()
    val initialState = treeState.layoutState.initialState
    val localTreeState = TreeState(treeState)

    val scope = InitialStateScope.forLayout(localTreeState)

    assertThat(initialState.pendingStateHandlers).isNotEmpty

    initialState.createKState(StringGlobalKey("key")) { 123 }
    assertThat(initialState.isCacheEmpty()).isFalse

    scope.close()

    assertThat(initialState.isCacheEmpty()).isTrue
    assertThat(initialState.pendingStateHandlers).isEmpty()
  }

  /**
   * Verifies that the stale cache scenario is prevented by clearing: a component removed and
   * recreated at the same position should run a fresh initializer, not reuse the old cached value.
   */
  @Test
  fun `cache clearing prevents stale initial state for recreated component`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val key = StringGlobalKey("component_at_position_P")

    // Cycle 1: component P created with valueA
    val resolve1 = TreeState(treeState)
    val scope1 = InitialStateScope.forResolve(resolve1)
    val value1 = initialState.createKState(key) { "valueA" }
    assertThat(value1).isEqualTo("valueA")
    scope1.close()

    // Cache is cleared after cycle 1 (simulates post-commit unregister)
    assertThat(initialState.isCacheEmpty()).isTrue

    // Cycle 2: component P removed — no createKState call for this key

    // Cycle 3: NEW component at position P — should get fresh initial state
    val resolve3 = TreeState(treeState)
    val scope3 = InitialStateScope.forResolve(resolve3)
    val value3 = initialState.createKState(key) { "valueB" }
    // Without cache clearing, this would return "valueA" (stale). With clearing, it's "valueB".
    assertThat(value3).isEqualTo("valueB")
    scope3.close()
  }

  /**
   * Verifies that without cache clearing (no register/unregister), a recreated component at the
   * same position inherits stale initial state from the cache.
   */
  @Test
  fun `without clearing - recreated component inherits stale initial state`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val key = StringGlobalKey("component_at_position_P")

    // Cycle 1: component P created with valueA — no register/unregister
    initialState.createKState(key) { "valueA" }

    // Cache is NOT cleared (no unregister)
    assertThat(initialState.isCacheEmpty()).isFalse

    // Cycle 3: NEW component at position P — cache returns stale valueA
    val value = initialState.createKState(key) { "valueB" }
    assertThat(value).isEqualTo("valueA") // stale! should be valueB
  }

  // ---------------------------------------------------------------------------
  // InitialState-drain regression tests (hook-identity mode)
  //
  // Documented in `docs/initialstate-drain-state-loss.md`. Before the fix,
  // `getUpdatedStateByHookKey` routed a missing-hookKey case through
  // `createOrGetInitialKStateByHookKey`. When InitialState had been drained between
  // resolves, that produced a fresh single-entry container and `addState` wiped any
  // prior entries committed under the same GlobalKey -- causing
  // `canSkipStateUpdate: HookKey not found` crashes on subsequent state updates.
  //
  // The fix replaces that path with `peekOrSeedInitialKStateByHookKey`, which only
  // returns the value/deps; the caller merges them into the existing committed
  // container preserving prior entries.
  // ---------------------------------------------------------------------------

  /**
   * Regression test for `initialstate-drain-state-loss.md`. Simulates the exact sequence that
   * crashed: committed state for hookKey1 exists, InitialState is drained, a re-render encounters a
   * new hookKey2. After the fix, both keys must coexist in the committed container.
   */
  @Test
  fun `peekOrSeed merge after InitialState drain preserves prior committed hook entries`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val key = StringGlobalKey("componentWithMultipleHooks")
    val hookKey1 = HookKey.create(callSiteId = 0, index = 0, parent = null)
    val hookKey2 = HookKey.create(callSiteId = 1, index = 0, parent = null)

    // Step 1: prior render committed hookKey1 = "value1".
    val priorContainer =
        KStateContainer.withNewStateByHookKey(
            kStateContainer = null,
            hookKey = hookKey1,
            value = "value1",
            deps = emptyArray<Any>(),
        )
    treeState.addState(key, ComponentState(value = priorContainer), false)

    // Step 2: InitialState was drained after the prior commit (post-commit unregister).
    assertThat(initialState.isCacheEmpty()).isTrue

    // Step 3: a new resolve encounters hookKey2 for the first time. Replicates the
    // body of `getUpdatedStateByHookKey`'s missing-hookKey branch.
    @Suppress("UNCHECKED_CAST")
    val existing = treeState.getState(key, false) as ComponentState<KStateContainer>
    assertThat(existing.value.hasHookKey(hookKey1)).isTrue
    assertThat(existing.value.hasHookKey(hookKey2)).isFalse

    val seeded =
        treeState.peekOrSeedInitialHookStateByHookKey(
            key,
            hookKey2,
            emptyArray<Any>(),
            HookInitializer { "value2" },
            false,
            "TestComponent",
        )
    val merged =
        KStateContainer.withNewStateByHookKey(existing.value, hookKey2, seeded.value, seeded.deps)
    treeState.addState(key, existing.copy(value = merged), false)

    // Step 4: both entries must be present.
    @Suppress("UNCHECKED_CAST")
    val finalContainer = (treeState.getState(key, false) as ComponentState<KStateContainer>).value
    assertThat(finalContainer.hasHookKey(hookKey1))
        .describedAs("hookKey1 must be preserved across the InitialState-drain merge")
        .isTrue
    assertThat(finalContainer.hasHookKey(hookKey2))
        .describedAs("hookKey2 should be added by the merge")
        .isTrue
    assertThat(finalContainer.getByHookKey(hookKey1)?.value).isEqualTo("value1")
    assertThat(finalContainer.getByHookKey(hookKey2)?.value).isEqualTo("value2")
  }

  /**
   * Demonstrates the original bug: routing the missing-hookKey case through
   * `createOrGetInitialKStateByHookKey` after a drain wipes prior committed entries. Pinned here so
   * any future regression that reintroduces this path is caught.
   */
  @Test
  fun `createOrGetInitial after InitialState drain produces single-entry container - bug repro`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val key = StringGlobalKey("componentWithMultipleHooks")
    val hookKey1 = HookKey.create(callSiteId = 0, index = 0, parent = null)
    val hookKey2 = HookKey.create(callSiteId = 1, index = 0, parent = null)

    val priorContainer =
        KStateContainer.withNewStateByHookKey(
            kStateContainer = null,
            hookKey = hookKey1,
            value = "value1",
            deps = emptyArray<Any>(),
        )
    treeState.addState(key, ComponentState(value = priorContainer), false)
    assertThat(initialState.isCacheEmpty()).isTrue // drained

    // Old path: create-from-initial-state. With a drained InitialState, this builds
    // the new container starting from null (empty), so the result has *only* hookKey2.
    val freshFromInitial =
        treeState.createOrGetInitialHookStateByHookKey(
            key,
            hookKey2,
            emptyArray<Any>(),
            HookInitializer { "value2" },
            false,
            "TestComponent",
        )

    assertThat(freshFromInitial.value.hasHookKey(hookKey1))
        .describedAs("documents why the old code path lost prior entries")
        .isFalse
    assertThat(freshFromInitial.value.hasHookKey(hookKey2)).isTrue
  }

  /**
   * The 3-way deps decision in [InitialState.peekOrSeedInitialKStateByHookKey]: when the cache
   * already holds an entry for [hookKey] with deps that *don't* match the caller's, we must run the
   * initializer with the caller's deps and *not* mutate the cache -- another in-flight resolve owns
   * that entry and overwriting would corrupt its `useStateWithDeps` semantics.
   */
  @Test
  fun `peekOrSeed with mismatched cached deps runs initializer without overwriting cache`() {
    val treeState = TreeState()
    val initialState = treeState.resolveState.initialState
    val key = StringGlobalKey("componentWithDepsMismatch")
    val hookKey = HookKey.create(callSiteId = 0, index = 0, parent = null)

    // Concurrent resolve seeded the cache with deps=["A"].
    treeState.peekOrSeedInitialHookStateByHookKey(
        key,
        hookKey,
        arrayOf("A"),
        HookInitializer { "valueA" },
        false,
        "TestComponent",
    )

    // This caller has deps=["B"] -- different from cache. Must compute fresh and not
    // overwrite the cached entry.
    var initializerCalls = 0
    val result =
        treeState.peekOrSeedInitialHookStateByHookKey(
            key,
            hookKey,
            arrayOf("B"),
            HookInitializer {
              initializerCalls++
              "valueB"
            },
            false,
            "TestComponent",
        )

    assertThat(initializerCalls).isEqualTo(1)
    assertThat(result.value).isEqualTo("valueB")
    assertThat(result.deps).containsExactly("B")

    // Cached entry under hookKey must still reflect the original deps=["A"], valueA --
    // not the mismatched caller's value.
    val cached = initialState.hookSnapshot(key)[hookKey]
    assertThat(cached?.value).isEqualTo("valueA")
    assertThat(cached?.deps).containsExactly("A")
  }

  @Test
  fun `hook seeded by one resolve is reused by a resolve that starts later`() {
    val treeState = TreeState()
    val key = StringGlobalKey("componentWithNewHook")
    val hookKey = HookKey.create(callSiteId = 0, index = 0, parent = null)
    var initializerCalls = 0

    // Resolve A is the only one in flight and reaches this hook for the first time.
    val seeded =
        treeState.peekOrSeedInitialHookStateByHookKey(
            key,
            hookKey,
            emptyArray<Any>(),
            HookInitializer {
              initializerCalls++
              Any()
            },
            false,
            "TestComponent",
        )

    // Resolve B starts afterwards and reaches the same hook. It has to observe A's value rather
    // than run the initializer again: an initializer that allocates would otherwise hand the two
    // resolves different instances for the same hook, and whichever committed last would win.
    val reused =
        treeState.peekOrSeedInitialHookStateByHookKey(
            key,
            hookKey,
            emptyArray<Any>(),
            HookInitializer {
              initializerCalls++
              Any()
            },
            false,
            "TestComponent",
        )

    assertThat(initializerCalls).isEqualTo(1)
    assertThat(reused.value).isSameAs(seeded.value)
  }

  private fun makeStateId(globalKey: GlobalKey): StateId = StateId.forSpec(0, globalKey)
}
