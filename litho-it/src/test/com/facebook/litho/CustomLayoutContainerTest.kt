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
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
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
import com.facebook.yoga.YogaDirection
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** What it means that each measurement of a `Layout`'s child owns its layout. */
@RunWith(LithoTestRunner::class)
class CustomLayoutContainerTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `placing an earlier measurement mounts the layout that measurement made`() {
    // Measures the child twice and places the first: the mounted child is the first's size.
    val measuresTwicePlacesFirst = LayoutFunctionN { ms, c ->
      val wide = ms[0].measure(SizeConstraints.exact(80, 20))
      ms[0].measure(SizeConstraints.exact(30, 20))
      layout(c.maxWidth, 20) { wide.place(0, 0) }
    }

    val leaf =
        leafFrames(render { Layout(listOf(CountingLeaf()), layout = measuresTwicePlacesFirst) })

    assertThat(leaf).containsExactly("0,0 80x20")
  }

  @Test
  fun `a container stretched by its flexbox parent places its children in the stretched box`() {
    // Declares the children's width; the Column stretches it to the full width, so the function
    // runs again under exactly that box and places from there.
    val rightAligned = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      val w = if (c.hasExactWidth) c.maxWidth else p.width
      layout(w, p.height) { p.place(w - p.width, 0) }
    }

    val leaf = leafFrames(
        render { Column { child(Layout(listOf(CountingLeaf()), layout = rightAligned)) } },
    )

    assertThat(leaf).containsExactly("${VIEWPORT - LEAF},0 ${LEAF}x$LEAF")
  }

  @Test
  fun `a container at the root, and one inside another, both place their children`() {
    val inner = Layout(listOf(CountingLeaf(), CountingLeaf()), layout = STACK)
    val leaves = leafFrames(render { Layout(listOf(CountingLeaf(), inner), layout = STACK) })

    assertThat(leaves).containsExactly("0,0 20x20", "0,20 20x20", "0,40 20x20")
  }

  @Test
  fun `a container whose box Yoga already knows is still placed when laid out again`() {
    val bump = AtomicInteger()
    val view = render {
      Row {
        child(Column(style = Style.width((10 + bump.get()).px).height(10.px)))
        child(
            Layout(
                listOf(CountingLeaf()),
                style = Style.width(50.px).height(30.px).flex(shrink = 0f),
                layout = STACK,
            ),
        )
      }
    }
    bump.set(5)
    view.setRoot(
        Row {
          child(Column(style = Style.width(15.px).height(10.px)))
          child(
              Layout(
                  listOf(CountingLeaf()),
                  style = Style.width(50.px).height(30.px).flex(shrink = 0f),
                  layout = STACK,
              ),
          )
        },
    )
    view.measure().layout()

    assertThat(leafFrames(view)).containsExactly("15,0 20x20")
  }

  @Test
  fun `each measurement lays the child out, and a placed one is not laid out again`() {
    LithoStats.resetAllCounters()
    val measuresTwice = LayoutFunctionN { ms, c ->
      ms[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity))
      val p = ms[0].measure(SizeConstraints.exact(LEAF, LEAF))
      layout(c.maxWidth, LEAF) { p.place(0, 0) }
    }

    render { Layout(listOf(CountingLeaf()), style = Style.width(100.px), layout = measuresTwice) }

    assertThat(LithoStats.customLayoutFunctionRunCount.get()).describedAs("runs").isEqualTo(1)
    assertThat(LithoStats.customLayoutChildLayoutCount.get()).describedAs("layouts").isEqualTo(2)
  }

  @Test
  fun `a failure in a measurement nothing places is never reported`() {
    val throwing = LayoutFunctionN { _, _ -> error("the layout function failed") }
    val probesOnly = LayoutFunctionN { ms, c ->
      ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      layout(c.maxWidth, LEAF) {}
    }

    val leaves = leafFrames(
        render {
          Layout(listOf(Layout(listOf(CountingLeaf()), layout = throwing)), layout = probesOnly)
        },
    )

    assertThat(leaves).isEmpty()
  }

  @Test
  fun `a failure in a placed container reaches the error boundary`() {
    val throwing = LayoutFunctionN { _, _ -> error("the layout function failed") }

    assertThatThrownBy {
          render {
            Layout(listOf(Layout(listOf(CountingLeaf()), layout = throwing)), layout = STACK)
          }
        }
        .hasMessageContaining("the layout function failed")
  }

  @Test
  fun `a failure while placing a placed container's children reaches the error boundary`() {
    val placingThrows = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      layout(c.maxWidth, p.height) { error("the placement failed") }
    }

    assertThatThrownBy {
          render {
            Layout(listOf(Layout(listOf(CountingLeaf()), layout = placingThrows)), layout = STACK)
          }
        }
        .hasMessageContaining("the placement failed")
  }

  @Test
  fun `a child's start margin follows the direction it inherits`() {
    val placeAtStart = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      layout(c.maxWidth, p.height) { p.placeRelative(0, 0) }
    }
    val leaf = CountingLeaf(style = Style.margin(start = 5.px))

    val frames = leafFrames(
        render {
          Column(style = Style.layoutDirection(YogaDirection.RTL)) {
            child(Layout(listOf(leaf), layout = placeAtStart))
          }
        },
    )

    assertThat(frames)
        .describedAs("at the right edge, its start margin on its right")
        .containsExactly("${VIEWPORT - 5 - LEAF},0 ${LEAF}x$LEAF")
  }

  private fun render(root: () -> Component): TestLithoView =
      lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { root() }

  /** Each CountingLeaf's frame in the root's coordinates, in tree order. */
  private fun leafFrames(view: TestLithoView): List<String> {
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val frames = ArrayList<String>()
    fun walk(result: LithoLayoutResult, x: Int, y: Int) {
      if (result.node.tailComponent is CountingLeaf) {
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

    val STACK = LayoutFunctionN { ms, c ->
      val ps = ms.map {
        it.measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      }
      layout(ps.maxOf { it.width }, ps.sumOf { it.height }) {
        var y = 0
        ps.forEach { placeable ->
          placeable.place(0, y)
          y += placeable.height
        }
      }
    }
  }
}

/** A Primitive that takes whatever size it is offered within 20x20, or exactly what is exact. */
private class CountingLeaf(private val style: Style? = null) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = FillBehavior,
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = style,
  )
}

private object FillBehavior : LayoutBehavior {
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
