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

package com.facebook.litho.parity

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.Layout
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.marginPercent
import com.facebook.litho.core.maxWidth
import com.facebook.litho.core.minWidth
import com.facebook.litho.core.padding
import com.facebook.litho.core.paddingPercent
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign

/**
 * Trees for the custom layout benchmarks, each built two ways that lay out identically: once with a
 * custom [Layout] and once with the flexbox container it stands in for. Kept out of the benchmark
 * classes so the correctness test and the timing benchmark lay out the same trees.
 */
class CustomLayoutWorkload(context: Context) {

  private val c = ComponentContext(context)

  /** Lays [component] out [widthPx] wide, synchronously, and returns the root result. */
  fun layOut(component: Component, widthPx: Int = WIDTH): LithoLayoutResult {
    val tree = ComponentTree.create(c).build()
    tree.setRootAndSizeSpecSync(
        component,
        SizeSpec.makeSizeSpec(widthPx, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
    )
    val result = checkNotNull(tree.mainThreadLayoutState?.rootLayoutResult) as LithoLayoutResult
    tree.release()
    return result
  }

  /** A tree kept alive for re-layout at alternating widths. */
  fun treeFor(component: Component): ComponentTree =
      ComponentTree.create(c, component).build().also {
        it.setSizeSpec(SizeSpec.makeSizeSpec(WIDTH, SizeSpec.EXACTLY), UNSPECIFIED)
      }

  fun relayout(tree: ComponentTree, widthPx: Int) {
    tree.setSizeSpec(SizeSpec.makeSizeSpec(widthPx, SizeSpec.EXACTLY), UNSPECIFIED)
  }

  /** [count] fixed-size children side by side, each with a margin. */
  fun flatRowCustom(count: Int): Component = Layout(List(count) { leaf(it) }, layout = SIDE_BY_SIDE)

  // Children keep their width, as the custom layout's do: a Row otherwise shrinks them to fit.
  fun flatRowFlexbox(count: Int): Component = Row {
    repeat(count) { child(leaf(it, Style.flex(shrink = 0f))) }
  }

  /** Containers nested [depth] deep, each holding two leaves and the next level. */
  fun nestedCustom(depth: Int): Component =
      if (depth == 0) leaf(0)
      else Layout(listOf(leaf(depth), leaf(depth + 1), nestedCustom(depth - 1)), layout = STACKED)

  fun nestedFlexbox(depth: Int): Component =
      if (depth == 0) leaf(0)
      else
          Column(alignItems = YogaAlign.FLEX_START) {
            child(leaf(depth))
            child(leaf(depth + 1))
            child(nestedFlexbox(depth - 1))
          }

  /** One child with its own box props, asked a question by a custom layout or by a Column. */
  class BoxCase(
      val name: String,
      val room: Int,
      val exact: Boolean,
      val style: Style,
      val contentWidth: Int = 50,
  )

  fun boxCaseCustom(case: BoxCase): Component {
    val ask = SizeConstraints(
        minWidth = if (case.exact) case.room else 0,
        maxWidth = case.room,
        minHeight = 0,
        maxHeight = SizeConstraints.Infinity,
    )
    return Layout(
        listOf(boxSubject(case)),
        style = Style.width(case.room.px),
        layout =
            LayoutFunctionN { measurables, constraints ->
              val p = measurables[0].measure(ask)
              layout(constraints.maxWidth, p.height) { p.place(0, 0) }
            },
    )
  }

  fun boxCaseFlexbox(case: BoxCase): Component = Column(
      alignItems = if (case.exact) null else YogaAlign.FLEX_START,
      style = Style.width(case.room.px),
  ) {
    child(boxSubject(case))
  }

  private fun boxSubject(case: BoxCase): Component =
      Column(style = case.style) {
        child(Column(style = Style.width(case.contentWidth.px).height(20.px)))
      }

  private fun leaf(index: Int, extra: Style = Style): Component = Column(
      style =
          extra
              .width((LEAF + index % 3).px)
              .height(LEAF.px)
              .margin(horizontal = MARGIN.px, vertical = MARGIN.px),
  )

  companion object {
    const val WIDTH: Int = 1080
    const val NARROW: Int = 720
    const val LEAF: Int = 40
    const val MARGIN: Int = 4
    private val UNSPECIFIED: Int
      get() = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED)

    /**
     * The situations measured in D122235564's test plan where a child comes out the same in a
     * custom layout as in a Column. The two that differ by design (a min above a max, and a
     * flexbox-only prop) are left out.
     */
    val BOX_CASES: List<BoxCase> = listOf(
        BoxCase("A1 no size, at most", 100, false, Style),
        BoxCase("A2 no size, exactly", 100, true, Style),
        BoxCase("B1 width 80", 100, false, Style.width(80.px)),
        BoxCase("B2 width 150 above the room", 100, false, Style.width(150.px)),
        BoxCase("B3 width 80, exactly 100", 100, true, Style.width(80.px)),
        BoxCase("B4 width 50%", 200, false, Style.widthPercent(50f)),
        BoxCase("C1 min 120, at most 80", 80, false, Style.minWidth(120.px)),
        BoxCase("C2 min 120, exactly 80", 80, true, Style.minWidth(120.px)),
        BoxCase("C3 max 60", 100, false, Style.maxWidth(60.px), contentWidth = 140),
        BoxCase("C4 max 60, exactly 100", 100, true, Style.maxWidth(60.px), contentWidth = 140),
        BoxCase(
            "D1 width 100 + margin 10",
            100,
            false,
            Style.width(100.px).margin(horizontal = 10.px),
        ),
        BoxCase("D2 margin 10", 100, false, Style.margin(horizontal = 10.px)),
        BoxCase("D3 margin 10, exactly 100", 100, true, Style.margin(horizontal = 10.px)),
        BoxCase("D4 margin 10%", 200, false, Style.marginPercent(horizontal = 10f)),
        BoxCase("E1 padding 10", 100, false, Style.padding(horizontal = 10.px)),
        BoxCase("E2 padding 10%", 300, false, Style.paddingPercent(horizontal = 10f)),
        BoxCase("E3 border 5", 100, false, Style.border(width = 5.px, color = Color.BLACK)),
        BoxCase("F1 width 100, aspect 2", 300, false, Style.width(100.px).aspectRatio(2f)),
        BoxCase("F2 aspect 2, exactly 100", 100, true, Style.aspectRatio(2f)),
    )

    /** Children left to right at their own size, as a `Row` lays them out. */
    val SIDE_BY_SIDE = LayoutFunctionN { measurables, _ ->
      val placeables = measurables.map {
        it.measure(SizeConstraints(maxWidth = SizeConstraints.Infinity))
      }
      layout(placeables.sumOf { it.width }, placeables.maxOfOrNull { it.height } ?: 0) {
        var x = 0
        placeables.forEach { placeable ->
          placeable.place(x, 0)
          x += placeable.width
        }
      }
    }

    /** Children top to bottom at their own size, as a `Column` that does not stretch them. */
    val STACKED = LayoutFunctionN { measurables, constraints ->
      val placeables = measurables.map {
        it.measure(SizeConstraints(maxWidth = constraints.maxWidth))
      }
      layout(placeables.maxOfOrNull { it.width } ?: 0, placeables.sumOf { it.height }) {
        var y = 0
        placeables.forEach { placeable ->
          placeable.place(0, y)
          y += placeable.height
        }
      }
    }

    /**
     * Every node under [root] as absolute bounds, in tree order: leaves only when [leavesOnly],
     * since a flexbox container and a custom layout may size themselves differently around the same
     * children.
     */
    fun bounds(root: LithoLayoutResult, leavesOnly: Boolean): List<Rect> {
      val out = ArrayList<Rect>()
      fun walk(result: LithoLayoutResult, x: Int, y: Int) {
        for (i in 0 until result.childCount) {
          val child = result.getChildAt(i)
          val cx = x + result.getXForChildAtIndex(i)
          val cy = y + result.getYForChildAtIndex(i)
          if (!leavesOnly || child.childCount == 0) {
            out.add(Rect(cx, cy, cx + child.width, cy + child.height))
          }
          walk(child, cx, cy)
        }
      }
      walk(root, 0, 0)
      return out
    }
  }
}
