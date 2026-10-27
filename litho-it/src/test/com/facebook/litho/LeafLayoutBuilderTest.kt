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
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.layout.BoxConstraints
import com.facebook.litho.layout.IntEdges
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.internal.LeafLayoutBuilder
import com.facebook.litho.layout.internal.LithoLayoutBuilder
import com.facebook.litho.layout.internal.YogaTreeBuilder
import com.facebook.litho.layout.resolveBoxConstraints
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.Text
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope as PrimitiveLayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [LeafLayoutBuilder] lays a `Layout`'s leaf child out as a one-node Yoga tree does, with no Yoga
 * node. The tests that compare the two run [YogaTreeBuilder] on the same leaves under the same
 * constraints.
 */
@OptIn(ExperimentalLithoApi::class)
@RunWith(LithoTestRunner::class)
class LeafLayoutBuilderTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @After
  fun clearTint() {
    tint = null
  }

  @Test
  fun `it lays a leaf out as a one-node Yoga tree does`() {
    for (component in leaves()) {
      for (constraints in constraintCases) {
        val (yoga, yogaBox) =
            builderFor(component, constraints) { context, node ->
              YogaTreeBuilder(context, node, previousDiffNode = null)
            }
        val (leaf, leafBox) =
            builderFor(component, constraints) { context, node ->
              LeafLayoutBuilder(context, node, previousDiffNode = null)
            }
        val case = "${component::class.java.simpleName} under $constraints"
        assertThat(leafBox).describedAs(case).isEqualTo(yogaBox)

        assertThat(leaf.measure(leafBox)).describedAs(case).isEqualTo(yoga.measure(yogaBox))
        val expected = yoga.build(yogaBox)
        val built = leaf.build(leafBox)

        assertThat(built.measureOutput).describedAs(case).isInstanceOf(LeafLayoutOutput::class.java)
        assertThat(recordOf(built)).describedAs(case).isEqualTo(recordOf(expected))
      }
    }
  }

  /**
   * Measured or built in a box it measured in before, even before the last, or in one whose specs
   * are compatible with an earlier measurement's, it reuses that measurement, as Yoga does: its
   * spec stays its latest measurement's, as on a Yoga cache hit, and `onBoundsDefined` corrects
   * what it mounts with for the size it's built at.
   */
  @Test
  fun `in a box it measured in before, it isn't measured again`() {
    val measures = ArrayList<SizeConstraints>()
    val (builder, wide) = builderFor(CountingPrimitive(measures), atMost(300, 100), ::leafBuilder)
    val narrow = wide.copy(borderBox = atMost(80, 100))
    val compatible = wide.copy(borderBox = atMost(120, 100))

    builder.measure(wide)
    builder.measure(wide)
    builder.measure(narrow)
    builder.measure(compatible)
    val built = builder.build(wide)

    assertThat(measures).containsExactly(atMost(300, 100), atMost(80, 100))
    assertThat(SizeSpec.getSize(built.widthSpec)).describedAs("its content spec").isEqualTo(80)
  }

  /** A component that always remeasures reuses a measurement only under the same specs. */
  @Test
  fun `a component that always remeasures is measured again under compatible specs`() {
    val compatibleCounts = ArrayList<Int>()
    val (compatibleBuilder, wide) =
        builderFor(AlwaysRemeasuringProbe(compatibleCounts), atMost(300, 100), ::leafBuilder)
    compatibleBuilder.measure(wide)
    compatibleBuilder.measure(wide.copy(borderBox = atMost(120, 100)))

    val equalCounts = ArrayList<Int>()
    val (equalBuilder, sameWide) =
        builderFor(AlwaysRemeasuringProbe(equalCounts), atMost(300, 100), ::leafBuilder)
    equalBuilder.measure(sameWide)
    equalBuilder.measure(sameWide)

    assertThat(compatibleCounts).describedAs("at most 300, then 120").containsExactly(300, 120)
    assertThat(equalCounts).describedAs("at most 300 twice").containsExactly(300)
  }

  @Test
  fun `it keeps its last eight measurements`() {
    val (context, _) = passFor(SimpleMountSpecTester())
    val output = LeafLayoutOutput(LayoutDirection.LTR, context)
    val height = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED)
    val widths = List(9) { SizeSpec.makeSizeSpec(100 + it, SizeSpec.EXACTLY) }

    widths.forEachIndexed { i, width ->
      output.recordMeasurement(width, height, IntSize(100 + i, 10))
    }

    assertThat(output.contentMeasuredUnder(widths[0], height, matchesCompatibleSpecs = true))
        .describedAs("the oldest")
        .isEqualTo(IntSize.Invalid)
    widths.forEachIndexed { i, width ->
      if (i > 0) {
        assertThat(output.contentMeasuredUnder(width, height, matchesCompatibleSpecs = true))
            .describedAs("measurement %s", i)
            .isEqualTo(IntSize(100 + i, 10))
      }
    }
  }

  @Test
  fun `its box props are its node's, and its edges are its box's`() {
    val component = SimpleMountSpecTester(
        style = Style.width(40.px).height(20.px).margin(all = 5.px).padding(all = 3.px),
    )
    val (builder, box) = builderFor(component, atMost(300, 100), ::leafBuilder)

    val built = builder.build(box)

    assertThat(box.margins.left).isEqualTo(5)
    assertThat(built.resolvedMargins).isEqualTo(box.margins)
    assertThat(built.paddingLeft).isEqualTo(3)
    assertThat(IntSize(built.width, built.height)).isEqualTo(IntSize(40, 20))
    assertThat(built.node.isPaddingSet).isTrue()
  }

  @Test
  fun `discarded, it can't be measured or built`() {
    val (builder, box) =
        builderFor(
            SimpleMountSpecTester(measuredWidth = 40, measuredHeight = 20),
            atMost(300, 100),
            ::leafBuilder,
        )
    builder.measure(box)

    builder.discard()

    assertThatThrownBy { builder.measure(box) }.isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.build(box) }.isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `built, it can't be measured or built again, and discarding it does nothing`() {
    val (builder, box) =
        builderFor(
            SimpleMountSpecTester(measuredWidth = 30, measuredHeight = 20),
            atMost(300, 100),
            ::leafBuilder,
        )
    val built = builder.build(box)

    assertThat(builder.discard()).isNull()
    assertThat((built.measureOutput as LeafLayoutOutput).layoutContext).isNotNull()
    assertThatThrownBy { builder.measure(box) }.isInstanceOf(IllegalStateException::class.java)
    assertThatThrownBy { builder.build(box) }.isInstanceOf(IllegalStateException::class.java)
  }

  // Across passes

  /** A leaf whose own content changes is a new node, so it misses the cache and measures again. */
  @Test
  fun `a leaf whose content changes under the same constraints is measured again`() {
    val measures = ArrayList<SizeConstraints>()
    var width: State<Int>? = null

    class Sized : KComponent() {
      @Suppress("KComponentUncachedAllocationDetector")
      override fun ComponentScope.render(): Component {
        val state = useState { 30 }
        width = state
        return Layout(
            listOf(SizedPrimitive(measures, state.value)),
            layout = MEASURES_AT_MOST,
        )
      }
    }

    val view = lithoTestRule.render(widthPx = 300, heightPx = 300) { Column { child(Sized()) } }
    lithoTestRule.act(view) { checkNotNull(width).update(60) }
    lithoTestRule.idle()

    assertThat(measures).describedAs("measured in both passes").hasSize(2)
    val root = view.componentTree.mainThreadLayoutState?.rootLayoutResult as LithoLayoutResult
    val leaf = root.getChildAt(0).getChildAt(0)
    assertThat(leaf.isCachedLayout).describedAs("not copied from the layout cache").isFalse()
    assertThat(leaf.width).describedAs("its new content's width").isEqualTo(60)
  }

  @Test
  fun `a leaf nothing updated in is copied from the layout cache, not measured again`() {
    val measures = ArrayList<SizeConstraints>()
    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column {
            child(Layout(listOf(CountingPrimitive(measures)), layout = MEASURES_AT_MOST))
            child(TintedFooter())
          }
        }
    val first = measures.size

    lithoTestRule.act(view) { checkNotNull(tint).update { it + 1 } }
    lithoTestRule.idle()

    assertThat(first).isEqualTo(1)
    assertThat(measures).describedAs("measures over both passes").hasSize(1)
    val root = view.componentTree.mainThreadLayoutState?.rootLayoutResult as LithoLayoutResult
    val leaf = root.getChildAt(0).getChildAt(0)
    assertThat(leaf.isCachedLayout).describedAs("copied from the layout cache").isTrue()
    assertThat(IntSize(leaf.width, leaf.height)).isEqualTo(IntSize(100, 20))
  }

  /**
   * A leaf measured at most is copied from the layout cache pass after pass with its measurement:
   * placing a copy it didn't measure records its bounds as exact specs, which must not make the
   * next pass's copy measure it, and call its `onBoundsDefined`, again.
   */
  @Test
  fun `a leaf copied from the layout cache isn't measured or bounded again over several passes`() {
    val tracker = LifecycleTracker()
    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column {
            child(
                Layout(
                    listOf(
                        MountSpecLifecycleTester.create(lithoTestRule.context)
                            .intrinsicSize(Size(40, 20))
                            .lifecycleTracker(tracker)
                            .build(),
                    ),
                    layout = MEASURES_AT_MOST,
                ),
            )
            child(TintedFooter())
          }
        }
    tracker.reset()

    repeat(3) {
      lithoTestRule.act(view) { checkNotNull(tint).update { it + 1 } }
      lithoTestRule.idle()
    }

    assertThat(tracker.steps)
        .describedAs("over three passes that update only the footer")
        .doesNotContain(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
  }

  /** The control for the test above: a pass that changes the leaf measures and bounds it. */
  @Test
  fun `a leaf whose props change is measured and bounded again`() {
    val tracker = LifecycleTracker()
    var width: State<Int>? = null

    class Tracked : KComponent() {
      @Suppress("KComponentUncachedAllocationDetector")
      override fun ComponentScope.render(): Component {
        val state = useState { 40 }
        width = state
        return Layout(
            listOf(
                MountSpecLifecycleTester.create(lithoTestRule.context)
                    .intrinsicSize(Size(state.value, 20))
                    .lifecycleTracker(tracker)
                    .build(),
            ),
            layout = MEASURES_AT_MOST,
        )
      }
    }

    val view = lithoTestRule.render(widthPx = 300, heightPx = 300) { Column { child(Tracked()) } }
    tracker.reset()
    lithoTestRule.act(view) { checkNotNull(width).update(50) }
    lithoTestRule.idle()

    assertThat(tracker.steps)
        .describedAs("a pass that changes the leaf")
        .contains(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
  }

  /**
   * Yoga hands a leaf's layout padding to Java only once its pass is done, so a MountSpec measured
   * in a Column doesn't see its own padding yet. Under a Layout it sees what it would there.
   */
  @Test
  fun `a MountSpec's onMeasure sees the padding under a Layout it would under a Column`() {
    val underLayout = ArrayList<Int>()
    val underColumn = ArrayList<Int>()

    lithoTestRule.render(widthPx = 300, heightPx = 300) {
      Layout(listOf(paddedProbe(underLayout)), layout = MEASURES_AT_MOST)
    }
    lithoTestRule.render(widthPx = 300, heightPx = 300) {
      Column { child(paddedProbe(underColumn)) }
    }

    assertThat(underColumn).describedAs("under a Column").isNotEmpty
    assertThat(underLayout.toSet()).describedAs("under a Layout").isEqualTo(underColumn.toSet())
  }

  /**
   * Measured again from the layout cache, it sees the padding it had, as Yoga's cached node does.
   */
  @Test
  fun `a MountSpec measured again from the cache sees the padding it would under a Column`() {
    val underLayout = ArrayList<Int>()
    val underColumn = ArrayList<Int>()

    val inLayout =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Layout(listOf(paddedProbe(underLayout)), layout = MEASURES_AT_MOST)
        }
    inLayout.setSizePx(250, 300).measure().layout()
    val inColumn =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Column { child(paddedProbe(underColumn)) }
        }
    inColumn.setSizePx(250, 300).measure().layout()

    assertThat(underLayout).describedAs("measured in both passes").hasSizeGreaterThan(1)
    assertThat(underLayout.toSet()).describedAs("under a Layout").isEqualTo(underColumn.toSet())
  }

  private fun paddedProbe(seen: MutableList<Int>): Component =
      Wrapper.create(lithoTestRule.context)
          .delegate(PaddingProbe(seen))
          .paddingPx(YogaEdge.LEFT, 7)
          .build()

  /**
   * A Primitive in a box exact on both axes isn't measured with its box, as Yoga doesn't measure a
   * leaf then. It's measured once it's placed, through the context its output keeps for that.
   */
  @Test
  fun `a Primitive in a box exact on both axes is measured once it's placed`() {
    val measures = ArrayList<SizeConstraints>()

    lithoTestRule.render(widthPx = 300, heightPx = 300) {
      Layout(
          listOf(CountingPrimitive(measures, style = Style.width(40.px).height(20.px))),
          layout = MEASURES_AT_MOST,
      )
    }

    assertThat(measures).containsExactly(SizeConstraints.exact(40, 20))
  }

  /**
   * A leaf's copy from the layout cache takes every field of its output but the two it's made with:
   * a field added without being copied fails here.
   */
  @Test
  fun `a leaf's cache copy carries every field of its output`() {
    val (context, _) = passFor(SimpleMountSpecTester())
    val source = LeafLayoutOutput(LayoutDirection.RTL, context)
    val fields =
        LeafLayoutOutput::class
            .java
            .declaredFields
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .filter { it.name !in setOf("layoutDirection", "layoutContext") }
            .onEach { it.isAccessible = true }
    fields.forEachIndexed { i, field ->
      field.set(
          source,
          when (field.type) {
            Int::class.javaPrimitiveType -> 101 + i
            Long::class.javaPrimitiveType -> 1001L + i
            Boolean::class.javaPrimitiveType -> true
            IntEdges::class.java -> IntEdges(i, i + 1, i + 2, i + 3)
            IntArray::class.java -> intArrayOf(i, i + 1, i + 2, i + 3)
            else -> error("no test value for ${field.name}: ${field.type}")
          },
      )
    }
    val copy = source.cachedCopy(context)

    assertThat(fields).describedAs("its fields").isNotEmpty
    fields.forEach { field ->
      assertThat(field.get(copy)).describedAs(field.name).isEqualTo(field.get(source))
    }
  }

  @Test
  fun `a MountSpec and Text are bounded and mounted at the size they measured`() {
    val tracker = LifecycleTracker()
    val view =
        lithoTestRule.render(widthPx = 300, heightPx = 300) {
          Layout(
              listOf(
                  MountSpecLifecycleTester.create(lithoTestRule.context)
                      .intrinsicSize(Size(30, 20))
                      .lifecycleTracker(tracker)
                      .build(),
                  Text.create(lithoTestRule.context).text("text").build(),
              ),
              layout = MEASURES_AT_MOST,
          )
        }
    val root = view.componentTree.mainThreadLayoutState?.rootLayoutResult as LithoLayoutResult

    assertThat(tracker.steps).contains(LifecycleStep.ON_MEASURE, LifecycleStep.ON_BOUNDS_DEFINED)
    assertThat(tracker.definedBounds).describedAs("onBoundsDefined").isEqualTo(Rect(0, 0, 30, 20))
    val mounted = view.lithoView.getChildAt(0)
    assertThat(Rect(mounted.left, mounted.top, mounted.right, mounted.bottom))
        .describedAs("its mounted view")
        .isEqualTo(Rect(0, 0, 30, 20))
    val text = root.getChildAt(1)
    assertThat(text.width).isGreaterThan(0)
    assertThat(text.contentRenderUnit).describedAs("Text's content").isNotNull()
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
      SizeConstraints(minWidth = 50, maxWidth = 200, minHeight = 10, maxHeight = 200),
      SizeConstraints(),
  )

  private fun leaves(): List<Component> = listOf(
      SimpleMountSpecTester(measuredWidth = 30, measuredHeight = 20),
      SimpleMountSpecTester(measuredWidth = 400, measuredHeight = 400),
      SimpleMountSpecTester(style = Style.width(30.px).height(20.px)),
      SimpleMountSpecTester(style = Style.width(30.px).height(20.px).padding(all = 4.px)),
      SimpleMountSpecTester(style = Style.padding(start = 6.px, top = 2.px).margin(all = 3.px)),
      Text.create(lithoTestRule.context).text("some text that wraps").textSizePx(20).build(),
      CountingPrimitive(ArrayList()),
      Column(style = Style.width(15.px).padding(all = 2.px)),
  )

  private fun atMost(width: Int, height: Int): SizeConstraints =
      SizeConstraints(maxWidth = width, maxHeight = height)

  // Harness

  private data class Record(
      val width: Int,
      val height: Int,
      val padding: List<Int>,
      val border: List<Int>,
      val widthSpec: Int,
      val heightSpec: Int,
      val contentWidth: Int,
      val contentHeight: Int,
  )

  private fun recordOf(result: LithoLayoutResult): Record = Record(
      result.width,
      result.height,
      listOf(result.paddingLeft, result.paddingTop, result.paddingRight, result.paddingBottom),
      listOf(result.borderLeft, result.borderTop, result.borderRight, result.borderBottom),
      result.widthSpec,
      result.heightSpec,
      result.contentWidth,
      result.contentHeight,
  )

  private fun leafBuilder(
      context: LayoutContext<LithoLayoutContext>,
      node: LithoNode,
  ): LithoLayoutBuilder = LeafLayoutBuilder(context, node, previousDiffNode = null)

  /**
   * A builder for [component]'s node, and the box its own props resolve to within [constraints].
   */
  private fun builderFor(
      component: Component,
      constraints: SizeConstraints,
      newBuilder: (LayoutContext<LithoLayoutContext>, LithoNode) -> LithoLayoutBuilder,
  ): Pair<LithoLayoutBuilder, BoxConstraints> {
    val (context, node) = passFor(component)
    val builder = newBuilder(context, node)
    return builder to
        resolveBoxConstraints(constraints, builder.boxProps, node.layoutDirection.isRTL)
  }

  /** [component] resolved, and a layout pass of its own to lay it out in. */
  private fun passFor(component: Component): Pair<LayoutContext<LithoLayoutContext>, LithoNode> {
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
    return LayoutContext(
        componentContext.androidContext,
        lithoLayoutContext,
        0,
        layoutCache,
        null,
    ) to node
  }
}

