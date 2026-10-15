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

package com.facebook.litho.state

import com.facebook.litho.state.BinderUpdateTracer.BinderUpdateInfo
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.BinderId
import com.facebook.rendercore.BinderType
import com.facebook.rendercore.ClassBinderKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class BinderUpdateTracerTest {

  private val tracer = BinderUpdateTracer()

  @Before
  fun setUp() {
    BinderUpdateTracer.isEnabled = true
  }

  @After
  fun tearDown() {
    BinderUpdateTracer.isEnabled = false
    BinderUpdateTracer.drainData(RENDER_UNIT_ID)
  }

  /**
   * A render unit resolves its mount binders before its attach binders, and both passes finish
   * before any of them bind. So two entries wait for a duration at once, and an unnamed binder is
   * keyed positionally - which makes a mount `binder:0` and an attach `binder:0` collide on name,
   * leaving the binder type as the only thing that tells them apart.
   */
  @Test
  fun `attaches each duration to the binder of the matching type`() {
    val mount = binderId(BinderType.MOUNT)
    val attach = binderId(BinderType.ATTACH)

    // Resolve order: mount first, then attach. Bind order follows the same sequence.
    tracer.onShouldUpdate(mount, result = true)
    tracer.onShouldUpdate(attach, result = true)
    tracer.onBindComplete(mount, durationNs = MOUNT_DURATION_NS)
    tracer.onBindComplete(attach, durationNs = ATTACH_DURATION_NS)

    val drained = checkNotNull(BinderUpdateTracer.drainData(RENDER_UNIT_ID))
    assertThat(drained.single { it.binderType == BinderType.MOUNT }.bindDurationNs)
        .isEqualTo(MOUNT_DURATION_NS)
    assertThat(drained.single { it.binderType == BinderType.ATTACH }.bindDurationNs)
        .isEqualTo(ATTACH_DURATION_NS)
  }

  @Test
  fun `reports that the reason was not collected when the flag is off`() {
    val binderId = binderId(BinderType.MOUNT)

    tracer.onShouldUpdate(binderId, result = true)
    // What RenderUnit dispatches for a dep-based binder when collectBindReasons is off.
    tracer.onDepsNotCollected(binderId)

    val drained = checkNotNull(BinderUpdateTracer.drainData(RENDER_UNIT_ID))
    // Nobody diffed the deps, so calling this a hand-written shouldUpdate is the opposite of true.
    assertThat(drained.single().describeReason()).isEqualTo("reason not collected")
  }

  @Test
  fun `reports a custom shouldUpdate only when deps were actually collected`() {
    // No onDepsNotCollected, so the binder genuinely has no dep model.
    tracer.onShouldUpdate(binderId(BinderType.MOUNT), result = true)

    val drained = checkNotNull(BinderUpdateTracer.drainData(RENDER_UNIT_ID))
    assertThat(drained.single().describeReason()).isEqualTo("custom shouldUpdate")
  }

  @Test
  fun `describes layout data when the deps compared equal`() {
    val info = BinderUpdateInfo(
        binderKey = "binder:0",
        binderType = BinderType.MOUNT,
        shouldUpdate = true,
        depChanges = emptyList(),
    )

    assertThat(info.describeReason()).isEqualTo("layout data")
  }

  private fun binderId(type: BinderType): BinderId =
      BinderId(RENDER_UNIT_ID, type, ClassBinderKey.of(BinderUpdateTracerTest::class.java))

  private companion object {
    const val RENDER_UNIT_ID = 42L
    const val MOUNT_DURATION_NS = 1_000L
    const val ATTACH_DURATION_NS = 2_000L
  }
}
