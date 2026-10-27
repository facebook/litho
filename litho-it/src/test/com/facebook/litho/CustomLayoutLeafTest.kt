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
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
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
import org.assertj.core.api.Assertions.assertThat
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

/** A container that measures each child under its own constraints and stacks them. */
private val STACK = LayoutFunctionN { measurables, constraints ->
  val placeables = measurables.map {
    it.measure(SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
  }
  layout(constraints.maxWidth, placeables.sumOf { it.height }) {
    var y = 0
    placeables.forEach { placeable ->
      placeable.place(0, y)
      y += placeable.height
    }
  }
}

/** A `Layout`'s leaf children, each laid out as a Yoga tree of one node. */
@RunWith(LithoTestRunner::class)
class CustomLayoutLeafTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `leaves and a subtree are each a Yoga tree of their own`() {
    val view = renderLeavesAndASubtree()
    val root = view.componentTree.mainThreadLayoutState?.rootLayoutResult as LithoLayoutResult
    val container = root.getChildAt(0)

    assertThat(container.childCount).isEqualTo(3)
    assertThat(container.getChildAt(0).measureOutput).isInstanceOf(YogaLayoutOutput::class.java)
    assertThat(container.getChildAt(1).measureOutput).isInstanceOf(YogaLayoutOutput::class.java)
    assertThat(container.getChildAt(1).width).isEqualTo(30)
    assertThat(container.getChildAt(1).height).isEqualTo(20)
    assertThat(container.getChildAt(2).measureOutput).isInstanceOf(YogaLayoutOutput::class.java)
    assertThat(container.getChildAt(2).height).describedAs("Column subtree").isEqualTo(10)
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
  fun `a Primitive under a Layout is measured in its own direction`() {
    val seen = ArrayList<LayoutDirection>()
    val leaf = DirectionRecordingLeaf(seen)
    lithoTestRule.render(widthPx = 200, heightPx = 200) {
      Column(style = Style.layoutDirection(YogaDirection.RTL)) {
        child(Layout(listOf(leaf), layout = FIRST_CHILD_AT_FULL_WIDTH))
      }
    }

    assertThat(seen).isNotEmpty.allMatch { it == LayoutDirection.RTL }
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
