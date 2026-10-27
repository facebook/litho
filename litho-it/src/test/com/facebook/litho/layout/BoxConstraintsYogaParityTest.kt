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

package com.facebook.litho.layout

import com.facebook.litho.NodeConfig
import com.facebook.litho.SizeSpec
import com.facebook.litho.layout.BoxConstraintsCases.text
import com.facebook.litho.layout.internal.layOutWithin
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaMeasureFunction
import com.facebook.yoga.YogaMeasureMode
import com.facebook.yoga.YogaMeasureOutput
import com.facebook.yoga.YogaNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The child box against Yoga itself: for the same declared props, a container child's resolved
 * edges are the edges Yoga resolves for that child in a Column of the same width. Sizes are
 * compared where the classes that lay a child out arrive, in the diff above.
 */
@RunWith(LithoTestRunner::class)
class BoxConstraintsYogaParityTest {

  /**
   * Covers every [BoxEdge] a prop can be declared on, pixels and percent, negative values, a later
   * declaration replacing an earlier one, and both directions. Yoga takes an edge's percentage of
   * the width its parent lays the child out in, which is the bound a container asks under; without
   * a bound both take it as 0.
   */
  @Test
  fun `resolved edges are the edges Yoga resolves for the same child`() {
    val mismatches = ArrayList<String>()
    var checked = 0
    for (declared in BoxConstraintsCases.generated(count = 400, seed = 11)) {
      for (width in listOf(200, 333, null)) {
        for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
          checked++
          mismatch(declared, width, direction)?.let(mismatches::add)
        }
      }
    }
    assertThat(checked).isEqualTo(400 * 3 * 2)
    assertThat(mismatches).isEmpty()
  }

  /** Yoga treats a NaN or infinite edge as undeclared, so the edges behind it apply. */
  @Test
  fun `an edge that is not a finite number resolves as Yoga resolves it`() {
    val mismatches = ArrayList<String>()
    var checked = 0
    for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
      for (isPercent in listOf(false, true)) {
        val declared =
            BoxConstraintsCases.Declared(
                margins =
                    listOf(
                        BoxConstraintsCases.EdgeDecl(BoxEdge.ALL, 10f),
                        BoxConstraintsCases.EdgeDecl(BoxEdge.LEFT, value, isPercent),
                    ),
                paddings =
                    listOf(
                        BoxConstraintsCases.EdgeDecl(BoxEdge.ALL, 4f),
                        BoxConstraintsCases.EdgeDecl(BoxEdge.START, value, isPercent),
                    ),
                borders =
                    listOf(
                        BoxConstraintsCases.EdgeDecl(BoxEdge.ALL, 2f),
                        BoxConstraintsCases.EdgeDecl(BoxEdge.TOP, value),
                    ),
            )
        for (width in listOf(200, null)) {
          for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
            checked++
            mismatch(declared, width, direction)?.let(mismatches::add)
          }
        }
      }
    }
    assertThat(checked).isEqualTo(3 * 2 * 2 * 2)
    assertThat(mismatches).isEmpty()
  }

  /** How [declared]'s resolved edges differ from Yoga's in a Column [width] wide, or null. */
  private fun mismatch(
      declared: BoxConstraintsCases.Declared,
      width: Int?,
      direction: YogaDirection,
  ): String? {
    val yoga = layOutInColumn(declared, width, direction)
    val box = resolveBoxConstraints(
        SizeConstraints(0, width ?: SizeConstraints.Infinity, 0, SizeConstraints.Infinity),
        declared.build(),
        isRtl = direction == YogaDirection.RTL,
    )
    val expected =
        "margins ${edges(yoga::getLayoutMargin)} padding ${edges(yoga::getLayoutPadding)} " +
            "border ${edges(yoga::getLayoutBorder)}"
    val actual =
        "margins ${text(box.margins)} padding ${text(box.padding)} border ${text(box.border)}"
    return if (actual == expected) null
    else "$declared width=$width $direction\n  box : $actual\n  yoga: $expected"
  }

  /** Lays [declared]'s edges out on a Yoga child of a Column [width] wide, or content-sized. */
  private fun layOutInColumn(
      declared: BoxConstraintsCases.Declared,
      width: Int?,
      direction: YogaDirection,
  ): YogaNode {
    val parent = NodeConfig.createYogaNode()
    parent.setDirection(direction)
    parent.alignItems = YogaAlign.FLEX_START
    if (width != null) {
      parent.setWidth(width.toFloat())
    }
    val child = NodeConfig.createYogaNode()
    declareEdges(child, declared)
    parent.addChildAt(child, 0)
    parent.calculateLayout(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED)
    return child
  }

  private fun edges(layout: (YogaEdge) -> Float): String {
    fun px(edge: YogaEdge): Int = FastMath.round(layout(edge))
    return "(${px(YogaEdge.LEFT)}, ${px(YogaEdge.TOP)}, ${px(YogaEdge.RIGHT)}, ${px(YogaEdge.BOTTOM)})"
  }

  /**
   * For generated box props under a range of constraints, in both directions, the size
   * [borderBoxSize] gives a leaf is the size a Yoga pass gives it: [borderBoxSize] against
   * `layOutWithin` on a Yoga leaf with the same content.
   */
  @Test
  fun `a leaf measured with no Yoga node is sized as a Yoga pass sizes it`() {
    val constraintsToTry = listOf(
        SizeConstraints(0, 200, 0, SizeConstraints.Infinity),
        SizeConstraints(0, 333, 0, SizeConstraints.Infinity),
        SizeConstraints(200, 200, 0, SizeConstraints.Infinity),
        SizeConstraints(),
        SizeConstraints(0, SizeConstraints.Infinity, 0, 90),
        SizeConstraints(0, SizeConstraints.Infinity, 90, 90),
        SizeConstraints(120, 120, 80, 80),
        SizeConstraints(0, 150, 0, 60),
        SizeConstraints(40, 150, 30, 60),
        SizeConstraints(0, 12, 0, 8),
    )
    val contents = listOf(30 to 25, 0 to 0, 400 to 300)
    val mismatches = ArrayList<String>()
    var compared = 0
    for ((contentWidth, contentHeight) in contents) {
      val content = YogaMeasureFunction { _, width, widthMode, height, heightMode ->
        YogaMeasureOutput.make(
            fit(contentWidth.toFloat(), width, widthMode),
            fit(contentHeight.toFloat(), height, heightMode),
        )
      }
      for (direction in listOf(YogaDirection.LTR, YogaDirection.RTL)) {
        for (declared in BoxConstraintsCases.generated(count = 200, seed = 7)) {
          for (constraints in constraintsToTry) {
            val box =
                resolveBoxConstraints(constraints, declared.build(), direction == YogaDirection.RTL)
            val yoga =
                NodeConfig.createYogaNode()
                    .apply {
                      setDirection(direction)
                      setMeasureFunction(content)
                      layOutWithin(box)
                    }
                    .let { node -> IntSize(node.layoutWidth.toInt(), node.layoutHeight.toInt()) }
            val direct =
                borderBoxSize(box) { widthSpec, heightSpec ->
                  IntSize(
                      measureFixed(contentWidth, widthSpec),
                      measureFixed(contentHeight, heightSpec),
                  )
                }
            compared++
            if (direct != yoga) {
              mismatches.add(
                  "content ${contentWidth}x$contentHeight $direction $declared $constraints: " +
                      "yoga ${yoga.width}x${yoga.height}, direct ${direct.width}x${direct.height}",
              )
            }
          }
        }
      }
    }
    assertThat(mismatches).describedAs("of %s compared", compared).isEmpty()
  }

  /** What content of a fixed size answers under [spec], as [fit] does for Yoga. */
  private fun measureFixed(size: Int, spec: Int): Int =
      when (SizeSpec.getMode(spec)) {
        SizeSpec.EXACTLY -> SizeSpec.getSize(spec)
        SizeSpec.AT_MOST -> minOf(size, SizeSpec.getSize(spec))
        else -> size
      }

  private fun fit(size: Float, space: Float, mode: YogaMeasureMode): Float =
      when (mode) {
        YogaMeasureMode.EXACTLY -> space
        YogaMeasureMode.AT_MOST -> minOf(size, space)
        else -> size
      }
}

/**
 * Declares [declared]'s margins, padding and border on a Yoga node, as a flexbox child has them.
 * Each [BoxEdge] is the [YogaEdge] of the same name.
 */
private fun declareEdges(node: YogaNode, declared: BoxConstraintsCases.Declared) {
  for (edge in declared.margins) {
    val yoga = YogaEdge.valueOf(edge.edge.name)
    if (edge.isPercent) node.setMarginPercent(yoga, edge.value)
    else node.setMargin(yoga, edge.value)
  }
  for (edge in declared.paddings) {
    val yoga = YogaEdge.valueOf(edge.edge.name)
    if (edge.isPercent) node.setPaddingPercent(yoga, edge.value)
    else node.setPadding(yoga, edge.value)
  }
  for (edge in declared.borders) {
    node.setBorder(YogaEdge.valueOf(edge.edge.name), edge.value)
  }
}
