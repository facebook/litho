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

import android.graphics.Color
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.Measurable
import com.facebook.litho.layout.MeasureScope
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A nested container built by its parent's builder, against the same container wrapped in a
 * `Column`, where Yoga lays it out as a leaf of the Column's tree: its size, its children's
 * positions and its baseline come out the same either way.
 */
@RunWith(LithoTestRunner::class)
class CustomLayoutNestedContainerTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a nested container is built by its parent where its parent is a container`() {
    val view = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { outer(inner()) }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val inner = checkNotNull(find(root) { it.node.layoutFunction === INNER })

    assertThat((inner.measureOutput as DefaultMeasureOutput).yogaNode)
        .describedAs("built by its parent's builder, with no Yoga node")
        .isNull()
  }

  @Test
  fun `a nested container built by its parent places its children as a Yoga pass does`() {
    for (rtl in listOf(false, true)) {
      val direct = innerBounds { outer(inner(), rtl) }
      val throughYoga = innerBounds { outer(Column { child(inner()) }, rtl) }

      assertThat(direct).describedAs("rtl=%s", rtl).isEqualTo(throughYoga)
    }
  }

  /**
   * The inner container's children start inside its padding (5) and border (2) on its start, top
   * and bottom: at the left edge in LTR, against the right edge in RTL. Absolute, so a mistake
   * shared with the Yoga pass above still shows.
   */
  @Test
  fun `a nested container's children are inset by its padding and border, in both directions`() {
    for (rtl in listOf(false, true)) {
      val view =
          lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { outer(inner(), rtl) }
      val root =
          checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
              as LithoLayoutResult
      val inner = checkNotNull(find(root) { it.node.layoutFunction === INNER })

      for (i in 0 until inner.childCount) {
        val child = inner.getChildAt(i)
        val childX = inner.getXForChildAtIndex(i)
        val gap = if (rtl) inner.width - (childX + child.width) else childX
        assertThat(gap).describedAs("child %s from its start edge, rtl=%s", i, rtl).isEqualTo(INSET)
      }
      assertThat(inner.getYForChildAtIndex(0))
          .describedAs("first child's top, rtl=%s", rtl)
          .isEqualTo(INSET)
      val lastIndex = inner.childCount - 1
      assertThat(
          inner.height -
              (inner.getYForChildAtIndex(lastIndex) + inner.getChildAt(lastIndex).height),
      )
          .describedAs("last child's bottom gap, rtl=%s", rtl)
          .isEqualTo(INSET)
    }
  }

  @Test
  fun `a nested container built by its parent has the baseline a Yoga pass gives it`() {
    // The nested container is the outer one's only child, so the outer baseline is its own.
    val direct = siblingY { Layout(listOf(inner()), layout = STACK) }
    val throughYoga = siblingY { Layout(listOf(Column { child(inner()) }), layout = STACK) }

    assertThat(direct).isEqualTo(throughYoga)
  }

  /**
   * Measured at 300 and then at 200 inside a Yoga child, and built at 200, a nested `Layout` places
   * its child from the run under 200: its function right-aligns the child, so the child's position
   * shows which run it was built from.
   */
  @Test
  fun `a nested Layout in a Yoga child built at its last width places from that run`() {
    val rightAligns = LayoutFunctionN { measurables, constraints ->
      val placeable = measurables[0].measure(SizeConstraints(maxWidth = 40, maxHeight = 20))
      layout(constraints.maxWidth, placeable.height) {
        placeable.place(constraints.maxWidth - placeable.width, 0)
      }
    }
    val wideThenNarrow = LayoutFunctionN { measurables, _ ->
      measurables[0].measure(SizeConstraints(maxWidth = 300, maxHeight = 50))
      val placeable = measurables[0].measure(SizeConstraints(maxWidth = 200, maxHeight = 50))
      layout(300, placeable.height) { placeable.place(0, 0) }
    }
    val inner =
        Layout(listOf(Column(style = Style.width(40.px).height(20.px))), layout = rightAligns)

    val view =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Layout(listOf(Column { child(inner) }), layout = wideThenNarrow)
        }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val result = checkNotNull(find(root) { it.node.layoutFunction === rightAligns })

    assertThat(result.width).describedAs("its width").isEqualTo(200)
    assertThat(result.getXForChildAtIndex(0))
        .describedAs("its child, right-aligned in 200")
        .isEqualTo(160)
  }

  /** A container with padding and a border, holding text and a fixed leaf, stacked. */
  private fun ResourcesScope.inner(): Component = Layout(
      listOf(
          Text(text = "inner", textSize = 20.px),
          Column(style = Style.width(30.px).height(15.px)),
      ),
      style = Style.padding(all = 5.px).border(width = 2.px, color = Color.BLACK),
      layout = INNER,
  )

  private fun outer(child: Component, rtl: Boolean = false): Component = Layout(
      listOf(Column(style = Style.width(40.px).height(10.px)), child),
      style =
          Style.padding(start = 3.px)
              .margin(top = 1.px)
              .layoutDirection(if (rtl) YogaDirection.RTL else YogaDirection.LTR),
      layout = STACK,
  )

  /** The inner container's size, and its descendants' bounds relative to it, depth first. */
  private fun innerBounds(component: ResourcesScope.() -> Component): List<String> {
    val view = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { component() }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val inner = checkNotNull(find(root) { it.node.layoutFunction === INNER })
    return buildList { collect(inner, 0, 0, this) }
  }

  private fun find(
      result: LithoLayoutResult,
      matches: (LithoLayoutResult) -> Boolean,
  ): LithoLayoutResult? {
    if (matches(result)) {
      return result
    }
    for (i in 0 until result.childCount) {
      find(result.getChildAt(i), matches)?.let {
        return it
      }
    }
    return null
  }

  private fun collect(result: LithoLayoutResult, x: Int, y: Int, into: MutableList<String>) {
    into.add("${result.node.headComponent.simpleName} $x,$y ${result.width}x${result.height}")
    for (i in 0 until result.childCount) {
      collect(
          result.getChildAt(i),
          x + result.getXForChildAtIndex(i),
          y + result.getYForChildAtIndex(i),
          into,
      )
    }
  }

  /** Where a sibling with no baseline of its own sits beside [subject] in a baseline row. */
  private fun siblingY(subject: ResourcesScope.() -> Component): Int {
    val view =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Row(alignItems = YogaAlign.BASELINE) {
            child(subject())
            child(Column(style = Style.width(20.px).height(SIBLING.px)))
          }
        }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val row = if (root.childCount == 2) root else root.getChildAt(0)
    return row.getYForChildAtIndex(1)
  }

  private companion object {
    const val VIEWPORT = 400
    const val SIBLING = 40

    /** The inner container's padding (5) plus its border (2). */
    const val INSET = 7

    /** Stacks the children from the start edge, so a right-to-left container mirrors them. */
    val STACK = LayoutFunctionN { measurables, constraints ->
      stackChildren(measurables, constraints)
    }

    /**
     * [STACK] again, written out separately: a lambda that captures nothing is a single shared
     * instance, so this is how the inner container gets a function of its own to be found by.
     */
    val INNER = LayoutFunctionN { measurables, constraints ->
      stackChildren(measurables, constraints)
    }

    fun MeasureScope.stackChildren(measurables: List<Measurable>, constraints: SizeConstraints) {
      val placeables = measurables.map {
        it.measure(
            SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight),
        )
      }
      layout(placeables.maxOf { it.width }, placeables.sumOf { it.height }) {
        var y = 0
        placeables.forEach { placeable ->
          placeable.placeRelative(0, y)
          y += placeable.height
        }
      }
    }
  }
}
