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

import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.px
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A [DeferredLithoLayoutResult] holds the resolved content of deferred layout
 * (`RenderWithConstraints`) in place of a Yoga child: it reports where that content sits and
 * releases it with the pass.
 */
@RunWith(LithoTestRunner::class)
class DeferredLithoLayoutResultTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `deferred holder is flagged and exposes its resolved content`() {
    val holder = deferredHolderOf(yogaDeferredTree())

    assertThat(holder.isDeferredLayoutResult).describedAs("deferred flag").isTrue()
    assertThat(holder.measuredLayoutResult)
        .describedAs("resolved content")
        .isNotNull()
        .isSameAs(holder.result)
  }

  @Test
  fun `ordinary result is not flagged and has no resolved content`() {
    val view =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column { child(SimpleMountSpecTester(style = Style.width(20.px).height(20.px))) }
        }
    val leaf = rootOf(view).getChildAt(0)

    assertThat(leaf.isDeferredLayoutResult).describedAs("deferred flag").isFalse()
    assertThat(leaf.measuredLayoutResult).describedAs("resolved content").isNull()
  }

  @Test
  fun `holder node is the deferred node`() {
    val holder = deferredHolderOf(yogaDeferredTree())

    assertThat(holder.node).describedAs("holder node").isInstanceOf(DeferredLithoNode::class.java)
    assertThat(holder.node.isDeferredNode).describedAs("deferred node flag").isTrue()
    assertThat(holder.result?.node?.isDeferredNode)
        .describedAs("resolved content node is real")
        .isFalse()
  }

  @Test
  fun `yoga content is positioned at its own-tree offset`() {
    val holder = deferredHolderOf(yogaDeferredTree())

    assertThat(holder.getXForChildAtIndex(0)).describedAs("child x").isEqualTo(5)
    assertThat(holder.getYForChildAtIndex(0)).describedAs("child y").isEqualTo(3)
  }

  @Test
  fun `layout-root content sits at the root margins`() {
    val holder = deferredHolderOf(layoutRootDeferredTree(withMargins = true))

    assertThat(holder.getXForChildAtIndex(0)).describedAs("child x").isEqualTo(5)
    assertThat(holder.getYForChildAtIndex(0)).describedAs("child y").isEqualTo(3)
  }

  @Test
  fun `marginless layout-root content sits at the origin`() {
    val holder = deferredHolderOf(layoutRootDeferredTree(withMargins = false))

    assertThat(holder.getXForChildAtIndex(0)).describedAs("child x").isEqualTo(0)
    assertThat(holder.getYForChildAtIndex(0)).describedAs("child y").isEqualTo(0)
  }

  @Test
  fun `child index beyond zero throws`() {
    val holder = deferredHolderOf(yogaDeferredTree())

    assertThatThrownBy { holder.getXForChildAtIndex(1) }
        .isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy { holder.getYForChildAtIndex(1) }
        .isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `unresolved holder positions its child at the origin`() {
    val holder = deferredHolderOf(yogaDeferredTree()).node.createLayoutResult(freshYogaOutput())

    assertThat(holder.result).describedAs("resolved content").isNull()
    assertThat(holder.measuredLayoutResult).describedAs("resolved content").isNull()
    assertThat(holder.getXForChildAtIndex(0)).describedAs("child x").isEqualTo(0)
    assertThat(holder.getYForChildAtIndex(0)).describedAs("child y").isEqualTo(0)
  }

  @Test
  fun `release drops the content context but keeps its position`() {
    val holder = deferredHolderOf(yogaDeferredTree())
    val content = checkNotNull(holder.measuredLayoutResult)
    val contentYogaNode = (content.measureOutput as YogaLayoutOutput).yogaNode
    contentYogaNode.data = Any()

    holder.releaseLayoutPhaseData()

    assertThat(contentYogaNode.data).describedAs("released content context").isNull()
    assertThat(holder.getXForChildAtIndex(0)).describedAs("child x kept").isEqualTo(5)
    assertThat(holder.getYForChildAtIndex(0)).describedAs("child y kept").isEqualTo(3)
  }

  private fun yogaDeferredTree(): TestLithoView =
      lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
        Column { child(YogaDeferredChild()) }
      }

  private fun layoutRootDeferredTree(withMargins: Boolean): TestLithoView =
      lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
        Column {
          child(if (withMargins) MarginedLayoutDeferredChild() else MarginlessLayoutDeferredChild())
        }
      }

  private fun deferredHolderOf(view: TestLithoView): DeferredLithoLayoutResult {
    val all = ArrayList<LithoLayoutResult>()
    fun walk(result: LithoLayoutResult) {
      all.add(result)
      for (i in 0 until result.childCount) {
        walk(result.getChildAt(i))
      }
    }
    walk(rootOf(view))
    return all.filterIsInstance<DeferredLithoLayoutResult>().single()
  }

  private fun rootOf(view: TestLithoView): LithoLayoutResult {
    val root = checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
    return root as LithoLayoutResult
  }

  private fun freshYogaOutput(): YogaLayoutOutput {
    return YogaLayoutOutput(
        yogaNode = NodeConfig.createYogaNode(),
        widthFromStyle = YogaValue.UNDEFINED,
        heightFromStyle = YogaValue.UNDEFINED,
        minWidthFromStyle = YogaValue.UNDEFINED,
        maxWidthFromStyle = YogaValue.UNDEFINED,
        minHeightFromStyle = YogaValue.UNDEFINED,
        maxHeightFromStyle = YogaValue.UNDEFINED,
    )
  }

  private companion object {
    const val VIEWPORT = 500
  }
}

/** Deferred content resolving to a MountSpec at a start margin of 5 and a top margin of 3. */
private class YogaDeferredChild : KComponent() {
  override fun ComponentScope.render(): Component = RenderWithConstraints { _ ->
    MountSpecLifecycleTester.create(context)
        .lifecycleTracker(LifecycleTracker())
        .intrinsicSize(Size(40, 30))
        .marginPx(YogaEdge.START, 5)
        .marginPx(YogaEdge.TOP, 3)
        .build()
  }
}

/** Deferred content whose root is a `Layout` at a left margin of 5 and a top margin of 3. */
private class MarginedLayoutDeferredChild : KComponent() {
  override fun ComponentScope.render(): Component = RenderWithConstraints { _ ->
    Layout(
        listOf(
            MountSpecLifecycleTester.create(context)
                .lifecycleTracker(LifecycleTracker())
                .intrinsicSize(Size(40, 30))
                .build(),
        ),
        style = Style.margin(left = 5.px, top = 3.px),
        layout = PLACES_AT_2_4,
    )
  }
}

/** Deferred content whose root is a marginless `Layout`. */
private class MarginlessLayoutDeferredChild : KComponent() {
  override fun ComponentScope.render(): Component = RenderWithConstraints { _ ->
    Layout(
        listOf(
            MountSpecLifecycleTester.create(context)
                .lifecycleTracker(LifecycleTracker())
                .intrinsicSize(Size(40, 30))
                .build(),
        ),
        layout = PLACES_AT_2_4,
    )
  }
}

private val PLACES_AT_2_4 = LayoutFunctionN { measurables, constraints ->
  val placeable = measurables[0].measure(constraints)
  layout(placeable.width + 2, placeable.height + 4) { placeable.place(2, 4) }
}
