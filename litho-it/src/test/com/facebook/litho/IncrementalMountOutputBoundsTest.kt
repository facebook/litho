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

import android.graphics.Rect
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for IncrementalMountOutput bounds finalization.
 *
 * Verifies that creating IMOs with hostOutput=null (deferring ensureHostBounds) and then calling
 * ensureHostBoundsFromChild in a bottom-up pass produces the same host bounds as the original
 * inline ensureHostBounds approach.
 */
@RunWith(LithoTestRunner::class)
class IncrementalMountOutputBoundsTest {

  @Test
  fun `child within host bounds does not expand host`() {
    val host = imo(id = 1, bounds = Rect(0, 0, 100, 100))
    val child = imo(id = 2, bounds = Rect(10, 10, 90, 90))

    IncrementalMountOutput.expandHostToContainChild(child, host)

    assertThat(host.bounds).isEqualTo(Rect(0, 0, 100, 100))
    assertThat(host.originalBounds).isNull()
  }

  @Test
  fun `child overflowing bottom expands host`() {
    val host = imo(id = 1, bounds = Rect(0, 0, 100, 100))
    val child = imo(id = 2, bounds = Rect(0, 0, 100, 150))

    IncrementalMountOutput.expandHostToContainChild(child, host)

    assertThat(host.bounds).isEqualTo(Rect(0, 0, 100, 150))
    assertThat(host.originalBounds).isEqualTo(Rect(0, 0, 100, 100))
  }

  @Test
  fun `child overflowing all sides expands host`() {
    val host = imo(id = 1, bounds = Rect(10, 10, 90, 90))
    val child = imo(id = 2, bounds = Rect(0, 0, 100, 100))

    IncrementalMountOutput.expandHostToContainChild(child, host)

    assertThat(host.bounds).isEqualTo(Rect(0, 0, 100, 100))
  }

  @Test
  fun `multiple children progressively expand host`() {
    val host = imo(id = 1, bounds = Rect(10, 10, 90, 90))
    val child1 = imo(id = 2, bounds = Rect(5, 10, 90, 90))
    val child2 = imo(id = 3, bounds = Rect(10, 10, 95, 90))
    val child3 = imo(id = 4, bounds = Rect(10, 5, 90, 95))

    IncrementalMountOutput.expandHostToContainChild(child1, host)
    IncrementalMountOutput.expandHostToContainChild(child2, host)
    IncrementalMountOutput.expandHostToContainChild(child3, host)

    assertThat(host.bounds).isEqualTo(Rect(5, 5, 95, 95))
  }

  @Test
  fun `bottom-up finalization expands nested hosts correctly`() {
    // Tree structure:
    //   root (0,0,200,200)
    //     hostA (10,10,100,100)
    //       child1 (5,5,95,95)     ← overflows hostA
    //       child2 (10,10,90,150)  ← overflows hostA bottom
    //     child3 (10,10,190,190)   ← within root

    val root = imo(id = 1, bounds = Rect(0, 0, 200, 200))
    val hostA = imo(id = 2, bounds = Rect(10, 10, 100, 100))
    val child1 = imo(id = 3, bounds = Rect(5, 5, 95, 95))
    val child2 = imo(id = 4, bounds = Rect(10, 10, 90, 150))
    val child3 = imo(id = 5, bounds = Rect(10, 10, 190, 190))

    // DFS pre-order: root, hostA, child1, child2, child3
    // Bottom-up (reverse): child3, child2, child1, hostA, root
    // Simulate bottom-up finalization:

    // child3 → root
    IncrementalMountOutput.expandHostToContainChild(child3, root)
    // child2 → hostA
    IncrementalMountOutput.expandHostToContainChild(child2, hostA)
    // child1 → hostA
    IncrementalMountOutput.expandHostToContainChild(child1, hostA)
    // hostA → root (hostA now has expanded bounds from child1+child2)
    IncrementalMountOutput.expandHostToContainChild(hostA, root)

    // hostA should contain child1 and child2
    assertThat(hostA.bounds).isEqualTo(Rect(5, 5, 100, 150))

    // root should contain hostA-expanded and child3
    assertThat(root.bounds).isEqualTo(Rect(0, 0, 200, 200))
  }

