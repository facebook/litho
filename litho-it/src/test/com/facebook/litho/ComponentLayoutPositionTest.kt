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
import com.facebook.litho.core.padding
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.rendercore.px
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The position a MountSpec reads from its `ComponentLayout`, whose own result doesn't hold it: its
 * parent decides it.
 */
@RunWith(LithoTestRunner::class)
class ComponentLayoutPositionTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  /**
   * While it is measured, before anything places it, it reads where its engine positioned it: the
   * origin the first time, and on a relayout the position its last layout gave it, which the copy
   * of its Yoga node carries.
   */
  @Test
  fun `@OnMeasure reads where its engine last positioned it`() {
    val tracker = LifecycleTracker()
    val view =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column(style = Style.padding(start = 7.px, top = 9.px)) { child(leaf(tracker)) }
        }

    view.setSizePx(VIEWPORT - 100, VIEWPORT).measure().layout()

    assertThat(tracker.measuredAt.map { it.x to it.y })
        .describedAs("the first pass, then the relayout")
        .containsExactly(0 to 0, 7 to 9)
  }

  /**
   * A MountSpec that deferred content resolves to reads its position in that content's own tree,
   * the margins its root sits at; its holder places the content there, so it mounts at the Column's
   * padding plus those margins.
   */
  @Test
  fun `a MountSpec in deferred content is positioned in the content's tree`() {
    val tracker = LifecycleTracker()

    val view =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column(style = Style.padding(start = 7.px, top = 9.px)) { child(DeferredLeaf(tracker)) }
        }

    assertThat(tracker.definedBounds.left to tracker.definedBounds.top)
        .describedAs("what @OnBoundsDefined reads")
        .isEqualTo(5 to 3)
    val mounted = mountBoundsOf(view)
    assertThat(mounted.left to mounted.top).describedAs("where it mounts").isEqualTo(12 to 12)
  }

  /** Laid out again, deferred content mounts where it did, whether reused or laid out anew. */
  @Test
  fun `deferred content under a Yoga root mounts at the same place when laid out again`() {
    val tracker = LifecycleTracker()
    val view =
        lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) {
          Column(style = Style.padding(start = 7.px, top = 9.px)) { child(DeferredLeaf(tracker)) }
        }
    val first = mountBoundsOf(view)

    view.setSizePx(VIEWPORT, VIEWPORT - 100).measure().layout()

    val second = mountBoundsOf(view)
    assertThat(first.left to first.top).describedAs("the first pass").isEqualTo(12 to 12)
    assertThat(second.left to second.top).describedAs("laid out again").isEqualTo(12 to 12)
  }

  /** The MountSpec's mount bounds, in its host's coordinates. */
  private fun mountBoundsOf(view: TestLithoView): Rect {
    val layoutState = checkNotNull(view.committedLayoutState)
    for (i in 0 until layoutState.getMountableOutputCount()) {
      val node = layoutState.getMountableOutputAt(i)
      if ((node.renderUnit as? LithoRenderUnit)?.component is MountSpecLifecycleTester) {
        return node.bounds
      }
    }
    error("no MountSpec output")
  }

  private fun leaf(tracker: LifecycleTracker): Component =
      MountSpecLifecycleTester.create(lithoTestRule.context)
          .lifecycleTracker(tracker)
          .intrinsicSize(Size(40, 30))
          .build()

  private companion object {
    const val VIEWPORT = 500
  }
}

/** Deferred content: a MountSpec at a start margin of 5 and a top margin of 3. */
private class DeferredLeaf(private val tracker: LifecycleTracker) : KComponent() {
  override fun ComponentScope.render(): Component = RenderWithConstraints { _ ->
    MountSpecLifecycleTester.create(context)
        .lifecycleTracker(tracker)
        .intrinsicSize(Size(40, 30))
        .marginPx(YogaEdge.START, 5)
        .marginPx(YogaEdge.TOP, 3)
        .build()
  }
}
