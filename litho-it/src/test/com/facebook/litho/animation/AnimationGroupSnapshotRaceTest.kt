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

package com.facebook.litho.animation

import com.facebook.litho.HookKey
import com.facebook.litho.StateUpdater
import com.facebook.litho.StringGlobalKey
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

/**
 * White-box tests for [AnimationGroupState]'s per-slot animated-value identity.
 *
 * Litho can resolve the same render generation on more than one thread (a background layout and a
 * main-thread sync layout). The previous design minted the long-lived [AnimatedValue] for a slot
 * *during render*, guarded by a hand-rolled lock+cache; two concurrent resolves could still diverge
 * and crash a positional identity check. This was observed as a real on-device crash from
 * `AnimatedContent`, which spins up a fresh per-slot group on every content swap.
 *
 * The current design removes that race by construction: `render()` only collects an immutable
 * [AnimationGroupRecord] declaration ([AnimationDescriptor]s — no shared mutation, no value
 * creation), and the long-lived [AnimatedValue] runtime is created in [AnimationGroupState.commit],
 * which runs in a commit-phase `useEffect` on the main thread. Slot creation is therefore
 * single-threaded and keyed by a stable [AnimationGroupState.childStateId], so sequential commits
 * of concurrent resolves converge on the same instances with no lock. These tests exercise that
 * directly (Robolectric serialises loopers, so the original race cannot be reproduced through the
 * component flow).
 */
@RunWith(LithoTestRunner::class)
class AnimationGroupSnapshotRaceTest {

  private fun newState(): AnimationGroupState<Boolean> = AnimationGroupState(
      initialState = false,
      treeId = 1,
      globalKey = StringGlobalKey("test"),
      baseHookKey = HookKey.create(callSiteId = 1, index = 0, parent = null),
      isLayoutContext = false,
      stateUpdater = mock(StateUpdater::class.java),
      label = "AnimatedContent",
  )

  private fun floatDesc(
      slot: Int,
      label: String = "fade",
      target: Float = 0f,
  ): AnimationDescriptor<Boolean, Float> =
      AnimationDescriptor(slot, FloatTypeConverter, { spring() }, { target }, label)

  /** Builds the immutable per-render declaration the runtime commits, targeting `false`. */
  private fun declaration(
      vararg descs: AnimationDescriptor<Boolean, *>,
  ): AnimationGroupRecord<Boolean> =
      AnimationGroupRecord(targetValue = false, descriptors = descs.toList())

  @Test
  fun slotValueIsNullBeforeFirstCommit() {
    val state = newState()
    // Render produced no committed declaration yet, so there is no long-lived value to read.
    assertThat(state.animatedValueAt(0)).isNull()
  }

  @Test
  fun commitCreatesStableSlotValuesPerSlot() {
    val state = newState()

    state.commit(declaration(floatDesc(0), floatDesc(1, "slide")))
    val v0 = state.animatedValueAt(0)
    val v1 = state.animatedValueAt(1)

    assertThat(v0).isNotNull
    assertThat(v1).isNotNull
    assertThat(v0).isNotSameAs(v1)

    // A later commit of the same shape reuses the same long-lived per-slot instances.
    state.commit(declaration(floatDesc(0), floatDesc(1, "slide")))
    assertThat(state.animatedValueAt(0)).isSameAs(v0)
    assertThat(state.animatedValueAt(1)).isSameAs(v1)
  }

  @Test
  fun concurrentPreCommitResolvesCommitWithoutCrashOrReplacement() {
    val state = newState()

    // Two concurrent resolves of the same generation each build their OWN immutable declaration.
    // No AnimatedValue is created during render and nothing on the shared state is mutated, so
    // there
    // is no creation race to lose.
    val resolveA = declaration(floatDesc(0), floatDesc(1, "slide"))
    val resolveB = declaration(floatDesc(0), floatDesc(1, "slide"))

    // Both commit sequentially on the main thread (the loser of the layout race commits second).
    state.commit(resolveA)
    val a0 = state.animatedValueAt(0)
    val a1 = state.animatedValueAt(1)

    state.commit(resolveB)

    // The second commit neither throws nor replaces the long-lived per-slot values.
    assertThat(state.animatedValueAt(0)).isSameAs(a0)
    assertThat(state.animatedValueAt(1)).isSameAs(a1)
  }

  @Test
  fun commitRejectsGenuineSlotCountChange() {
    val state = newState()
    state.commit(declaration(floatDesc(0)))

    // A later render that changes the slot count is a real programming error and must still be
    // rejected — committing does not silently grow the slot set after the first declaration.
    assertThatThrownBy { state.commit(declaration(floatDesc(0), floatDesc(1, "slide"))) }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("changed animation count")
  }

  @Test
  fun slotValue_readsPropFallbackBeforeCommit_thenCommittedValue() {
    val state = newState()
    // The proxy handed back during render derives its pre-commit fallback from the descriptor and
    // reads it until the slot is committed — render never touches the runtime.
    val proxy = SlotValue(state, slot = 0, floatDesc(0, target = 7f))
    assertThat(proxy.value).isEqualTo(7f)

    // After commit, the proxy reads the committed AnimatedValue (initialised from the description's
    // target at the settled state), not the fallback.
    state.commit(declaration(floatDesc(0, target = 5f)))
    assertThat(proxy.value).isEqualTo(5f)
  }

  @Test
  fun childStateId_isDistinctPerSlotAndStable() {
    val state = newState()
    val id0 = state.childStateId(0)
    val id1 = state.childStateId(1)
    assertThat(id0).isNotEqualTo(id1)
    // Stable across calls, so a binder's recorded read keeps matching the re-render signal.
    assertThat(state.childStateId(0)).isEqualTo(id0)
  }
}
