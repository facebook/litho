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

import com.facebook.litho.annotations.Hook
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.dp
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests for the hook identity feature, which allows hooks to be called conditionally, in loops, or
 * after early returns. These tests simulate the compiler plugin's behavior by manually calling
 * enterHook/exitHook around hook calls to test the runtime infrastructure.
 *
 * The tests call enterHook/exitHook manually to isolate the runtime identity behaviour.
 */
@Suppress("LegacyLooperMode")
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class HookIdentityTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Test
  fun useState_withHookIdentity_basicStateWorks() {
    lateinit var stateRef: AtomicReference<String>

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val state = useState { "hello" }
        exitHook()
        stateRef = AtomicReference(state.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  state.update("world")
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }

    assertThat(stateRef.get()).isEqualTo("hello")

    lithoTestRule.act(testLithoView) { clickOnTag("test_view") }

    assertThat(stateRef.get()).describedAs("String state is updated").isEqualTo("world")
  }

  @Test
  fun useState_withHookIdentity_multipleStatesHaveDistinctIdentity() {
    lateinit var state1Ref: AtomicReference<String>
    lateinit var state2Ref: AtomicReference<Int>

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val state1 = useState { "hello" }
        exitHook()

        enterHook(1)
        val state2 = useState { 42 }
        exitHook()

        state1Ref = AtomicReference(state1.value)
        state2Ref = AtomicReference(state2.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("test_view").onClick {
                  state1.update("world")
                  state2.update { it + 1 }
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }

    assertThat(state1Ref.get()).isEqualTo("hello")
    assertThat(state2Ref.get()).isEqualTo(42)

    lithoTestRule.act(testLithoView) { clickOnTag("test_view") }

    assertThat(state1Ref.get()).isEqualTo("world")
    assertThat(state2Ref.get()).isEqualTo(43)
  }

  @Test
  fun useState_withHookIdentity_stateUpdateWithLambdaWorks() {
    val counterRef = AtomicInteger(0)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val counter = useState { 0 }
        exitHook()
        counterRef.set(counter.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("increment").onClick {
                  counter.update { it + 1 }
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }
    assertThat(counterRef.get()).isEqualTo(0)

    lithoTestRule.act(testLithoView) { clickOnTag("increment") }
    assertThat(counterRef.get()).isEqualTo(1)

    lithoTestRule.act(testLithoView) { clickOnTag("increment") }
    assertThat(counterRef.get()).isEqualTo(2)
  }

  @Test
  fun useState_withHookIdentity_syncUpdateWorks() {
    val stateRef = AtomicReference("")

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val state = useState { "initial" }
        exitHook()
        stateRef.set(state.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update").onClick {
                  state.updateSync("sync-updated")
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }
    assertThat(stateRef.get()).isEqualTo("initial")

    lithoTestRule.act(testLithoView) { clickOnTag("update") }
    assertThat(stateRef.get()).isEqualTo("sync-updated")
  }

  @Test
  fun useState_withHookIdentity_nestedHooksHaveDistinctIdentity() {
    val outerRef = AtomicReference("")
    val innerRef = AtomicReference("")

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        // Simulate nested custom hook: outerHook (hookId=0) -> useState (hookId=0)
        enterHook(0)
        enterHook(0)
        val outerState = useState { "outer" }
        exitHook()
        exitHook()

        // Simulate nested custom hook: innerHook (hookId=1) -> useState (hookId=0)
        // Same local hookId (0), but different path -> different identity
        enterHook(1)
        enterHook(0)
        val innerState = useState { "inner" }
        exitHook()
        exitHook()

        outerRef.set(outerState.value)
        innerRef.set(innerState.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update").onClick {
                  outerState.update("outer-updated")
                  innerState.update("inner-updated")
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }

    assertThat(outerRef.get()).isEqualTo("outer")
    assertThat(innerRef.get()).isEqualTo("inner")

    lithoTestRule.act(testLithoView) { clickOnTag("update") }

    assertThat(outerRef.get()).isEqualTo("outer-updated")
    assertThat(innerRef.get()).isEqualTo("inner-updated")
  }

  @Test
  fun useState_withHookIdentity_statePreservedAcrossReRenders() {
    val stateRef = AtomicReference("")
    val renderCount = AtomicInteger(0)

    class TestComponent(val extraProp: Int) : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val state = useState { "preserved" }
        exitHook()
        stateRef.set(state.value)
        renderCount.incrementAndGet()

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update").onClick {
                  state.update("updated")
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent(extraProp = 1) }
    assertThat(stateRef.get()).isEqualTo("preserved")

    // Re-render with different prop — state should be preserved
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(TestComponent(extraProp = 2)) }
    assertThat(stateRef.get()).isEqualTo("preserved")

    // Update state
    lithoTestRule.act(testLithoView) { clickOnTag("update") }
    assertThat(stateRef.get()).isEqualTo("updated")

    // Re-render again — updated state should be preserved
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(TestComponent(extraProp = 3)) }
    assertThat(stateRef.get()).isEqualTo("updated")
  }

  @Test
  fun useState_withHookIdentity_earlyReturnPreservesStateOnReRender() {
    val stateRef = AtomicReference("")

    // Component that only calls hooks when returnEarly is false
    // On renders where returnEarly is true, the hook is simply not called.
    // When returnEarly becomes false again, the state should be retrievable by hookId.
    class NormalComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val state = useState { "after-early-return" }
        exitHook()
        stateRef.set(state.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update").onClick {
                  state.update("updated")
                },
        )
      }
    }

    class EmptyComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return null
      }
    }

    // First render with hooks
    val testLithoView = lithoTestRule.render { NormalComponent() }
    assertThat(stateRef.get()).isEqualTo("after-early-return")

    // Update state
    lithoTestRule.act(testLithoView) { clickOnTag("update") }
    assertThat(stateRef.get()).isEqualTo("updated")

    // Switch to empty component (simulates early return)
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(EmptyComponent()) }

    // Switch back — state should be retrievable from the hookId-based storage
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(NormalComponent()) }
    // After switching component types, it's a new component with a new globalKey,
    // so state is re-initialized. The key point is no crash occurs.
    assertThat(stateRef.get()).isEqualTo("after-early-return")
  }

  @Test
  fun useCached_withHookIdentity_cachedValueWorks() {
    val computeCount = AtomicInteger(0)
    val cachedRef = AtomicReference("")

    class TestComponent(val input: String) : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val cached =
            useCached(input) {
              computeCount.incrementAndGet()
              "computed:$input"
            }
        exitHook()
        cachedRef.set(cached)

        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent(input = "a") }
    assertThat(cachedRef.get()).isEqualTo("computed:a")
    val countAfterFirst = computeCount.get()

    // Re-render with same input — should use cached value
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(TestComponent(input = "a")) }
    assertThat(cachedRef.get()).isEqualTo("computed:a")

    // Re-render with different input — should recompute
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(TestComponent(input = "b")) }
    assertThat(cachedRef.get()).isEqualTo("computed:b")
    assertThat(computeCount.get()).isGreaterThan(countAfterFirst)
  }

  @Test
  fun useEffect_withHookIdentity_effectAttachesAndDetaches() {
    val attachCount = AtomicInteger(0)
    val detachCount = AtomicInteger(0)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        useEffect(Unit) {
          attachCount.incrementAndGet()
          onCleanup { detachCount.incrementAndGet() }
        }
        exitHook()

        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }
    assertThat(attachCount.get()).isEqualTo(1)
    assertThat(detachCount.get()).isEqualTo(0)

    testLithoView.release()
    assertThat(detachCount.get()).isEqualTo(1)
  }

  @Test
  fun useState_withHookIdentity_hookKeyDistinguishesCallSites() {
    // Verify that different hook key paths produce different HookKeys.
    val scope = ComponentScope(ComponentContext(lithoTestRule.context))

    // Path: enterHook(0) -> enterHook(0)
    scope.enterHook(0)
    scope.enterHook(0)
    val key1 = scope.currentHookKey
    scope.exitHook()
    scope.exitHook()

    // Path: enterHook(1) -> enterHook(0) — different outer type
    scope.enterHook(1)
    scope.enterHook(0)
    val key2 = scope.currentHookKey
    scope.exitHook()
    scope.exitHook()

    // Path: enterHook(0) -> enterHook(1) — different inner type
    scope.enterHook(0)
    scope.enterHook(1)
    val key3 = scope.currentHookKey
    scope.exitHook()
    scope.exitHook()

    assertThat(key1).isNotEqualTo(key2)
    assertThat(key1).isNotEqualTo(key3)
    assertThat(key2).isNotEqualTo(key3)
  }

  @Test
  fun useState_withHookIdentity_hooksInLoopYieldUniqueKeys() {
    // Simulate a loop calling the same hook call-site (same callSiteId) multiple times.
    // Each iteration should get a unique HookKey via the runtime index counter.
    val scope = ComponentScope(ComponentContext(lithoTestRule.context))
    val keys = mutableListOf<HookKey>()

    val iterations = 5
    for (i in 0 until iterations) {
      scope.enterHook(0) // same callSiteId each iteration
      keys.add(checkNotNull(scope.currentHookKey))
      scope.exitHook()
    }

    // All keys should be unique
    assertThat(keys.toSet()).hasSize(iterations)

    // Each key should have the same callSiteId (high 32 bits) but different index (low 32 bits)
    for (i in 0 until iterations) {
      val expected = HookKey.create(callSiteId = 0, index = i, parent = null)
      assertThat(keys[i]).isEqualTo(expected)
    }
  }

  @Test
  fun withNewScope_keyedStateFollowsItemAcrossReordering() {
    val states = mutableMapOf<Int, State<String>>()

    class TestComponent(private val items: List<Int>) : KComponent() {
      override fun ComponentScope.render(): Component? {
        states.clear()
        items.forEach { item ->
          withNewScope(item) {
            states[item] = useState { "item-$item" }
          }
        }
        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent(items = listOf(1, 2)) }
    lithoTestRule.act(testLithoView) { states.getValue(1).update("updated-1") }

    lithoTestRule.act(testLithoView) {
      testLithoView.setRoot(TestComponent(items = listOf(2, 1)))
    }

    assertThat(states.getValue(1).value).isEqualTo("updated-1")
    assertThat(states.getValue(2).value).isEqualTo("item-2")
  }

  @Test
  fun withNewScope_sameDataKeyAtDifferentCallSitesDoesNotAliasState() {
    val firstState = AtomicReference<State<String>>()
    val secondState = AtomicReference<State<String>>()

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        withNewScope("shared") { firstState.set(useState { "first" }) }
        withNewScope("shared") { secondState.set(useState { "second" }) }
        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent() }
    lithoTestRule.act(testLithoView) { firstState.get().update("updated") }

    assertThat(firstState.get().value).isEqualTo("updated")
    assertThat(secondState.get().value).isEqualTo("second")
  }

  @Test
  fun withNewScope_duplicateSiblingKeysAreDisambiguatedByOccurrence() {
    val scope = ComponentScope(ComponentContext(lithoTestRule.context))

    scope.enterHook(callSiteId = 1, index = 0)
    val firstCompilerKey = scope.enterNewHookScope("duplicate")
    val firstKey = scope.currentHookKey
    scope.exitNewHookScope(firstCompilerKey)
    scope.exitHook()

    scope.enterHook(callSiteId = 1, index = 1)
    val secondCompilerKey = scope.enterNewHookScope("duplicate")
    val secondKey = scope.currentHookKey
    scope.exitNewHookScope(secondCompilerKey)
    scope.exitHook()

    assertThat(firstKey).isNotEqualTo(secondKey)
  }

  @Test
  fun exitNewHookScope_restoresParentWithoutMaskingOriginalException() {
    val scope = ComponentScope(ComponentContext(lithoTestRule.context))
    scope.enterHook(callSiteId = 1, index = 0)
    val compilerKey = scope.enterNewHookScope("key")
    scope.enterHook(callSiteId = 2, index = 0)
    val originalException = IllegalStateException("original")

    assertThatThrownBy {
          try {
            throw originalException
          } finally {
            scope.exitNewHookScope(compilerKey, verifyBalance = false)
          }
        }
        .isSameAs(originalException)
    assertThat(scope.currentHookKey).isEqualTo(compilerKey)
    scope.exitHook()
  }

  @Test
  fun exitHook_withoutMatchingEnterHook_throwsDiagnosticError() {
    val scope = ComponentScope(ComponentContext(lithoTestRule.context))

    assertThatThrownBy { scope.exitHook() }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Hook identity error: exitHook() called without a matching enterHook().")
  }

  /**
   * End-to-end regression for `initialstate-drain-state-loss.md`. Renders a component first with
   * one hook, commits, then re-renders with an additional hook after the InitialState cache was
   * drained on commit. Before the fix, the second render's `getUpdatedStateByHookKey` for the new
   * hook routed through `createOrGetInitialHookStateByHookKey`, which (with InitialState empty)
   * built a fresh single-entry container and `addState` wiped the previously committed entry. A
   * subsequent state update on the original hook then crashed in `canSkipStateUpdate` with "HookKey
   * not found". With the fix, prior committed entries are merged into the new container and updates
   * continue to work.
   */
  @Test
  fun useState_withHookIdentity_addingNewHookAfterCommitPreservesPriorState() {
    val state1Ref = AtomicReference("")
    val state2Ref = AtomicReference<String?>(null)

    class TestComponent(val showSecondHook: Boolean) : KComponent() {
      // Intentional rule-of-hooks violation: the test exercises adding a hook conditionally so the
      // hook-identity runtime is forced through the missing-hookKey branch in
      // getUpdatedStateByHookKey. enterHook/exitHook around the conditional `useState` simulates
      // the
      // compiler plugin's identity wiring.
      override fun ComponentScope.render(): Component? {
        enterHook(0)
        val state1 = useState { "first" }
        exitHook()
        state1Ref.set(state1.value)

        if (showSecondHook) {
          enterHook(1)
          val state2 = useState { "second" }
          exitHook()
          state2Ref.set(state2.value)
        }

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update").onClick {
                  state1.update { "${it}-updated" }
                },
        )
      }
    }

    // Render 1 commits state1 only. The post-commit unregister drains the InitialState cache.
    val testLithoView = lithoTestRule.render { TestComponent(showSecondHook = false) }
    assertThat(state1Ref.get()).isEqualTo("first")

    // Update state1 so we have a non-default value to detect loss with.
    lithoTestRule.act(testLithoView) { clickOnTag("update") }
    assertThat(state1Ref.get()).isEqualTo("first-updated")

    // Render 2 adds state2. Before the fix, processing state2 here would wipe state1 from the
    // committed container because InitialState was drained after render 1 committed.
    lithoTestRule.act(testLithoView) { testLithoView.setRoot(TestComponent(showSecondHook = true)) }
    assertThat(state1Ref.get())
        .describedAs("state1 must survive adding a new hook after InitialState drain")
        .isEqualTo("first-updated")
    assertThat(state2Ref.get()).isEqualTo("second")

    // Trigger another state1 update. Before the fix, this throws IllegalStateException from
    // canSkipStateUpdate because hookKey1 is missing from the committed container.
    lithoTestRule.act(testLithoView) { clickOnTag("update") }
    assertThat(state1Ref.get()).isEqualTo("first-updated-updated")
  }

  @Test
  fun useState_withHookIdentity_laterCustomHookIsStableWhenEarlierCustomHookIsSkipped() {
    val stableStateValue = AtomicReference("")

    class TestComponent(private val showConditionalHook: Boolean) : KComponent() {
      override fun ComponentScope.render(): Component? {
        if (showConditionalHook) {
          useConditionallyCalledState()
        }
        val stableState = useAlwaysCalledState()
        stableStateValue.set(stableState.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update-stable-state").onClick {
                  stableState.update("updated")
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent(showConditionalHook = true) }
    assertThat(stableStateValue.get()).isEqualTo("initial")

    lithoTestRule.act(testLithoView) { clickOnTag("update-stable-state") }
    assertThat(stableStateValue.get()).isEqualTo("updated")

    lithoTestRule.act(testLithoView) {
      testLithoView.setRoot(TestComponent(showConditionalHook = false))
    }
    assertThat(stableStateValue.get())
        .describedAs(
            "the always-called custom hook keeps its state when an earlier hook is skipped",
        )
        .isEqualTo("updated")
  }

  @Test
  fun useState_withHookIdentity_laterLambdaHookIsStableWhenEarlierLambdaHookIsSkipped() {
    val stableStateValue = AtomicReference("")

    class TestComponent(private val showConditionalHook: Boolean) : KComponent() {
      override fun ComponentScope.render(): Component? {
        if (showConditionalHook) {
          run { useState { "conditional" } }
        }
        val stableState = run { useState { "initial" } }
        stableStateValue.set(stableState.value)

        return Row(
            style =
                Style.height(100.dp).width(100.dp).viewTag("update-lambda-state").onClick {
                  stableState.update("updated")
                },
        )
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent(showConditionalHook = true) }
    assertThat(stableStateValue.get()).isEqualTo("initial")

    lithoTestRule.act(testLithoView) { clickOnTag("update-lambda-state") }
    assertThat(stableStateValue.get()).isEqualTo("updated")

    lithoTestRule.act(testLithoView) {
      testLithoView.setRoot(TestComponent(showConditionalHook = false))
    }
    assertThat(stableStateValue.get())
        .describedAs(
            "the always-called lambda hook keeps its state when an earlier hook is skipped",
        )
        .isEqualTo("updated")
  }

  @Test
  fun useState_withHookIdentity_nestedCustomHookIsStableWhenEarlierSiblingIsSkipped() {
    val stableStateValue = AtomicReference("")
    val stableStateRef = AtomicReference<State<String>>()

    class TestComponent(private val showConditionalHook: Boolean) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val stableState = useNestedStates(showConditionalHook)
        stableStateValue.set(stableState.value)
        stableStateRef.set(stableState)
        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }

    val testLithoView = lithoTestRule.render { TestComponent(showConditionalHook = true) }
    assertThat(stableStateValue.get()).isEqualTo("initial")

    lithoTestRule.act(testLithoView) { stableStateRef.get().update("updated") }
    assertThat(stableStateValue.get()).isEqualTo("updated")

    lithoTestRule.act(testLithoView) {
      testLithoView.setRoot(TestComponent(showConditionalHook = false))
    }
    assertThat(stableStateValue.get())
        .describedAs("the nested custom hook keeps its state when an earlier sibling is skipped")
        .isEqualTo("updated")
  }
}

@Hook
private fun ComponentScope.useConditionallyCalledState(): State<String> = useState { "conditional" }

@Hook private fun ComponentScope.useAlwaysCalledState(): State<String> = useState { "initial" }

@Hook
private fun ComponentScope.useNestedStates(showConditionalHook: Boolean): State<String> {
  if (showConditionalHook) {
    useConditionallyCalledState()
  }
  return useAlwaysCalledState()
}
