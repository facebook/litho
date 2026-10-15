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
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.ChildBoxCases
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.resolveChildBox
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaMeasureFunction
import com.facebook.yoga.YogaMeasureMode
import com.facebook.yoga.YogaMeasureOutput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val INF = SizeConstraints.Infinity

/** A container that measures each child under its own constraints and stacks them. */
private val STACK = LayoutFunctionN { ms, c ->
  val ps = ms.map { it.measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight)) }
  layout(c.maxWidth, ps.sumOf { it.height }) {
    var y = 0
    ps.forEach { placeable ->
      placeable.place(0, y)
      y += placeable.height
    }
  }
}

/** Leaves a container lays out without a Yoga pass. */
@RunWith(LithoTestRunner::class)
class CustomLayoutLeafTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `leaves are laid out directly and a subtree through Yoga`() {
    LithoStats.resetAllCounters()
    val view = renderLeavesAndASubtree()
    val root = view.componentTree.mainThreadLayoutState?.rootLayoutResult as LithoLayoutResult
    val container = root.getChildAt(0)

    assertThat(container.getChildAt(0).layoutOutput.boxGeometry).describedAs("Text").isNotNull()
    assertThat(container.getChildAt(1).layoutOutput.boxGeometry)
        .describedAs("Primitive")
        .isNotNull()
    assertThat(container.getChildAt(2).layoutOutput.boxGeometry)
        .describedAs("Column subtree")
        .isNull()
    assertThat(container.getChildAt(1).width).isEqualTo(30)
    assertThat(container.getChildAt(1).height).isEqualTo(20)
    assertThat(LithoStats.customLayoutLeafCount.get()).isGreaterThan(0)
  }

  private fun renderLeavesAndASubtree() =
      lithoTestRule.render(widthPx = 500, heightPx = 500) {
        Column {
          child(
              Layout(
                  listOf(
                      Text.create(lithoTestRule.context).text("text").build(),
                      SimpleMountSpecTester(measuredWidth = 30, measuredHeight = 20),
                      Column {
                        child(SimpleMountSpecTester(style = Style.width(10.px).height(10.px)))
                      },
                  ),
                  layout = STACK,
              ),
          )
        }
      }

  @Test
  fun `the inspector shows a Layout child at its laid-out size and resolved margins`() {
    val measures = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      layout(c.maxWidth, p.height) { p.place(0, 0) }
    }
    val leaf = SimpleMountSpecTester(style = Style.width(30.px).height(20.px).margin(start = 7.px))

    val view =
        lithoTestRule.render(widthPx = 200, heightPx = 200) {
          Layout(listOf(leaf), layout = measures)
        }
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    val child = DebugLayoutNode(root.getChildAt(0))

    assertThat(root.getChildAt(0).layoutOutput.boxGeometry).isNotNull()
    assertThat(child.layoutWidth).isEqualTo(30f)
    assertThat(child.layoutHeight).isEqualTo(20f)
    assertThat(child.getLayoutMargin(YogaEdge.LEFT)).isEqualTo(7f)
    assertThat(child.getLayoutMargin(YogaEdge.START)).isEqualTo(7f)
  }

  @Test
  fun `a Primitive laid out directly is measured in its own direction`() {
    val seen = ArrayList<LayoutDirection>()
    val leaf = DirectionRecordingLeaf(seen)
    val measures = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = c.maxHeight))
      layout(c.maxWidth, p.height) { p.place(0, 0) }
    }

    lithoTestRule.render(widthPx = 200, heightPx = 200) {
      Column(style = Style.layoutDirection(YogaDirection.RTL)) {
        child(Layout(listOf(leaf), layout = measures))
      }
    }

    assertThat(seen).isNotEmpty.allMatch { it == LayoutDirection.RTL }
  }

  /**
   * For generated box props under a range of constraints, in both directions, the size the direct
   * path gives a leaf is the size a Yoga pass gives it: [boxSize] against `layOutWithin` on a Yoga
   * leaf with the same content.
   */
  @Test
  fun `a leaf laid out directly is sized as a Yoga pass sizes it`() {
    val constraintsToTry = listOf(
        SizeConstraints(0, 200, 0, INF),
        SizeConstraints(0, 333, 0, INF),
        SizeConstraints(200, 200, 0, INF),
        SizeConstraints(),
        SizeConstraints(0, INF, 0, 90),
        SizeConstraints(0, INF, 90, 90),
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
        for (declared in ChildBoxCases.generated(count = 200, seed = 7)) {
          for (constraints in constraintsToTry) {
            val box = resolveChildBox(constraints, declared.build(), direction == YogaDirection.RTL)
            val yoga =
                NodeConfig.createYogaNode()
                    .apply {
                      setDirection(direction)
                      setMeasureFunction(content)
                      layOutWithin(box)
                    }
                    .let { MeasuredSize(it.layoutWidth.toInt(), it.layoutHeight.toInt()) }
            val direct =
                boxSize(box) { widthSpec, heightSpec ->
                  measureFixed(contentWidth, widthSpec) to measureFixed(contentHeight, heightSpec)
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

/** A Primitive that records the direction its layout scope reports each time it is measured. */
private class DirectionRecordingLeaf(private val seen: MutableList<LayoutDirection>) :
    PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = DirectionRecordingBehavior(seen),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class DirectionRecordingBehavior(private val seen: MutableList<LayoutDirection>) :
    LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult {
    seen.add((extraContext as LithoExtraContextForLayoutScope).layoutDirection)
    return PrimitiveLayoutResult(width = 10, height = 10)
  }
}
