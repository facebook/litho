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
import com.facebook.litho.layout.ChildBoxCases.text
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.SizeConstraints
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
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
class ChildBoxYogaParityTest {

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
    for (declared in ChildBoxCases.generated(count = 400, seed = 11)) {
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
            ChildBoxCases.Declared(
                margins =
                    listOf(
                        ChildBoxCases.EdgeDecl(BoxEdge.ALL, 10f),
                        ChildBoxCases.EdgeDecl(BoxEdge.LEFT, value, isPercent),
                    ),
                paddings =
                    listOf(
                        ChildBoxCases.EdgeDecl(BoxEdge.ALL, 4f),
                        ChildBoxCases.EdgeDecl(BoxEdge.START, value, isPercent),
                    ),
                borders =
                    listOf(
                        ChildBoxCases.EdgeDecl(BoxEdge.ALL, 2f),
                        ChildBoxCases.EdgeDecl(BoxEdge.TOP, value),
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
      declared: ChildBoxCases.Declared,
      width: Int?,
      direction: YogaDirection,
  ): String? {
    val yoga = layOutInColumn(declared, width, direction)
    val box = resolveChildBox(
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
      declared: ChildBoxCases.Declared,
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
}

/**
 * Declares [declared]'s margins, padding and border on a Yoga node, as a flexbox child has them.
 * Each [BoxEdge] is the [YogaEdge] of the same name.
 */
private fun declareEdges(node: YogaNode, declared: ChildBoxCases.Declared) {
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
