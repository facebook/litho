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
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
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

  /** The wide content: [onState] gets its own state, 260 wide until updated. */
  private class StateWidth(private val onState: (State<Int>) -> Unit) : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useState { 260 }
      onState(state)
      return tagged("wide", state.value, 20)
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
