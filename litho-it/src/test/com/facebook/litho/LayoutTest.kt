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

import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val VIEWPORT = 1000
private const val CHILD = 100
private const val BOX_W = 300
private const val BOX_H = 150
private const val STRIDE = 150

private fun box(): Component = Column(style = Style.width(CHILD.px).height(CHILD.px))

/**
 * The Kotlin `Text` is a `ResourcesScope` extension, and these fixtures are built outside any
 * `render`. A scope made here is enough: nothing in `Text` reads it until the component resolves.
 */
private fun text(c: ComponentContext, value: String): Component =
    ComponentScope(c).run { Text(text = value) }

/**
 * `Layout` as a component, separately from what any particular layout function does.
 *
 * These cover the component's own behaviour: that it can be returned from an ordinary `render`,
 * that it carries a `Style`, that a null child is dropped, and that it compares equal to an
 * identical one so its subtree is not re-resolved every pass.
 *
 * Hooks need no test of their own. `Layout` runs no user code while resolving, because the children
 * were built inside the enclosing `render` before it was constructed, so there is no second
 * recorder scope that could be missing one.
 */
@RunWith(LithoTestRunner::class)
class LayoutTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  /**
   * A container is returned from `render` like any other component, so the hooks around it are the
   * enclosing component's own and need nothing re-plumbed. That is the argument for `Layout` being
   * a component rather than a component kind.
   */
  @Test
  fun `a container is returnable from an ordinary KComponent render`() {
    // Local, and captured by the component below, so nothing survives this test. A field on the
    // companion object would be written by any future test that renders a component like this one
    // and read by this one, with no reset in between.
    val observed = AtomicInteger(-1)

    class Stateful : KComponent() {
      // The children are built here, so caching the list would hand back the previous render's.
      @Suppress("KComponentUncachedAllocationDetector")
      override fun ComponentScope.render(): Component {
        val counter = useState { 42 }
        observed.set(counter.value)
        return Layout(listOf(box()), layout = InARow)
      }
    }

    val result = renderAndFindContainer(Stateful())

    assertThat(observed.get()).describedAs("the enclosing component's own hook ran").isEqualTo(42)
    assertThat(result.childCount).describedAs("and the container still declared its child").isOne
  }

  @Test
  fun `Style on the container is applied`() {
    class Styled : KComponent() {
      // The children are built here, so caching the list would hand back the previous render's.
      @Suppress("KComponentUncachedAllocationDetector")
      override fun ComponentScope.render(): Component =
          Layout(listOf(box()), style = Style.width(BOX_W.px).height(BOX_H.px), layout = InARow)
    }

    val result = renderAndFindContainer(Styled())

    assertThat(result.width).isEqualTo(BOX_W)
    assertThat(result.height).isEqualTo(BOX_H)
  }

  @Test
  fun `a container mounts no content of its own`() {
    assertThat(Layout(emptyList(), layout = InARow).mountType)
        .describedAs(
            "MountType.VIEW would make willMountView short-circuit needsHostView, and the " +
                "container's children would lose the host they are placed into",
        )
        .isEqualTo(Component.MountType.NONE)
    assertThat(renderAndFindContainer(Layout(listOf(box()), layout = InARow)).contentRenderUnit)
        .describedAs("and laid out, the container produces nothing to mount for itself")
        .isNull()
  }

  /**
   * A container is placed before the containers beneath it.
   *
   * The outer one has a definite size, so Yoga never runs its measure function; the inner one gets
   * its size only when the outer places it. Placing the inner one first would place its child
   * against a width of nothing, and nothing would place it again.
   */
  @Test
  fun `a container is placed before the containers beneath it`() {
    val alignEnd = LayoutFunctionN { measurables, c ->
      val p = measurables[0].measure(SizeConstraints.exact(CHILD, CHILD))
      layout(c.maxWidth, CHILD) { p.place(x = c.maxWidth - CHILD, y = 0) }
    }
    val inner = Layout(listOf(box()), layout = alignEnd)
    val outer = Layout(
        listOf(inner),
        style = Style.width(BOX_W.px).height(BOX_H.px),
        layout = { measurables, _ ->
          val p = measurables[0].measure(SizeConstraints.exact(BOX_W, CHILD))
          layout(BOX_W, BOX_H) { p.place(0, 0) }
        },
    )

    val placedInner = renderAndFindContainer(outer).getChildAt(0)

    assertThat(placedInner.getXForChildAtIndex(0))
        .describedAs("the inner container placed its child against the width the outer gave it")
        .isEqualTo(BOX_W - CHILD)
  }

  /**
   * Both directions matter, so both are checked here.
   *
   * Equal compares equal: the children are built before the container is, so it holds a plain list
   * of components and two containers built the same way match.
   *
   * Unequal compares unequal: [Component.isEquivalentProps] only compares `@Comparable`-annotated
   * fields, and a Kotlin component has none. Inheriting it would compare nothing, so every
   * container would match every other and a changed child would never reach the screen.
   */
  @Test
  fun `containers compare by the children they were given`() {
    val c = lithoTestRule.context

    assertThat(
        Layout(emptyList(), layout = InARow).isEquivalentTo(Layout(emptyList(), layout = InARow)),
    )
        .describedAs("same children, so the subtree is not re-resolved")
        .isTrue()

    assertThat(
        Layout(emptyList(), layout = InARow)
            .isEquivalentTo(Layout(listOf(Column()), layout = InARow)),
    )
        .describedAs("a declared child is a difference, even an empty one")
        .isFalse()

    assertThat(
        Layout(listOf(Column()), layout = InARow)
            .isEquivalentTo(Layout(listOf(text(c, "x")), layout = InARow)),
    )
        .describedAs("and so is a child of a different type")
        .isFalse()

    assertThat(
        Layout(listOf(text(c, "a")), layout = InARow)
            .isEquivalentTo(Layout(listOf(text(c, "a")), layout = InARow)),
    )
        .describedAs(
            "distinct children carrying equal props compare equal. This is the case " +
                "isPureRender relies on: it is what lets shouldUpdate skip re-resolving a " +
                "subtree that was rebuilt but did not change",
        )
        .isTrue()
  }

  /**
   * A null child is refused when the container is built, however it got into the list, so it fails
   * the same way on every path rather than only when resolve is interrupted.
   */
  @Test
  fun `a null child smuggled into the list is refused when the container is built`() {
    @Suppress("UNCHECKED_CAST") val children = listOf(Column(), null) as List<Component>

    val failure = runCatching { Layout(children, layout = InARow) }.exceptionOrNull()

    assertThat(failure)
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessageContaining("child 1")
  }

  /**
   * Two containers holding the same children in a different order are not the same container.
   *
   * Nor are two over the same children that lay them out with different functions, as a named form
   * and a list form written separately do.
   */
  @Test
  fun `children compare in the order they were declared`() {
    val c = lithoTestRule.context
    val one = Column()
    val two = text(c, "b")

    assertThat(
        Layout(listOf(one, two), layout = InARow)
            .isEquivalentTo(Layout(listOf(one, two), layout = InARow)),
    )
        .isTrue()

    assertThat(
        Layout(listOf(one, two), layout = InARow)
            .isEquivalentTo(Layout(listOf(two, one), layout = InARow)),
    )
        .describedAs("order is part of the container, so the children cannot be reordered")
        .isFalse()

    val named =
        Layout(one, two) { first, second, _ ->
          val a = first.measure(SizeConstraints())
          val b = second.measure(SizeConstraints())
          layout(a.width + b.width, maxOf(a.height, b.height)) {
            a.place(0, 0)
            b.place(a.width, 0)
          }
        }
    assertThat(named.isEquivalentTo(Layout(listOf(one, two), layout = InARow)))
        .describedAs("a different layout function over the same children is a different container")
        .isFalse()
  }

  /**
   * A container is equivalent to the last one when its layout function is of the same class and
   * what the function captured is equivalent: the Litho compiler plugin infers the deps of a lambda
   * written at the call site from its captures.
   *
   * So a lambda re-created on every render is the same container while the values it captured stay
   * the same, and a different one when one changes, where comparing the class alone would keep the
   * container laid out against the stale value. A lambda that captures only values the plugin
   * ignores, such as the component context, has no deps, so its class alone decides. A function
   * passed in rather than written at the call site, such as an `object`, is compared with `equals`.
   */
  @Test
  fun `the layout function's class and its captured values decide equivalence`() {
    val child = Column()
    val context = lithoTestRule.context
    fun nonCapturing(): Layout =
        Layout(listOf(child), layout = { _, con -> layout(con.maxWidth, CHILD) {} })
    fun capturing(height: Int): Layout =
        Layout(listOf(child), layout = { _, con -> layout(con.maxWidth, height) {} })
    fun capturingOnlyTheContext(): Layout = Layout(
        listOf(child),
        layout = { _, constraints ->
          val screenWidth = context.androidContext.resources.displayMetrics.widthPixels
          layout(minOf(constraints.maxWidth, screenWidth), CHILD) {}
        },
    )

    assertThat(
        Layout(listOf(child), layout = FixedHeight)
            .isEquivalentTo(Layout(listOf(child), layout = FixedHeight)),
    )
        .describedAs("the same object function over the same children: the same container")
        .isTrue()
    assertThat(nonCapturing().isEquivalentTo(nonCapturing()))
        .describedAs("a lambda capturing nothing: still the same container")
        .isTrue()
    assertThat(capturing(1).isEquivalentTo(capturing(1)))
        .describedAs("a new lambda capturing the same value: still the same container")
        .isTrue()
    assertThat(capturing(1).isEquivalentTo(capturing(2)))
        .describedAs("a captured value changed: a different container, though the class is shared")
        .isFalse()
    assertThat(capturingOnlyTheContext().isEquivalentTo(capturingOnlyTheContext()))
        .describedAs("a new lambda capturing only the ignored context: still the same container")
        .isTrue()
    assertThat(
        Layout(listOf(child), layout = FixedHeight)
            .isEquivalentTo(
                Layout(listOf(child), layout = { _, con -> layout(con.maxWidth, 1) {} }),
            ),
    )
        .describedAs("switching to a different function is a change")
        .isFalse()
  }

  /**
   * Deps passed by hand are compared whenever both sides pass them, even for one function: the
   * function is the same instance in the first assertions here, so only the deps can tell the
   * containers apart. `deps = emptyArray()` says the function depends on nothing, so its class
   * alone decides, whatever the lambda captured. A side that passes nothing keeps the default and
   * has its function compared with `equals` instead, as for a function the plugin could not read.
   */
  @Test
  fun `deps passed by hand decide equivalence for one function`() {
    val child = Column()
    fun explicitlyNone(height: Int): Layout = Layout(
        listOf(child),
        deps = emptyArray(),
        layout = { _, constraints -> layout(constraints.maxWidth, height) {} },
    )

    assertThat(
        Layout(listOf(child), deps = arrayOf(1), layout = FixedHeight)
            .isEquivalentTo(Layout(listOf(child), deps = arrayOf(1), layout = FixedHeight)),
    )
        .describedAs("the same deps by hand: the same container")
        .isTrue()
    assertThat(
        Layout(listOf(child), deps = arrayOf(1), layout = FixedHeight)
            .isEquivalentTo(Layout(listOf(child), deps = arrayOf(2), layout = FixedHeight)),
    )
        .describedAs("deps that differ by hand: a different container, though the function is one")
        .isFalse()
    assertThat(explicitlyNone(1).isEquivalentTo(explicitlyNone(2)))
        .describedAs("no deps by hand: the class alone decides, whatever the lambda captured")
        .isTrue()
    assertThat(
        Layout(listOf(child), deps = arrayOf(1), layout = FixedHeight)
            .isEquivalentTo(Layout(listOf(child), layout = FixedHeight)),
    )
        .describedAs("deps on one side only fall back to the function, which is the same one")
        .isTrue()
  }

  /**
   * How many children a container takes and which shape its layout function has are one choice, so
   * a container that names its children receives them by name instead of indexing a list.
   *
   * The constructor settles the shape at every arity, which is why none of these lambdas needs its
   * parameters typed. Passing a container the shape for a different child count fails to compile,
   * rather than reading past the end of a list during layout.
   */
  @Test
  fun `an arity overload hands the layout function its children by name`() {
    class OneUp : KComponent() {
      override fun ComponentScope.render(): Component = Layout(
          box(),
          layout = { only, constraints ->
            val a = only.measure(SizeConstraints.exact(CHILD, CHILD))
            layout(constraints.maxWidth, CHILD) { a.place(x = STRIDE, y = 0) }
          },
      )
    }

    class TwoUp : KComponent() {
      override fun ComponentScope.render(): Component = Layout(
          box(),
          box(),
          layout = { first, second, constraints ->
            val a = first.measure(SizeConstraints.exact(CHILD, CHILD))
            val b = second.measure(SizeConstraints.exact(CHILD, CHILD))
            layout(constraints.maxWidth, CHILD) {
              a.place(x = 0, y = 0)
              b.place(x = STRIDE, y = 0)
            }
          },
      )
    }

    class ThreeUp : KComponent() {
      override fun ComponentScope.render(): Component = Layout(
          box(),
          box(),
          box(),
          layout = { first, second, third, constraints ->
            val a = first.measure(SizeConstraints.exact(CHILD, CHILD))
            val b = second.measure(SizeConstraints.exact(CHILD, CHILD))
            val c = third.measure(SizeConstraints.exact(CHILD, CHILD))
            layout(constraints.maxWidth, CHILD) {
              a.place(x = 0, y = 0)
              b.place(x = STRIDE, y = 0)
              c.place(x = STRIDE * 2, y = 0)
            }
          },
      )
    }

    assertThat(placedXs(OneUp()))
        .describedAs("placed where the layout function put them, not by flex")
        .containsExactly(STRIDE)
    assertThat(placedXs(TwoUp())).containsExactly(0, STRIDE)
    assertThat(placedXs(ThreeUp())).containsExactly(0, STRIDE, STRIDE * 2)
  }

  private class ThrowsWhileResolving : KComponent() {
    override fun ComponentScope.render(): Component = throw IllegalStateException(RESOLVE_FAILURE)
  }

  /** Produces no node, which no sanctioned component API can do. */
  private class Vanishing : Component() {
    override fun resolve(
        resolveContext: ResolveContext,
        scopedComponentInfo: ScopedComponentInfo,
        parentWidthSpec: Int,
        parentHeightSpec: Int,
    ): ComponentResolveResult = ComponentResolveResult(null, null)

    override fun resolveDeferred(
        calculationContext: CalculationContext,
        componentContext: ComponentContext,
        parentContext: ComponentContext,
    ): ComponentResolveResult = ComponentResolveResult(null, null)
  }

  private fun fixedLeaf(): Component =
      SimpleMountSpecTester(style = Style.width(CHILD.px).height(CHILD.px))

  private fun placedXs(component: Component): List<Int> {
    val result = renderAndFindContainer(component)
    return (0 until result.childCount).map { result.getXForChildAtIndex(it) }
  }

  private fun renderAndFindContainer(component: Component): LithoLayoutResult {
    val testView = lithoTestRule.render(widthPx = VIEWPORT, heightPx = VIEWPORT) { wrap(component) }
    val root = checkNotNull(testView.componentTree.mainThreadLayoutState?.rootLayoutResult)
    return checkNotNull(findContainer(root as LithoLayoutResult)) { "container not found in tree" }
  }

  // A container is never the root host, so tests place it one level down, as real usage does.
  private fun wrap(component: Component): Component = Column { child(component) }

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
    private const val RESOLVE_FAILURE = "thrown while resolving"

    /**
     * Lays children left to right at their own size, which is what flexbox did before a function
     * was required. Measured unconstrained on purpose: handing on the container's own constraints
     * would stretch every child to fill them.
     */
    private val InARow = LayoutFunctionN { measurables, _ ->
      val placeables = measurables.map { it.measure(SizeConstraints()) }
      layout(placeables.sumOf { it.width }, placeables.maxOfOrNull { it.height } ?: 0) {
        var x = 0
        placeables.forEach { placeable ->
          placeable.place(x = x, y = 0)
          x += placeable.width
        }
      }
    }

    /** Declared once, so two containers using it hold the same object and compare equal. */
    private val FixedHeight = LayoutFunctionN { measurables, constraints ->
      val p = measurables[0].measure(SizeConstraints.exact(CHILD, CHILD))
      layout(constraints.maxWidth, CHILD) { p.place(x = 0, y = 0) }
    }
  }
}
