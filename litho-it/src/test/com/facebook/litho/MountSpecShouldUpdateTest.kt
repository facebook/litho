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

// TestDrawableComponent is deprecated but intentionally used here — this test exercises
// MountSpecLithoRenderUnit.shouldUpdateMountItem which only applies to legacy MountSpec components.
// Primitive components use a different code path (PrimitiveLithoRenderUnit).
import android.graphics.Color
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_DIRTY
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_UNKNOWN
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_UPDATED
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [MountSpecLithoRenderUnit.shouldUpdateMountItem] verifying no behavior change after
 * removing the chain-continuity check (previousLayoutStateId == currentLayoutStateId) and its
 * associated IDs from LithoLayoutData.
 *
 * These tests use render units from DIFFERENT layout passes (mounted vs new) to verify real mount
 * scenarios. They confirm the same results as the old code: STATE_DIRTY forces update,
 * STATE_UPDATED skips, STATE_UNKNOWN falls through to shouldComponentUpdate.
 */
@RunWith(LithoTestRunner::class)
class MountSpecShouldUpdateTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  /**
   * STATE_DIRTY forces update even when the mounted and new render units come from different layout
   * passes. This catches state-driven rendering changes that shouldComponentUpdate might miss.
   */
  @Test
  fun stateDirty_forcesUpdate_acrossLayoutPasses() {
    // Layout pass 1: RED component
    val firstComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(10), heightSpec = exactly(10)) { firstComponent }
    val firstLayoutState = testLithoView.componentTree.mainThreadLayoutState!!
    val (mountedUnit, mountedData) = findRenderUnitWithState(firstLayoutState, STATE_UNKNOWN)

    // Layout pass 2: BLUE component → STATE_DIRTY for the drawable output
    val secondComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.BLUE)
            .build()
    testLithoView.setRoot(secondComponent)
    val secondLayoutState = testLithoView.componentTree.mainThreadLayoutState!!
    val (newUnit, newData) = findRenderUnitWithState(secondLayoutState, STATE_DIRTY)

    // Mounted render unit is from pass 1, new is from pass 2
    val result =
        MountSpecLithoRenderUnit.shouldUpdateMountItem(mountedUnit, newUnit, mountedData, newData)
    assertThat(result).isTrue()
  }

  @Test
  fun commonPropChange_marksPureRenderMountSpecDirty() {
    val firstComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .alpha(0.5f)
            .build()
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(10), heightSpec = exactly(10)) { firstComponent }

    val secondComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .alpha(0.1f)
            .build()
    testLithoView.setRoot(secondComponent)

    val secondLayoutState = testLithoView.componentTree.mainThreadLayoutState!!
    findRenderUnitWithState(secondLayoutState, STATE_DIRTY)
  }

  /**
   * STATE_UPDATED skips update when the component hasn't changed across layout passes.
   *
   * This validates the chain check removal: STATE_UPDATED is only set when the component is
   * genuinely unchanged from the diff baseline. Due to state accumulation, if any intermediate
   * layout changed this component, this layout would also see the change and set STATE_DIRTY
   * instead. So STATE_UPDATED always means "safe to skip."
   */
  @Test
  fun stateUpdated_skipsUpdate_acrossLayoutPasses() {
    // Layout pass 1: RED component (isPureRender so shouldComponentUpdate compares props)
    val component =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(10), heightSpec = exactly(10)) { component }
    val firstLayoutState = testLithoView.componentTree.mainThreadLayoutState!!
    val (mountedUnit, mountedData) = findRenderUnitWithState(firstLayoutState, STATE_UNKNOWN)

    // Layout pass 2: same props → STATE_UPDATED
    val sameComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    testLithoView.setRoot(sameComponent)
    val secondLayoutState = testLithoView.componentTree.mainThreadLayoutState!!
    val (newUnit, newData) = findRenderUnitWithState(secondLayoutState, STATE_UPDATED)

    // Mounted from pass 1, new from pass 2 — different render unit objects
    assertThat(mountedUnit).isNotSameAs(newUnit)
    val result =
        MountSpecLithoRenderUnit.shouldUpdateMountItem(mountedUnit, newUnit, mountedData, newData)
    assertThat(result).isFalse()
  }

  /**
   * STATE_UPDATED skips update even across THREE layout passes, simulating the concurrent layout
   * scenario where an intermediate layout was mounted.
   *
   * Pass 1 (baseline N) → Pass 2 (same props, mounted as intermediate A) → Pass 3 (same props).
   * Pass 3 is reduced against Pass 2, but we mount with Pass 1's render unit as "current." Without
   * the chain check, STATE_UPDATED still correctly skips because the component is genuinely
   * unchanged across all passes.
   */
  @Test
  fun stateUpdated_skipsUpdate_withIntermediateLayout() {
    val component =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(10), heightSpec = exactly(10)) { component }
    val pass1State = testLithoView.componentTree.mainThreadLayoutState!!
    val (pass1Unit, pass1Data) = findRenderUnitWithState(pass1State, STATE_UNKNOWN)

    // Pass 2: same props
    val sameComponent2 =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    testLithoView.setRoot(sameComponent2)

    // Pass 3: same props again
    val sameComponent3 =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    testLithoView.setRoot(sameComponent3)
    val pass3State = testLithoView.componentTree.mainThreadLayoutState!!
    val (pass3Unit, pass3Data) = findRenderUnitWithState(pass3State, STATE_UPDATED)

    // Simulate: pass 1 is mounted, pass 3 is the new layout (pass 2 was intermediate)
    assertThat(pass1Unit).isNotSameAs(pass3Unit)
    val result =
        MountSpecLithoRenderUnit.shouldUpdateMountItem(pass1Unit, pass3Unit, pass1Data, pass3Data)
    assertThat(result).isFalse()
  }

  /**
   * STATE_DIRTY forces update across three passes when props change in the latest pass, regardless
   * of what intermediate layouts did.
   */
  @Test
  fun stateDirty_forcesUpdate_withIntermediateLayout() {
    val component =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    val testLithoView =
        lithoTestRule.render(widthSpec = exactly(10), heightSpec = exactly(10)) { component }
    val pass1State = testLithoView.componentTree.mainThreadLayoutState!!
    val (pass1Unit, pass1Data) = findRenderUnitWithState(pass1State, STATE_UNKNOWN)

    // Pass 2: same props (intermediate)
    val sameComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.RED)
            .build()
    testLithoView.setRoot(sameComponent)

    // Pass 3: DIFFERENT props
    val differentComponent =
        TestDrawableComponent.create(lithoTestRule.context, true, false, false)
            .color(Color.BLUE)
            .build()
    testLithoView.setRoot(differentComponent)
    val pass3State = testLithoView.componentTree.mainThreadLayoutState!!
    val (pass3Unit, pass3Data) = findRenderUnitWithState(pass3State, STATE_DIRTY)

    // Simulate: pass 1 mounted, pass 3 is new (pass 2 was intermediate)
    val result =
        MountSpecLithoRenderUnit.shouldUpdateMountItem(pass1Unit, pass3Unit, pass1Data, pass3Data)
    assertThat(result).isTrue()
  }

  private fun findRenderUnitWithState(
      layoutState: LayoutState,
      @MountSpecLithoRenderUnit.UpdateState targetState: Int,
  ): Pair<MountSpecLithoRenderUnit, Any?> {
    for (i in 1 until layoutState.getMountableOutputCount()) {
      val node = layoutState.getMountableOutputAt(i)!!
      if (node.renderUnit is MountSpecLithoRenderUnit) {
        val ru = node.renderUnit as MountSpecLithoRenderUnit
        if (ru.updateState == targetState) {
          return ru to node.layoutData
        }
      }
    }
    throw AssertionError("No render unit with state $targetState found")
  }
}
