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
import com.facebook.litho.core.margin
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.Placeable
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A `Layout` child's size table: what it measured under each set of constraints this pass, and the
 * placeables made from it.
 */
@RunWith(LithoTestRunner::class)
class CustomLayoutSizeTableTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `the same constraints return the same placeable`() {
    LithoStats.resetAllCounters()
    val placeables = ArrayList<Placeable>()
    val asksABA = LayoutFunctionN { ms, c ->
      placeables.add(ms[0].measure(A))
      placeables.add(ms[0].measure(B))
      placeables.add(ms[0].measure(A))
      placeables.add(ms[0].measure(B))
      layout(c.maxWidth, LEAF) { placeables[0].place(0, 0) }
    }

    render { Layout(listOf(SizedLeaf()), layout = asksABA) }

    assertThat(placeables[2]).isSameAs(placeables[0])
    assertThat(placeables[3]).isSameAs(placeables[1])
    assertThat(placeables[1]).isNotSameAs(placeables[0])
    assertThat(LithoStats.customLayoutChildLayoutCount.get()).describedAs("layouts").isEqualTo(2)
  }

  @Test
  fun `a placeable stays as it was when the child is measured under other constraints`() {
    val seen = ArrayList<String>()
    val asksAThenB = LayoutFunctionN { ms, c ->
      val a = ms[0].measure(A)
      seen.add("A: ${describe(a)}")
      seen.add("B: ${describe(ms[0].measure(B))}")
      val again = ms[0].measure(A)
      seen.add("A again: ${describe(again)}, same: ${again === a}")
      layout(c.maxWidth, LEAF) { a.place(0, 0) }
    }

    render { Layout(listOf(SizedLeaf(style = MARGINS)), layout = asksAThenB) }

    assertThat(seen)
        .containsExactly(
            "A: 28x30 of 28x30 at 3,4",
            "B: 48x30 of 48x30 at 3,4",
            "A again: 28x30 of 28x30 at 3,4, same: true",
        )
  }

  @Test
  fun `placeables under several constraints are each kept`() {
    LithoStats.resetAllCounters()
    val seen = ArrayList<String>()
    val asksAABAB = LayoutFunctionN { ms, c ->
      val a = ms[0].measure(A)
      val sameA = ms[0].measure(A) === a
      val b = ms[0].measure(B)
      seen.add("A: ${describe(a)}, again the same: $sameA")
      seen.add("B: ${describe(b)}")
      seen.add("both kept: ${ms[0].measure(A) === a && ms[0].measure(B) === b}")
      layout(c.maxWidth, LEAF) { b.place(0, 0) }
    }

    render { Layout(listOf(SizedLeaf()), layout = asksAABAB) }

    assertThat(seen)
        .containsExactly(
            "A: 20x20 of 20x20 at 0,0, again the same: true",
            "B: 40x30 of 40x30 at 0,0",
            "both kept: true",
        )
    assertThat(LithoStats.customLayoutChildLayoutCount.get()).describedAs("layouts").isEqualTo(2)
  }

  private fun render(root: () -> Component) {
    lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { root() }
  }

  private fun describe(p: Placeable): String =
      "${p.width}x${p.height} of ${p.measuredWidth}x${p.measuredHeight} at " +
          "${p.marginLeft},${p.marginTop}"

  private companion object {
    const val VIEWPORT = 500
    const val LEAF = 20
    val A: SizeConstraints = SizeConstraints(maxWidth = 100, maxHeight = 100)
    val B: SizeConstraints = SizeConstraints(maxWidth = 100, minHeight = 30, maxHeight = 30)

    /** 3 left, 4 top, 5 right, 6 bottom: 8 wider and 10 taller than the border box. */
    val MARGINS: Style = Style.margin(left = 3.px, top = 4.px, right = 5.px, bottom = 6.px)
  }
}

/**
 * A Primitive 20x20 within its bounds, except under a minimum height: then it is 40 wide and as
 * tall as that minimum, so A and B give it different sizes.
 */
private class SizedLeaf(private val style: Style? = null) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = SizedFill,
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = style,
  )
}

private object SizedFill : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(
      width = if (sizeConstraints.minHeight > 0) 40 else 20,
      height = 20.coerceIn(sizeConstraints.minHeight, sizeConstraints.maxHeight),
  )
}
