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

package com.facebook.rendercore.simplelist

import android.graphics.Rect
import android.view.View
import android.view.View.MeasureSpec
import androidx.recyclerview.widget.RecyclerView
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.Node
import com.facebook.rendercore.RenderResultFuture
import com.facebook.rendercore.ResolveResult
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.utils.MeasureSpecUtils
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableList.Builder
import java.util.ArrayList
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MeasureListUtilsTest {

  private lateinit var layoutContext: LayoutContext<Any?>

  @Before
  fun setup() {
    layoutContext =
        LayoutContext<Any?>(RuntimeEnvironment.application, null, -1, LayoutCache(), null)
  }

  // === V1 Baseline Tests - MeasureSpec Combinations ===

  // When both width and height are EXACTLY specified, the list returns the spec sizes
  // directly without measuring any children.
  @Test
  fun testExactWidthAndHeightSizeVScroll() {
    val width = 600
    val height = 1_200
    val widthSpec = MeasureSpecUtils.exactly(width)
    val heightSpec = MeasureSpecUtils.exactly(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.VERTICAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.VERTICAL, height),
        createChildren(20, childWidth, childHeight),
    )
    val vScrollSize =
        measureList(layoutContext, widthSpec, heightSpec, RecyclerView.VERTICAL, 0, 0, 0, items)
    Assertions.assertThat(vScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(vScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(0)
  }

  // Horizontal equivalent of the above: both axes EXACTLY specified means no measurement needed.
  @Test
  fun testExactWidthAndHeightSizeHScroll() {
    val width = 600
    val height = 1_200
    val widthSpec = MeasureSpecUtils.exactly(width)
    val heightSpec = MeasureSpecUtils.exactly(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.HORIZONTAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.HORIZONTAL, height),
        createChildren(20, childWidth, childHeight),
    )
    val hScrollSize =
        measureList(layoutContext, widthSpec, heightSpec, RecyclerView.HORIZONTAL, 0, 0, 0, items)
    Assertions.assertThat(hScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(hScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(0)
  }

  // With exact width and AT_MOST height in vertical scroll, children are measured until
  // the cumulative height reaches the AT_MOST budget.
  @Test
  fun testExactWidthAndAtMostHeightSizeVScroll() {
    val width = 600
    val height = 800
    val widthSpec = MeasureSpecUtils.exactly(width)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.VERTICAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.VERTICAL, height),
        createChildren(20, childWidth, childHeight),
    )
    val vScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(vScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(vScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(7)
  }

  // In horizontal scroll with AT_MOST height, the cross-axis height shrinks to the
  // tallest child rather than using the full AT_MOST budget.
  @Test
  fun testExactWidthAndAtMostHeightSizeHScroll() {
    val width = 600
    val height = 800
    val widthSpec = MeasureSpecUtils.exactly(width)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.HORIZONTAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.HORIZONTAL, height),
        createChildren(20, childWidth, childHeight),
    )
    val hScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(hScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(hScrollSize[1]).isEqualTo(childHeight)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With both axes AT_MOST in vertical scroll, width shrinks to the widest child while
  // height fills up to the AT_MOST bound. All children are measured for the cross-axis max.
  @Test
  fun testAtMostWidthAndAtMostHeightSizeVScroll() {
    val width = 600
    val height = 800
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val vScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(vScrollSize[0]).isEqualTo(childWidth)
    Assertions.assertThat(vScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With both axes AT_MOST in horizontal scroll, width fills to the bound while height
  // shrinks to the tallest child. All children are measured for the cross-axis max.
  @Test
  fun testAtMostWidthAndAtMostHeightSizeHScroll() {
    val width = 600
    val height = 800
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val hScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(hScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(hScrollSize[1]).isEqualTo(childHeight)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // When width is AT_MOST(0) in vertical scroll, the result width stays at zero while the
  // height budget is still consumed normally by measuring children.
  @Test
  fun testAtMostZeroWidthAndAtMostHeightSizeVScroll() {
    val width = 0
    val height = 800
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val vScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(vScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(vScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(7)
  }

  // When height is AT_MOST(0) in horizontal scroll, the result height stays at zero while
  // children are still measured to accumulate the main-axis width.
  @Test
  fun testAtMostWidthAndAtMostZeroHeightSizeHScroll() {
    val width = 600
    val height = 0
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val vScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(vScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(vScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(2)
  }

  // When children are larger than the AT_MOST bound, the cross-axis expands to the actual
  // child size. This test verifies both vertical and horizontal scroll orientations.
  @Test
  fun testAtMostWidthAndAtMostHeightSizeWithLargeChildSize() {
    val width = 600
    val height = 800
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 600
    val childHeight = 400
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(300),
        MeasureSpecUtils.exactly(100),
        createChildren(20, childWidth, childHeight),
    )
    val vScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(vScrollSize[0]).isEqualTo(childWidth)
    Assertions.assertThat(vScrollSize[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(2)
    val itemsHScroll = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(300),
        MeasureSpecUtils.exactly(100),
        createChildren(20, childWidth, childHeight),
    )
    val hScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        itemsHScroll,
    )
    Assertions.assertThat(hScrollSize[0]).isEqualTo(childWidth)
    Assertions.assertThat(hScrollSize[1]).isEqualTo(childHeight)
    Assertions.assertThat(measurePrepared(itemsHScroll)).isEqualTo(20)
  }

  // In horizontal scroll with large children and spaceBefore, the main-axis width caps at
  // the AT_MOST bound while the cross-axis height expands to the full child height.
  @Test
  fun testAtMostWidthAndAtMostHeightSizeWithLargeChildSizeHScroll() {
    val width = 600
    val height = 800
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 560
    val childHeight = 800
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(300),
        MeasureSpecUtils.exactly(100),
        createChildren(20, childWidth, childHeight),
    )
    val hScrollSize = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        20, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(hScrollSize[0]).isEqualTo(width)
    Assertions.assertThat(hScrollSize[1]).isEqualTo(childHeight)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // With UNSPECIFIED width in vertical scroll, all children must be measured to find the
  // widest one, and the result width equals the maximum child width.
  @Test
  fun testUnspecifiedWidthAndAtMostHeightSizeVScroll() {
    val height = 800
    val widthSpec = MeasureSpecUtils.unspecified()
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(childWidth)
    Assertions.assertThat(size[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With UNSPECIFIED height in vertical scroll, all children are measured and the result
  // height equals the sum of all child heights.
  @Test
  fun testExactWidthAndUnspecifiedHeightSizeVScroll() {
    val width = 600
    val widthSpec = MeasureSpecUtils.exactly(width)
    val heightSpec = MeasureSpecUtils.unspecified()
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        0, /*space before*/
        0, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(childHeight * 20)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With UNSPECIFIED width in horizontal scroll, all children are measured and the result
  // width equals the sum of all child widths.
  @Test
  fun testUnspecifiedWidthAndExactHeightSizeHScroll() {
    val height = 600
    val widthSpec = MeasureSpecUtils.unspecified()
    val heightSpec = MeasureSpecUtils.exactly(height)
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        0, /*space before*/
        0, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(childWidth * 20)
    Assertions.assertThat(size[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With AT_MOST width and UNSPECIFIED height in horizontal scroll, the main-axis width
  // caps at the bound while the cross-axis height equals the tallest child.
  @Test
  fun testAtMostWidthAndUnspecifiedHeightSizeHScroll() {
    val width = 600
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.unspecified()
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        0, /*space before*/
        20, /*space between*/
        0, /*space after*/
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(childHeight)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // === V1 Spacing Tests ===

  // In horizontal scroll with spaceBefore, spaceBetween, and spaceAfter, the measurement
  // stops once the accumulated width (including spacing) exceeds the AT_MOST bound.
  @Test
  fun testSpacingInHScroll() {
    val width = 600
    val height = 1_200
    val widthSpec = MeasureSpecUtils.atMost(width)
    val heightSpec = MeasureSpecUtils.exactly(height)
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.HORIZONTAL,
        20, /*space before*/
        20, /*space between*/
        20, /*space after*/
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // In vertical scroll with a single child, the total height includes spaceBefore, the
  // child height, and spaceAfter.
  @Test
  fun testSpacingInVScroll() {
    val width = 600
    val height = 600
    val widthSpec = MeasureSpecUtils.exactly(width)
    val heightSpec = MeasureSpecUtils.atMost(height)
    val childWidth = 100
    val childHeight = 500
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(1, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        widthSpec,
        heightSpec,
        RecyclerView.VERTICAL,
        20, /*space before*/
        20, /*space between*/
        20, /*space after*/
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(540)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // === V2 Baseline Tests (no margins) ===

  // In V2 with both axes set to not wrap, the list returns the spec sizes directly
  // without measuring any children.
  @Test
  fun testV2VScrollNoWrap() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.exactly(1200),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = false,
        items,
    )
    // Both axes are not wrapping, so both are set immediately with no children measured.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(1200)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(0)
  }

  // Horizontal equivalent: both axes not wrapping returns spec sizes with no measurement.
  @Test
  fun testV2HScrollNoWrap() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.exactly(1200),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = false,
        items,
    )
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(1200)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(0)
  }

  // With wrapHeight enabled in vertical scroll, children are measured until the cumulative
  // height (including spacing) reaches the AT_MOST bound.
  @Test
  fun testV2VScrollWrapHeight() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
    )
    // Width is not wrapping so it stays at 600. With wrapHeight, each child is 100 tall
    // plus 20 spacing. After 7 children the combined height is 840, which exceeds 800.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(7)
  }

  // With wrapWidth enabled in horizontal scroll, children are measured until the cumulative
  // width reaches the AT_MOST bound.
  @Test
  fun testV2HScrollWrapWidth() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.exactly(800),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = false,
        items,
    )
    // Height is not wrapping so it stays at 800. With wrapWidth, each child is 300 wide
    // with no spacing. After 2 children the combined width is 600, meeting the bound.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(2)
  }

  // When both axes wrap in vertical scroll, the main-axis height caps at the bound but all
  // children are still measured to determine the cross-axis maximum width.
  @Test
  fun testV2VScrollWrapBoth() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        wrapWidth = true,
        wrapHeight = true,
        items,
    )
    // Both axes wrap. Height caps at 800 after 7 children (7*120 = 840 exceeds 800).
    // But the cross-axis width of 300 is less than 600, so all 20 children are measured
    // to find the maximum width.
    Assertions.assertThat(size[0]).isEqualTo(childWidth)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With UNSPECIFIED height and wrapHeight enabled, all children are measured and the
  // result height equals the sum of all child heights.
  @Test
  fun testV2VScrollWrapHeightUnspecified() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
    )
    // Width is not wrapping so it stays at 600. With UNSPECIFIED height and wrapHeight
    // enabled, all children are measured and heights are summed.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(childHeight * 20)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // With UNSPECIFIED width and wrapWidth enabled in horizontal scroll, all children are
  // measured and the result width equals the sum of all child widths.
  @Test
  fun testV2HScrollWrapWidthUnspecified() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.unspecified(),
        MeasureSpecUtils.exactly(800),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = false,
        items,
    )
    // With UNSPECIFIED width and wrapWidth enabled, all children are measured and widths
    // are summed. Height is not wrapping so it stays at 800.
    Assertions.assertThat(size[0]).isEqualTo(childWidth * 20)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // When both axes wrap with spacing, all children fit under the AT_MOST bound and the
  // total includes spaceBefore, spaceBetween for each gap, and spaceAfter.
  @Test
  fun testV2VScrollWrapBothWithSpacing() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.atMost(1000),
        RecyclerView.VERTICAL,
        20,
        10,
        20,
        wrapWidth = true,
        wrapHeight = true,
        items,
    )
    // In vertical scroll with spacing, the first child gets spaceBefore 20 + child 100 +
    // spaceBetween 10 = 130. Middle children each get 100 + 10 = 110. The last child gets
    // 100 + spaceAfter 20 = 120. Total height is 130 + 3*110 + 120 = 580, which is under
    // the 1000 bound. The cross-axis width of 200 is under the 600 bound.
    Assertions.assertThat(size[0]).isEqualTo(childWidth)
    Assertions.assertThat(size[1]).isEqualTo(580)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // With UNSPECIFIED specs and width not wrapping, V2 sets width to Integer.MAX_VALUE.
  // Height wraps normally by summing all child heights.
  @Test
  fun testV2VScrollNoWrapWidthWrapHeightUnspecified() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.unspecified(),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
    )
    // With UNSPECIFIED specs and width not wrapping, width becomes Integer.MAX_VALUE.
    // Height wraps normally by summing all child heights.
    Assertions.assertThat(size[0]).isEqualTo(Integer.MAX_VALUE)
    Assertions.assertThat(size[1]).isEqualTo(childHeight * 20)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // === Margin Tests For Collections (V1) ===

  // Uniform margins increase the effective size of each child, so the total height grows
  // proportionally when all children are measured with UNSPECIFIED height.
  @Test
  fun testVScrollUnspecifiedHeightUniformMargins() {
    val width = 600
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(width),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, left = 10, top = 20, right = 10, bottom = 20),
    )
    // Each child is 320x140 with margins, all 20 are measured, and total height is 20 * 140.
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(2800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // Horizontal equivalent: uniform left/right margins increase effective child width, so
  // the total width grows when all children are measured with UNSPECIFIED width.
  @Test
  fun testHScrollUnspecifiedWidthUniformMargins() {
    val height = 800
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.unspecified(),
        MeasureSpecUtils.exactly(height),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, left = 15, top = 10, right = 15, bottom = 10),
    )
    // Each child is 330x120 with margins, all 20 are measured, and total width is 20 * 330.
    Assertions.assertThat(size[0]).isEqualTo(6600)
    Assertions.assertThat(size[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // Main-axis margins cause earlier termination because fewer children fit within the
  // AT_MOST height budget. Without margins 8 children fit; with margins only 4.
  @Test
  fun testVScrollAtMostHeightMarginsAffectEarlyTermination() {
    val width = 600
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(width),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, top = 50, bottom = 50),
    )
    // Effective child height is 100 + 100 = 200. After 4 children the total reaches 800,
    // which meets the AT_MOST bound and caps.
    // Without margins, 8 children would be needed to reach 800.
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // Horizontal equivalent: left/right margins reduce the number of children that fit
  // within the AT_MOST width budget, triggering earlier measurement termination.
  @Test
  fun testHScrollAtMostWidthMarginsAffectEarlyTermination() {
    val height = 800
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.exactly(height),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, left = 25, right = 25),
    )
    // Effective child width is 100 + 50 = 150. After 4 children the total reaches 600,
    // which meets the AT_MOST bound and caps.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(height)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // With per-child asymmetric margins, the cross-axis width equals the maximum effective
  // child width (child width + left margin + right margin) across all children.
  @Test
  fun testVScrollAtMostWidthAsymmetricPerChildMargins() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(4, childWidth, childHeight),
    )
    val margins = marginsOf(
        Rect(
            50,
            10,
            0,
            10,
        ), // horizontal margins 50, vertical margins 20, effective child 250x120
        Rect(
            0,
            10,
            100,
            10,
        ), // horizontal margins 100, vertical margins 20, effective child 300x120
        Rect(
            30,
            10,
            20,
            10,
        ), // horizontal margins 50, vertical margins 20, effective child 250x120
        Rect(
            10,
            10,
            10,
            10,
        ), // horizontal margins 20, vertical margins 20, effective child 220x120
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        margins,
    )
    // The cross-axis width is the maximum effective child width: max(250, 300, 250, 220) = 300,
    // capped at min(300, 600) = 300. The main-axis height is the sum of all effective child
    // heights: 4 * 120 = 480.
    Assertions.assertThat(size[0]).isEqualTo(300)
    Assertions.assertThat(size[1]).isEqualTo(480)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // Horizontal equivalent: per-child asymmetric top/bottom margins determine the
  // cross-axis height as the maximum effective child height across all children.
  @Test
  fun testHScrollAtMostHeightAsymmetricPerChildMargins() {
    val childWidth = 100
    val childHeight = 200
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(4, childWidth, childHeight),
    )
    val margins = marginsOf(
        Rect(
            10,
            50,
            10,
            0,
        ), // horizontal margins 20, vertical margins 50, effective child 120x250
        Rect(
            10,
            0,
            10,
            100,
        ), // horizontal margins 20, vertical margins 100, effective child 120x300
        Rect(
            10,
            30,
            10,
            20,
        ), // horizontal margins 20, vertical margins 50, effective child 120x250
        Rect(
            10,
            10,
            10,
            10,
        ), // horizontal margins 20, vertical margins 20, effective child 120x220
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.unspecified(),
        MeasureSpecUtils.atMost(800),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        items,
        margins,
    )
    // The main-axis width is the sum of all effective child widths: 4 * 120 = 480.
    // The cross-axis height is the maximum effective child height: max(250, 300, 250, 220) = 300,
    // capped at min(300, 800) = 300.
    Assertions.assertThat(size[0]).isEqualTo(480)
    Assertions.assertThat(size[1]).isEqualTo(300)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // Margins and spacing combine additively: both contribute to the total main-axis size.
  // The first child includes spaceBefore, middle children include spaceBetween, and the
  // last child includes spaceAfter.
  @Test
  fun testVScrollMarginsWithSpacing() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(1000),
        RecyclerView.VERTICAL,
        20,
        10,
        20,
        items,
        uniformMargins(5, top = 10, bottom = 10),
    )
    // Effective child height is 120 (100 + 20 margins).
    // Child 0 (first, not last): spaceBefore + 120 + spaceBetween = 20 + 120 + 10 = 150
    // Children 1-3 (middle): each child is 120 + spaceBetween 10 = 130, totaling 3 * 130 = 390
    // Child 4 (last): 120 + spaceAfter = 120 + 20 = 140
    // Total = 150 + 390 + 140 = 680
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(680)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // Horizontal equivalent: margins and spacing both add to total width, following the
  // same first/middle/last spacing pattern.
  @Test
  fun testHScrollMarginsWithSpacing() {
    val childWidth = 100
    val childHeight = 200
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(1000),
        MeasureSpecUtils.exactly(800),
        RecyclerView.HORIZONTAL,
        20,
        10,
        20,
        items,
        uniformMargins(5, left = 10, right = 10),
    )
    // Effective child width is 120 (100 + 20 margins).
    // Child 0: 20 + 120 + 10 = 150
    // Children 1-3: each child is 120 + spaceBetween 10 = 130, totaling 390
    // Child 4: 120 + 20 = 140
    // Total = 150 + 390 + 140 = 680
    Assertions.assertThat(size[0]).isEqualTo(680)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // Passing all-zero margins produces identical results to passing null, confirming that
  // zero margins are a no-op.
  @Test
  fun testVScrollZeroMarginsMatchesNullMargins() {
    val width = 600
    val childWidth = 300
    val childHeight = 100
    val itemsWithNull = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.VERTICAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.VERTICAL, 800),
        createChildren(20, childWidth, childHeight),
    )
    val sizeWithNull = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(width),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        itemsWithNull,
        null,
    )
    val itemsWithZero = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.VERTICAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.VERTICAL, 800),
        createChildren(20, childWidth, childHeight),
    )
    val sizeWithZero = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(width),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        itemsWithZero,
        uniformMargins(20),
    )
    Assertions.assertThat(sizeWithZero[0]).isEqualTo(sizeWithNull[0])
    Assertions.assertThat(sizeWithZero[1]).isEqualTo(sizeWithNull[1])
    Assertions.assertThat(measurePrepared(itemsWithZero)).isEqualTo(measurePrepared(itemsWithNull))
  }

  // When the margin list is shorter than the item count, children beyond the list length
  // get zero margins. The first 5 children use margins, the remaining 15 do not.
  @Test
  fun testVScrollPartialMarginList() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(5, top = 20, bottom = 20),
    )
    // First 5 children have effective height 100 + 40 = 140, totaling 5 * 140 = 700.
    // Remaining 15 children have no margins, totaling 15 * 100 = 1500.
    // Total = 2200
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(2200)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // When both axes are EXACTLY specified, the list returns spec sizes immediately without
  // measuring any children, so margins have no effect on the result.
  @Test
  fun testExactBothMarginsIgnored() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.exactly(1200),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, left = 50, top = 50, right = 50, bottom = 50),
    )
    // Both axes are EXACTLY specified, so the list returns immediately without measurement.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(1200)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(0)
  }

  // Cross-axis margins widen the effective child, increasing the measured max width.
  // The result width is the effective child width capped at the AT_MOST bound.
  @Test
  fun testVScrollCrossAxisMarginsAffectMaxWidth() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.exactly(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(5, left = 100, right = 100),
    )
    // Effective child width is 100 + 200 = 300.
    // The cross-axis max of 300 is less than 600, so it never caps during the loop.
    // After the loop, width resolves to min(300, 600) = 300.
    Assertions.assertThat(size[0]).isEqualTo(300)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // When cross-axis margins push the effective child width past the AT_MOST bound, the
  // width caps at the bound and measurement terminates early since both axes are resolved.
  @Test
  fun testVScrollCrossAxisMarginsExceedBound() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(300),
        MeasureSpecUtils.exactly(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, left = 200, right = 200),
    )
    // Effective child width is 100 + 400 = 500, which exceeds AT_MOST(300), so width caps
    // immediately. Both axes are resolved after the first child, so measurement stops.
    Assertions.assertThat(size[0]).isEqualTo(300)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // Horizontal equivalent: cross-axis top/bottom margins push effective height past the
  // AT_MOST bound, so height caps and measurement stops after the first child.
  @Test
  fun testHScrollCrossAxisMarginsAffectMaxHeight() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(300),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, top = 100, bottom = 100),
    )
    // Effective child height is 100 + 200 = 300, which meets the AT_MOST(300) bound,
    // so height caps immediately. Both axes are resolved after the first child.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(300)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // === LTR/RTL Symmetry Tests ===

  // Asymmetric left/right margins with the same total produce identical measurement
  // results, confirming that only the margin sum matters (LTR/RTL parity).
  @Test
  fun testVScrollAsymmetricHorizontalMarginsSameTotal() {
    val childWidth = 200
    val childHeight = 100
    // Margins with left=30,right=10 (total 40) vs left=10,right=30 (total 40)
    val itemsA = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val sizeA = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        itemsA,
        uniformMargins(5, left = 30, right = 10),
    )
    val itemsB = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val sizeB = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        itemsB,
        uniformMargins(5, left = 10, right = 30),
    )
    // Both configurations have total horizontal margins of 40, so the effective width is
    // 240 and the results must be identical.
    Assertions.assertThat(sizeA[0]).isEqualTo(sizeB[0])
    Assertions.assertThat(sizeA[1]).isEqualTo(sizeB[1])
    Assertions.assertThat(sizeA[0]).isEqualTo(240)
    Assertions.assertThat(sizeA[1]).isEqualTo(500)
    Assertions.assertThat(measurePrepared(itemsA)).isEqualTo(measurePrepared(itemsB))
  }

  // === V1 Edge Cases ===

  // A single child is both first and last, so it receives both spaceBefore and spaceAfter
  // in addition to its margins.
  @Test
  fun testVScrollSingleChildWithMargins() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(1, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        20,
        0,
        20,
        items,
        uniformMargins(1, left = 10, top = 20, right = 10, bottom = 20),
    )
    // The single child is both first and last, so its effective size is 220x140 (with
    // margins of 10 left, 20 top, 10 right, 20 bottom). In vertical scroll, the total
    // height is spaceBefore 20 + effective child height 140 + spaceAfter 20 = 180.
    // The cross-axis width is the effective child width of 220.
    Assertions.assertThat(size[0]).isEqualTo(220)
    Assertions.assertThat(size[1]).isEqualTo(180)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // An empty margin list behaves identically to null margins, meaning all children get
  // zero margins.
  @Test
  fun testVScrollEmptyMarginList() {
    val width = 600
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureListUtils.getDefaultChildWidthSpec(RecyclerView.VERTICAL, width),
        MeasureListUtils.getDefaultChildHeightSpec(RecyclerView.VERTICAL, 800),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(width),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        items,
        emptyList(),
    )
    // An empty list means all children get zero margins, producing the same result as null.
    Assertions.assertThat(size[0]).isEqualTo(width)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(7)
  }

  // When a single child's margins exceed the entire AT_MOST budget, the measurement caps
  // at the bound after measuring just that one child.
  @Test
  fun testVScrollLargeMarginsExceedAtMost() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureList(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        items,
        uniformMargins(20, top = 500, bottom = 500),
    )
    // Effective child height is 100 + 1000 = 1100, which exceeds 800, so it caps after
    // the first child.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // === V2 Margin Tests ===

  // Uniform top/bottom margins increase the effective child height, reducing the number
  // of children that fit within the AT_MOST height budget.
  @Test
  fun testV2VScrollWrapHeightUniformMargins() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
        uniformMargins(20, top = 20, bottom = 20),
    )
    // Width is not wrapping so it stays at 600. Effective child height is 100 + 40 = 140.
    // After 6 children the total is 840 which exceeds 800, so height caps at 800.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(6)
  }

  // Uniform left/right margins increase the effective child width, reducing the number
  // of children that fit within the AT_MOST width budget in horizontal scroll.
  @Test
  fun testV2HScrollWrapWidthUniformMargins() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.exactly(800),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = false,
        items,
        uniformMargins(20, left = 15, right = 15),
    )
    // Height is not wrapping so it stays at 800. Effective child width is 100 + 30 = 130.
    // After 5 children the total is 650 which exceeds 600, so width caps at 600.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // When both axes wrap with margins, the main-axis height caps at the bound but all
  // children are still measured to find the cross-axis maximum effective width.
  @Test
  fun testV2VScrollWrapBothMargins() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = true,
        items,
        uniformMargins(20, left = 10, top = 10, right = 10, bottom = 10),
    )
    // Each child has an effective size of 220x120 (with 10px margins on each side). In
    // vertical scroll, height caps after 7 children since 7 * 120 = 840 exceeds the 800
    // bound. However, the cross-axis width of 220 is still under 600, so all 20 children
    // are measured to find the maximum width.
    Assertions.assertThat(size[0]).isEqualTo(220)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // Large main-axis margins double the effective child height, halving the number of
  // children that fit within the AT_MOST budget compared to no margins.
  @Test
  fun testV2VScrollWrapHeightMarginsAffectEarlyTermination() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
        uniformMargins(20, top = 50, bottom = 50),
    )
    // Effective child height is 200. After 4 children the total reaches 800, capping.
    // Without margins, 8 children needed. Margins halve the count.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // Horizontal equivalent: large left/right margins reduce the number of children that
  // fit within the AT_MOST width budget, triggering earlier termination.
  @Test
  fun testV2HScrollWrapWidthMarginsAffectEarlyTermination() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.exactly(800),
        RecyclerView.HORIZONTAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = false,
        items,
        uniformMargins(20, left = 25, right = 25),
    )
    // Effective child width is 150. After 4 children the total reaches 600, capping.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // Margins and spacing combine additively in the V2 algorithm, following the same
  // first/middle/last spacing pattern as V1.
  @Test
  fun testV2VScrollWrapHeightMarginsWithSpacing() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(1000),
        RecyclerView.VERTICAL,
        20,
        10,
        20,
        wrapWidth = false,
        wrapHeight = true,
        items,
        uniformMargins(5, top = 10, bottom = 10),
    )
    // Each child has an effective height of 120 (100 + top 10 + bottom 10 margins). The
    // first child gets spaceBefore 20 + 120 + spaceBetween 10 = 150. Middle children each
    // get 120 + 10 = 130. The last child gets 120 + spaceAfter 20 = 140. Total height is
    // 150 + 3*130 + 140 = 680, which is under the 1000 bound.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(680)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // Horizontal equivalent: margins and spacing both add to total width in the V2
  // algorithm.
  @Test
  fun testV2HScrollWrapWidthMarginsWithSpacing() {
    val childWidth = 100
    val childHeight = 200
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(1000),
        MeasureSpecUtils.exactly(800),
        RecyclerView.HORIZONTAL,
        20,
        10,
        20,
        wrapWidth = true,
        wrapHeight = false,
        items,
        uniformMargins(5, left = 10, right = 10),
    )
    // Each child has an effective width of 120 (100 + left 10 + right 10 margins). The
    // first child gets spaceBefore 20 + 120 + spaceBetween 10 = 150. Middle children each
    // get 120 + 10 = 130. The last child gets 120 + spaceAfter 20 = 140. Total width is
    // 150 + 3*130 + 140 = 680, which is under the 1000 bound.
    Assertions.assertThat(size[0]).isEqualTo(680)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // With per-child asymmetric margins, the cross-axis width equals the maximum effective
  // child width across all children, capped at the AT_MOST bound.
  @Test
  fun testV2VScrollWrapBothAsymmetricMargins() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(4, childWidth, childHeight),
    )
    val margins = marginsOf(
        Rect(50, 10, 0, 10), // horizontal margins 50, vertical margins 20, effective 250x120
        Rect(0, 10, 100, 10), // horizontal margins 100, vertical margins 20, effective 300x120
        Rect(30, 10, 20, 10), // horizontal margins 50, vertical margins 20, effective 250x120
        Rect(10, 10, 10, 10), // horizontal margins 20, vertical margins 20, effective 220x120
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = true,
        items,
        margins,
    )
    // Cross-axis: max(250, 300, 250, 220) = 300, min(300, 600) = 300
    // Main-axis combined height is 4 * 120 = 480. Since height is UNSPECIFIED, there is no cap.
    Assertions.assertThat(size[0]).isEqualTo(300)
    Assertions.assertThat(size[1]).isEqualTo(480)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(4)
  }

  // When the margin list is shorter than the item count, children beyond the list length
  // receive zero margins. The first 5 children use margins while the remaining 15 do not.
  @Test
  fun testV2VScrollWrapHeightPartialMarginList() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
        uniformMargins(5, top = 20, bottom = 20),
    )
    // First 5 children have effective height 140, totaling 700. The remaining 15 have
    // effective height 100, totaling 1500. Grand total is 2200.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(2200)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(20)
  }

  // Passing all-zero margins produces identical results to passing null, confirming that
  // zero margins are a no-op in the V2 algorithm as well.
  @Test
  fun testV2VScrollWrapHeightZeroMarginsMatchNull() {
    val childWidth = 300
    val childHeight = 100
    val itemsNull = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val sizeNull = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        wrapWidth = false,
        wrapHeight = true,
        itemsNull,
        null,
    )
    val itemsZero = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val sizeZero = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        wrapWidth = false,
        wrapHeight = true,
        itemsZero,
        uniformMargins(20),
    )
    Assertions.assertThat(sizeZero[0]).isEqualTo(sizeNull[0])
    Assertions.assertThat(sizeZero[1]).isEqualTo(sizeNull[1])
    Assertions.assertThat(measurePrepared(itemsZero)).isEqualTo(measurePrepared(itemsNull))
  }

  // With both axes set to not wrap, the list returns spec sizes immediately without
  // measuring any children, making margins completely irrelevant.
  @Test
  fun testV2NoWrapMarginsIgnored() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.exactly(1200),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = false,
        items,
        uniformMargins(20, left = 50, top = 50, right = 50, bottom = 50),
    )
    // Both axes are not wrapping, so the list returns spec sizes immediately with margins
    // irrelevant.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(1200)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(0)
  }

  // Cross-axis margins widen the effective child in V2, increasing the measured maximum
  // width. The result is the effective child width capped at the AT_MOST bound.
  @Test
  fun testV2VScrollWrapWidthCrossAxisMarginsAffectMax() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.exactly(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = false,
        items,
        uniformMargins(5, left = 100, right = 100),
    )
    // Effective child width is 300. The cross-axis max of 300 is less than 600.
    // Height is not wrapping so it stays at 800. After all 5: min(300, 600) = 300.
    Assertions.assertThat(size[0]).isEqualTo(300)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(5)
  }

  // === V2 LTR/RTL Symmetry ===

  // Asymmetric left/right margins with the same total horizontal margin produce identical
  // V2 measurement results, confirming LTR/RTL layout parity.
  @Test
  fun testV2VScrollAsymmetricHorizontalMarginsSameTotal() {
    val childWidth = 200
    val childHeight = 100
    val itemsA = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val sizeA = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = true,
        itemsA,
        uniformMargins(5, left = 30, right = 10),
    )
    val itemsB = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(5, childWidth, childHeight),
    )
    val sizeB = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.unspecified(),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = true,
        wrapHeight = true,
        itemsB,
        uniformMargins(5, left = 10, right = 30),
    )
    // Both configurations have total horizontal margins of 40, giving effective width 240.
    // The results must match.
    Assertions.assertThat(sizeA[0]).isEqualTo(sizeB[0])
    Assertions.assertThat(sizeA[1]).isEqualTo(sizeB[1])
    Assertions.assertThat(sizeA[0]).isEqualTo(240)
    Assertions.assertThat(sizeA[1]).isEqualTo(500)
    Assertions.assertThat(measurePrepared(itemsA)).isEqualTo(measurePrepared(itemsB))
  }

  // === V2 Edge Cases ===

  // In V2, a single child is both first and last, so it receives both spaceBefore and
  // spaceAfter in addition to its margins.
  @Test
  fun testV2VScrollSingleChildWithMargins() {
    val childWidth = 200
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(1, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.atMost(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        20,
        0,
        20,
        wrapWidth = true,
        wrapHeight = true,
        items,
        uniformMargins(1, left = 10, top = 20, right = 10, bottom = 20),
    )
    // Single child (first AND last): effective 220×140
    // In vertical scroll, spaceBefore=20 and spaceAfter=20 plus the child height of 140
    // gives a total offset height of 180.
    Assertions.assertThat(size[0]).isEqualTo(220)
    Assertions.assertThat(size[1]).isEqualTo(180)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  // An empty margin list behaves identically to null margins in V2, meaning all children
  // get zero margins.
  @Test
  fun testV2VScrollEmptyMarginList() {
    val childWidth = 300
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        20,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
        emptyList(),
    )
    // An empty list means zero margins for all children, same result as null. 7 children
    // are measured (with spacing of 20 between each).
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(7)
  }

  // When a single child's margins exceed the entire AT_MOST budget in V2, the measurement
  // caps at the bound after measuring just that one child.
  @Test
  fun testV2VScrollLargeMarginsExceedAtMost() {
    val childWidth = 100
    val childHeight = 100
    val items = createListOfItems(
        layoutContext,
        MeasureSpecUtils.exactly(childWidth),
        MeasureSpecUtils.exactly(childHeight),
        createChildren(20, childWidth, childHeight),
    )
    val size = measureListV2(
        layoutContext,
        MeasureSpecUtils.exactly(600),
        MeasureSpecUtils.atMost(800),
        RecyclerView.VERTICAL,
        0,
        0,
        0,
        wrapWidth = false,
        wrapHeight = true,
        items,
        uniformMargins(20, top = 500, bottom = 500),
    )
    // Effective child height is 1100, which exceeds the AT_MOST bound of 800, so measurement
    // caps after the first child.
    Assertions.assertThat(size[0]).isEqualTo(600)
    Assertions.assertThat(size[1]).isEqualTo(800)
    Assertions.assertThat(measurePrepared(items)).isEqualTo(1)
  }

  private fun createListOfItems(
      c: LayoutContext<Any?>,
      childWidthSpec: Int,
      childHeightSpec: Int,
      children: ImmutableList<TestNode>,
  ): List<ListItem<TestNode>> {
    val result: MutableList<ListItem<TestNode>> = ArrayList()
    for (i in children.indices) {
      val child = children[i]
      val renderResultFuture: RenderResultFuture<*, *> = RenderResultFuture<Any?, Any?>(
          c.androidContext,
          ResolveResult<Node<Any?>, Any?>(child),
          c.renderContext,
          null,
          null,
          -1,
          SizeConstraints.fromMeasureSpecs(childWidthSpec, childHeightSpec),
      )
      result.add(
          object : ListItem<TestNode> {
            override fun getId(): Long = child.id

            override fun getNode(): TestNode = child

            override fun getRenderResultFuture(): RenderResultFuture<*, *> = renderResultFuture

            override fun isEquivalentTo(other: ListItem<TestNode>): Boolean = false
          },
      )
    }
    return result
  }

  companion object {
    private fun createChildren(size: Int, width: Int, height: Int): ImmutableList<TestNode> {
      val builder = ImmutableList.Builder<TestNode>()
      for (i in 0 until size) {
        builder.add(TestNode(0, 0, width, height))
      }
      return builder.build()
    }

    private fun measureList(
        c: LayoutContext<Any?>,
        widthSpec: Int,
        heightSpec: Int,
        orientation: Int,
        spaceBefore: Int,
        spaceBetween: Int,
        spaceAfter: Int,
        items: List<ListItem<TestNode>>,
        childMargins: List<Rect>? = null,
    ): IntArray =
        MeasureListUtils.measureListSize<Any, TestNode>(
            c,
            widthSpec,
            heightSpec,
            spaceBefore,
            spaceBetween,
            spaceAfter,
            orientation,
            false,
            items,
            childMargins,
            MeasureListUtils.NO_PADDING,
        )

    private fun measurePrepared(items: List<ListItem<TestNode>>): Int {
      var prepared = 0
      for (i in items.indices) {
        if (items[i].renderResultFuture.isDone) {
          prepared++
        }
      }
      return prepared
    }

    private fun measureListV2(
        c: LayoutContext<Any?>,
        widthSpec: Int,
        heightSpec: Int,
        orientation: Int,
        spaceBefore: Int,
        spaceBetween: Int,
        spaceAfter: Int,
        wrapWidth: Boolean,
        wrapHeight: Boolean,
        items: List<ListItem<TestNode>>,
        childMargins: List<Rect>? = null,
    ): IntArray =
        MeasureListUtils.measureListSizeV2<Any, TestNode>(
            c,
            widthSpec,
            heightSpec,
            spaceBefore,
            spaceBetween,
            spaceAfter,
            orientation,
            wrapWidth,
            wrapHeight,
            false,
            items,
            childMargins,
            MeasureListUtils.NO_PADDING,
        )

    private fun uniformMargins(
        count: Int,
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0,
    ): List<Rect> = List(count) { Rect(left, top, right, bottom) }

    private fun marginsOf(vararg rects: Rect): List<Rect> = rects.toList()
  }
}
