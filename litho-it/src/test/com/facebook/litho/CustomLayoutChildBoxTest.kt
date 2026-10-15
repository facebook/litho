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
import android.graphics.Rect
import com.facebook.litho.YogaLayoutOutput.Companion.getYogaNode
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.marginPercent
import com.facebook.litho.core.maxHeight
import com.facebook.litho.core.maxWidth
import com.facebook.litho.core.maxWidthPercent
import com.facebook.litho.core.minHeight
import com.facebook.litho.core.minWidth
import com.facebook.litho.core.minWidthPercent
import com.facebook.litho.core.padding
import com.facebook.litho.core.paddingPercent
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.Placeable
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val VIEWPORT = 1000
private const val CHILD = 100
private const val PARITY_WIDTH = 300
private const val ROW_CONTENT_WIDTH = 30
private const val ROW_CONTENT_HEIGHT = 25

/** Child styles a container lays out exactly as a Column does, stretched or not. */
private val COLUMN_PARITY: List<Pair<String, Style>> = listOf(
    "no props" to Style,
    "padding by logical edge" to
        Style.padding(start = 7.px, top = 5.px, end = 11.px, bottom = 3.px),
    "percent padding" to Style.paddingPercent(all = 5f),
    "percent start padding" to Style.paddingPercent(start = 10f),
    "margins by logical edge" to Style.margin(start = 7.px, top = 5.px, end = 11.px, bottom = 3.px),
    "percent margins" to Style.marginPercent(all = 5f),
    "percent vertical margins" to Style.marginPercent(vertical = 10f),
    "negative start margin" to Style.margin(start = (-9).px),
    "min width" to Style.minWidth(200.px),
    "percent min width" to Style.minWidthPercent(80f),
    "min and max height" to Style.minHeight(40.px).maxHeight(60.px),
    "width" to Style.width(80.px),
    "percent width" to Style.widthPercent(50f),
    "width, margins and padding" to
        Style.width(120.px).margin(start = 10.px, end = 4.px).padding(start = 6.px),
    "max width" to Style.maxWidth(20.px),
    "percent max width" to Style.maxWidthPercent(10f),
    "max width and margins" to Style.maxWidth(50.px).margin(start = 10.px, end = 10.px),
    "aspect ratio with a width" to Style.width(80.px).aspectRatio(2f),
    "aspect ratio with a height" to Style.height(40.px).aspectRatio(2f),
    "aspect ratio with a percent width" to Style.widthPercent(20f).aspectRatio(0.5f),
)

/**
 * Rows that differ from a stretching Column by a rule (see the next test). Under an exact width, a
 * child whose own width, min or max decides its width is coerced into that width and drawn centred
 * in it, where a Column puts it at the leading edge; exact constraints also override a width
 * derived from an aspect ratio, and a percentage width fills an exact room.
 */
private val UNSTRETCHED_ONLY: Set<String> = setOf(
    "min width",
    "percent min width",
    "width",
    "percent width",
    "width, margins and padding",
    "max width",
    "percent max width",
    "max width and margins",
    "aspect ratio with a width",
    "aspect ratio with a height",
    "aspect ratio with a percent width",
)

/** Measures the only child under [constraints] and places it at the origin. */
private fun measuringUnder(constraints: (SizeConstraints) -> SizeConstraints): LayoutFunctionN =
    LayoutFunctionN { ms, c ->
      val p = ms[0].measure(constraints(c))
      layout(c.maxWidth, p.height) { p.place(0, 0) }
    }

/**
 * A custom layout applies a child's own box props itself — margins, size, min and max, aspect
 * ratio, padding and border — within the constraints it measures the child under, and Yoga lays out
 * only the child's content.
 */
