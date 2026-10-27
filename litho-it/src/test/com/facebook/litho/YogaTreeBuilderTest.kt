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
import com.facebook.litho.YogaLayoutOutput.Companion.getYogaNode
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.layout.BoxConstraints
import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.layout.internal.YogaTreeBuilder
import com.facebook.litho.layout.internal.layOutWithin
import com.facebook.litho.layout.resolveBoxConstraints
import com.facebook.litho.stateupdates.ComponentWithMeasureCall
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import com.facebook.yoga.YogaDirection
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [YogaTreeBuilder] lays a `Layout`'s child out as one Yoga pass of a fresh tree in its box does,
 * checked on the same nodes under the same constraints: its size, and its tree's sizes and
 * positions. Then what's its own: one tree laid out again in place, written to the layout cache
 * when it's built, or when it drops a tree holding content measured at render.
 */
@RunWith(LithoTestRunner::class)
class YogaTreeBuilderTest {

  // As one Yoga pass lays it out

  @Test
  fun `it lays a child out as one Yoga pass of a fresh tree does`() {
    for (component in components()) {
      for (constraints in constraintCases) {
        val expected = layOutInOnePass(component, constraints)
        val (builder, box) = builderUnder(component, constraints)

        val size = builder.measure(box)
        val result = builder.build(box)

        val case = "${component.simpleName} under $constraints"
        assertThat(size).describedAs(case).isEqualTo(IntSize(expected.width, expected.height))
        assertThat(geometryOf(result)).describedAs(case).isEqualTo(geometryOf(expected))
      }
    }
  }

  // One tree, in place

  @Test
  fun `measured within another box, it lays the same tree out again`() {
    val (builder, wide) = builderUnder(fixedRow(), SizeConstraints.exact(300, 100))
    val narrow = wide.copy(borderBox = SizeConstraints.exact(120, 100))

    assertThat(builder.measure(wide)).isEqualTo(IntSize(300, 100))
    assertThat(builder.measure(narrow)).isEqualTo(IntSize(120, 100))
    val result = builder.build(wide)

    assertThat(geometryOf(result))
        .describedAs("laid out again in the wide box, as a fresh tree is")
        .isEqualTo(geometryOf(layOutInOnePass(fixedRow(), SizeConstraints.exact(300, 100))))
  }

  @Test
  fun `its box props are its node's, before it's measured`() {
    val (builder, _) =
        builderUnder(
            SimpleMountSpecTester(style = Style.width(40.px).height(20.px).margin(all = 5.px)),
            SizeConstraints.exact(300, 100),
        )

    val box =
        resolveBoxConstraints(SizeConstraints.exact(300, 100), builder.boxProps, isRtl = false)

    assertThat(box.margins.left).isEqualTo(5)
    assertThat(builder.measure(box)).isEqualTo(IntSize(40, 20))
  }

  @Test
  fun `deferred content is resolved again in each new box`() {
    val (builder, wide) =
        builderUnder(
            Row {
              child(
                  RenderWithConstraints { constraints ->
                    if (constraints.maxWidth <= 100)
                        SimpleMountSpecTester(style = Style.width(20.px).height(40.px))
                    else SimpleMountSpecTester(style = Style.width(150.px).height(10.px))
                  },
              )
            },
            atMost(300, 300),
        )
    val narrow = wide.copy(borderBox = atMost(100, 300))

    assertThat(builder.measure(wide)).isEqualTo(IntSize(150, 10))
    assertThat(builder.measure(narrow)).isEqualTo(IntSize(20, 40))
    assertThat(builder.measure(wide)).isEqualTo(IntSize(150, 10))
    val result = builder.build(wide)

    assertThat(IntSize(result.width, result.height)).isEqualTo(IntSize(150, 10))
  }

  @Test
  fun `a box that differs only in its margins keeps its tree and its deferred content`() {
    var resolves = 0
    val (builder, plain) =
        builderUnder(
            Row {
              child(
                  RenderWithConstraints {
                    resolves++
                    SimpleMountSpecTester(style = Style.width(40.px).height(20.px))
                  },
              )
            },
            atMost(300, 300),
        )
    val inset = plain.copy(margins = IntEdges(7, 3, 0, 0))

    assertThat(builder.measure(plain)).isEqualTo(IntSize(40, 20))
    val tree = builder.build(inset)

    assertThat(resolves).isEqualTo(1)
    assertThat(IntSize(tree.width, tree.height)).isEqualTo(IntSize(40, 20))
    assertThat(tree.resolvedMargins).isEqualTo(IntEdges(7, 3, 0, 0))
  }

  // The layout cache

  @Test
  fun `its tree is written to the layout cache when it's built, not when it's measured`() {
    val (builder, box, layoutCache) = builderUnder(fixedRow(), atMost(300, 300))

    builder.measure(box)
    assertThat(cachedResults(layoutCache)).isEmpty()

    val result = builder.build(box)
    assertThat(cachedResults(layoutCache)).contains(result)
  }

  @Test
  fun `a discarded tree holding nothing measured at render isn't written to the layout cache`() {
    val (builder, box, layoutCache) = builderUnder(fixedRow(), atMost(300, 300))

    builder.measure(box)
    builder.discard()

    assertThat(cachedResults(layoutCache)).isEmpty()
  }