/** Measures each child at most within its room, and places them at the origin. */
private val MEASURES_AT_MOST = LayoutFunctionN { measurables, constraints ->
  val placeables = measurables.map {
    it.measure(SizeConstraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
  }
  layout(constraints.maxWidth, placeables.maxOfOrNull { it.height } ?: 0) {
    placeables.forEach { it.place(0, 0) }
  }
}

private var tint: State<Int>? = null

/** A sibling whose state, updated, lays the tree out again without touching the `Layout`. */
private class TintedFooter : KComponent() {
  override fun ComponentScope.render(): Component {
    val state = useState { 0 }
    tint = state
    return Column(style = Style.width(10.px).height((10 + state.value).px))
  }
}

/** A Primitive 100 by 20 within its constraints, recording each one it's measured under. */
private class CountingPrimitive(
    private val measures: MutableList<SizeConstraints>,
    private val style: Style? = null,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = CountingBehavior(measures),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = style,
  )
}

/** A Primitive as wide as [width] and 20 tall, recording each constraint it's measured under. */
private class SizedPrimitive(
    private val measures: MutableList<SizeConstraints>,
    private val width: Int,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive = LithoPrimitive(
      layoutBehavior = SizedBehavior(measures, width),
      mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
      style = null,
  )
}

private class SizedBehavior(
    private val measures: MutableList<SizeConstraints>,
    private val width: Int,
) : LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult {
    measures.add(sizeConstraints)
    return PrimitiveLayoutResult(width = width, height = 20)
  }

  override fun isEquivalentTo(other: LayoutBehavior): Boolean =
      other is SizedBehavior && other.width == width
}

