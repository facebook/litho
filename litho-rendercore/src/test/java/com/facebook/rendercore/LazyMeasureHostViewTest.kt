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

package com.facebook.rendercore

import android.content.Context
import android.view.View
import com.facebook.rendercore.RenderResult.Companion.render
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LazyMeasureHostViewTest {

  private lateinit var context: Context
  private lateinit var hostView: TestLazyMeasureHostView

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    hostView = TestLazyMeasureHostView(context)
  }

  // -- onMeasure tests --

  @Test
  fun onMeasure_withNoProvider_setsMeasuredDimensionToZero() {
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
    )
    assertThat(hostView.measuredWidth).isEqualTo(0)
    assertThat(hostView.measuredHeight).isEqualTo(0)
  }

  @Test
  fun onMeasure_withProvider_setsMeasuredDimensionFromRenderTree() {
    val node = TestNode(0, 0, 150, 250)
    node.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(150)
    assertThat(hostView.measuredHeight).isEqualTo(250)
  }

  @Test
  fun onMeasure_afterClearingProvider_resetsDimensionsToZero() {
    val node = TestNode(0, 0, 150, 250)
    node.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(150)

    hostView.setLazyRenderTreeProvider(null)
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(0)
    assertThat(hostView.measuredHeight).isEqualTo(0)
  }

  // -- setLazyRenderTreeProvider tests --

  @Test
  fun setLazyRenderTreeProvider_withSameProvider_doesNotRequestLayout() {
    val node = TestNode()
    node.setRenderUnit(TestRenderUnit())
    val provider = createProvider(node)
    hostView.setLazyRenderTreeProvider(provider)

    // Force layout to clear the layout-requested flag
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    hostView.layout(0, 0, 100, 100)
    assertThat(hostView.isLayoutRequested).isFalse

    // Setting the same provider should be a no-op
    hostView.setLazyRenderTreeProvider(provider)
    assertThat(hostView.isLayoutRequested).isFalse
  }

  @Test
  fun setLazyRenderTreeProvider_withDifferentProvider_requestsLayout() {
    val node1 = TestNode()
    node1.setRenderUnit(TestRenderUnit())
    val node2 = TestNode()
    node2.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node1))

    // Force layout to clear the layout-requested flag
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    hostView.layout(0, 0, 100, 100)
    assertThat(hostView.isLayoutRequested).isFalse

    hostView.setLazyRenderTreeProvider(createProvider(node2))
    assertThat(hostView.isLayoutRequested).isTrue
  }

  @Test
  fun setLazyRenderTreeProvider_withNull_clearsCurrentRenderResult() {
    val node = TestNode(0, 0, 80, 80)
    node.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(80)

    hostView.setLazyRenderTreeProvider(null)
    assertThat(hostView.getTestCurrentRenderTree()).isNull()
  }

  @Test
  fun setLazyRenderTreeProvider_nullAfterMount_unmountsAndClearsRenderTree() {
    val node = TestNode(0, 0, 70, 70)
    node.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
    assertThat(hostView.getTestCurrentRenderTree()).isNotNull

    hostView.setLazyRenderTreeProvider(null)
    assertThat(hostView.getTestCurrentRenderTree()).isNull()

    // After clearing, measure should return 0,0
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(0)
    assertThat(hostView.measuredHeight).isEqualTo(0)
  }

  // -- performLayout tests --

  @Test
  fun performLayout_withNoProvider_renderTreeRemainsNull() {
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    )
    hostView.layout(0, 0, 100, 100)
    // With no provider, render tree should remain null and dimensions should be 0
    assertThat(hostView.getTestCurrentRenderTree()).isNull()
    assertThat(hostView.measuredWidth).isEqualTo(0)
    assertThat(hostView.measuredHeight).isEqualTo(0)
  }

  @Test
  fun performLayout_withProvider_mountsRenderTree() {
    val node = TestNode()
    val renderUnit = TestRenderUnit()
    val didMount = BooleanArray(1)
    renderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            SimpleTestBinder { didMount[0] = true },
        ),
    )
    node.setRenderUnit(renderUnit)
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(didMount[0]).isFalse
    hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
    assertThat(didMount[0]).isTrue
  }

  @Test
  fun performLayout_afterProviderChange_mountsNewRenderTree() {
    val node1 = TestNode(0, 0, 60, 60)
    val renderUnit1 = TestRenderUnit()
    val mountCount = IntArray(1)
    renderUnit1.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            SimpleTestBinder { mountCount[0]++ },
        ),
    )
    node1.setRenderUnit(renderUnit1)
    hostView.setLazyRenderTreeProvider(createProvider(node1))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
    assertThat(mountCount[0]).isEqualTo(1)

    // Change provider and re-layout
    val node2 = TestNode(0, 0, 80, 80)
    val renderUnit2 = TestRenderUnit()
    val mountCount2 = IntArray(1)
    renderUnit2.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            SimpleTestBinder { mountCount2[0]++ },
        ),
    )
    node2.setRenderUnit(renderUnit2)
    hostView.setLazyRenderTreeProvider(createProvider(node2))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
    assertThat(mountCount2[0]).isEqualTo(1)
    assertThat(hostView.measuredWidth).isEqualTo(80)
  }

  // -- getCurrentRenderTree tests --

  @Test
  fun getCurrentRenderTree_beforeMeasure_returnsNull() {
    assertThat(hostView.getTestCurrentRenderTree()).isNull()
  }

  @Test
  fun getCurrentRenderTree_afterMeasureWithProvider_returnsNonNull() {
    val node = TestNode(0, 0, 50, 50)
    node.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    val renderTree = hostView.getTestCurrentRenderTree()
    assertThat(renderTree).isNotNull
    assertThat(renderTree!!.width).isEqualTo(50)
    assertThat(renderTree.height).isEqualTo(50)
  }

  // -- lifecycle tests --

  @Test
  fun onDetachedAndReattached_viewStillFunctionsCorrectly() {
    val node = TestNode(0, 0, 60, 70)
    node.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)

    hostView.onDetachedFromWindow()
    hostView.onAttachedToWindow()

    // After reattach, view should still function correctly with a new provider
    val node2 = TestNode(0, 0, 40, 45)
    node2.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node2))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(40)
    assertThat(hostView.measuredHeight).isEqualTo(45)
  }

  // -- measure with different specs --

  @Test
  fun onMeasure_calledMultipleTimes_updatesCorrectly() {
    val node1 = TestNode(0, 0, 50, 60)
    node1.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node1))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(50)
    assertThat(hostView.measuredHeight).isEqualTo(60)

    // Change to a different provider with different dimensions
    val node2 = TestNode(0, 0, 90, 95)
    node2.setRenderUnit(TestRenderUnit())
    hostView.setLazyRenderTreeProvider(createProvider(node2))
    hostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
    )
    assertThat(hostView.measuredWidth).isEqualTo(90)
    assertThat(hostView.measuredHeight).isEqualTo(95)
  }

  // -- helper classes --

  private class TestLazyMeasureHostView(context: Context) : LazyMeasureHostView(context) {
    fun getTestCurrentRenderTree(): RenderTree? = getCurrentRenderTree()
  }

  // -- helper methods --

  @Suppress("UNCHECKED_CAST")
  private fun createProvider(node: TestNode): LazyMeasureHostView.LazyRenderTreeProvider<Any?> {
    return LazyMeasureHostView.LazyRenderTreeProvider { sizeConstraints, previousResult ->
      render(
          context,
          ResolveResult<Node<Any?>, Any?>(node),
          null,
          null,
          previousResult as RenderResult<Any?, Any?>?,
          0,
          sizeConstraints,
      )
    }
  }
}