  /**
   * Measuring content `Component.measure` sized at render uses that size up, so a tree holding it,
   * discarded, is written to the layout cache for a later pass to copy rather than measure afresh.
   */
  @Test
  fun `a discarded tree holding content measured at render is written to the layout cache`() {
    val context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val (builder, box, layoutCache) =
        builderUnder(
            Row {
              child(
                  ComponentWithMeasureCall.create(context)
                      .component(SimpleMountSpecTester(style = Style.width(30.px).height(20.px)))
                      .widthSpec(SizeSpec.makeSizeSpec(30, SizeSpec.EXACTLY))
                      .heightSpec(SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY))
                      .shouldCacheResult(true)
                      .build(),
              )
            },
            atMost(300, 300),
        )

    builder.measure(box)
    builder.discard()

    // Not only the measured content, which `Component.measure` caches itself: the tree's root.
    assertThat(cachedResults(layoutCache).map { (it as LithoLayoutResult).node.headComponent })
        .describedAs("the discarded tree's root")
        .anyMatch { it.simpleName == "Row" }
  }

  // Its lifetime

  @Test
  fun `built, it can't be measured or built again, and discarding it does nothing`() {
    val (builder, box) = builderUnder(fixedRow(), atMost(300, 300))
    builder.measure(box)
    builder.build(box)

    assertThat(builder.discard()).describedAs("discarded after it was built").isNull()
    assertThatThrownBy { builder.measure(box) }.isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.build(box) }.isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `discarded, it can't be measured or built`() {
    val (builder, box) = builderUnder(fixedRow(), atMost(300, 300))
    builder.measure(box)

    builder.discard()

    assertThatThrownBy { builder.measure(box) }.isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.build(box) }.isInstanceOf(IllegalStateException::class.java)
  }

  // Cases

  private val constraintCases = listOf(
      atMost(300, 300),
      SizeConstraints.exact(250, 120),
      SizeConstraints(
          minWidth = 0,
          maxWidth = 80,
          minHeight = 0,
          maxHeight = SizeConstraints.Infinity,
      ),
      SizeConstraints(),
  )

  private fun components(): List<Component> = listOf(
      SimpleMountSpecTester(style = Style.width(30.px).height(20.px)),
      SimpleMountSpecTester(style = Style.width(30.px).height(20.px).padding(all = 4.px)),
      fixedRow(),
      Column(style = Style.padding(all = 6.px).margin(start = 3.px)) {
        child(SimpleMountSpecTester(style = Style.width(50.px).height(10.px)))
        child(SimpleMountSpecTester(style = Style.width(20.px).height(15.px).margin(all = 2.px)))
      },
      Row(style = Style.layoutDirection(YogaDirection.RTL).padding(start = 8.px)) {
        child(SimpleMountSpecTester(style = Style.width(40.px).height(10.px)))
        child(SimpleMountSpecTester(style = Style.width(25.px).height(30.px)))
      },
  )

  private fun fixedRow(): Component = Row {
    child(SimpleMountSpecTester(style = Style.width(60.px).height(40.px)))
    child(SimpleMountSpecTester(style = Style.width(30.px).height(20.px).margin(all = 4.px)))
  }

  private fun atMost(width: Int, height: Int): SizeConstraints =
      SizeConstraints(maxWidth = width, maxHeight = height)

  // Harness

  private data class Geometry(val width: Int, val height: Int, val children: List<ChildGeometry>)

  private data class ChildGeometry(val x: Int, val y: Int, val geometry: Geometry)

  private fun geometryOf(result: LithoLayoutResult): Geometry = Geometry(
      result.width,
      result.height,
      (0 until result.childCount).map { i ->
        ChildGeometry(
            result.getXForChildAtIndex(i),
            result.getYForChildAtIndex(i),
            geometryOf(result.getChildAt(i)),
        )
      },
  )

  private data class BuilderInBox(
      val builder: YogaTreeBuilder,
      val box: BoxConstraints,
      val layoutCache: LayoutCache,
  )

  /**
   * A builder for [component]'s node, and the box its own props resolve to within [constraints].
   */
  private fun builderUnder(component: Component, constraints: SizeConstraints): BuilderInBox {
    val (context, node, layoutCache) = passFor(component)
    val builder = YogaTreeBuilder(context, node, previousDiffNode = null)
    val box = resolveBoxConstraints(constraints, builder.boxProps, node.layoutDirection.isRTL)
    return BuilderInBox(builder, box, layoutCache)
  }

  /** A fresh tree of [component], laid out once in the box its own props resolve to. */
  private fun layOutInOnePass(
      component: Component,
      constraints: SizeConstraints,
  ): LithoLayoutResult {
    val (context, node, _) = passFor(component)
    val tree = LithoYogaLayoutFunction.newTree(context, node, previousDiffNode = null)
    val box = resolveBoxConstraints(
        constraints,
        checkNotNull(tree.childBoxProps),
        node.layoutDirection.isRTL,
    )
    tree.getYogaNode().layOutWithin(box)
    return tree
  }

  private data class Pass(
      val context: LayoutContext<LithoLayoutContext>,
      val node: LithoNode,
      val layoutCache: LayoutCache,
  )

  /** [component] resolved, and a layout pass of its own to lay it out in. */
  private fun passFor(component: Component): Pass {
    val componentContext =
        ComponentTree.create(ComponentContext(ApplicationProvider.getApplicationContext<Context>()))
            .build()
            .context
    val resolveContext = componentContext.setRenderStateContextForTests()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
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
        null,
    )
    return Pass(
        LayoutContext(componentContext.androidContext, lithoLayoutContext, 0, layoutCache, null),
        node,
        layoutCache,
    )
  }

  private fun cachedResults(layoutCache: LayoutCache): List<LayoutResult> =
      layoutCache.toCacheData().cache.values.mapNotNull { item ->
        (item as? LayoutCache.CacheItem)?.layoutResult
      }
}