private class CountingBehavior(private val measures: MutableList<SizeConstraints>) :
    LayoutBehavior {
  override fun PrimitiveLayoutScope.layout(
      sizeConstraints: SizeConstraints,
  ): PrimitiveLayoutResult {
    measures.add(sizeConstraints)
    return PrimitiveLayoutResult(
        width = minOf(100, sizeConstraints.maxWidth),
        height = minOf(20, sizeConstraints.maxHeight),
    )
  }

  override fun isEquivalentTo(other: LayoutBehavior): Boolean = other is CountingBehavior
}

/** A measured MountSpec, 100 by 10, that always remeasures and records each width spec's size. */
private class AlwaysRemeasuringProbe(private val widths: MutableList<Int>) :
    SpecGeneratedComponent("AlwaysRemeasuringProbe") {

  override fun canMeasure(): Boolean = true

  override fun shouldAlwaysRemeasure(): Boolean = true

  override fun getMountType(): MountType = MountType.DRAWABLE

  override fun onCreateMountContent(context: Context): Any = ColorDrawable()

  override fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      interStagePropsContainer: InterStagePropsContainer?,
  ) {
    widths.add(SizeSpec.getSize(widthSpec))
    size.width = 100
    size.height = 10
  }
}

/** A measured MountSpec that records the left padding it sees each time it's measured. */
private class PaddingProbe(private val seen: MutableList<Int>) :
    SpecGeneratedComponent("PaddingProbe") {

  override fun canMeasure(): Boolean = true

  override fun getMountType(): MountType = MountType.DRAWABLE

  override fun onCreateMountContent(context: Context): Any = ColorDrawable()

  override fun onMeasure(
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      interStagePropsContainer: InterStagePropsContainer?,
  ) {
    seen.add(layout.paddingLeft)
    // All its room, so a narrower one measures it again.
    size.width = SizeSpec.getSize(widthSpec)
    size.height = 10
  }
}
