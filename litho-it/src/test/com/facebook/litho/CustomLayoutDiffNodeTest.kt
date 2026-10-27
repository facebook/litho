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

@file:OptIn(ExperimentalLithoApi::class)

package com.facebook.litho

import android.view.View
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.effects.useEffect
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A `Layout`'s child gets its own DiffNode from the last pass, whichever siblings were placed. */
@RunWith(LithoTestRunner::class)
class CustomLayoutDiffNodeTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a child keeps its own diff state when a container switches which sibling it places`() {
    // Two equivalent siblings, told apart only by their keys. The first pass places the second, the
    // next the first: the second's layout effect is cleaned up and the first's runs. Handed its
    // sibling's diff state, the first would carry over the sibling's effect, and neither would
    // fire.
    val events = ArrayList<String>()
    val placesOne = { which: Int ->
      LayoutFunctionN { ms, c ->
        val p = ms[which].measure(SizeConstraints.exact(LEAF, LEAF))
        layout(c.maxWidth, LEAF) { p.place(0, 0) }
      }
    }
    val second = placesOne(1)
    val first = placesOne(0)
    val children = { listOf(EffectLeaf("leaf", events), EffectLeaf("leaf", events)) }
    val view = render { Layout(children(), layout = second) }
    assertThat(events).describedAs("first pass").containsExactly("run leaf")

    events.clear()
    view.setRoot(Layout(children(), layout = first))
    view.measure().layout()

    assertThat(events)
        .describedAs("after switching")
        .containsExactlyInAnyOrder("cleanup leaf", "run leaf")
  }

  @Test
  fun `deferred content laid out again in the same box reuses its last measurement`() {
    // The content is resolved afresh each pass, under its holder's key. Handed the DiffNode its
    // holder's place in the last pass holds, its leaf measured under the same spec isn't measured
    // again.
    val measures = intArrayOf(0)
    val tree = {
      Layout(listOf(RenderWithConstraints { _ -> MeasureCountingLeaf(measures) }), layout = STACKED)
    }
    val counts = ArrayList<Int>()
    val view = render(tree)
    counts.add(measures[0])
    repeat(2) {
      measures[0] = 0
      view.setRoot(tree())
      view.measure().layout()
      counts.add(measures[0])
    }

    assertThat(counts).describedAs("measures in each pass").containsExactly(1, 0, 0)
  }

  @Test
  fun `a child under a deferred holder's last key isn't handed the holder's content's DiffNode`() {
    // The last pass's child under this key was a deferred holder, whose content's DiffNode sits
    // under a key below it. The child under that key now isn't deferred: it gets no DiffNode, and
    // is measured as in a fresh layout, though it's equivalent to that content.
    val measures = intArrayOf(0)
    val view = render {
      Layout(
          listOf(key("child") { RenderWithConstraints { _ -> MeasureCountingLeaf(measures) } }),
          layout = STACKED,
      )
    }
    measures[0] = 0

    view.setRoot(Layout(listOf(key("child") { MeasureCountingLeaf(measures) }), layout = STACKED))
    view.measure().layout()

    assertThat(measures[0]).describedAs("measures in the next pass").isEqualTo(1)
  }

  @Test
  fun `each placed deferred child reuses its own measurement when its siblings aren't placed`() {
    // The `Layout` places every other child, so a placed child's DiffNode from the last pass sits
    // at another index than the child. Each leaf counts its own measures, and isn't equivalent to
    // its siblings': handed a sibling's DiffNode, it would be measured again.
    val measures = List(6) { intArrayOf(0) }
    val tree = {
      Layout(
          measures.map { counter -> RenderWithConstraints { _ -> MeasureCountingLeaf(counter) } },
          layout = PLACES_EVERY_OTHER,
      )
    }
    val view = render(tree)
    assertThat(measures.map { counter -> counter[0] })
        .describedAs("measures in the first pass")
        .containsExactly(1, 1, 1, 1, 1, 1)

    measures.forEach { counter -> counter[0] = 0 }
    view.setRoot(tree())
    view.measure().layout()

    assertThat(measures.map { counter -> counter[0] })
        .describedAs("measures in the next pass: only the unplaced children have no DiffNode")
        .containsExactly(0, 1, 0, 1, 0, 1)
  }

  private fun render(root: () -> Component): TestLithoView =
      lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { root() }

  private companion object {
    const val VIEWPORT = 500
    const val LEAF = 20

    val PLACES_EVERY_OTHER = LayoutFunctionN { measurables, constraints ->
      val placeables = measurables.map { measurable ->
        measurable.measure(SizeConstraints(maxWidth = constraints.maxWidth))
      }
      layout(constraints.maxWidth, LEAF) {
        placeables.forEachIndexed { index, placeable ->
          if (index % 2 == 0) placeable.place(index * LEAF, 0)
        }
      }
    }

    val STACKED = LayoutFunctionN { measurables, constraints ->
      val placeables = measurables.map { measurable ->
        measurable.measure(SizeConstraints(maxWidth = constraints.maxWidth))
      }
      layout(constraints.maxWidth, placeables.sumOf { placeable -> placeable.height }) {
        var y = 0
        placeables.forEach { placeable ->
          placeable.place(0, y)
          y += placeable.height
        }
      }
    }
  }
}

/** A Primitive 20 x 20 that counts in [measures] each time it's measured. */
private class MeasureCountingLeaf(private val measures: IntArray) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = MeasureCountingBehavior(measures),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class MeasureCountingBehavior(private val measures: IntArray) : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult {
    measures[0]++
    return PrimitiveLayoutResult(width = 20, height = 20)
  }

  override fun isEquivalentTo(other: LayoutBehavior): Boolean =
      other is MeasureCountingBehavior && other.measures === measures
}

/** A Primitive whose layout effect records when it runs and is cleaned up, by its own name. */
private class EffectLeaf(private val name: String, private val events: MutableList<String>) :
    PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = EffectBehavior(name, events),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class EffectBehavior(private val name: String, private val events: MutableList<String>) :
    LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult {
    useEffect(name) {
      events.add("run $name")
      onCleanup { events.add("cleanup $name") }
    }
    return PrimitiveLayoutResult(width = 20, height = 20)
  }

  override fun isEquivalentTo(other: LayoutBehavior): Boolean =
      other is EffectBehavior && other.name == name
}
