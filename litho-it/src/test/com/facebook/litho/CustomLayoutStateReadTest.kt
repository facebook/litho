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

import android.view.View
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * State read while laying a container out reaches the next layout.
 *
 * With read tracking on, a state update skips layout entirely unless something recorded reading
 * that state. A read made only during layout is therefore invisible unless layout records it, and a
 * container that reads its own state or drops a child is where layout can lose one.
 */
@RunWith(LithoTestRunner::class)
class CustomLayoutStateReadTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private val defaultConfig = ComponentsConfiguration.defaultInstance

  @Before
  fun setup() {
    ComponentsConfiguration.defaultInstance = defaultConfig.copy(enableStateReadTracking = true)
  }

  @After
  fun teardown() {
    ComponentsConfiguration.defaultInstance = defaultConfig
  }

  /**
   * A layout function that reads state lays its container out again when that state changes.
   *
   * The read happens only inside the layout function — a header that collapses against a scroll
   * offset reads it there — so nothing but layout can record it.
   */
  @Test
  fun `a layout function's state read lays its container out again`() {
    lateinit var offset: State<Int>

    class Collapsing : KComponent() {
      override fun ComponentScope.render(): Component {
        val state = useState { 0 }
        offset = state
        return Layout(
            listOf(SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))),
            layout =
                LayoutFunctionN { measurables, _ ->
                  val p = measurables[0].measure(SizeConstraints.exact(CHILD, CHILD))
                  val y = state.value
                  layout(CHILD, CHILD + y) { p.place(x = 0, y = y) }
                },
        )
      }
    }

    val testView = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { Collapsing() }
    assertThat(containerOf(testView).getYForChildAtIndex(0)).isEqualTo(0)

    lithoTestRule.act(testView) { offset.updateSync(OFFSET) }
    lithoTestRule.idle()

    assertThat(containerOf(testView).getYForChildAtIndex(0))
        .describedAs("placed at the new offset")
        .isEqualTo(OFFSET)
  }

  /**
   * A container whose size depends on a child grows when the child grows through state only the
   * child reads. The container attaches no Yoga children, so it relies on being marked dirty when
   * it is partly reused.
   */
  @Test
  fun `a leaf that grows through its own state grows its container`() {
    lateinit var h: State<Int>

    class Growing : KComponent() {
      override fun ComponentScope.render(): Component {
        val s = useState { CHILD }
        h = s
        return SimpleMountSpecTester(style = Style.width(CHILD.px).height(s.value.px))
      }
    }

    val testView =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column(alignItems = YogaAlign.FLEX_START) {
            child(Layout(listOf(Growing()), layout = tallAsChild))
          }
        }
    lithoTestRule.act(testView) { h.updateSync(CHILD * 2) }
    lithoTestRule.idle()

    assertThat(containerOf(testView).height).isEqualTo(CHILD * 2)
  }

  /** As above, with the child a container whose own height is read only in its layout function. */
  @Test
  fun `a nested container that grows through its own state grows its parent`() {
    lateinit var h: State<Int>

    class GrowingLayout : KComponent() {
      override fun ComponentScope.render(): Component {
        val s = useState { CHILD }
        h = s
        return Layout(
            listOf(SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))),
            layout =
                LayoutFunctionN { ms, _ ->
                  val p = ms[0].measure(SizeConstraints.exact(CHILD, CHILD))
                  layout(CHILD, s.value) { p.place(0, 0) }
                },
        )
      }
    }

    val testView =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column(alignItems = YogaAlign.FLEX_START) {
            child(Layout(listOf(GrowingLayout()), layout = tallAsChild))
          }
        }
    lithoTestRule.act(testView) { h.updateSync(CHILD * 2) }
    lithoTestRule.idle()

    assertThat(containerOf(testView).height).isEqualTo(CHILD * 2)
  }

  /** State read only where children are placed lays the container out again too. */
  @Test
  fun `a state read only while placing lays the container out again`() {
    lateinit var offset: State<Int>

    class Sliding : KComponent() {
      override fun ComponentScope.render(): Component {
        val state = useState { 0 }
        offset = state
        return Layout(
            listOf(SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))),
            layout =
                LayoutFunctionN { measurables, c ->
                  val p = measurables[0].measure(SizeConstraints.exact(CHILD, CHILD))
                  layout(c.maxWidth, CHILD * 2) { p.place(x = 0, y = state.value) }
                },
        )
      }
    }

    val testView = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { Sliding() }
    lithoTestRule.act(testView) { offset.updateSync(OFFSET) }
    lithoTestRule.idle()

    assertThat(containerOf(testView).getYForChildAtIndex(0))
        .describedAs("placed at the new offset")
        .isEqualTo(OFFSET)
  }

  /**
   * A child the container leaves out because it does not fit is placed once it shrinks through
   * state only the child reads: whether it fits was decided from a measurement nothing placed.
   */
  @Test
  fun `a child left out because it did not fit is placed once its own state shrinks it`() {
    lateinit var w: State<Int>

    // Its width is read only while it is laid out, so only a layout records the read.
    class Sized : KComponent() {
      override fun ComponentScope.render(): Component {
        val s = useState { CHILD * 2 }
        w = s
        return Layout(
            listOf(SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))),
            layout =
                LayoutFunctionN { ms, _ ->
                  val p = ms[0].measure(SizeConstraints.exact(CHILD, CHILD))
                  layout(s.value, CHILD) { p.place(0, 0) }
                },
        )
      }
    }
    val placesIfFits = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity, maxHeight = CHILD))
      layout(c.maxWidth, CHILD) { if (p.width <= CHILD) p.place(0, 0) }
    }

    val testView =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Layout(listOf(Sized()), layout = placesIfFits)
        }
    assertThat(rootOf(testView).childCount).describedAs("too wide: left out").isEqualTo(0)

    lithoTestRule.act(testView) { w.updateSync(CHILD / 2) }
    lithoTestRule.idle()

    assertThat(rootOf(testView).childCount).describedAs("fits now: placed").isEqualTo(1)
  }

  /**
   * A placement that fails for a state it read lays out again once that state changes, and
   * succeeds: the read is kept although the placement threw.
   */
  @Test
  fun `a placement that fails because of a state it read recovers when that state changes`() {
    lateinit var ready: State<Boolean>

    class Guarded : KComponent() {
      override fun ComponentScope.render(): Component {
        val state = useState { false }
        ready = state
        useErrorBoundary { _: Exception -> }
        return Layout(
            listOf(SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))),
            layout =
                LayoutFunctionN { ms, c ->
                  val p = ms[0].measure(SizeConstraints.exact(CHILD, CHILD))
                  layout(c.maxWidth, CHILD) {
                    check(state.value) { "not ready" }
                    p.place(0, 0)
                  }
                },
        )
      }
    }

    val testView = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { Guarded() }
    assertThat(containerOf(testView).childCount).describedAs("failed: nothing placed").isEqualTo(0)

    lithoTestRule.act(testView) { ready.updateSync(true) }
    lithoTestRule.idle()

    assertThat(containerOf(testView).childCount).describedAs("placed once ready").isEqualTo(1)
  }

  /**
   * A layout function that fails because of what a child measured, from state the child reads only
   * while laying out, lays out again once that state changes, and succeeds.
   */
  @Test
  fun `a function that fails on a child's state-driven size recovers when the state changes`() {
    lateinit var width: State<Int>

    class Host : KComponent() {
      override fun ComponentScope.render(): Component {
        val state = useState { CHILD * 2 }
        width = state
        useErrorBoundary { _: Exception -> }
        return Layout(
            listOf(WidthFromState(state)),
            layout =
                LayoutFunctionN { ms, c ->
                  val p = ms[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity))
                  check(p.width <= CHILD) { "too wide" }
                  layout(c.maxWidth, CHILD) { p.place(0, 0) }
                },
        )
      }
    }

    val testView = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { Host() }
    assertThat(containerOf(testView).childCount).describedAs("failed: nothing placed").isEqualTo(0)

    lithoTestRule.act(testView) { width.updateSync(CHILD / 2) }
    lithoTestRule.idle()

    assertThat(containerOf(testView).childCount).describedAs("placed once it fits").isEqualTo(1)
  }

  private fun rootOf(testView: TestLithoView): LithoLayoutResult =
      checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
          as LithoLayoutResult

  private fun containerOf(testView: TestLithoView): LithoLayoutResult {
    val root =
        checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    return checkNotNull(findContainer(root)) { "container not found" }
  }

  private fun findContainer(result: LithoLayoutResult): LithoLayoutResult? {
    if (result.node.tailComponent is Layout) return result
    for (i in 0 until result.childCount) {
      findContainer(result.getChildAt(i))?.let {
        return it
      }
    }
    return null
  }

  companion object {
    private const val VIEWPORT = 1000
    private const val CHILD = 100
    private const val OFFSET = 30
  }

  /** As tall and wide as its only child, measured without a height bound. */
  private val tallAsChild = LayoutFunctionN { ms, c ->
    val p = ms[0].measure(SizeConstraints(0, c.maxWidth, 0, SizeConstraints.Infinity))
    layout(p.width, p.height) { p.place(0, 0) }
  }
}

/** A Primitive as wide as [width] says, read only while it lays out. */
private class WidthFromState(private val width: State<Int>) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = WidthFromStateBehavior(width),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class WidthFromStateBehavior(private val width: State<Int>) : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(width = width.value, height = 100)
}
