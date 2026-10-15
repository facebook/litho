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

package com.facebook.rendercore.incrementalmount

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PremountLifecycleTest {

  @Test
  fun `absence is untracked in a new session`() {
    val (lifecycle, session) = createLifecycle()

    assertThat(lifecycle.stateOf(ID)).isNull()
    assertThat(lifecycle.beginPremountAcquire(session, ID, alreadyOwned = false))
        .isEqualTo(PremountLifecycle.AcquireDecision.ACQUIRE)
  }

  @Test
  fun `successful worker acquisition becomes premounted`() {
    val (lifecycle, session) = createLifecycle()

    val decision = lifecycle.beginPremountAcquire(session, ID, alreadyOwned = false)
    val current = lifecycle.finishPremountAcquire(session, ID, acquired = true)

    assertThat(decision).isEqualTo(PremountLifecycle.AcquireDecision.ACQUIRE)
    assertThat(current).isTrue()
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.PREMOUNTED)
  }

  @Test
  fun `pre-existing ownership is not classified as premounted`() {
    val (lifecycle, session) = createLifecycle()

    assertThat(lifecycle.beginPremountAcquire(session, ID, alreadyOwned = true))
        .isEqualTo(PremountLifecycle.AcquireDecision.SKIP)
    assertThat(lifecycle.stateOf(ID)).isNull()
  }

  @Test
  fun `scroll marks a premounted output as seen`() {
    val (lifecycle, session) = createLifecycle()
    lifecycle.beginPremountAcquire(session, ID, alreadyOwned = false)
    lifecycle.finishPremountAcquire(session, ID, acquired = true)

    assertThat(lifecycle.markSeen(session, ID)).isTrue()
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.SEEN)
    assertThat(lifecycle.wantsMounted(ID)).isTrue()
  }

  @Test
  fun `premounted output is retained off screen`() {
    val (lifecycle, session) = createLifecycle()
    lifecycle.beginPremountAcquire(session, ID, alreadyOwned = false)
    lifecycle.finishPremountAcquire(session, ID, acquired = true)

    assertThat(lifecycle.beginOffscreenRelease(session, ID))
        .isEqualTo(PremountLifecycle.ReleaseDecision.RETAIN)
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.PREMOUNTED)
  }

  @Test
  fun `seen output is released after its reference is released`() {
    val (lifecycle, session) = createVisibleLifecycle()

    val decision = lifecycle.beginOffscreenRelease(session, ID)
    val current = lifecycle.finishOffscreenRelease(session, ID, decision, released = true)

    assertThat(decision).isEqualTo(PremountLifecycle.ReleaseDecision.RELEASE_AND_RECORD)
    assertThat(current).isTrue()
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.RELEASED)
    assertThat(lifecycle.wantsMounted(ID)).isFalse()
  }

  @Test
  fun `reentrant seen wins over an older release`() {
    val (lifecycle, session) = createVisibleLifecycle()

    val decision = lifecycle.beginOffscreenRelease(session, ID)
    lifecycle.markSeen(session, ID)
    lifecycle.finishOffscreenRelease(session, ID, decision, released = true)

    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.SEEN)
    assertThat(lifecycle.wantsMounted(ID)).isTrue()
  }

  @Test
  fun `failed release of a seen output stays seen`() {
    val (lifecycle, session) = createVisibleLifecycle()

    val decision = lifecycle.beginOffscreenRelease(session, ID)
    lifecycle.finishOffscreenRelease(session, ID, decision, released = false)

    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.SEEN)
    assertThat(lifecycle.wantsMounted(ID)).isTrue()
  }

  @Test
  fun `released output is seen again on scroll`() {
    val (lifecycle, session) = createVisibleLifecycle()
    val decision = lifecycle.beginOffscreenRelease(session, ID)
    lifecycle.finishOffscreenRelease(session, ID, decision, released = true)

    lifecycle.markSeen(session, ID)

    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.SEEN)
  }

  @Test
  fun `released output is skipped by premount`() {
    val (lifecycle, session) = createVisibleLifecycle()
    val decision = lifecycle.beginOffscreenRelease(session, ID)
    lifecycle.finishOffscreenRelease(session, ID, decision, released = true)

    assertThat(lifecycle.beginPremountAcquire(session, ID, alreadyOwned = false))
        .isEqualTo(PremountLifecycle.AcquireDecision.SKIP)
  }

  @Test
  fun `released output that is held again is released off screen`() {
    val (lifecycle, session) = createVisibleLifecycle()
    val decision = lifecycle.beginOffscreenRelease(session, ID)
    lifecycle.finishOffscreenRelease(session, ID, decision, released = true)
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.RELEASED)

    assertThat(lifecycle.beginOffscreenRelease(session, ID))
        .isEqualTo(PremountLifecycle.ReleaseDecision.RELEASE)
  }

  @Test
  fun `lifecycle ids are unique`() {
    val first = PremountLifecycle()
    val second = PremountLifecycle()

    assertThat(first.id).isPositive()
    assertThat(second.id).isNotEqualTo(first.id)
  }

  @Test
  fun `session ids remain unique across renewal and restart`() {
    val lifecycle = PremountLifecycle()

    val first = lifecycle.startSession()
    val second = checkNotNull(lifecycle.renewSession())
    lifecycle.stopSession()
    val third = lifecycle.startSession()

    assertThat(listOf(first.id, second.id, third.id)).containsExactly(1L, 2L, 3L)
  }

  @Test
  fun `session renewal invalidates an acquire completion without losing newer demand`() {
    val (lifecycle, oldSession) = createLifecycle()
    lifecycle.beginPremountAcquire(oldSession, ID, alreadyOwned = false)
    val newSession = checkNotNull(lifecycle.renewSession())
    lifecycle.markSeen(newSession, ID)

    assertThat(lifecycle.finishPremountAcquire(oldSession, ID, acquired = true)).isFalse()
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.SEEN)
  }

  @Test
  fun `session renewal invalidates a release completion without losing newer demand`() {
    val (lifecycle, oldSession) = createVisibleLifecycle()
    val decision = lifecycle.beginOffscreenRelease(oldSession, ID)
    val newSession = checkNotNull(lifecycle.renewSession())
    lifecycle.markSeen(newSession, ID)

    assertThat(
        lifecycle.finishOffscreenRelease(
            oldSession,
            ID,
            decision,
            released = true,
        ),
    )
        .isFalse()
    assertThat(lifecycle.stateOf(ID)).isEqualTo(PremountLifecycle.State.SEEN)
  }

  @Test
  fun `removed and re-added output starts untracked`() {
    val (lifecycle, oldSession) = createVisibleLifecycle()
    val decision = lifecycle.beginOffscreenRelease(oldSession, ID)
    lifecycle.finishOffscreenRelease(oldSession, ID, decision, released = true)
    val newSession = checkNotNull(lifecycle.renewSession())

    assertThat(lifecycle.removeOutput(newSession, ID)).isTrue()
    assertThat(lifecycle.stateOf(ID)).isNull()
    assertThat(lifecycle.beginPremountAcquire(newSession, ID, alreadyOwned = false))
        .isEqualTo(PremountLifecycle.AcquireDecision.ACQUIRE)
  }

  @Test
  fun `teardown clears state and invalidates completions`() {
    val (lifecycle, session) = createVisibleLifecycle()

    lifecycle.stopSession()

    assertThat(lifecycle.stateOf(ID)).isNull()
    assertThat(lifecycle.wantsMounted(ID)).isFalse()
    assertThat(lifecycle.finishPremountAcquire(session, ID, acquired = true)).isFalse()
    assertThat(lifecycle.beginOffscreenRelease(session, ID))
        .isEqualTo(PremountLifecycle.ReleaseDecision.STALE)
  }

  private fun createVisibleLifecycle(): Pair<PremountLifecycle, PremountSession> {
    val (lifecycle, session) = createLifecycle()
    lifecycle.markSeen(session, ID)
    return lifecycle to session
  }

  private fun createLifecycle(): Pair<PremountLifecycle, PremountSession> {
    val lifecycle = PremountLifecycle()
    return lifecycle to lifecycle.startSession()
  }

  companion object {
    private const val ID = 42L
  }
}
