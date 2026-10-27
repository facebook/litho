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
import com.facebook.litho.core.paddingPercent
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaUnit
import com.facebook.yoga.YogaValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Places its first child at its full width, as tall as the child measures. */
private val FIRST_CHILD_AT_FULL_WIDTH = LayoutFunctionN { measurables, constraints ->
  val placeable =
      measurables[0].measure(
          SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight),
      )
  layout(constraints.maxWidth, placeable.height) { placeable.place(0, 0) }
}

/** What the inspector shows for a `Layout`, its children and a plain Column. */
@RunWith(LithoTestRunner::class)
class CustomLayoutInspectorTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `the inspector shows a Layout child at its laid-out size and resolved margins`() {
    val leaf = SimpleMountSpecTester(style = Style.width(30.px).height(20.px).margin(start = 7.px))

    val view =
        lithoTestRule.render(widthPx = 200, heightPx = 200) {
          Layout(listOf(leaf), layout = FIRST_CHILD_AT_FULL_WIDTH)
        }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val child = DebugLayoutNode(root.getChildAt(0))

    assertThat(child.layoutWidth).isEqualTo(30f)
    assertThat(child.layoutHeight).isEqualTo(20f)
    assertThat(child.getLayoutMargin(YogaEdge.LEFT)).isEqualTo(7f)
    assertThat(child.getLayoutMargin(YogaEdge.START)).isEqualTo(7f)
    assertThat(child.width)
        .describedAs("its declared width")
        .isEqualTo(YogaValue(30f, YogaUnit.POINT))
    assertThat(child.getMargin(YogaEdge.START))
        .describedAs("its declared margin")
        .isEqualTo(YogaValue(7f, YogaUnit.POINT))
  }

  /**
   * A `Layout`'s child laid out as a Yoga tree shows the box style it declares, and Yoga's default
   * for the rest: its Yoga node holds the box it was laid out in.
   */
  @Test
  fun `the inspector shows a Layout's Row child's undeclared box style as Yoga's default`() {
    val row =
        Row(style = Style.height(20.px).padding(all = 4.px)) {
          child(SimpleMountSpecTester(style = Style.width(30.px).height(10.px)))
        }

    val view =
        lithoTestRule.render(widthPx = 200, heightPx = 200) {
          Layout(listOf(row), layout = FIRST_CHILD_AT_FULL_WIDTH)
        }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val child = DebugLayoutNode(root.getChildAt(0))

    assertThat(child.width)
        .describedAs("undeclared width: Yoga's default, not the box it was laid out in")
        .isEqualTo(YogaValue.AUTO)
    assertThat(child.getPadding(YogaEdge.LEFT))
        .describedAs("padding declared only on ALL, on LEFT")
        .isEqualTo(YogaValue.UNDEFINED)
    assertThat(child.getPadding(YogaEdge.ALL))
        .describedAs("its declared padding")
        .isEqualTo(YogaValue(4f, YogaUnit.POINT))
    assertThat(child.height)
        .describedAs("its declared height")
        .isEqualTo(YogaValue(20f, YogaUnit.POINT))
  }

  /** Its style is on the Yoga node its Column laid it out with, which the inspector reads. */
  @Test
  fun `the inspector shows a Layout in a Column with its flexbox style and Yoga's padding`() {
    for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
      val view =
          lithoTestRule.render(widthPx = 333, heightPx = 200) {
            Column(style = Style.layoutDirection(direction)) {
              child(
                  Layout(
                      listOf(SimpleMountSpecTester(style = Style.width(30.px).height(20.px))),
                      style =
                          Style.flex(grow = 1f).margin(start = 8.px).paddingPercent(start = 10f),
                      layout = FIRST_CHILD_AT_FULL_WIDTH,
                  ),
              )
            }
          }
      val root =
          checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
              as LithoLayoutResult
      val inspected = DebugLayoutNode(root.getChildAt(0))

      assertThat(inspected.flexGrow).describedAs("flex grow, $direction").isEqualTo(1f)
      assertThat(inspected.getMargin(YogaEdge.START))
          .describedAs("its declared margin, $direction")
          .isEqualTo(YogaValue(8f, YogaUnit.POINT))
      assertThat(inspected.getLayoutPadding(YogaEdge.START))
          .describedAs("Yoga's padding, unrounded, $direction")
          .isCloseTo(33.3f, within(0.01f))
    }
  }

  @Test
  fun `the inspector shows a plain Column's percent padding unrounded, in both directions`() {
    for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
      val view =
          lithoTestRule.render(widthPx = 333, heightPx = 200) {
            Column(style = Style.layoutDirection(direction).paddingPercent(start = 10f)) {
              child(SimpleMountSpecTester(style = Style.width(30.px).height(20.px)))
            }
          }
      val root =
          checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
              as LithoLayoutResult
      val inspected = DebugLayoutNode(root)

      assertThat(inspected.getLayoutPadding(YogaEdge.START))
          .describedAs("start padding, $direction")
          .isCloseTo(33.3f, within(0.01f))
      val physical = if (direction == YogaDirection.RTL) YogaEdge.RIGHT else YogaEdge.LEFT
      assertThat(inspected.getLayoutPadding(physical))
          .describedAs("the physical edge start resolves to, $direction")
          .isCloseTo(33.3f, within(0.01f))
    }
  }

  @Test
  fun `the inspector shows a Layout under a Layout at its margins, declared and resolved`() {
    val inner = Layout(
        listOf(SimpleMountSpecTester(style = Style.width(30.px).height(20.px))),
        style = Style.margin(start = 7.px, top = 3.px),
        layout = FIRST_CHILD_AT_FULL_WIDTH,
    )

    val view =
        lithoTestRule.render(widthPx = 200, heightPx = 200) {
          Layout(listOf(inner), layout = FIRST_CHILD_AT_FULL_WIDTH)
        }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val child = DebugLayoutNode(root.getChildAt(0))

    assertThat(child.getLayoutMargin(YogaEdge.START)).isEqualTo(7f)
    assertThat(child.getLayoutMargin(YogaEdge.TOP)).isEqualTo(3f)
    assertThat(child.getLayoutMargin(YogaEdge.END)).isEqualTo(0f)
    assertThat(child.getLayoutMargin(YogaEdge.BOTTOM)).isEqualTo(0f)
    val defaults = NodeConfig.createYogaNode()
    assertThat(child.flexDirection).describedAs("Yoga's default").isEqualTo(defaults.flexDirection)
    assertThat(child.getMargin(YogaEdge.START))
        .describedAs("its declared margin")
        .isEqualTo(YogaValue(7f, YogaUnit.POINT))
    assertThat(child.getMargin(YogaEdge.END))
        .describedAs("Yoga's default")
        .isEqualTo(defaults.getMargin(YogaEdge.END))
  }

  /**
   * A plain Column's laid-out border on every edge, in both directions. `Border` takes physical
   * edges, so start and end follow the direction.
   */
  @Test
  fun `the inspector shows a plain Column's border on every edge, in both directions`() {
    for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
      val view =
          lithoTestRule.render(widthPx = 200, heightPx = 200) {
            Column(
                style =
                    Style.layoutDirection(direction)
                        .border(
                            Border.create(lithoTestRule.context)
                                .widthPx(YogaEdge.LEFT, 1)
                                .widthPx(YogaEdge.TOP, 2)
                                .widthPx(YogaEdge.RIGHT, 3)
                                .widthPx(YogaEdge.BOTTOM, 4)
                                .color(YogaEdge.ALL, Color.BLACK)
                                .build(),
                        ),
            ) {
              child(SimpleMountSpecTester(style = Style.width(30.px).height(20.px)))
            }
          }
      val root =
          checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
              as LithoLayoutResult
      val inspected = DebugLayoutNode(root)
      val isRtl = direction == YogaDirection.RTL
      val expected = mapOf(
          YogaEdge.LEFT to 1f,
          YogaEdge.TOP to 2f,
          YogaEdge.RIGHT to 3f,
          YogaEdge.BOTTOM to 4f,
          YogaEdge.START to if (isRtl) 3f else 1f,
          YogaEdge.END to if (isRtl) 1f else 3f,
      )

      for ((edge, width) in expected) {
        assertThat(inspected.getLayoutBorderWidth(edge))
            .describedAs("laid-out border, $edge, $direction")
            .isEqualTo(width)
      }
    }
  }

  /**
   * A `Layout` under a `Layout`, with padding, a border and RTL, shows the edges it was laid out
   * with on every edge, as a Column in its place does, and its border on the physical edges
   * `Border` declares.
   */
  @Test
  fun `the inspector shows a Layout under a Layout's padding and border, in RTL`() {
    val style =
        Style.layoutDirection(YogaDirection.RTL)
            .padding(start = 5.px, top = 6.px, end = 7.px, bottom = 8.px)
            .border(
                Border.create(lithoTestRule.context)
                    .widthPx(YogaEdge.LEFT, 1)
                    .widthPx(YogaEdge.TOP, 2)
                    .widthPx(YogaEdge.RIGHT, 3)
                    .widthPx(YogaEdge.BOTTOM, 4)
                    .color(YogaEdge.ALL, Color.BLACK)
                    .build(),
            )
    val leaf = { SimpleMountSpecTester(style = Style.width(30.px).height(20.px)) }
    fun inspectedChild(child: Component): DebugLayoutNode {
      val view =
          lithoTestRule.render(widthPx = 200, heightPx = 200) {
            Layout(listOf(child), layout = FIRST_CHILD_AT_FULL_WIDTH)
          }
      val root =
          checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
              as LithoLayoutResult
      return DebugLayoutNode(root.getChildAt(0))
    }
    val layout =
        inspectedChild(Layout(listOf(leaf()), style = style, layout = FIRST_CHILD_AT_FULL_WIDTH))
    val column = inspectedChild(Column(style = style) { child(leaf()) })
    val edges = listOf(
        YogaEdge.LEFT,
        YogaEdge.TOP,
        YogaEdge.RIGHT,
        YogaEdge.BOTTOM,
        YogaEdge.START,
        YogaEdge.END,
    )

    for (edge in edges) {
      assertThat(layout.getLayoutPadding(edge))
          .describedAs("padding, $edge")
          .isEqualTo(column.getLayoutPadding(edge))
      assertThat(layout.getLayoutBorderWidth(edge))
          .describedAs("laid-out border, $edge")
          .isEqualTo(column.getLayoutBorderWidth(edge))
    }
    for (edge in edges.take(4)) {
      assertThat(layout.getBorderWidth(edge))
          .describedAs("border, $edge")
          .isEqualTo(column.getBorderWidth(edge))
    }
    assertThat(layout.getLayoutPadding(YogaEdge.LEFT))
        .describedAs("RTL: end padding on the left")
        .isEqualTo(7f)
    assertThat(layout.getLayoutBorderWidth(YogaEdge.START))
        .describedAs("RTL: start is the right border")
        .isEqualTo(3f)
  }
}
