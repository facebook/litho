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

import android.graphics.Rect
import android.view.View
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deferred content (`RenderWithConstraints`) resolves from the constraints it is measured under, so
 * a pass must resolve it the way a fresh layout would, whatever it reuses from the last pass.
 */
@RunWith(LithoTestRunner::class)
class CustomLayoutDeferredContentTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  /** Narrow content within [THRESHOLD] across, wide content beyond. */
  private fun sizeDependent(): Component = RenderWithConstraints { c ->
    if (c.hasBoundedWidth && c.maxWidth <= THRESHOLD) tagged("narrow", 20, 40)
    else tagged("wide", 100, 30)
  }

  @Test
  fun `deferred content copied from the last pass is resolved under the constraints it is measured with`() {
    // The inner Layout measures the deferred child unbounded, so it is always the wide content. A
    // resize copies the child from the layout cache, without its content: if Yoga never measures
    // the copy, its content is resolved at its final size, 100 across, which is the narrow one.
    val tree = {
      Layout(
          listOf(Layout(listOf(tagged("leaf", 40, 20), sizeDependent()), layout = SIDE_BY_SIDE)),
          layout = FILLS,
      )
    }
    val view = lithoTestRule.render(widthPx = 360, heightPx = VIEWPORT) { tree() }
    assertThat(shows(view)).describedAs("first layout").isEqualTo("wide")

    view.setSizePx(1080, VIEWPORT).measure().layout()

    assertThat(shows(view)).describedAs("after a resize, as a fresh layout").isEqualTo("wide")
  }

  /**
   * Content `Component.measure` sized when it was rendered, under a `Layout`'s child the layout
   * cache copies, keeps the size that call measured, as a fresh render does: a copy measured again
   * would resolve it at Yoga's size. Here a padded 100 x 20 Primitive measured at exactly 100,
   * beside a 10 x 200 column, under a `Layout` left out at 240 and placed at 480.
   */
  @Test
  fun `content measured at render under a copied child keeps the size it was measured at`() {
    val placesIfItFits = LayoutFunctionN { ms, c ->
      val ps = ms.map { it.measure(SizeConstraints(maxWidth = SizeConstraints.Infinity)) }
      layout(c.maxWidth, ps.maxOf { it.height }) {
        var x = 0
        for (p in ps) {
          if (x + p.width <= c.maxWidth) {
            p.place(x, 0)
            x += p.width
          }
        }
      }
    }
    val fills = LayoutFunctionN { ms, c ->
      val w = if (c.hasBoundedWidth) c.maxWidth else 300
      val ps = ms.map { it.measure(SizeConstraints(minWidth = w, maxWidth = w)) }
      layout(w, ps.sumOf { it.height }) {
        var y = 0
        for (p in ps) {
          p.place(0, y)
          y += p.height
        }
      }
    }
    val tree = {
      Layout(
          listOf(
              Column(style = Style.width(120.px).height(20.px)),
              Layout(
                  listOf(
                      Row {
                        child(
                            ComponentWithMeasureCall.create(lithoTestRule.context)
                                .component(
                                    MeasuredContentPrimitive(
                                        Style.padding(all = 7.px).wrapInView().viewTag("measured"),
                                    ),
                                )
                                .widthSpec(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
                                .heightSpec(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
                                .shouldCacheResult(true)
                                .build(),
                        )
                        child(Column(style = Style.width(10.px).height(200.px)))
                      },
                  ),
                  layout = fills,
              ),
          ),
          layout = placesIfItFits,
      )
    }
    val fresh =
        frameOf(lithoTestRule.render(widthPx = 480, heightPx = VIEWPORT) { tree() }, "measured")
    assertThat(fresh).describedAs("a fresh render").isEqualTo(Rect(120, 0, 220, 200))
    val view = lithoTestRule.render(widthPx = 240, heightPx = VIEWPORT) { tree() }

    view.setSizePx(480, VIEWPORT).measure().layout()

    assertThat(frameOf(view, "measured"))
        .describedAs("after a resize, as a fresh render")
        .isEqualTo(fresh)
  }

  /**
   * A `Layout`'s child it measures and doesn't place, holding content `Component.measure` sized at
   * render: a 50-high column around a Primitive that measures 20 high and grows to 50, so it's
   * measured again at its final size once placed. The child the layout cache copies keeps that
   * content, which a resize then places.
   */
  @Test
  fun `a grown Primitive in content measured at render, unplaced, is measured again once placed`() {
    val view = lithoTestRule.render(widthPx = 240, heightPx = VIEWPORT) { grownInUnplacedContent() }
    assertThat(frameOf(view, "measured")).describedAs("not placed").isNull()

    view.setSizePx(480, VIEWPORT).measure().layout()

    assertThat(frameOf(view, "measured"))
        .describedAs("placed after a resize")
        .isEqualTo(Rect(120, 0, 220, 50))
  }

  /**
   * The same content, placed in two passes after the first, each copying the child from the layout
   * cache: each pass lays out a copy of the content it keeps, not the committed layout's, which
   * other passes read.
   */
  @Test
  fun `a pass that places kept content lays out its own copy of it`() {
    val view = lithoTestRule.render(widthPx = 240, heightPx = VIEWPORT) { grownInUnplacedContent() }
    view.setSizePx(480, VIEWPORT).measure().layout()
    val committed = keptContentOf(view)

    view.setSizePx(470, VIEWPORT).measure().layout()

    assertThat(keptContentOf(view)).isNotSameAs(committed)
    assertThat(frameOf(view, "measured")).isEqualTo(Rect(120, 0, 220, 50))
  }

  private fun keptContentOf(view: TestLithoView): LithoLayoutResult {
    val root =
        checkNotNull(view.componentTree.mainThreadLayoutState?.rootLayoutResult)
            as LithoLayoutResult
    return checkNotNull(holderIn(root)?.actualDeferredNodeResult)
  }

  private fun holderIn(result: LithoLayoutResult): DeferredLithoLayoutResult? =
      result as? DeferredLithoLayoutResult
          ?: (0 until result.childCount).firstNotNullOfOrNull { holderIn(result.getChildAt(it)) }

  /**
   * A `Layout`'s second child, measured and placed only if it fits, holds content
   * `Component.measure` sized at render: a 50-high column around a Primitive that measures 20 high
   * and grows to 50.
   */
  private fun grownInUnplacedContent(): Component {
    val placesIfItFits = LayoutFunctionN { ms, c ->
      val ps = ms.map { it.measure(SizeConstraints(maxWidth = SizeConstraints.Infinity)) }
      layout(c.maxWidth, ps.maxOf { it.height }) {
        var x = 0
        for (p in ps) {
          if (x + p.width <= c.maxWidth) {
            p.place(x, 0)
            x += p.width
          }
        }
      }
    }
    return Layout(
        listOf(
            Column(style = Style.width(120.px).height(20.px)),
            Row {
              child(
                  ComponentWithMeasureCall.create(lithoTestRule.context)
                      .component(
                          Column(style = Style.height(50.px)) {
                            child(
                                MeasuredContentPrimitive(
                                    Style.flex(grow = 1f).wrapInView().viewTag("measured"),
                                ),
                            )
                          },
                      )
                      .widthSpec(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
                      .heightSpec(SizeSpec.makeSizeSpec(50, SizeSpec.EXACTLY))
                      .shouldCacheResult(true)
                      .build(),
              )
              child(Column(style = Style.width(200.px).height(20.px)))
            },
        ),
        layout = placesIfItFits,
    )
  }

  /**
   * A child the `Layout` measures and doesn't place, holding content `Component.measure` sized at
   * render beside a leaf whose width is state read while it's laid out. The `Layout` declares the
   * child's width, so updating that state must lay the child out again, not take it from the layout
   * cache as it was.
   */
  @Test
  fun `an unplaced child with content measured at render is laid out again on its state change`() {
    var width: State<Int>? = null
    val probesWidth = LayoutFunctionN { measurables, constraints ->
      val probed = measurables[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity))
      layout(probed.width, 10) {}
    }
    val tree = {
      Row {
        child(
            Layout(
                style = Style.wrapInView().viewTag("probe"),
                components =
                    listOf(
                        Row {
                          child(
                              ComponentWithMeasureCall.create(lithoTestRule.context)
                                  .component(MeasuredContentPrimitive(Style))
                                  .widthSpec(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
                                  .heightSpec(SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY))
                                  .shouldCacheResult(true)
                                  .build(),
                          )
                          child(LayoutStateWidth { width = it })
                        },
                    ),
                layout = probesWidth,
            ),
        )
      }
    }
    val view = lithoTestRule.render(widthPx = 480, heightPx = VIEWPORT) { tree() }
    assertThat(frameOf(view, "probe")?.width()).describedAs("before").isEqualTo(110)

    lithoTestRule.act(view) { checkNotNull(width).update(30) }
    lithoTestRule.idle()

    assertThat(frameOf(view, "probe")?.width()).describedAs("after the update").isEqualTo(130)
  }

  /** Its holder places deferred content at its margins, as it places a Yoga root's. */
  @Test
  fun `deferred content whose root is a Layout is placed at its margins`() {
    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column {
            child(
                RenderWithConstraints {
                  Layout(
                      listOf(tagged("inner", 40, 20)),
                      style = Style.margin(left = 10.px, top = 6.px),
                      layout = SIDE_BY_SIDE,
                  )
                },
            )
          }
        }

    assertThat(frameOf(view, "inner")?.left).describedAs("left").isEqualTo(10)
    assertThat(frameOf(view, "inner")?.top).describedAs("top").isEqualTo(6)
  }

  @Test
  fun `state read rendering an unplaced measurement's deferred content lays it out again`() {
    // The Layout measures the deferred child unbounded, which renders the wide content, a
    // component holding state that sets its width, then exactly at that width, which renders the
    // narrow content: only that one is placed, so the state is read only rendering the measurement
    // that is not placed. Its update must lay the Layout out again, as a fresh layout would.
    var width: State<Int>? = null
    val tree = {
      Layout(
          listOf(
              RenderWithConstraints { c ->
                if (c.hasBoundedWidth && c.maxWidth <= 600) Row { child(tagged("narrow", 140, 20)) }
                else Column { child(StateWidth { width = it }) }
              },
          ),
          layout = MEASURE_THEN_EXACTLY,
      )
    }
    val view = lithoTestRule.render(widthPx = 360, heightPx = VIEWPORT) { tree() }
    assertThat(frames(view)).describedAs("placed").isEqualTo(placedBefore())

    lithoTestRule.act(view) { checkNotNull(width).update(20) }
    lithoTestRule.idle()

    // The wide content is now 20 across, so the narrow content is measured exactly 20 across, and
    // its 140-wide leaf shrinks to it.
    assertThat(frames(view))
        .describedAs("laid out again at the new width")
        .isEqualTo(listOf(Rect(0, 0, 20, 20), null))
  }

  /**
   * Each level of a chain of `Layout`s measures its Column, which holds deferred content and the
   * next level, unbounded and then exactly at its width, so each tree holding the content is laid
   * out in two boxes. Each is laid out again in place, keeping the builders of the `Layout`s in it,
   * so the content renders a number of times linear in the depth.
   */
  @Test
  fun `deferred content under nested Layouts renders a number of times linear in their depth`() {
    val placingLatest =
        (1..6).map { depth ->
          countsOf(depth, deferred = true, placesLatest = true).renders
        }
    val placingEarlier =
        (1..6).map { depth ->
          countsOf(depth, deferred = true, placesLatest = false).renders
        }

    assertThat(placingLatest)
        .describedAs("renders by depth, placing the latest measurement: 3 x depth - 1")
        .containsExactly(2, 5, 8, 11, 14, 17)
    assertThat(placingEarlier)
        .describedAs("renders by depth, placing the earlier measurement: 5 x depth - 2")
        .containsExactly(3, 8, 13, 18, 23, 28)
  }

  /**
   * The same chain, its content `Component.measure` sized at render: laid out again in another box,
   * the content is measured again, not rendered again, so it renders once per level, as it would
   * under no `Layout`.
   */
  @Test
  fun `content measured at render under nested Layouts renders once per level`() {
    val renders =
        (1..6).map { depth ->
          val count = intArrayOf(0)
          lithoTestRule.render(widthPx = 360, heightPx = VIEWPORT) {
            nestedMeasured(depth, measuresUnboundedThenExactly(placesLatest = true), count)
          }
          count[0]
        }

    assertThat(renders).describedAs("renders by depth").containsExactly(1, 2, 3, 4, 5, 6)
  }

  /** The same chain six deep runs as many functions and lays out as many children either way. */
  @Test
  fun `nested Layouts lay out as much with deferred content as without it`() {
    for (placesLatest in listOf(true, false)) {
      val withoutContent = countsOf(depth = 6, deferred = false, placesLatest = placesLatest)
      val withContent = countsOf(depth = 6, deferred = true, placesLatest = placesLatest)

      assertThat(withoutContent.functionRuns to withoutContent.childLayouts)
          .describedAs("without deferred content, placing the latest: $placesLatest")
          .isEqualTo((if (placesLatest) 15L else 20L) to 17L)
      assertThat(withContent.functionRuns to withContent.childLayouts)
          .describedAs("with deferred content, placing the latest: $placesLatest")
          .isEqualTo(withoutContent.functionRuns to withoutContent.childLayouts)
    }
  }

  private class PassCounts(val renders: Int, val functionRuns: Long, val childLayouts: Long)

  private fun countsOf(depth: Int, deferred: Boolean, placesLatest: Boolean): PassCounts {
    val renders = intArrayOf(0)
    val layout = measuresUnboundedThenExactly(placesLatest)
    LithoStats.resetAllCounters()
    lithoTestRule.render(widthPx = 360, heightPx = VIEWPORT) {
      nested(depth, deferred, layout, renders)
    }
    return PassCounts(
        renders[0],
        LithoStats.customLayoutFunctionRunCount.get(),
        LithoStats.customLayoutChildLayoutCount.get(),
    )
  }

  /** [depth] `Layout`s, each over a Column of a 10 x 10 leaf, deferred or not, and the next. */
  private fun nested(
      depth: Int,
      deferred: Boolean,
      layout: LayoutFunctionN,
      renders: IntArray,
  ): Component {
    if (depth == 0) return square()
    return Layout(
        listOf(
            Column {
              if (deferred) {
                child(
                    RenderWithConstraints { _ ->
                      renders[0]++
                      square()
                    },
                )
              } else {
                child(square())
              }
              child(nested(depth - 1, deferred, layout, renders))
            },
        ),
        layout = layout,
    )
  }

  /** As [nested], each level's content a 10 x 10 leaf `Component.measure` sized at render. */
  private fun nestedMeasured(depth: Int, layout: LayoutFunctionN, renders: IntArray): Component {
    if (depth == 0) return square()
    return Layout(
        listOf(
            Column {
              child(
                  ComponentWithMeasureCall.create(lithoTestRule.context)
                      .component(CountsRenders(renders))
                      .widthSpec(SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY))
                      .heightSpec(SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY))
                      .shouldCacheResult(true)
                      .build(),
              )
              child(nestedMeasured(depth - 1, layout, renders))
            },
        ),
        layout = layout,
    )
  }

  /** The wide content: [onState] gets its own state, 260 wide until updated. */
  private class StateWidth(private val onState: (State<Int>) -> Unit) : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useState { 260 }
      onState(state)
      return tagged("wide", state.value, 20)
    }
  }

  /** A leaf [onState] gets the state of, whose width is that state read while it's laid out. */
  private class LayoutStateWidth(private val onState: (State<Int>) -> Unit) : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useState { 10 }
      onState(state)
      return StateWidthPrimitive(state)
    }
  }

  private fun frames(view: TestLithoView): List<Rect?> =
      listOf(frameOf(view, "narrow"), frameOf(view, "wide"))

  private fun shows(view: TestLithoView): String = listOfNotNull(
      "narrow".takeIf { frameOf(view, it) != null },
      "wide".takeIf { frameOf(view, it) != null },
  )
      .joinToString("+")
      .ifEmpty { "none" }

  private fun frameOf(view: TestLithoView, tag: String): Rect? {
    val v = view.lithoView.findViewWithTag<View>(tag) ?: return null
    val rect = Rect(0, 0, v.width, v.height)
    view.lithoView.offsetDescendantRectToMyCoords(v, rect)
    return rect
  }

  private companion object {
    const val VIEWPORT = 4000
    const val THRESHOLD = 400

    /** The narrow content, 140 across, placed at the wide content's 260: no wide content shown. */
    private fun placedBefore(): List<Rect?> = listOf(Rect(0, 0, 140, 20), null)

    private fun square(): Component = Column(style = Style.width(10.px).height(10.px))

    /**
     * Measures its only child unbounded, then exactly at its own width, and places the latest
     * measurement or the earlier one.
     */
    private fun measuresUnboundedThenExactly(placesLatest: Boolean) =
        LayoutFunctionN { measurables, constraints ->
          val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 300
          val unbounded =
              measurables[0].measure(
                  SizeConstraints(maxWidth = SizeConstraints.Infinity, maxHeight = VIEWPORT),
              )
          val exact =
              measurables[0].measure(
                  SizeConstraints(minWidth = width, maxWidth = width, maxHeight = VIEWPORT),
              )
          layout(width, unbounded.height) {
            (if (placesLatest) exact else unbounded).place(0, 0)
          }
        }

    private fun tagged(tag: String, w: Int, h: Int): Component =
        Column(style = Style.width(w.px).height(h.px).wrapInView().viewTag(tag))

    val FILLS = LayoutFunctionN { ms, c ->
      val w = c.maxWidth
      val ps = ms.map { it.measure(SizeConstraints(minWidth = w, maxWidth = w, maxHeight = 400)) }
      layout(w, ps.sumOf { it.height }) { ps.forEach { it.place(0, 0) } }
    }

    val SIDE_BY_SIDE = LayoutFunctionN { ms, c ->
      val ps = ms.map {
        it.measure(SizeConstraints(maxWidth = SizeConstraints.Infinity, maxHeight = 400))
      }
      layout(c.maxWidth, ps.maxOfOrNull { it.height } ?: 0) {
        var x = 0
        for (p in ps) {
          p.place(x, 0)
          x += p.width
        }
      }
    }

    val MEASURE_THEN_EXACTLY = LayoutFunctionN { ms, c ->
      val natural =
          ms[0].measure(SizeConstraints(maxWidth = SizeConstraints.Infinity, maxHeight = 400))
      val exact = ms[0].measure(SizeConstraints.exact(natural.width, natural.height))
      layout(c.maxWidth, exact.height) { exact.place(0, 0) }
    }
  }
}

/** A 10 x 10 leaf counting its renders in [renders]. */
private class CountsRenders(private val renders: IntArray) : KComponent() {
  override fun ComponentScope.render(): Component {
    renders[0]++
    return Column(style = Style.width(10.px).height(10.px))
  }
}

/** A Primitive that is always 100 x 20, mounting a plain View. */
private class MeasuredContentPrimitive(private val style: Style) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = MeasuredContentPrimitiveBehavior,
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = style,
  )
}

private object MeasuredContentPrimitiveBehavior : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(width = 100, height = 20)
}

/** A Primitive 20 high, whose width is [width] read while it's laid out. */
// It reads the state while it's laid out, the read under test, so it takes the state, not a value.
@Suppress("KComponentStateInConstructorDetector")
private class StateWidthPrimitive(private val width: State<Int>) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = StateWidthBehavior(width),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class StateWidthBehavior(private val width: State<Int>) : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(width = width.value, height = 20)
}
