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

  private fun render(root: () -> Component): TestLithoView =
      lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { root() }

  private companion object {
    const val VIEWPORT = 500
    const val LEAF = 20
  }
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
