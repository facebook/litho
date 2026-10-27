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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.BoxConstraints
import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.Measurable
import com.facebook.litho.layout.MeasureScope
import com.facebook.litho.layout.NO_SIZE_DECLARED
import com.facebook.litho.layout.Placeable
import com.facebook.litho.layout.internal.DefaultLayoutBuilder
import com.facebook.litho.layout.resolveBoxConstraints
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

/**
 * The rules [DefaultLayoutBuilder] lays a `Layout` out by, checked on the builder directly: when
 * its function runs, the size it answers, what a failure does, and how it measures, places and
 * builds its children.
 */
@RunWith(LithoTestRunner::class)
class DefaultLayoutBuilderTest {

  // The box it answers

  @Test
  fun `it answers the size its function declared, within the box`() {
    val builder = builderFor { _ -> layout(120, 40) {} }

    assertThat(builder.measure(boxUnder(atMost(300, 300)))).isEqualTo(IntSize(120, 40))
  }

  @Test
  fun `its function runs inside the padding and border, which the size it answers adds back`() {
    val seen = mutableListOf<SizeConstraints>()
    val builder = builderFor { constraints ->
      seen.add(constraints)
      layout(100, 50) {}
    }

    val size = builder.measure(boxUnder(atMost(300, 300), padding = edges(10), border = edges(2)))

    assertThat(seen).containsExactly(atMost(276, 276))
    assertThat(size).isEqualTo(IntSize(124, 74))
  }

  /**
   * As before builders, a minimum reaches the function only on an exact axis: a function that hands
   * its constraints to a child would otherwise force that child up to it. The answer still keeps
   * it.
   */
  @Test
  fun `its function sees a minimum only on an exact axis, and the answer keeps it`() {
    val seen = mutableListOf<SizeConstraints>()
    val builder = builderFor { constraints ->
      seen.add(constraints)
      layout(10, 10) {}
    }
    val room = SizeConstraints(minWidth = 150, maxWidth = 300, minHeight = 100, maxHeight = 100)

    val size = builder.measure(boxUnder(room, padding = edges(10)))

    assertThat(seen)
        .containsExactly(
            SizeConstraints(minWidth = 0, maxWidth = 280, minHeight = 80, maxHeight = 80),
        )
    assertThat(size).isEqualTo(IntSize(150, 100))
  }

  @Test
  fun `on an exact axis it answers the box, whatever its function declared`() {
    val builder = builderFor { _ -> layout(50, 500) {} }

    assertThat(builder.measure(boxUnder(SizeConstraints.exact(200, 100))))
        .isEqualTo(IntSize(200, 100))
  }

  /** `layout(constraints.maxWidth, …)` declares Infinity on an unbounded axis. */
  @Test
  fun `an unbounded size it declares stays unbounded with its edges added`() {
    val builder = builderFor { constraints -> layout(constraints.maxWidth, 10) {} }

    val size = builder.measure(boxUnder(SizeConstraints(maxHeight = 300), padding = edges(10)))

    assertThat(size.width).isEqualTo(SizeConstraints.Infinity)
    assertThat(size.height).isEqualTo(30)
  }

  @Test
  fun `its function runs in its own direction`() {
    var seen: LayoutDirection? = null
    val builder =
        builderFor(Style.layoutDirection(YogaDirection.RTL)) { _ ->
          seen = layoutDirection
          layout(10, 10) {}
        }

    builder.measure(boxUnder(atMost(300, 300)))

    assertThat(seen).isEqualTo(LayoutDirection.RTL)
  }

  // Its output

