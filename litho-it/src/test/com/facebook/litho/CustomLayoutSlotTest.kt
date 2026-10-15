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

import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunction3
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A `Layout`'s children keep the slots they were declared in, present or not. */
@RunWith(LithoTestRunner::class)
class CustomLayoutSlotTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a function naming three children gets all three when the middle one is null`() {
    val sizes = ArrayList<String>()
    val three = LayoutFunction3 { a, b, c, constraints ->
      val pa = a.measure(SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = 100))
      val pb = b.measure(SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = 100))
      val pc = c.measure(SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = 100))
      listOf(pa, pb, pc).forEach { sizes.add("${it.width}x${it.height}") }
      layout(constraints.maxWidth, 100) {
        pa.place(0, 0)
        pc.place(0, 0)
      }
    }

    lithoTestRule.render(widthPx = 200, heightPx = 200) {
      Layout(leaf(), null, leaf(), layout = three)
    }

    assertThat(sizes).containsExactly("10x10", "0x0", "10x10")
  }

  @Test
  fun `a child that renders nothing keeps its slot`() {
    val indices = ArrayList<Int>()
    val counts = LayoutFunctionN { ms, constraints ->
      indices.add(ms.size)
      val last =
          ms.last().measure(SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = 100))
      layout(constraints.maxWidth, 100) { last.place(0, 0) }
    }

    val view =
        lithoTestRule.render(widthPx = 200, heightPx = 200) {
          Layout(listOf(leaf(), RendersNothing(), leaf()), layout = counts)
        }

    assertThat(indices).isNotEmpty.allMatch { it == 3 }
    val root = checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
    assertThat((root as LithoLayoutResult).getChildAt(0).width)
        .describedAs("the last child")
        .isEqualTo(10)
  }

  private fun leaf(): Component = SimpleMountSpecTester(style = Style.width(10.px).height(10.px))

  private class RendersNothing : KComponent() {
    override fun ComponentScope.render(): Component? = null
  }
}
