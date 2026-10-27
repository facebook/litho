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

import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.state.StateId
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A layout reused from the last pass is checked against a fresh one, and a difference throws. */
@RunWith(LithoTestRunner::class)
class CustomLayoutShadowValidationTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private var validationWas = false

  @Before
  fun turnOn() {
    validationWas = ComponentsConfiguration.customLayoutShadowValidation
    ComponentsConfiguration.customLayoutShadowValidation = true
    LithoStats.resetAllCounters()
  }

  @After
  fun restore() {
    ComponentsConfiguration.customLayoutShadowValidation = validationWas
    CustomLayoutShadowValidation.testOnlyAlterExpected = null
  }

  @Test
  fun `a reused layout that matches a fresh one passes`() {
    val view = renderWithReusedChild(FixedLeaf())

    updateElsewhere(view)

    assertThat(LithoStats.customLayoutShadowCheckCount.get())
        .describedAs("the reused child was checked")
        .isGreaterThan(0)
  }

  @Test
  fun `a child laid out again is checked for a subtree copied below it`() {
    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Layout(
              listOf(
                  Column {
                    child(Column { child(FixedLeaf()) })
                    child(Footer())
                  },
              ),
              layout = STACKED,
          )
        }

    updateElsewhere(view)

    assertThat(LithoStats.customLayoutShadowCheckCount.get())
        .describedAs("the child, whose first Column was copied whole, was checked")
        .isGreaterThan(0)
  }

  /**
   * A reused child with margins, padding, a border, RTL and a nested `Layout` matches a fresh one,
   * and a fresh one placing the nested `Layout`'s child a pixel over is a mismatch: positions
   * inside the nested `Layout` are compared too.
   */
  @Test
  fun `a reused child with box edges, RTL and a nested Layout is checked to its positions`() {
    val child = {
      Column(
          style =
              Style.layoutDirection(YogaDirection.RTL)
                  .padding(start = 5.px, top = 3.px)
                  .border(
                      Border.create(lithoTestRule.context)
                          .widthPx(YogaEdge.LEFT, 2)
                          .widthPx(YogaEdge.RIGHT, 4)
                          .color(YogaEdge.ALL, Color.BLACK)
                          .build(),
                  ),
      ) {
        child(
            Layout(
                listOf(FixedLeaf()),
                style = Style.margin(start = 6.px, top = 2.px),
                layout = STACKED,
            ),
        )
      }
    }
    val view = renderWithReusedChild(child())
    updateElsewhere(view)
    assertThat(LithoStats.customLayoutShadowCheckCount.get())
        .describedAs("the reused child was checked")
        .isGreaterThan(0)

    CustomLayoutShadowValidation.testOnlyAlterExpected = { it.nudgeDeepest() }
    assertThatThrownBy { updateElsewhere(view) }
        .hasStackTraceContaining(CustomLayoutShadowValidation.Mismatch::class.java.simpleName)
  }

  /** This geometry with its deepest first child moved one pixel right. */
  private fun CustomLayoutShadowValidation.Geometry.nudgeDeepest():
      CustomLayoutShadowValidation.Geometry {
    val first = children.firstOrNull() ?: return this
    val moved =
        if (first.geometry.children.isEmpty()) first.copy(x = first.x + 1)
        else first.copy(geometry = first.geometry.nudgeDeepest())
    return copy(children = listOf(moved) + children.drop(1))
  }

  /**
   * A leaf directly under a `Layout`, copied from the layout cache when a sibling's state changes,
   * is checked as a copy too: a fresh layout that differs from it is a mismatch.
   */
  @Test
  fun `a leaf under a Layout copied from the layout cache is checked`() {
    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column {
            child(Layout(listOf(FixedLeaf()), layout = STACKED))
            child(Footer())
          }
        }
    CustomLayoutShadowValidation.testOnlyAlterExpected = { it.copy(width = it.width + 1) }

    assertThatThrownBy { updateElsewhere(view) }
        .hasStackTraceContaining(CustomLayoutShadowValidation.Mismatch::class.java.simpleName)
  }

  @Test
  fun `a reused layout that differs from a fresh one throws`() {
    val view = renderWithReusedChild(FixedLeaf())
    CustomLayoutShadowValidation.testOnlyAlterExpected = { it.copy(width = it.width + 1) }

    assertThatThrownBy { updateElsewhere(view) }
        .hasStackTraceContaining(CustomLayoutShadowValidation.Mismatch::class.java.simpleName)
  }

  @Test
  fun `a check counts none of the customLayout and subtreeCache counters`() {
    val view = renderWithReusedChild(FixedLeaf())
    LithoStats.resetAllCounters()
    ComponentsConfiguration.customLayoutShadowValidation = false
    updateElsewhere(view)
    val without = layoutCounters()

    LithoStats.resetAllCounters()
    ComponentsConfiguration.customLayoutShadowValidation = true
    updateElsewhere(view)

    assertThat(LithoStats.customLayoutShadowCheckCount.get()).isGreaterThan(0)
    assertThat(without.getValue("customLayoutChildLayoutCount"))
        .describedAs("the `Layout`'s child is measured by the pass")
        .isGreaterThan(0)
    assertThat(layoutCounters())
        .describedAs("counters, with the check's own layout not counted")
        .isEqualTo(without)
  }

  @Test
  fun `an error in a check is a Mismatch, and no error boundary is told`() {
    val reported = ArrayList<Exception>()

    class Guarded : KComponent() {
      override fun ComponentScope.render(): Component {
        useErrorBoundary { e: Exception -> reported.add(e) }
        return Layout(
            listOf(Column { child(RenderWithConstraints { ThrowsWhileChecking() }) }),
            layout = STACKED,
        )
      }
    }

    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column {
            child(Guarded())
            child(Footer())
          }
        }

    assertThatThrownBy { updateElsewhere(view) }
        .hasStackTraceContaining(CustomLayoutShadowValidation.Mismatch::class.java.simpleName)
        .hasStackTraceContaining("thrown while checking")
    assertThat(reported).describedAs("errors reported to the boundary").isEmpty()
  }

  @Test
  fun `a check records no state read`() {
    lateinit var width: State<Int>

    class Host : KComponent() {
      @Suppress("KComponentUncachedAllocationDetector")
      override fun ComponentScope.render(): Component {
        val state = useState { 40 }
        width = state
        return Layout(listOf(Column { child(ReadsWidth(state)) }), layout = STACKED)
      }
    }

    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column {
            child(Host())
            child(Footer())
          }
        }
    ComponentsConfiguration.customLayoutShadowValidation = false
    updateElsewhere(view)
    val without = stateReadsOf(view)
    LithoStats.resetAllCounters()

    ComponentsConfiguration.customLayoutShadowValidation = true
    updateElsewhere(view)

    assertThat(LithoStats.customLayoutShadowCheckCount.get()).isGreaterThan(0)
    assertThat(without.keys).describedAs("the leaf's read of its width").isNotEmpty()
    assertThat(stateReadsOf(view)).describedAs("readers of each state").isEqualTo(without)
  }

  private var tint: State<Int>? = null

  /** A sibling with its own state: updating it leaves the `Layout`'s nodes as they were. */
  private inner class Footer : KComponent() {
    override fun ComponentScope.render(): Component {
      val state = useState { 0 }
      tint = state
      return Column(style = Style.width((10 + state.value).px).height(10.px))
    }
  }

  /**
   * A `Layout` whose child, [child] in a Yoga tree, is reused from the layout cache, as a whole
   * Yoga subtree, when a sibling's state changes.
   */
  private fun renderWithReusedChild(child: Component): TestLithoView =
      lithoTestRule.render(widthPx = 300, heightPx = 300) {
        Column {
          child(Layout(listOf(Column { child(child) }), layout = STACKED))
          child(Footer())
        }
      }

  private fun updateElsewhere(view: TestLithoView) {
    val state = checkNotNull(tint)
    lithoTestRule.act(view) { state.update { it + 1 } }
    lithoTestRule.idle()
  }

  /** Every `customLayout*` and `subtreeCache*` counter but the check's own, by name. */
  private fun layoutCounters(): Map<String, Long> =
      LithoStats::class
          .java
          .declaredFields
          .filter {
            AtomicLong::class.java.isAssignableFrom(it.type) &&
                (it.name.startsWith("customLayout") || it.name.startsWith("subtreeCache")) &&
                it.name != "customLayoutShadowCheckCount"
          }
          .associate { field ->
            field.isAccessible = true
            field.name to (field.get(null) as AtomicLong).get()
          }

  private fun stateReadsOf(view: TestLithoView): Map<StateId, Set<GlobalKey>> =
      checkNotNull(view.committedLayoutState).stateReads.orEmpty().mapValues { it.value.asSet() }

  /**
   * A child copied from the last pass that holds content `Component.measure` sized when it was
   * rendered is not checked: a cold layout, with no render, sizes that content under the
   * constraints Yoga gives it, where a fresh render takes the size the call cached. Here a padded
   * 100 x 20 Primitive measured at exactly 100, beside a 10 x 200 column, under a `Layout` left out
   * at 240 and placed at 480, keeps its 100.
   */
  @Test
  fun `a copied child holding content measured at render is not checked`() {
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
    val measured =
        ComponentWithMeasureCall.create(lithoTestRule.context)
            .component(
                MeasuredPrimitive(
                    Style.padding(all = 7.px).wrapInView().contentDescription("measured"),
                ),
            )
            .widthSpec(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
            .heightSpec(SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY))
            .shouldCacheResult(true)
            .build()
    val view =
        lithoTestRule.render(widthPx = 240, heightPx = 4000) {
          Layout(
              listOf(
                  Column(style = Style.width(120.px).height(20.px)),
                  Layout(
                      listOf(
                          Row {
                            child(measured)
                            child(Column(style = Style.width(10.px).height(200.px)))
                          },
                      ),
                      layout = fills,
                  ),
              ),
              layout = placesIfItFits,
          )
        }

    view.setSizePx(480, 4000).measure().layout()

    val content = checkNotNull(findByDescription(view.lithoView, "measured"))
    val frame = Rect(0, 0, content.width, content.height)
    view.lithoView.offsetDescendantRectToMyCoords(content, frame)
    assertThat(frame.width()).describedAs("the size measured at render").isEqualTo(100)
  }

  private fun findByDescription(view: View, description: String): View? =
      if (view.contentDescription?.toString() == description) view
      else
          (view as? ViewGroup)?.let { group ->
            (0 until group.childCount).firstNotNullOfOrNull {
              findByDescription(group.getChildAt(it), description)
            }
          }

  private companion object {
    val STACKED = LayoutFunctionN { ms, c ->
      val ps = ms.map { it.measure(SizeConstraints(maxWidth = c.maxWidth, maxHeight = 400)) }
      layout(c.maxWidth, ps.sumOf { it.height }) {
        var y = 0
        for (p in ps) {
          p.place(0, y)
          y += p.height
        }
      }
    }
  }
}

/** A leaf measured by its layout behavior: 40 by 20. */
private class FixedLeaf : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = FixedLeafBehavior,
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private object FixedLeafBehavior : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(width = 40, height = 20)
}

/** A leaf whose width is state it reads while it is measured. */
private class ReadsWidth(private val width: State<Int>) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = ReadsWidthBehavior(width),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class ReadsWidthBehavior(private val width: State<Int>) : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(width = width.value, height = 20)
}

/** Content that renders a 40 by 20 box, except in a shadow-validation layout, where it throws. */
private class ThrowsWhileChecking : KComponent() {
  override fun ComponentScope.render(): Component {
    check(!CustomLayoutShadowValidation.isRunning) { "thrown while checking" }
    return Column(style = Style.width(40.px).height(20.px))
  }
}

/** A Primitive that is always 100 x 20, mounting a plain View. */
private class MeasuredPrimitive(private val style: Style) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = MeasuredPrimitiveBehavior,
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = style,
  )
}

private object MeasuredPrimitiveBehavior : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult = PrimitiveLayoutResult(width = 100, height = 20)
}