  @Test
  fun `its output is its border box, edges and direction, and it has no failure or reads`() {
    val builder = builderFor(Style.layoutDirection(YogaDirection.RTL)) { _ -> layout(100, 50) {} }

    val result =
        builder.build(
            boxUnder(
                atMost(300, 300),
                padding = IntEdges(1, 2, 3, 4),
                border = IntEdges(5, 6, 7, 8),
            ),
        )
    val output = outputOf(result)

    assertThat(IntSize(output.width, output.height)).isEqualTo(IntSize(116, 70))
    assertThat(IntSize(result.contentWidth, result.contentHeight)).isEqualTo(IntSize(100, 50))
    assertThat(
        listOf(output.paddingLeft, output.paddingTop, output.paddingRight, output.paddingBottom),
    )
        .containsExactly(1, 2, 3, 4)
    assertThat(listOf(output.borderLeft, output.borderTop, output.borderRight, output.borderBottom))
        .containsExactly(5, 6, 7, 8)
    assertThat(output.layoutDirection).isEqualTo(LayoutDirection.RTL)
    assertThat(positionsOf(result)).isEmpty()
    assertThat(result.stateReads).isNull()
    assertThat(result.failure).isNull()
  }

  /**
   * Its content box is its border box less its edges, not what its function declared; its specs are
   * the content constraints its function ran under.
   */
  @Test
  fun `built in an exact box larger than it declared, its content box fills the box`() {
    val builder = builderFor { _ -> layout(100, 50) {} }

    val result = builder.build(boxUnder(SizeConstraints.exact(300, 200), padding = edges(10)))
    val output = outputOf(result)

    assertThat(IntSize(output.width, output.height)).isEqualTo(IntSize(300, 200))
    assertThat(IntSize(result.contentWidth, result.contentHeight)).isEqualTo(IntSize(280, 180))
    assertThat(listOf(result.widthSpec, result.heightSpec))
        .containsExactly(
            SizeSpec.makeSizeSpec(280, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(180, SizeSpec.EXACTLY),
        )
    assertThat(output.layoutDirection).isEqualTo(LayoutDirection.LTR)
  }

  // When its function runs

  @Test
  fun `measured again in the same box, its function doesn't run again`() {
    var runs = 0
    val builder = builderFor { _ ->
      runs++
      layout(10, 10) {}
    }

    builder.measure(boxUnder(atMost(300, 300)))
    builder.measure(boxUnder(atMost(300, 300)))

    assertThat(runs).isEqualTo(1)
  }

  @Test
  fun `built in the box it was measured in, it places from that run`() {
    var runs = 0
    val builder = builderFor { _ ->
      runs++
      layout(10, 10) {}
    }

    builder.measure(boxUnder(atMost(300, 300)))
    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(runs).isEqualTo(1)
    assertThat(result.width).isEqualTo(10)
  }

  /** Yoga's rule: a size measured is valid at exactly that size, as when Yoga doesn't stretch. */
  @Test
  fun `built exactly the size its function declared, it places from that run`() {
    var runs = 0
    val builder = builderFor { _ ->
      runs++
      layout(250, 40) {}
    }

    builder.measure(boxUnder(atMost(300, 300)))
    builder.build(boxUnder(SizeConstraints.exact(250, 40)))

    assertThat(runs).isEqualTo(1)
  }

  @Test
  fun `built stretched past what its function declared, its function runs again, exactly`() {
    val seen = mutableListOf<SizeConstraints>()
    val builder = builderFor { constraints ->
      seen.add(constraints)
      layout(250, 40) {}
    }

    builder.measure(boxUnder(atMost(300, 300)))
    val result = builder.build(boxUnder(SizeConstraints.exact(280, 40)))

    assertThat(seen).containsExactly(atMost(300, 300), SizeConstraints.exact(280, 40))
    assertThat(result.width).isEqualTo(280)
  }

  /** Only the latest run is kept: placing an earlier measurement costs one more run. */
  @Test
  fun `built in a box it was measured in before its latest run, its function runs again`() {
    var runs = 0
    val builder = builderFor { constraints ->
      runs++
      layout(constraints.maxWidth.coerceAtMost(200), 10) {}
    }

    builder.measure(boxUnder(atMost(300, 300)))
    builder.measure(boxUnder(SizeConstraints.exact(250, 10)))
    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(runs).isEqualTo(3)
    assertThat(result.width).isEqualTo(200)
  }

  // A failure

  @Test
  fun `a function that throws measures nothing inside its edges, and its result carries it`() {
    val failure = IllegalStateException("boom")
    val builder = builderFor { _ -> throw failure }

    val size = builder.measure(boxUnder(atMost(300, 300), padding = edges(10)))
    val result = builder.build(boxUnder(atMost(300, 300), padding = edges(10)))

    assertThat(size).isEqualTo(IntSize(20, 20))
    assertThat(result.failure).isSameAs(failure)
    assertThat(positionsOf(result)).isEmpty()
  }

  /**
   * The parent already laid out around its 0 x 0, so a later success would be placed unmeasured.
   */
  @Test
  fun `a failure is final for the pass`() {
    var runs = 0
    val failure = IllegalStateException("boom")
    val builder = builderFor { _ ->
      runs++
      if (runs == 1) throw failure
      layout(10, 10) {}
    }

    builder.measure(boxUnder(atMost(300, 300)))
    val size = builder.measure(boxUnder(atMost(100, 100)))
    val result = builder.build(boxUnder(SizeConstraints.exact(0, 0)))

    assertThat(runs).isEqualTo(1)
    assertThat(size).isEqualTo(IntSize(0, 0))
    assertThat(result.failure).isSameAs(failure)
  }

  /** A canceled pass's tree is never committed: its function's earlier run is dropped too. */
  @Test
  fun `once its pass's future is released, its function doesn't run and it places nothing`() {
    var released = false
    val future: TreeFuture<*> = mock { on { isReleased } doAnswer { released } }
    var runs = 0
    val builder =
        builderFor(children = listOf(leaf(30, 20)), future = future) { measurables, _ ->
          runs++
          val placeable = measurables[0].measure(atMost(200, 200))
          layout(40, 30) { placeable.place(0, 0) }
        }

    assertThat(builder.measure(boxUnder(atMost(300, 300)))).isEqualTo(IntSize(40, 30))
    released = true
    val size = builder.measure(boxUnder(atMost(100, 100)))
    val result = builder.build(boxUnder(atMost(100, 100)))

    assertThat(runs).isEqualTo(1)
    assertThat(size).isEqualTo(IntSize(0, 0))
    assertThat(result.childCount).isZero()
    assertThat(positionsOf(result)).isEmpty()
  }

  @Test
  fun `a function that declares no size fails`() {
    val builder = builderFor { _ -> }

    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(result.failure)
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage(NO_SIZE_DECLARED)
  }

  @Test
  fun `a placement that throws keeps the declared size, and its result carries it`() {
    val failure = IllegalStateException("boom")
    val builder = builderFor { _ -> layout(40, 30) { throw failure } }

    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(IntSize(result.width, result.height)).isEqualTo(IntSize(40, 30))
    assertThat(result.failure).isSameAs(failure)
  }

  @Test
  fun `an Error isn't a layout's failure, so it propagates`() {
    val builder = builderFor { _ -> throw AssertionError("not a layout failure") }

    assertThatThrownBy { builder.measure(boxUnder(atMost(300, 300))) }
        .isInstanceOf(AssertionError::class.java)
  }

  // Its children

  @Test
  fun `it measures a child under the constraints its function asks, and builds what it placed`() {
    val placeables = mutableListOf<Placeable>()
    val builder =
        builderFor(children = listOf(leaf(30, 20), leaf(40, 10, margin = 2))) { measurables, _ ->
          val first = measurables[0].measure(SizeConstraints(maxWidth = 200, maxHeight = 200))
          val second = measurables[1].measure(SizeConstraints(maxWidth = 200, maxHeight = 200))
          placeables.add(first)
          placeables.add(second)
          layout(100, 50) {
            first.place(5, 6)
            second.place(50, 0)
          }
        }

    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(
        placeables.map { placeable ->
          IntSize(placeable.measuredWidth, placeable.measuredHeight)
        },
    )
        .containsExactly(IntSize(30, 20), IntSize(44, 14))
    assertThat(placeables.map { placeable -> placeable.constraints })
        .containsOnly(SizeConstraints(maxWidth = 200, maxHeight = 200))
    assertThat(result.childCount).isEqualTo(2)
    assertThat(IntSize(result.getChildAt(1).width, result.getChildAt(1).height))
        .isEqualTo(IntSize(40, 10))
    // Each child's border box: where it was placed, past its left and top margins.
    assertThat(positionsOf(result)).containsExactly(5, 6, 52, 2)
  }

  @Test
  fun `measured again under the same constraints, a child answers the same placeable`() {
    val placeables = mutableListOf<Placeable>()
    val builder =
        builderFor(children = listOf(leaf(30, 20))) { measurables, _ ->
          placeables.add(measurables[0].measure(atMost(200, 200)))
          placeables.add(measurables[0].measure(atMost(200, 200)))
          layout(30, 20) { placeables[0].place(0, 0) }
        }

    builder.measure(boxUnder(atMost(300, 300)))

    assertThat(placeables[1]).isSameAs(placeables[0])
  }

  @Test
  fun `a child it measures but doesn't place isn't built`() {
    val builder =
        builderFor(children = listOf(leaf(30, 20), leaf(40, 10))) { measurables, _ ->
          val first = measurables[0].measure(atMost(200, 200))
          measurables[1].measure(atMost(200, 200))
          layout(30, 20) { first.place(0, 0) }
        }

    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(result.childCount).isEqualTo(1)
    assertThat(result.getChildAt(0).width).isEqualTo(30)
  }

  @Test
  fun `a Layout child is laid out by its own builder`() {
    val inner = Layout(
        listOf(leaf(10, 10)),
        layout =
            LayoutFunctionN { measurables, _ ->
              val placeable = measurables[0].measure(atMost(100, 100))
              layout(60, 30) { placeable.place(3, 4) }
            },
    )
    val builder =
        builderFor(children = listOf(inner)) { measurables, _ ->
          val placeable = measurables[0].measure(atMost(200, 200))
          layout(100, 50) { placeable.place(0, 0) }
        }

    val child = builder.build(boxUnder(atMost(300, 300))).getChildAt(0)

    assertThat(IntSize(child.width, child.height)).isEqualTo(IntSize(60, 30))
    assertThat(positionsOf(child)).containsExactly(3, 4)
  }

  @Test
  fun `right to left, a child placed from the start is mirrored within the declared width`() {
    val builder =
        builderFor(Style.layoutDirection(YogaDirection.RTL), children = listOf(leaf(30, 20))) {
            measurables,
            _ ->
          val placeable = measurables[0].measure(atMost(200, 200))
          layout(100, 50) { placeable.placeRelative(start = 10) }
        }

    val result = builder.build(boxUnder(atMost(300, 300)))

    assertThat(positionsOf(result)).containsExactly(60, 0)
  }

  @Test
  fun `its box props are its node's own`() {
    val builder =
        builderFor(Style.width(120.px).margin(all = 4.px).padding(all = 6.px)) { _ ->
          layout(10, 10) {}
        }

    val box = resolveBoxConstraints(atMost(300, 300), builder.boxProps, false)

    assertThat(box.margins).isEqualTo(edges(4))
    assertThat(box.padding).isEqualTo(edges(6))
    assertThat(box.borderBox.hasExactWidth).isTrue
    assertThat(box.borderBox.maxWidth).isEqualTo(120)
  }

  // Its lifetime

  @Test
  fun `its result is marked as holding a Layout`() {
    val builder = builderFor { _ -> layout(10, 10) {} }

    assertThat(builder.build(boxUnder(atMost(300, 300))).subtreeHasCustomLayout).isTrue()
  }

  @Test
  fun `built, it can't be measured or built again, and discarding it does nothing`() {
    val builder = builderFor { _ -> layout(10, 10) {} }
    builder.build(boxUnder(atMost(300, 300)))

    assertThatThrownBy { builder.boxProps }.isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.measure(boxUnder(atMost(300, 300))) }
        .isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.measureUnder(atMost(300, 300)) }
        .isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.build(boxUnder(atMost(300, 300))) }
        .isInstanceOf(IllegalStateException::class.java)
    assertThat(builder.discard()).describedAs("discarded after it was built").isNull()
  }

  @Test
  fun `discarded, it can't be measured or built`() {
    val builder = builderFor { _ -> layout(10, 10) {} }
    builder.measure(boxUnder(atMost(300, 300)))
    builder.discard()

    assertThatThrownBy { builder.boxProps }.isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.measure(boxUnder(atMost(300, 300))) }
        .isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.measureUnder(atMost(300, 300)) }
        .isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.build(boxUnder(atMost(300, 300))) }
        .isInstanceOf(IllegalStateException::class.java)
    assertThat(builder.discard()).describedAs("discarded twice").isNull()
  }

  // Helpers

  private fun builderFor(
      style: Style = Style,
      future: TreeFuture<*>? = null,
      function: MeasureScope.(SizeConstraints) -> Unit,
  ): DefaultLayoutBuilder =
      builderFor(style, emptyList(), future) { _, constraints -> function(this, constraints) }

  private fun builderFor(
      style: Style = Style,
      children: List<Component>,
      future: TreeFuture<*>? = null,
      function: MeasureScope.(List<Measurable>, SizeConstraints) -> Unit,
  ): DefaultLayoutBuilder {
    val componentContext =
        ComponentTree.create(ComponentContext(ApplicationProvider.getApplicationContext<Context>()))
            .build()
            .context
    val resolveContext = componentContext.setRenderStateContextForTests()
    val node = requireNotNull(
        Resolver.resolveTree(
            resolveContext,
            componentContext,
            Layout(
                children,
                style = style,
                layout =
                    LayoutFunctionN { measurables, constraints ->
                      function(this, measurables, constraints)
                    },
            ),
        ),
    )
    val layoutCache = LayoutCache()
    val lithoLayoutContext = LithoLayoutContext(
        resolveContext.treeId,
        resolveContext.cache,
        componentContext,
        resolveContext.treeState,
        resolveContext.layoutVersion,
        resolveContext.rootComponentId,
        resolveContext.isAccessibilityEnabled,
        layoutCache,
        null,
        future,
    )
    return DefaultLayoutBuilder(
        LayoutContext(componentContext.androidContext, lithoLayoutContext, 0, layoutCache, null),
        node,
        previousDiffNode = null,
    )
  }

  private fun boxUnder(
      room: SizeConstraints,
      padding: IntEdges = IntEdges.None,
      border: IntEdges = IntEdges.None,
  ): BoxConstraints = BoxConstraints(IntEdges.None, padding, border, room)

  private fun atMost(width: Int, height: Int): SizeConstraints =
      SizeConstraints(maxWidth = width, maxHeight = height)

  private fun edges(all: Int): IntEdges = IntEdges(all, all, all, all)

  private fun leaf(width: Int, height: Int, margin: Int = 0): Component =
      SimpleMountSpecTester(style = Style.width(width.px).height(height.px).margin(all = margin.px))

  private fun outputOf(result: LithoLayoutResult): DefaultMeasureOutput =
      result.measureOutput as DefaultMeasureOutput

  /** Each built child's left and top, as [result]'s output places them. */
  private fun positionsOf(result: LithoLayoutResult): List<Int> {
    val output = outputOf(result)
    return (0 until result.childCount).flatMap { index ->
      listOf(output.childX(index), output.childY(index))
    }
  }
}
