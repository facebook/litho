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
import android.view.View
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

/**
 * A container can position its own children.
 *
 * The assertions here are chosen against specific ways this can fail quietly:
 * - children never reaching the layout result at all, so the container renders empty
 * - a child that is itself a container measuring 0x0, because nothing laid out its subtree
 * - positions silently coming from Yoga's flex rather than from the container
 * - incremental mount losing the ability to cull, which is the whole reason for one tree
 */
@RunWith(LithoTestRunner::class)
class CustomChildPlacementTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  /** Lays children out left to right at a fixed stride, the way a pager does. */
  private fun horizontalStride(stride: Int): LayoutFunctionN =
      LayoutFunctionN { measurables, constraints ->
        val placeables = measurables.map { it.measure(SizeConstraints.exact(CHILD_W, CHILD_H)) }
        layout(
            width = if (constraints.hasBoundedWidth) constraints.maxWidth else CHILD_W,
            height = placeables.maxOfOrNull { it.height } ?: 0,
        ) {
          placeables.forEachIndexed { i, p -> p.place(x = i * stride, y = 0) }
        }
      }

  /** Stacks children vertically, so a short viewport leaves most of them outside it. */
  private fun verticalStride(): LayoutFunctionN = LayoutFunctionN { measurables, constraints ->
    val placeables = measurables.map { it.measure(SizeConstraints.exact(CHILD_W, CHILD_H)) }
    layout(width = CHILD_W, height = placeables.size * CHILD_H) {
      placeables.forEachIndexed { i, p ->
        p.place(x = 0, y = i * CHILD_H)
      }
    }
  }

  /**
   * Measures every child at the same size, then places only the first [placeCount] of them.
   *
   * The container's size does not depend on how many it placed, so renders using this differ in
   * exactly one thing: which children were placed.
   */
  private fun placeSome(placeCount: Int): LayoutFunctionN =
      LayoutFunctionN { measurables, constraints ->
        val placeables = measurables.map { it.measure(SizeConstraints.exact(CHILD_W, CHILD_H)) }
        layout(width = CHILD_W, height = VIEWPORT_H) {
          placeables.take(placeCount).forEachIndexed { i, p -> p.place(x = 0, y = i * CHILD_H) }
        }
      }

  @Test
  fun `a child that is itself a container is laid out`() {
    val result = renderContainer(horizontalStride(STRIDE), childCount = 3)

    assertThat(result.childCount)
        .describedAs("if the children never reach the result, the container renders empty")
        .isEqualTo(3)
    // The height is what tells: measured at CHILD_H, where the child's own content is INNER_H.
    for (i in 0 until result.childCount) {
      assertThat(result.getChildAt(i).width)
          .describedAs("child $i width — zero here means nothing laid out the child's own subtree")
          .isEqualTo(CHILD_W)
      assertThat(result.getChildAt(i).height).describedAs("child $i height").isEqualTo(CHILD_H)
    }
  }

  @Test
  fun `positions come from the container, not from Yoga`() {
    val result = renderContainer(horizontalStride(STRIDE), childCount = 3)

    assertThat((0 until result.childCount).map { result.getXForChildAtIndex(it) })
        .describedAs("Yoga would have stacked these; the stride is the container's own")
        .containsExactly(0, STRIDE, 2 * STRIDE)
  }

  /**
   * A grandchild mounts where its parent was placed. Read from the render tree, whose absolute
   * bounds are what the reducer folds a container's placements into; the layout result alone holds
   * positions relative to each parent and would pass with the placements ignored.
   */
  @Test
  fun `grandchildren are positioned inside their parent`() {
    val testView =
        lithoTestRule.render(widthPx = VIEWPORT_W, heightPx = VIEWPORT_H) {
          Column {
            child(
                Layout(
                    List(2) {
                      Column(style = Style.width(CHILD_W.px).height(CHILD_H.px)) {
                        child(
                            SimpleMountSpecTester(
                                style = Style.width(CHILD_W.px).height(INNER_H.px),
                            ),
                        )
                      }
                    },
                    layout = horizontalStride(STRIDE),
                ),
            )
          }
        }

    val tree = checkNotNull(testView.committedLayoutState).toRenderTree()
    val grandchildLefts =
        (0 until tree.mountableOutputCount)
            .map { tree.getRenderTreeNodeAtIndex(it) }
            .filter { it.bounds.height() == INNER_H }
            .map { it.getAbsoluteBounds(Rect()).left }
            .sorted()
    assertThat(grandchildLefts)
        .describedAs("each grandchild mounts at the position its parent was placed at")
        .containsExactly(0, STRIDE)
  }

  /**
   * Only the children the container placed inside the viewport mount. Compared against the same
   * container holding only as many children as fit, so the numbers are relative: stacked by the
   * container, 10 children of CHILD_H in a VIEWPORT_H viewport mount exactly what 2 do.
   */
  @Test
  fun `incremental mount culls children the container placed outside the viewport`() {
    val fits = VIEWPORT_H / CHILD_H
    val ten = mountedInViewport(childCount = 10)

    assertThat(ten)
        .describedAs("the children placed below the viewport are culled")
        .isEqualTo(mountedInViewport(childCount = fits))
    assertThat(ten)
        .describedAs("and the ones inside it do mount")
        .isGreaterThan(mountedInViewport(childCount = 0))
  }

  private fun mountedInViewport(childCount: Int): Int {
    val testView =
        lithoTestRule.render(widthPx = CHILD_W, heightPx = VIEWPORT_H) {
          Column { child(Container(verticalStride(), childCount)) }
        }
    // Notifying with the rect the view already has is a no-op, so force a change first.
    testView.lithoView.notifyVisibleBoundsChanged(Rect(), true, VisibilityChangeReason.UNKNOWN)
    testView.lithoView.notifyVisibleBoundsChanged(
        Rect(0, 0, CHILD_W, VIEWPORT_H),
        true,
        VisibilityChangeReason.UNKNOWN,
    )
    return testView.lithoView.mountItemCount
  }

  @Test
  fun `measuring a child twice is allowed, and the last measure wins`() {
    val doubleMeasure = LayoutFunctionN { measurables, constraints ->
      measurables.first().measure(SizeConstraints.exact(CHILD_W, CHILD_H))
      val second = measurables.first().measure(SizeConstraints.exact(CHILD_W * 2, CHILD_H))
      layout(CHILD_W, CHILD_H) { second.place(0, 0) }
    }

    val result = renderContainer(doubleMeasure, childCount = 1)

    assertThat(result.getChildAt(0).width)
        .describedAs("flexbox measures at the flex basis, then again at the resolved size")
        .isEqualTo(CHILD_W * 2)
  }

  @Test
  fun `placing a child twice is refused`() {
    val doublePlace = LayoutFunctionN { measurables, constraints ->
      val p = measurables.first().measure(SizeConstraints.exact(CHILD_W, CHILD_H))
      layout(CHILD_W, CHILD_H) {
        p.place(0, 0)
        p.place(50, 50)
      }
    }

    assertThatThrownBy { renderContainer(doublePlace, childCount = 1) }
        .hasMessageContaining("has already been placed")
  }

  /**
   * A re-layout at a new size keeps the container's placements.
   *
   * Not the cache-clone guard in `LithoYogaLayoutFunction.buildYogaTree`: the clone path needs a
   * clean node, and changing size specs dirties it, so it is never reached from here. That guard is
   * covered in `LithoYogaLayoutCacheTest` by `a container that positions its own children is saved
   * into the layout cache` and the diff-node tests that reuse a cached container.
   */
  @Test
  fun `a re-layout at a new size keeps the container's placements`() {
    val testView =
        lithoTestRule.render(widthPx = VIEWPORT_W, heightPx = VIEWPORT_H) {
          Column { child(Container(horizontalStride(STRIDE), 3)) }
        }

    // A different size, deliberately: re-using the same specs short-circuits in isCompatibleSpec
    // and runs no layout at all, so the cache would never be consulted.
    lithoTestRule.idle()
    testView.setSizeSpecs(
        SizeSpec.makeSizeSpec(VIEWPORT_W, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(VIEWPORT_H + 137, SizeSpec.EXACTLY),
    )
    lithoTestRule.idle()

    val root = checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
    val container = checkNotNull(findCustomLayoutResult(root as LithoLayoutResult))

    assertThat(container.childCount).isEqualTo(3)
    assertThat(container.getXForChildAtIndex(2))
        .describedAs("the placements are recomputed at the new size, not lost")
        .isEqualTo(2 * STRIDE)
  }

  @Test
  fun `a component with no layout function is unaffected`() {
    val testView = lithoTestRule.render {
      Column {
        child(Column(style = Style.width(CHILD_W.px).height(CHILD_H.px)))
        child(Column(style = Style.width(CHILD_W.px).height(CHILD_H.px)))
      }
    }
    val root = checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)

    assertThat(findCustomLayoutResult(root as LithoLayoutResult))
        .describedAs("nothing asked to place its own children, so no custom result should exist")
        .isNull()
    assertThat(root.getYForChildAtIndex(1))
        .describedAs("Yoga must still stack these vertically exactly as before")
        .isEqualTo(CHILD_H)
  }

  private fun renderContainer(fn: LayoutFunctionN, childCount: Int): LithoLayoutResult {
    // Wrapped in a Column deliberately: Yoga does not call a measure function on a node whose size
    // is already definite, so a container used directly as the root under exact constraints would
    // never run its layout function.
    val testView =
        lithoTestRule.render(widthPx = VIEWPORT_W, heightPx = VIEWPORT_H) {
          Column { child(Container(fn, childCount)) }
        }
    val root = checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)

    return checkNotNull(findCustomLayoutResult(root as LithoLayoutResult)) {
      "no CustomLayoutResult in the tree — the container did not take the custom layout path"
    }
  }

  private fun mountItemCount(fn: LayoutFunctionN, childCount: Int): Int =
      lithoTestRule
          .render(widthPx = VIEWPORT_W, heightPx = VIEWPORT_H) {
            Column { child(Container(fn, childCount)) }
          }
          .lithoView
          .mountItemCount

  private fun findDiffNode(node: DiffNode, key: GlobalKey): DiffNode? {
    if (node.componentGlobalKey == key) return node
    for (i in 0 until node.childCount) {
      node.getChildAt(i)?.let { child ->
        findDiffNode(child, key)?.let {
          return it
        }
      }
    }
    return null
  }

  /** What `DebugInfoReporter` logged at WARNING: category, then attributes. */
  private fun lithoWarnings(): List<String> =
      ShadowLog.getLogsForTag("LithoWarning:").map { it.msg }

  private fun containerIn(testView: TestLithoView): LithoLayoutResult {
    val root =
        checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    return checkNotNull(findLayout(root)) { "container not found" }
  }

  private fun findLayout(result: LithoLayoutResult): LithoLayoutResult? {
    if (result.node.tailComponent is Layout) return result
    for (i in 0 until result.childCount) {
      findLayout(result.getChildAt(i))?.let {
        return it
      }
    }
    return null
  }

  /** A leaf as wide as [width] answers when it is measured. */
  private class StateSizedLeaf(private val width: () -> Int) : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
        layoutBehavior = StateSizedLayoutBehavior(width),
        mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
        style = null,
    )
  }

  private class StateSizedLayoutBehavior(private val width: () -> Int) : LayoutBehavior {
    override fun PrimitiveLayoutScope.layout(
        sizeConstraints: SizeConstraints,
    ): PrimitiveLayoutResult = PrimitiveLayoutResult(size = IntSize(width(), CHILD_H))
  }

  private fun findCustomLayoutResult(result: LithoLayoutResult): LithoLayoutResult? {
    if (result is CustomLayoutResult) return result
    for (i in 0 until result.childCount) {
      findCustomLayoutResult(result.getChildAt(i))?.let {
        return it
      }
    }
    return null
  }

  companion object {
    private const val VIEWPORT_W = 1000
    private const val VIEWPORT_H = 200
    private const val CHILD_W = 300
    private const val CHILD_H = 100
    private const val INNER_H = 40
    private const val STRIDE = 320
  }

  /** Minimal container whose children are themselves containers, which is the case that fails. */
  private class Container(
      private val layoutFunction: LayoutFunctionN,
      private val childCount: Int,
  ) : Component() {

    override fun resolve(
        resolveContext: ResolveContext,
        scopedComponentInfo: ScopedComponentInfo,
        parentWidthSpec: Int,
        parentHeightSpec: Int,
    ): ComponentResolveResult {
      val node = LithoNode()
      val c = scopedComponentInfo.context

      repeat(childCount) {
        node.child(
            resolveContext,
            c,
            Column(style = Style.backgroundColor(0xFF3B5998.toInt())) {
              child(Column(style = Style.width(CHILD_W.px).height(INNER_H.px)))
            },
        )
      }

      node.layoutFunction = layoutFunction
      return ComponentResolveResult(node, null)
    }

    override fun resolveDeferred(
        calculationContext: CalculationContext,
        componentContext: ComponentContext,
        parentContext: ComponentContext,
    ): ComponentResolveResult = ComponentResolveResult(null, null)
  }
}
