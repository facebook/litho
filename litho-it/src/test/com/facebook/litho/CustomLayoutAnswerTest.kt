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
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
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

/** A question a `Layout`'s child was already asked this pass costs nothing to ask again. */
@RunWith(LithoTestRunner::class)
class CustomLayoutAnswerTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a child asked the same question twice answers with the layout it made`() {
    LithoStats.resetAllCounters()
    val asksTwice = LayoutFunctionN { ms, c ->
      val first = ms[0].measure(SizeConstraints.exact(LEAF, LEAF))
      val again = ms[0].measure(SizeConstraints.exact(LEAF, LEAF))
      check(first === again) { "the repeat was not answered from memory" }
      layout(c.maxWidth, LEAF) { again.place(0, 0) }
    }

    render { Layout(listOf(AnswerLeaf()), style = Style.width(100.px), layout = asksTwice) }

    assertThat(LithoStats.customLayoutChildLayoutCount.get()).describedAs("layouts").isEqualTo(1)
    assertThat(LithoStats.customLayoutAnswerReusedCount.get()).isEqualTo(1)
  }

  @Test
  fun `a container laid out again in a stretched box asks its children nothing new`() {
    LithoStats.resetAllCounters()
    // Yoga measures it at the Column's width, where it declares only its children's width; the
    // Column then stretches it to that width, so it runs again there and asks each child the same
    // question.
    val column = Column { child(Layout(List(CHILDREN) { AnswerLeaf() }, layout = UNBOUNDED_ROW)) }

    render { column }

    assertThat(LithoStats.customLayoutFunctionRunCount.get()).describedAs("runs").isEqualTo(2)
    assertThat(LithoStats.customLayoutChildLayoutCount.get())
        .describedAs("one layout per child")
        .isEqualTo(CHILDREN.toLong())
    assertThat(LithoStats.customLayoutAnswerReusedCount.get()).isEqualTo(CHILDREN.toLong())
  }

  @Test
  fun `a layout two measurements of a container share is placed by each at its own position`() {
    // Both measurements of the inner container ask its leaf the same question, so they share the
    // leaf's layout, and each places it against its own width. The outer one places the first.
    val atEnd = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints.exact(LEAF, LEAF))
      layout(c.maxWidth, LEAF) { p.place(c.maxWidth - LEAF, 0) }
    }
    val inner = Layout(listOf(AnswerLeaf()), layout = atEnd)
    val placesFirst = LayoutFunctionN { ms, c ->
      val narrow = ms[0].measure(SizeConstraints.exact(60, LEAF))
      ms[0].measure(SizeConstraints.exact(90, LEAF))
      layout(c.maxWidth, LEAF) { narrow.place(0, 0) }
    }

    LithoStats.resetAllCounters()
    val leaves = leafFrames(render { Layout(listOf(inner), layout = placesFirst) })

    assertThat(LithoStats.customLayoutAnswerReusedCount.get()).isPositive()
    assertThat(leaves)
        .describedAs("at the end of the 60 it was placed in")
        .containsExactly("40,0 20x20")
  }

  @Test
  fun `every question a child was asked this pass is answered from memory`() {
    LithoStats.resetAllCounters()
    // Six different questions, then each of them again.
    val widths = listOf(10, 20, 30, 40, 50, 60)
    val asksEach = LayoutFunctionN { ms, c ->
      for (w in widths) ms[0].measure(SizeConstraints.exact(w, LEAF))
      val last = widths.map { ms[0].measure(SizeConstraints.exact(it, LEAF)) }.last()
      layout(c.maxWidth, LEAF) { last.place(0, 0) }
    }

    render { Layout(listOf(AnswerLeaf()), layout = asksEach) }

    assertThat(LithoStats.customLayoutChildLayoutCount.get())
        .describedAs("one layout per distinct question")
        .isEqualTo(widths.size.toLong())
  }

  @Test
  fun `a shared answer that is itself a Layout is placed by each container at its own position`() {
    // The shared answer is itself a Layout, whose leaf sits 5 into it.
    val leafAt5 = LayoutFunctionN { ms, _ ->
      val p = ms[0].measure(SizeConstraints.exact(LEAF, LEAF))
      layout(LEAF + 5, LEAF) { p.place(5, 0) }
    }
    val atEnd = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = LEAF))
      layout(c.maxWidth, LEAF) { p.place(c.maxWidth - p.width, 0) }
    }
    val inner = Layout(listOf(Layout(listOf(AnswerLeaf()), layout = leafAt5)), layout = atEnd)
    val placesFirst = LayoutFunctionN { ms, c ->
      val narrow = ms[0].measure(SizeConstraints.exact(60, LEAF))
      ms[0].measure(SizeConstraints.exact(90, LEAF))
      layout(c.maxWidth, LEAF) { narrow.place(0, 0) }
    }
    val placesSecond = LayoutFunctionN { ms, c ->
      ms[0].measure(SizeConstraints.exact(60, LEAF))
      val wide = ms[0].measure(SizeConstraints.exact(90, LEAF))
      layout(c.maxWidth, LEAF) { wide.place(0, 0) }
    }

    val first = leafFrames(render { Layout(listOf(inner), layout = placesFirst) })
    val second = leafFrames(render { Layout(listOf(inner), layout = placesSecond) })

    assertThat(first).describedAs("in the 60").containsExactly("40,0 20x20")
    assertThat(second).describedAs("in the 90").containsExactly("70,0 20x20")
  }

  private fun render(root: () -> Component): TestLithoView =
      lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { root() }

  /** Each AnswerLeaf's frame in the root's coordinates, in tree order. */
  private fun leafFrames(view: TestLithoView): List<String> {
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val frames = ArrayList<String>()
    fun walk(result: LithoLayoutResult, x: Int, y: Int) {
      if (result.node.tailComponent is AnswerLeaf) {
        frames.add("$x,$y ${result.width}x${result.height}")
      }
      for (i in 0 until result.childCount) {
        walk(
            result.getChildAt(i),
            x + result.getXForChildAtIndex(i),
            y + result.getYForChildAtIndex(i),
        )
      }
    }
    walk(root, 0, 0)
    return frames
  }

  private companion object {
    const val VIEWPORT = 500
    const val LEAF = 20
    const val CHILDREN = 5

    /** Measures each child unbounded and lays them out in a row, declaring their total width. */
    val UNBOUNDED_ROW = LayoutFunctionN { ms, c ->
      val ps = ms.map { it.measure(SizeConstraints(maxWidth = LEAF, maxHeight = LEAF)) }
      layout(ps.sumOf { it.width }.coerceAtMost(c.maxWidth), LEAF) {
        var x = 0
        ps.forEach { placeable ->
          placeable.place(x, 0)
          x += placeable.width
        }
      }
    }
  }
}

/** A Primitive that takes whatever size it is offered within 20x20, or exactly what is exact. */
private class AnswerLeaf : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = AnswerFill,
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private object AnswerFill : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(
      width =
          if (sizeConstraints.hasExactWidth) sizeConstraints.maxWidth
          else 20.coerceAtMost(sizeConstraints.maxWidth),
      height =
          if (sizeConstraints.hasExactHeight) sizeConstraints.maxHeight
          else 20.coerceAtMost(sizeConstraints.maxHeight),
  )
}