@RunWith(LithoTestRunner::class)
class CustomLayoutChildBoxTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a vertical percent margin is taken of the constraints' width, as in CSS`() {
    val child = SimpleMountSpecTester(style = Style.height(CHILD.px).marginPercent(top = 10f))

    val result = renderAndFindContainer(
        Layout(
            listOf(child),
            layout = measuringUnder { SizeConstraints(0, 200, 0, VIEWPORT) },
        ),
    )

    assertThat(result.getYForChildAtIndex(0))
        .describedAs("10% of the 200 width, not of the height")
        .isEqualTo(20)
  }

  @Test
  fun `a percent padding is resolved against the constraints the child is placed with`() {
    val child = SimpleMountSpecTester(style = Style.height(CHILD.px).paddingPercent(all = 5f))
    val layout = LayoutFunctionN { ms, c ->
      ms[0].measure(SizeConstraints(0, 400, 0, CHILD * 2))
      val p = ms[0].measure(SizeConstraints(0, 200, 0, CHILD * 2))
      layout(c.maxWidth, p.height) { p.place(0, 0) }
    }

    val laidOut = renderAndFindContainer(Layout(listOf(child), layout = layout)).getChildAt(0)

    assertThat(laidOut.paddingLeft)
        .describedAs("5% of 200, not of the 400 asked first")
        .isEqualTo(10)
  }

  @Test
  fun `a child's content keeps the direction it inherits`() {
    val row =
        Row(style = Style.width(CHILD.px)) {
          child(SimpleMountSpecTester(style = Style.width(10.px).height(10.px)))
          child(SimpleMountSpecTester(style = Style.width(20.px).height(10.px)))
        }
    val rtl =
        Column(style = Style.layoutDirection(YogaDirection.RTL)) {
          child(Layout(listOf(row), layout = measuringUnder { it.copy(minWidth = 0) }))
        }

    val laidOutRow = renderAndFindContainer(rtl).getChildAt(0)

    assertThat(laidOutRow.getXForChildAtIndex(0))
        .describedAs("the first item of an RTL row sits at its right edge")
        .isEqualTo(CHILD - 10)
  }

  @Test
  fun `padding that cannot fit answers the smallest box the child can have`() {
    val child = SimpleMountSpecTester(style = Style.padding(all = 8.px))

    val result = renderAndFindContainer(
        Layout(listOf(child), layout = measuringUnder { SizeConstraints.exact(10, 10) }),
    )

    assertThat(result.getChildAt(0).width).isEqualTo(16)
  }

  @Test
  fun `a flexbox-item prop on a container child is not written to its node`() {
    val child =
        SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px).flex(grow = 1f))

    val result = renderAndFindContainer(
        Layout(listOf(child), layout = measuringUnder { it.copy(minWidth = 0) }),
    )

    assertThat(result.getChildAt(0).getYogaNode().flexGrow).isEqualTo(0f)
    assertThat(result.getChildAt(0).width).isEqualTo(CHILD)
  }

  /**
   * The inspector shows a container child's border on every edge: the declared width where there is
   * one, and what the node was laid out with — 0 — where there is none.
   */
  @Test
  fun `the inspector shows a container child's border`() {
    val border =
        Border.create(lithoTestRule.context)
            .widthPx(YogaEdge.LEFT, 4)
            .color(YogaEdge.LEFT, Color.RED)
            .build()
    val bordered =
        SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px).border(border))
    val plain = SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))
    val stack = LayoutFunctionN { ms, c ->
      val ps = ms.map {
        it.measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      }
      layout(c.maxWidth, CHILD) { ps.forEach { it.place(0, 0) } }
    }

    val result = renderAndFindContainer(Layout(listOf(bordered, plain), layout = stack))

    assertThat(DebugLayoutNode(result.getChildAt(0)).getBorderWidth(YogaEdge.LEFT)).isEqualTo(4f)
    assertThat(DebugLayoutNode(result.getChildAt(0)).getBorderWidth(YogaEdge.TOP)).isEqualTo(0f)
    assertThat(DebugLayoutNode(result.getChildAt(1)).getBorderWidth(YogaEdge.LEFT)).isEqualTo(0f)
  }

  @Test
  fun `a child with margins is laid out once for its first measurement`() {
    val child =
        SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px).margin(all = 7.px))
    val layout = LayoutFunctionN { ms, c ->
      val before = LithoStats.customLayoutChildLayoutCount.get()
      val p = ms[0].measure(SizeConstraints(0, 300, 0, 300))
      // A later run of the function is answered from this pass's memo and lays nothing out.
      val layouts = LithoStats.customLayoutChildLayoutCount.get() - before
      check(layouts <= 1L) { "laid out $layouts times" }
      layout(c.maxWidth, p.height) { p.place(0, 0) }
    }

    val result = renderAndFindContainer(Layout(listOf(child), layout = layout))

    assertThat(result.getXForChildAtIndex(0)).isEqualTo(7)
    assertThat(result.getChildAt(0).width).isEqualTo(CHILD)
  }

  // ─────────────────────────────────────────────── against Yoga

  /**
   * A child laid out by a container lands exactly where Yoga puts the same child in a Column that
   * offers it the same room, content included, in both directions.
   *
   * The container asks with the Column's width as an upper bound, as a Column that does not stretch
   * its children offers it, or as an exact width, as one that stretches them does. The Column rows
   * only use props an exact width does not override. Each child is a small row, so its content's
   * own layout is compared too, not just the child's box.
   */
  @Test
  fun `a child lays out as Yoga lays it out in a Column`() {
    val mismatches = ArrayList<String>()
    for ((name, style) in COLUMN_PARITY) {
      for (stretch in listOf(false, true)) {
        if (stretch && name in UNSTRETCHED_ONLY) continue
        for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
          val box = subtreeBounds(inContainer(boxChild(style), stretch), direction)
          val yoga = subtreeBounds(inColumn(boxChild(style), stretch), direction)
          if (box != yoga) {
            mismatches.add("[$name] stretch=$stretch $direction\n  box : $box\n  yoga: $yoga")
          }
        }
      }
    }
    for ((name, child) in parityLeaves()) {
      for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
        val box = subtreeBounds(inContainer(child(), stretch = false), direction)
        val yoga = subtreeBounds(inColumn(child(), stretch = false), direction)
        if (box != yoga) mismatches.add("[$name] $direction\n  box : $box\n  yoga: $yoga")
      }
    }
    assertThat(mismatches).isEmpty()
  }

  /**
   * Where a container child deliberately differs from the same child in a Column. Each difference
   * is one of the child box's rules; a change here is a change to the contract, not a fix.
   */
  @Test
  fun `a child differs from a Column only where its box rules say so`() {
    fun boxWidth(style: Style, stretch: Boolean) =
        subtreeBounds(inContainer(boxChild(style), stretch), YogaDirection.LTR)[0].width()
    fun columnWidth(style: Style, stretch: Boolean) =
        subtreeBounds(inColumn(boxChild(style), stretch), YogaDirection.LTR)[0].width()

    fun boxLeft(style: Style, stretch: Boolean) =
        subtreeBounds(inContainer(boxChild(style), stretch), YogaDirection.LTR)[0].left
    fun columnLeft(style: Style, stretch: Boolean) =
        subtreeBounds(inColumn(boxChild(style), stretch), YogaDirection.LTR)[0].left

    // A declared width, an own minimum and an own maximum win over the constraints, as in a
    // Column. Outside the constraints the child is then coerced into them and drawn centred, where
    // a Column puts it at the leading edge.
    assertThat(boxWidth(Style.width(80.px), stretch = true)).isEqualTo(80)
    assertThat(columnWidth(Style.width(80.px), stretch = true)).isEqualTo(80)
    assertThat(boxLeft(Style.width(80.px), stretch = true))
        .describedAs("80 centred in the exact 300")
        .isEqualTo(columnLeft(Style.width(80.px), stretch = true) + (PARITY_WIDTH - 80) / 2)
    assertThat(boxWidth(Style.minWidth(400.px), stretch = false)).isEqualTo(400)
    assertThat(columnWidth(Style.minWidth(400.px), stretch = false)).isEqualTo(400)
    assertThat(boxLeft(Style.minWidth(400.px), stretch = false))
        .describedAs("400 centred in the 300 it was allowed: 50 past each side")
        .isEqualTo(columnLeft(Style.minWidth(400.px), stretch = false) - 50)
    assertThat(boxWidth(Style.maxWidth(80.px), stretch = true)).isEqualTo(80)
    assertThat(columnWidth(Style.maxWidth(80.px), stretch = true)).isEqualTo(80)

    // An own max beats an own min, as in Compose; a Column applies the min.
    assertThat(boxWidth(Style.minWidth(90.px).maxWidth(50.px), stretch = false)).isEqualTo(50)
    assertThat(columnWidth(Style.minWidth(90.px).maxWidth(50.px), stretch = false)).isEqualTo(90)

    // An own min replaces an exact minimum, so the content chooses; a Column stretches it.
    assertThat(boxWidth(Style.minWidth(200.px), stretch = true)).isEqualTo(200)
    assertThat(columnWidth(Style.minWidth(200.px), stretch = true)).isEqualTo(PARITY_WIDTH)

    // A percentage width fills an exact room as `fillMaxWidth` does; a Column keeps the percentage.
    assertThat(boxWidth(Style.widthPercent(50f), stretch = true)).isEqualTo(PARITY_WIDTH)
    assertThat(columnWidth(Style.widthPercent(50f), stretch = true)).isEqualTo(PARITY_WIDTH / 2)

    // Exact constraints win over a width derived from an aspect ratio. A Column that stretches its
    // children keeps the derived width.
    assertThat(boxWidth(Style.height(40.px).aspectRatio(2f), stretch = true))
        .isEqualTo(PARITY_WIDTH)
    assertThat(columnWidth(Style.height(40.px).aspectRatio(2f), stretch = true)).isEqualTo(80)

    // An aspect ratio needs one axis fixed by the constraints or the child's own size. A Column
    // also
    // derives the width from the height the content measured at.
    assertThat(boxWidth(Style.aspectRatio(2f), stretch = false)).isEqualTo(ROW_CONTENT_WIDTH)
    assertThat(columnWidth(Style.aspectRatio(2f), stretch = false))
        .isEqualTo(ROW_CONTENT_HEIGHT * 2)

    // A percentage of an axis the constraints leave unbounded counts as auto, as in CSS. A Column
    // takes it of the height it has available.
    val percentHeight =
        subtreeBounds(inContainer(boxChild(Style.heightPercent(50f)), false), YogaDirection.LTR)[0]
    assertThat(percentHeight.height()).isEqualTo(ROW_CONTENT_HEIGHT)
  }

  /** A small row, so a child's own content layout is compared, not only its box. */
  private fun boxChild(style: Style): Component =
      Row(style = style) {
        child(SimpleMountSpecTester(style = Style.width(10.px).height(10.px)))
        child(SimpleMountSpecTester(style = Style.width(20.px).height(ROW_CONTENT_HEIGHT.px)))
      }

  private fun parityLeaves(): List<Pair<String, () -> Component>> = listOf(
      "text with padding and margins" to
          {
            Text.create(lithoTestRule.context)
                .text("custom layout parity")
                .kotlinStyle(
                    Style.padding(start = 7.px, end = 11.px).margin(start = 5.px, top = 3.px),
                )
                .build()
          },
      "text with percent padding and a max width" to
          {
            Text.create(lithoTestRule.context)
                .text("custom layout parity")
                .kotlinStyle(Style.paddingPercent(start = 5f).maxWidth(60.px))
                .build()
          },
      "nested container with padding and margins" to
          {
            Layout(
                listOf(SimpleMountSpecTester(style = Style.width(40.px).height(12.px))),
                style = Style.padding(start = 6.px, top = 2.px).margin(end = 9.px),
                layout =
                    LayoutFunctionN { ms, c ->
                      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth))
                      layout(p.width, p.height) { p.place(0, 0) }
                    },
            )
          },
  )

  /**
   * A container sized to its only child and placing it at its origin, so the child lands where the
   * Column places it in either direction. It asks with the Column's width, as an upper bound or,
   * for a stretching Column, exactly.
   */
  private fun inContainer(child: Component, stretch: Boolean): Component = Layout(
      listOf(child),
      layout =
          LayoutFunctionN { ms, _ ->
            val constraints =
                if (stretch)
                    SizeConstraints(PARITY_WIDTH, PARITY_WIDTH, 0, SizeConstraints.Infinity)
                else SizeConstraints(maxWidth = PARITY_WIDTH)
            val p = ms[0].measure(constraints)
            layout(p.width, p.height) { p.place(0, 0) }
          },
  )

  private fun inColumn(child: Component, stretch: Boolean): Component = Column(
      alignItems = if (stretch) YogaAlign.STRETCH else YogaAlign.FLEX_START,
      style = Style.width(PARITY_WIDTH.px),
  ) {
    child(child)
  }

  /** Absolute bounds of the child and every node under it, in tree order. */
  private fun subtreeBounds(subject: Component, direction: YogaDirection): List<Rect> {
    val testView =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column(alignItems = YogaAlign.FLEX_START, style = Style.layoutDirection(direction)) {
            child(subject)
          }
        }
    val root =
        checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    // The container is sized to its child and the Column is not, so the walk starts at the child,
    // in absolute coordinates.
    val holder = root.getChildAt(0)
    val originX = root.getXForChildAtIndex(0)
    val originY = root.getYForChildAtIndex(0)
    val bounds = ArrayList<Rect>()
    fun walk(result: LithoLayoutResult, x: Int, y: Int) {
      bounds.add(Rect(x, y, x + result.width, y + result.height))
      for (i in 0 until result.childCount) {
        walk(
            result.getChildAt(i),
            x + result.getXForChildAtIndex(i),
            y + result.getYForChildAtIndex(i),
        )
      }
    }
    walk(
        holder.getChildAt(0),
        originX + holder.getXForChildAtIndex(0),
        originY + holder.getYForChildAtIndex(0),
    )
    return bounds
  }

  @Test
  fun `a child whose own edges cannot fit is kept within its room and centred`() {
    val child = SimpleMountSpecTester(style = Style.padding(all = 8.px))

    val result = renderAndFindContainer(
        Layout(listOf(child), layout = measuringUnder { SizeConstraints.exact(10, 10) }),
    )

    assertThat(result.getChildAt(0).width).describedAs("its padding floors it at 16").isEqualTo(16)
    assertThat(result.getXForChildAtIndex(0))
        .describedAs("16 centred in the 10 it occupies: 3 px past each side")
        .isEqualTo(-3)
  }

  /**
   * A child that takes more than it was allowed occupies what it was allowed, and is drawn centred
   * in it, overflowing both sides equally, as in Compose. Its real size is kept as its measured
   * size.
   */
  @Test
  fun `a child larger than its room occupies the room and is drawn centred in it`() {
    val seen = AtomicReference<Placeable>()
    val result = renderAndFindContainer(
        Layout(
            listOf(SimpleMountSpecTester(style = Style.width(150.px).height(CHILD.px))),
            layout =
                LayoutFunctionN { ms, c ->
                  val p = ms[0].measure(SizeConstraints(maxWidth = 100, maxHeight = c.maxHeight))
                  seen.set(p)
                  layout(p.width, p.height) { p.place(0, 0) }
                },
        ),
    )

    assertThat(seen.get().width).describedAs("the space it occupies").isEqualTo(100)
    assertThat(seen.get().measuredWidth).describedAs("the size it took").isEqualTo(150)
    assertThat(result.getChildAt(0).width).isEqualTo(150)
    assertThat(result.getXForChildAtIndex(0)).describedAs("25 past each side").isEqualTo(-25)
  }

  /**
   * A child smaller than an exact room is centred in it too. An odd difference truncates toward
   * zero, as Compose's does, so the extra pixel goes to the right.
   */
  @Test
  fun `a child smaller than an exact room is centred, the odd pixel to the right`() {
    val result = renderAndFindContainer(
        Layout(
            listOf(SimpleMountSpecTester(style = Style.width(81.px).height(CHILD.px))),
            layout = measuringUnder { c -> SizeConstraints(100, 100, 0, c.maxHeight) },
        ),
    )

    assertThat(result.getChildAt(0).width).isEqualTo(81)
    assertThat(result.getXForChildAtIndex(0)).describedAs("19 / 2 truncates to 9").isEqualTo(9)
  }

  private fun renderAndFindContainer(component: Component): LithoLayoutResult {
    val testView =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          if (component is Layout) Column { child(component) } else component
        }
    val root = checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
    return checkNotNull(findContainer(root as LithoLayoutResult)) { "container not found" }
  }

  private fun findContainer(result: LithoLayoutResult): LithoLayoutResult? {
    if (result.node.tailComponent is Layout) return result
    for (i in 0 until result.childCount) {
      findContainer(result.getChildAt(i))?.let {
        return it
      }
    }
    return null
  }
}
