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
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextDrawable
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import java.lang.ref.WeakReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** What a `Layout`'s child reuses from the last pass, and what it does not. */
@RunWith(LithoTestRunner::class)
class CustomLayoutReuseTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `a child's subtree laid out again in an unchanged tree is copied from the last pass`() {
    val view = render(width = 300) { container() }
    assertThat(childRoot(view).isCachedLayout).describedAs("first pass").isFalse()

    view.setSizePx(400, VIEWPORT).measure().layout()

    assertThat(childRoot(view).isCachedLayout).describedAs("re-layout").isTrue()
  }

  /**
   * A child the function measured but did not place gets its bounds defined when a later pass
   * places its copy: the `Text` in it mounts. The child is a `Row`, so the `Text` is laid out in
   * the `Row`'s Yoga tree.
   */
  @Test
  fun `a Text under a child left out of one pass mounts when a later pass places it`() {
    val textInRow = Row {
      child(
          Text.create(lithoTestRule.context)
              .text("a")
              .textSizePx(14)
              .kotlinStyle(Style.width(200.px))
              .build(),
      )
    }
    val view = render(width = 100) { Layout(listOf(textInRow), layout = PLACES_IF_IT_FITS) }

    view.setSizePx(300, VIEWPORT).measure().layout()

    assertThat(view.lithoView.drawables.filterIsInstance<TextDrawable>()).hasSize(1)
  }

  @Test
  fun `a copied child is placed with the box props it was laid out under`() {
    val view = render(width = 300) { container() }
    val before = frames(view)

    view.setSizePx(400, VIEWPORT).measure().layout()

    assertThat(frames(view)).isEqualTo(before).containsExactly("5,0 70x20", "5,0 20x20")
  }

  @Test
  fun `a node a Layout no longer holds is laid out afresh, with its own box props`() {
    // The same Row moves from a Layout to a Column: under the Column its width and margin are on
    // its node again, so a copy laid out as a Layout's child would lose them.
    val row =
        Row(style = Style.width(70.px).margin(start = 5.px)) {
          child(SimpleMountSpecTester(style = Style.width(20.px).height(20.px)))
        }
    val view = render(width = 300) { Layout(listOf(row), layout = AT_MOST_100) }
    view.setRoot(Column { child(row) }).measure().layout()

    assertThat(frames(view)).containsExactly("5,0 70x20", "5,0 20x20")
  }

  @Test
  fun `a child measured in two boxes has its cached layout released with its pass`() {
    // Places a bounded measurement, then probes its child unbounded: the child's one tree is laid
    // out in each box, then again in the placed box, the layout the layout cache keeps.
    val placesThenProbes = LayoutFunctionN { measurables, constraints ->
      val placeable = measurables[0].measure(SizeConstraints(maxWidth = 100, maxHeight = 100))
      measurables[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity, maxHeight = 100))
      layout(constraints.maxWidth, placeable.height) { placeable.place(0, 0) }
    }
    // A Yoga tree, whose builder writes the cache only with the layout it builds.
    val view =
        render(width = 300) {
          Layout(
              listOf(
                  Column(style = Style.width(20.px).height(20.px)) {
                    child(SimpleMountSpecTester())
                  },
              ),
              layout = placesThenProbes,
          )
        }
    val cachedLayout = lastLayoutOfChild(view)

    for (width in listOf(400, 300, 400, 300)) {
      view.setSizePx(width, VIEWPORT).measure().layout()
    }

    assertThat(collected(cachedLayout)).describedAs("the first pass's cached layout").isTrue()
  }

  @Test
  fun `a child's tree dropped into the layout cache holds no builder`() {
    // Measures its child and doesn't place it: the child's tree is dropped, not built.
    val measuresOnly = LayoutFunctionN { measurables, constraints ->
      measurables[0].measure(constraints)
      layout(constraints.maxWidth, 0) {}
    }
    // Content `Component.measure` sized at render: the dropped tree is written to the cache.
    val view =
        render(width = 300) {
          Layout(
              listOf(
                  Column {
                    child(
                        Layout(emptyList(), layout = LayoutFunctionN { _, _ -> layout(10, 10) {} }),
                    )
                    child(
                        ComponentWithMeasureCall.create(lithoTestRule.context)
                            .component(
                                SimpleMountSpecTester(style = Style.width(10.px).height(10.px)),
                            )
                            .widthSpec(SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY))
                            .heightSpec(SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY))
                            .shouldCacheResult(true)
                            .build(),
                    )
                  },
              ),
              layout = measuresOnly,
          )
        }
    val cachedTree = checkNotNull(lastLayoutOfChild(view).get())
    val layoutResult = cachedTree.getChildAt(0)

    assertThat(layoutResult.node.layoutFunction).describedAs("the cached Layout").isNotNull()
    assertThat(layoutResult.defaultLayoutBuilder).describedAs("its builder").isNull()
  }

  /** The child's layout the layout cache kept from this pass, held weakly. */
  private fun lastLayoutOfChild(view: TestLithoView): WeakReference<LithoLayoutResult> {
    val state = checkNotNull(view.componentTree.mainThreadLayoutState)
    val child = (state.rootLayoutResult as LithoLayoutResult).node.getChildAt(0)
    val item = checkNotNull(checkNotNull(state.layoutCacheData).get<LayoutCache.CacheItem>(child))
    return WeakReference(item.layoutResult as LithoLayoutResult)
  }

  private fun collected(ref: WeakReference<*>): Boolean {
    repeat(20) {
      if (ref.get() == null) return true
      System.gc()
      @Suppress("UNUSED_VARIABLE") val pressure = ByteArray(1 shl 20)
      Thread.sleep(10)
    }
    return ref.get() == null
  }

  @Test
  fun `a keyed parent switching between a Column and a Layout keeps the child's box`() {
    lateinit var useLayout: State<Boolean>

    class Switching : KComponent() {
      override fun ComponentScope.render(): Component {
        val state = useState { false }
        useLayout = state
        val row =
            Row(style = Style.width(70.px).margin(start = 5.px)) {
              child(SimpleMountSpecTester(style = Style.width(20.px).height(20.px)))
            }
        return key("parent") {
          if (state.value) Layout(listOf(row), layout = AT_MOST_100) else Column { child(row) }
        }
      }
    }

    val view = lithoTestRule.render(widthPx = 300, heightPx = VIEWPORT) { Switching() }
    val asColumn = frames(view)
    lithoTestRule.act(view) { useLayout.updateSync(true) }
    lithoTestRule.idle()
    val asLayout = frames(view)
    lithoTestRule.act(view) { useLayout.updateSync(false) }
    lithoTestRule.idle()
    val backToColumn = frames(view)

    assertThat(listOf(asColumn, asLayout, backToColumn))
        .containsExactly(
            listOf("5,0 70x20", "5,0 20x20"),
            listOf("5,0 70x20", "5,0 20x20"),
            listOf("5,0 70x20", "5,0 20x20"),
        )
  }

  private fun container(): Component = Layout(
      listOf(
          Row(style = Style.width(70.px).margin(start = 5.px)) {
            child(SimpleMountSpecTester(style = Style.width(20.px).height(20.px)))
          },
      ),
      layout = AT_MOST_100,
  )

  private fun render(width: Int, root: () -> Component): TestLithoView =
      lithoTestRule.render(widthPx = width, heightPx = VIEWPORT) { root() }

  private fun rootResult(view: TestLithoView): LithoLayoutResult =
      checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult) as LithoLayoutResult

  /** The root of the container's only child's subtree. */
  private fun childRoot(view: TestLithoView): LithoLayoutResult = rootResult(view).getChildAt(0)

  /** The root's only child's frame and its leaf's, in the root's coordinates. */
  private fun frames(view: TestLithoView): List<String> {
    val root = rootResult(view)
    val child = root.getChildAt(0)
    val x = root.getXForChildAtIndex(0)
    val y = root.getYForChildAtIndex(0)
    val leaf = child.getChildAt(0)
    val leafX = x + child.getXForChildAtIndex(0)
    val leafY = y + child.getYForChildAtIndex(0)
    return listOf(
        "$x,$y ${child.width}x${child.height}",
        "$leafX,$leafY ${leaf.width}x${leaf.height}",
    )
  }

  private companion object {
    const val VIEWPORT = 500

    /** Measures its child at most 100 wide, whatever its own width, and places it at the origin. */
    /** Measures its child unbounded, and places it only if it fits. */
    val PLACES_IF_IT_FITS = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity))
      layout(c.maxWidth, p.height) {
        if (p.width <= c.maxWidth) {
          p.place(0, 0)
        }
      }
    }

    val AT_MOST_100 = LayoutFunctionN { ms, c ->
      val p = ms[0].measure(SizeConstraints(maxWidth = 100, maxHeight = 100))
      layout(c.maxWidth, p.height) { p.place(0, 0) }
    }
  }
}
