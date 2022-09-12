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

package com.facebook.litho

import android.graphics.Rect
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.incrementalmount.IncrementalMountOutput
import com.facebook.rendercore.incrementalmount.IncrementalMountRenderCoreExtension
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import java.util.ArrayList
import java.util.Collections
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayoutStateCalculateTopsAndBottomsTest {
  @JvmField @Rule val lithoViewRule: LithoViewRule = LithoViewRule()

  @Test
  fun testCalculateTopsAndBottoms() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(
                    Column.create(c)
                        .child(SimpleMountSpecTester.create(c).wrapInView().heightPx(50)))
                .child(SimpleMountSpecTester.create(c).heightPx(20))
                .child(
                    SimpleMountSpecTester.create(c)
                        .positionType(YogaPositionType.ABSOLUTE)
                        .positionPx(YogaEdge.TOP, 10)
                        .positionPx(YogaEdge.BOTTOM, 30))
                .build()
          }
        }
    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 50 + 20) { component }
    val layoutState = testLithoView.componentTree.mainThreadLayoutState!!
    Assertions.assertThat(layoutState.mountableOutputCount).isEqualTo(5)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[0].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[1].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[2].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[3].bounds.top).isEqualTo(10)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[4].bounds.top).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[0].bounds.bottom).isEqualTo(40)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[1].bounds.bottom).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[2].bounds.bottom).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[3].bounds.bottom).isEqualTo(70)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[4].bounds.bottom).isEqualTo(70)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[2].index).isEqualTo(2)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[3].index).isEqualTo(4)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[4].index).isEqualTo(3)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[0].index).isEqualTo(4)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[1].index).isEqualTo(2)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[2].index).isEqualTo(1)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[3].index).isEqualTo(3)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[4].index).isEqualTo(0)
  }

  @Test
  fun testCalculateTopsAndBottomsWhenEqual() {
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(SimpleMountSpecTester.create(c).heightPx(50))
                .child(
                    SimpleMountSpecTester.create(c)
                        .wrapInView()
                        .positionType(YogaPositionType.ABSOLUTE)
                        .positionPx(YogaEdge.TOP, 0)
                        .positionPx(YogaEdge.BOTTOM, 0))
                .build()
          }
        }
    val testLithoView = lithoViewRule.render(widthPx = 100, heightPx = 50) { component }
    val layoutState = testLithoView.componentTree.mainThreadLayoutState!!
    Assertions.assertThat(layoutState.mountableOutputCount).isEqualTo(4)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[0].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[1].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[2].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[3].bounds.top).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[0].bounds.bottom).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[1].bounds.bottom).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[2].bounds.bottom).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[3].bounds.bottom).isEqualTo(50)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[0].index).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[1].index).isEqualTo(1)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[2].index).isEqualTo(2)
    Assertions.assertThat(layoutState.outputsOrderedByTopBounds[3].index).isEqualTo(3)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[3].index).isEqualTo(0)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[2].index).isEqualTo(1)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[1].index).isEqualTo(2)
    Assertions.assertThat(layoutState.outputsOrderedByBottomBounds[0].index).isEqualTo(3)
  }

  @Test
  fun testTopsComparatorIsEquivalenceRelation() {
    val outputs: Array<IncrementalMountOutput?> = arrayOfNulls<IncrementalMountOutput>(4)
    outputs[0] = createIncrementMountOutput(0, 10, 0)
    outputs[1] = createIncrementMountOutput(0, 10, 1)
    outputs[2] = createIncrementMountOutput(0, 20, 2)
    outputs[3] = createIncrementMountOutput(0, 20, 3)

    // reflexive
    for (output in outputs) {
      Assertions.assertThat(
              IncrementalMountRenderCoreExtension.sTopsComparator.compare(output, output))
          .isEqualTo(0)
    }

    // symmetric
    for (i in 0..3) {
      for (j in i + 1..3) {
        Assertions.assertThat(
                -1 *
                    IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                        outputs[j], outputs[i]))
            .isEqualTo(
                IncrementalMountRenderCoreExtension.sTopsComparator.compare(outputs[i], outputs[j]))
      }
    }

    // transitivity
    for (i in 0..3) {
      for (j in 0..3) {
        for (k in 0..3) {
          if (i != j && j != k && i != k) {
            if (IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                outputs[i], outputs[j]) ==
                IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                    outputs[j], outputs[k])) {
              Assertions.assertThat(
                      IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                          outputs[j], outputs[k]))
                  .isEqualTo(
                      IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                          outputs[i], outputs[j]))
            }
          }
        }
      }
    }
  }

  @Test
  fun testBottomsComparatorIsEquivalenceRelation() {
    val outputs: Array<IncrementalMountOutput?> = arrayOfNulls<IncrementalMountOutput>(4)
    outputs[0] = createIncrementMountOutput(0, 10, 0)
    outputs[1] = createIncrementMountOutput(0, 10, 1)
    outputs[2] = createIncrementMountOutput(0, 20, 2)
    outputs[3] = createIncrementMountOutput(0, 20, 3)

    // reflexive
    for (output in outputs) {
      Assertions.assertThat(
              IncrementalMountRenderCoreExtension.sBottomsComparator.compare(output, output))
          .isEqualTo(0)
    }

    // symmetric
    for (i in 0..3) {
      for (j in i + 1..3) {
        Assertions.assertThat(
                -1 *
                    IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                        outputs[j], outputs[i]))
            .isEqualTo(
                IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                    outputs[i], outputs[j]))
      }
    }

    // transitivity
    for (i in 0..3) {
      for (j in 0..3) {
        for (k in 0..3) {
          if (i != j && j != k && i != k) {
            if (IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                outputs[i], outputs[j]) ==
                IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                    outputs[j], outputs[k])) {
              Assertions.assertThat(
                      IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                          outputs[j], outputs[k]))
                  .isEqualTo(
                      IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                          outputs[i], outputs[j]))
            }
          }
        }
      }
    }
  }

  @Test
  fun testTopsComparatorWithOverflow() {
    val maxIntTop: IncrementalMountOutput = createIncrementMountOutput(Int.MAX_VALUE, 20, 0)
    val largeNegativeTop: IncrementalMountOutput = createIncrementMountOutput(-2147483646, 20, 1)
    val minIntTop: IncrementalMountOutput = createIncrementMountOutput(Int.MIN_VALUE, 20, 2)
    val largePositiveTop: IncrementalMountOutput = createIncrementMountOutput(2147483646, 20, 3)
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                maxIntTop, largeNegativeTop))
        .isPositive()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                largeNegativeTop, maxIntTop))
        .isNegative()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                minIntTop, largePositiveTop))
        .isNegative()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sTopsComparator.compare(
                largePositiveTop, minIntTop))
        .isPositive()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sTopsComparator.compare(minIntTop, maxIntTop))
        .isNegative()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sTopsComparator.compare(maxIntTop, minIntTop))
        .isPositive()
  }

  @Test
  fun testBottomComparatorWithOverflow() {
    val maxIntBottom: IncrementalMountOutput = createIncrementMountOutput(20, Int.MAX_VALUE, 0)
    val largeNegativeBottom: IncrementalMountOutput = createIncrementMountOutput(20, -2147483646, 1)
    val minIntBottom: IncrementalMountOutput = createIncrementMountOutput(20, Int.MIN_VALUE, 2)
    val largePositiveBottom: IncrementalMountOutput = createIncrementMountOutput(20, 2147483646, 3)
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                maxIntBottom, largeNegativeBottom))
        .isPositive()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                largeNegativeBottom, maxIntBottom))
        .isNegative()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                minIntBottom, largePositiveBottom))
        .isNegative()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                largePositiveBottom, minIntBottom))
        .isPositive()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                minIntBottom, maxIntBottom))
        .isNegative()
    Assertions.assertThat(
            IncrementalMountRenderCoreExtension.sBottomsComparator.compare(
                maxIntBottom, minIntBottom))
        .isPositive()
  }

  @Test
  fun testTopsComparatorAdheresToContract() {
    val nodes: MutableList<IncrementalMountOutput> = ArrayList<IncrementalMountOutput>()
    var currentIndex = 0
    nodes.add(createIncrementMountOutput(0, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(0, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(21, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(47, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(21, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(21, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483628, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483617, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483617, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483617, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483617, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483568, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483557, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483557, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483646, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483477, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483278, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483612, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(-2147483611, 10, currentIndex++))
    nodes.add(createIncrementMountOutput(2147483647, 20, currentIndex++))
    Collections.sort(nodes, IncrementalMountRenderCoreExtension.sTopsComparator)
  }

  @Test
  fun testBottomsComparatorAdheresToContract() {
    val nodes: MutableList<IncrementalMountOutput> = ArrayList<IncrementalMountOutput>()
    var currentIndex = 0
    nodes.add(createIncrementMountOutput(20, 0, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 0, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 21, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 47, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 21, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 21, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483628, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483617, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483568, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483557, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483557, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483646, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483477, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483278, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483612, currentIndex++))
    nodes.add(createIncrementMountOutput(20, -2147483611, currentIndex++))
    nodes.add(createIncrementMountOutput(20, 2147483647, currentIndex++))
    Collections.sort(nodes, IncrementalMountRenderCoreExtension.sBottomsComparator)
  }

  companion object {
    private fun createIncrementMountOutput(
        top: Int,
        bottom: Int,
        index: Int,
        hostId: Long = 0
    ): IncrementalMountOutput {
      return IncrementalMountOutput((index + 1) * 1L, index, Rect(0, top, 10, bottom), false, null)
    }
  }
}