  @Test
  fun `bottom-up finalization propagates overflow through nested hosts`() {
    // Tree: root → hostA → hostB → child (overflows all)
    val root = imo(id = 1, bounds = Rect(0, 0, 100, 100))
    val hostA = imo(id = 2, bounds = Rect(10, 10, 90, 90))
    val hostB = imo(id = 3, bounds = Rect(20, 20, 80, 80))
    val child = imo(id = 4, bounds = Rect(0, 0, 100, 120))

    // Bottom-up: child → hostB, hostB → hostA, hostA → root
    IncrementalMountOutput.expandHostToContainChild(child, hostB)
    IncrementalMountOutput.expandHostToContainChild(hostB, hostA)
    IncrementalMountOutput.expandHostToContainChild(hostA, root)

    assertThat(hostB.bounds).isEqualTo(Rect(0, 0, 100, 120))
    assertThat(hostA.bounds).isEqualTo(Rect(0, 0, 100, 120))
    assertThat(root.bounds).isEqualTo(Rect(0, 0, 100, 120))
  }

  @Test
  fun `inline ensureHostBounds matches bottom-up finalization`() {
    // Verify that the old approach (inline, with hostOutput set) produces
    // the same result as the new approach (hostOutput=null, bottom-up pass).

    // --- Old approach: inline expansion via constructor ---
    val hostOld = imo(id = 1, bounds = Rect(10, 10, 90, 90))
    val child1Old =
        IncrementalMountOutput(2, 1, Rect(5, 10, 90, 90), false, null, hostOld, Any(), hostOld.id)
    val child2Old =
        IncrementalMountOutput(3, 2, Rect(10, 10, 95, 95), false, null, hostOld, Any(), hostOld.id)

    // --- New approach: null hostOutput + bottom-up pass ---
    val hostNew = imo(id = 1, bounds = Rect(10, 10, 90, 90))
    val child1New = imo(id = 2, bounds = Rect(5, 10, 90, 90))
    val child2New = imo(id = 3, bounds = Rect(10, 10, 95, 95))

    // Bottom-up: child2 → host, child1 → host
    IncrementalMountOutput.expandHostToContainChild(child2New, hostNew)
    IncrementalMountOutput.expandHostToContainChild(child1New, hostNew)

    assertThat(hostNew.bounds).isEqualTo(hostOld.bounds)
  }

  @Test
  fun `excludeFromIncrementalMount propagates transitively through nested hosts`() {
    // Tree: root → hostA → hostB → child(excludeFromIM=true)
    // Bottom-up pass: child→hostB, hostB→hostA, hostA→root
    // Every ancestor should have excludeFromIncrementalMount=true after the pass.
    val root = imo(id = 1, bounds = Rect(0, 0, 200, 200))
    val hostA = imo(id = 2, bounds = Rect(10, 10, 190, 190))
    val hostB = imo(id = 3, bounds = Rect(20, 20, 180, 180))
    val child = imo(id = 4, bounds = Rect(30, 30, 170, 170), excludeFromIM = true)

    assertThat(hostB.excludeFromIncrementalMount()).isFalse()
    assertThat(hostA.excludeFromIncrementalMount()).isFalse()
    assertThat(root.excludeFromIncrementalMount()).isFalse()

    IncrementalMountOutput.expandHostToContainChild(child, hostB)
    IncrementalMountOutput.expandHostToContainChild(hostB, hostA)
    IncrementalMountOutput.expandHostToContainChild(hostA, root)

    assertThat(hostB.excludeFromIncrementalMount()).isTrue()
    assertThat(hostA.excludeFromIncrementalMount()).isTrue()
    assertThat(root.excludeFromIncrementalMount()).isTrue()
  }

  @Test
  fun `bounds is defensive copy — not same object`() {
    val rect = Rect(10, 20, 30, 40)
    val output = IncrementalMountOutput(
        1,
        0,
        rect,
        false,
        null,
        null,
        Any(),
        IncrementalMountOutput.NO_HOST_ID,
    )

    // bounds should be a copy, not the same object (ensureHostBounds mutates host bounds)
    assertThat(output.bounds).isNotSameAs(rect)
    assertThat(output.bounds).isEqualTo(rect)
  }

  private fun imo(
      id: Long,
      bounds: Rect,
      excludeFromIM: Boolean = false,
  ): IncrementalMountOutput = IncrementalMountOutput(
      id,
      0,
      Rect(bounds),
      excludeFromIM,
      null,
      null,
      Any(),
      IncrementalMountOutput.NO_HOST_ID,
  )
}
