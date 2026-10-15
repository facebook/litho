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

package com.facebook.litho.layout

import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints

/**
 * Positions the children of a container.
 *
 * Yoga can express a row and a column. It cannot express a pager, where each page sits at a stride
 * the container computes; or a split pane, where a divider the user drags decides the sizes; or a
 * collapsing header, where one child's position depends on another's scroll offset. A layout
 * function is how a container says where its children go.
 *
 * Children remain part of the parent's Litho tree. They are not a nested tree, so incremental
 * mount, visibility and state all continue to work across them without the container forwarding
 * anything by hand.
 *
 * Sealed, so the only implementations are the arity shapes below. A container picks one by how many
 * parameters its lambda declares, and the constructor it matches says how many children it may
 * name, so handing a two-child function to a three-child container fails to compile.
 *
 * The intrinsic-size questions all have answers here, each of which runs the layout function and
 * reads the size back. Override one when the container knows its answer more cheaply — a pager is
 * one page wide however many pages it holds — which needs an object rather than a lambda.
 */
@ExperimentalLithoApi
sealed interface LayoutFunction {

  /**
   * Measure the children, then say how big this container is and where each child goes.
   *
   * @param measurables the children, to measure and then place
   * @param constraints what the parent will allow this container to occupy
   * @throws IllegalStateException if it returns without calling [LayoutScope.layout], since the
   *   container would then have no size and its children nowhere to go
   */
  fun LayoutScope.measure(measurables: List<Measurable>, constraints: SizeConstraints)

  /** The widest this container would like to be at [height], ignoring any upper bound. */
  fun IntrinsicLayoutScope.maxIntrinsicWidth(measurables: List<Measurable>, height: Int): Int =
      measureForIntrinsic(measurables, atMostConstraints(SizeConstraints.Infinity, height)).width

  /**
   * The narrowest this container can be at [height] without its content breaking out of it.
   *
   * This is what a container contributes to the CSS Flexbox §4.5 automatic minimum, so a `Row` that
   * squeezes it cannot take it below this. Nothing here reaches it: Litho asks that question
   * through a second measure function, which the diffs above register.
   */
  fun IntrinsicLayoutScope.minIntrinsicWidth(measurables: List<Measurable>, height: Int): Int =
      measureForIntrinsic(measurables, atMostConstraints(0, height)).width

  /** The tallest this container would like to be at [width], ignoring any upper bound. */
  fun IntrinsicLayoutScope.maxIntrinsicHeight(measurables: List<Measurable>, width: Int): Int =
      measureForIntrinsic(measurables, atMostConstraints(width, SizeConstraints.Infinity)).height

  /** The shortest this container can be at [width]. The other axis of [minIntrinsicWidth]. */
  fun IntrinsicLayoutScope.minIntrinsicHeight(measurables: List<Measurable>, width: Int): Int =
      measureForIntrinsic(measurables, atMostConstraints(width, 0)).height
}

/**
 * A layout function for a container whose child count is not known until runtime.
 *
 * ```kotlin
 * Layout(pages.map { Page(it).key(it.id) }) { measurables, c -> … }
 * ```
 */
@ExperimentalLithoApi
fun interface LayoutFunctionN : LayoutFunction {
  override fun LayoutScope.measure(measurables: List<Measurable>, constraints: SizeConstraints)
}

/**
 * A layout function for a container with exactly one child, which it receives by name.
 *
 * ```kotlin
 * Layout(icon) { child, c ->
 *   val placeable = child.measure(c)
 *   layout(c.maxWidth, placeable.height) {
 *     placeable.place(x = (c.maxWidth - placeable.width) / 2, y = 0)
 *   }
 * }
 * ```
 *
 * Naming the children beats indexing a list for them: `measurables[0]` is a lookup that can be
 * wrong, `child` cannot be.
 */
@ExperimentalLithoApi
fun interface LayoutFunction1 : LayoutFunction {

  fun LayoutScope.measureChildren(child: Measurable, constraints: SizeConstraints)

  override fun LayoutScope.measure(measurables: List<Measurable>, constraints: SizeConstraints) {
    measurables.requireArity(1)
    measureChildren(measurables[0], constraints)
  }
}

/** A layout function for a container with exactly two children, which it receives by name. */
@ExperimentalLithoApi
fun interface LayoutFunction2 : LayoutFunction {

  fun LayoutScope.measureChildren(
      child1: Measurable,
      child2: Measurable,
      constraints: SizeConstraints,
  )

  override fun LayoutScope.measure(measurables: List<Measurable>, constraints: SizeConstraints) {
    measurables.requireArity(2)
    measureChildren(measurables[0], measurables[1], constraints)
  }
}

/** A layout function for a container with exactly three children, which it receives by name. */
@ExperimentalLithoApi
fun interface LayoutFunction3 : LayoutFunction {

  fun LayoutScope.measureChildren(
      child1: Measurable,
      child2: Measurable,
      child3: Measurable,
      constraints: SizeConstraints,
  )

  override fun LayoutScope.measure(measurables: List<Measurable>, constraints: SizeConstraints) {
    measurables.requireArity(3)
    measureChildren(measurables[0], measurables[1], measurables[2], constraints)
  }
}

/**
 * Refuses a child list that does not match the arity the layout function names.
 *
 * The `Layout` constructors pair a container's child count with the one shape that fits it, and a
 * child rendering to nothing still holds its slot, so a well-formed call site cannot get here. It
 * takes a `Component` whose `resolve` produces no node at all — and naming that beats the
 * `IndexOutOfBoundsException` reading past the end would raise from inside the layout pass.
 */
private fun List<Measurable>.requireArity(expected: Int) {
  check(size == expected) {
    "A layout function naming $expected children was handed $size. The constructors pair the two, " +
        "so one of the declared children resolved to no node at all."
  }
}

/** The answer either maximum gives when the container did not supply a cheaper one. */
internal fun IntrinsicLayoutScope.measuredIntrinsicSize(
    measurables: List<Measurable>,
    maxWidth: Int,
    maxHeight: Int,
): IntSize = measureForIntrinsic(measurables, atMostConstraints(maxWidth, maxHeight))

/**
 * A child that has been resolved but not yet laid out.
 *
 * Created by the framework during layout, where the context needed to lay a child out exists. An
 * instance is only meaningful inside the [LayoutFunction.measure] call that received it.
 */
@ExperimentalLithoApi
interface Measurable {

  /**
   * Lay this child out under [constraints] and report the size it took.
   *
   * **May be called more than once per pass.** CSS Flexbox §9.7 resolves flexible lengths by
   * measuring a child at its flex basis, distributing free space, and measuring again at the
   * resolved size, so a flexbox written on this API necessarily measures twice.
   *
   * Each call lays the child out under its constraints, and the [Placeable] it returns carries that
   * layout: whichever placeable the container places is what mounts, so measuring a child once to
   * probe it and again to place it costs the probe and nothing more.
   *
   * The child's own size props — width, min and max, aspect ratio, margins, padding and border —
   * apply within [constraints], and the placeable reports the size it took, kept within them.
   */
  fun measure(constraints: SizeConstraints): Placeable
}

/**
 * A child that has been laid out and knows its size, but not yet where it sits.
 *
 * Only the framework can make one — the constructor is internal — which is what lets
 * [PlaceableScope.place] trust [childIndex]. `place` is an extension on this type rather than a
 * function taking an index, so the index travels with the placeable and cannot be fabricated to
 * place a child at the wrong slot.
 */
@ExperimentalLithoApi
class Placeable
internal constructor(
    /**
     * The width this child occupies, margins included, within the constraints it was measured at.
     */
    val width: Int,
    /**
     * The height this child occupies, margins included, within the constraints it was measured at.
     */
    val height: Int,
    /**
     * The declaration index this child came from. Framework-only: it is how `place` finds a slot.
     */
    internal val childIndex: Int,
    /**
     * The width this child asked for, margins included, before it was kept within its constraints.
     * Differs from [width] only when the child does not fit them: it is then centred in [width].
     */
    val measuredWidth: Int = width,
    /** The height this child asked for. The other axis of [measuredWidth]. */
    val measuredHeight: Int = height,
    /** The layout this measurement made, which is what mounts if it is placed. */
    internal val result: LithoLayoutResult? = null,
    /** Where the child's border box sits inside [measuredWidth]: its left and top margins. */
    internal val marginLeft: Int = 0,
    internal val marginTop: Int = 0,
    /** The container whose layout function was handed this child. */
    internal val owner: Any? = null,
    /** The constraints this child was measured under. */
    internal val constraints: SizeConstraints,
) {
  /** This answer, handed to [container] for the same child: the same layout, placeable there. */
  internal fun forContainer(container: Any): Placeable =
      if (owner === container) this
      else
          Placeable(
              width = width,
              height = height,
              childIndex = childIndex,
              measuredWidth = measuredWidth,
              measuredHeight = measuredHeight,
              result = result,
              marginLeft = marginLeft,
              marginTop = marginTop,
              owner = container,
              constraints = constraints,
          )

  /** How far this child is shifted to centre [measuredWidth] in [width]. */
  internal val offsetX: Int
    get() = (width - measuredWidth) / 2

  /** How far this child is shifted to centre [measuredHeight] in [height]. */
  internal val offsetY: Int
    get() = (height - measuredHeight) / 2
}

/**
 * What a layout function can reach whether it is measuring or answering an intrinsic question.
 *
 * [LayoutScope] adds the verb that places children; [IntrinsicLayoutScope] adds the one that asks
 * what a container would do without placing anything.
 */
@ExperimentalLithoApi sealed class MeasuringScope constructor()

/**
 * Receiver for [LayoutFunction.measure], and where a pass leaves what it works out.
 *
 * A layout function reports through the scope rather than by returning a value, so everything a
 * pass produces arrives by one route and a later addition needs no new return type.
 */
@ExperimentalLithoApi
class LayoutScope internal constructor() : MeasuringScope() {

  internal var width: Int = 0
    private set

  internal var height: Int = 0
    private set

  /**
   * The placement block, still unevaluated, or null until [layout] has been called.
   *
   * A block rather than coordinates because a position only means something against a size that has
   * settled, and Yoga may still override [width] — a stretching parent routinely does. Coordinates
   * computed during measurement would be wrong in exactly that case. The framework runs the block
   * once the size is final.
   */
  internal var placement: (PlaceableScope.() -> Unit)? = null
    private set

  /**
   * Declare this container's size and how its children are positioned.
   *
   * Positioning happens inside the block and nowhere else: `place` exists only on [PlaceableScope],
   * so placing a child while still measuring is a compile error rather than a child placed against
   * a size that has not settled.
   *
   * @param width the width this container asks for; the parent may override it
   * @param height the height this container asks for; the parent may override it
   * @param placement runs later, once the final size is known, which may differ from [width] by
   *   [height]. Capture whatever it needs — [Placeable]s, sizes, anything computed while measuring
   *   — and it will see those values.
   * @throws IllegalStateException if called twice, since one of the two sizes would be discarded
   * @throws IllegalArgumentException if [width] or [height] is negative
   */
  fun layout(width: Int, height: Int, placement: PlaceableScope.() -> Unit) {
    check(this.placement == null) {
      "layout() has already been called on this container. Calling it twice means one of the two " +
          "sizes, and one of the two placement blocks, is silently discarded."
    }
    require(width >= 0 && height >= 0) {
      "A container cannot be ${width}x$height. Both dimensions must be at least 0."
    }
    this.width = width
    this.height = height
    this.placement = placement
  }
}

/**
 * Receiver for a [LayoutFunction]'s intrinsic-size questions.
 *
 * Separate from [LayoutScope] so an intrinsic query cannot place anything: it asks what a container
 * *would* do. A [LayoutScope] holds its placement block unevaluated and only the framework runs it,
 * so asking for an intrinsic size cannot place a child or fire a side effect the block holds.
 */
@ExperimentalLithoApi
class IntrinsicLayoutScope
internal constructor(
    private val layoutFunction: LayoutFunction,
) : MeasuringScope() {

  /**
   * Runs the layout function under [constraints] and reports the size it asked for.
   *
   * Only the size is read; the placements are discarded unevaluated. An honest answer costs a
   * measure pass, which is what an override exists to avoid.
   *
   * @throws IllegalStateException if the layout function returns without calling
   *   [LayoutScope.layout], since there is then no size to report
   */
  fun measureForIntrinsic(
      measurables: List<Measurable>,
      constraints: SizeConstraints,
  ): IntSize {
    val scope = LayoutScope()
    with(layoutFunction) { scope.measure(measurables, constraints) }
    // Without this the omission is answered rather than reported: the scope still holds its
    // initial 0x0, and an intrinsic of zero is a size Yoga will act on.
    checkNotNull(scope.placement) { NO_SIZE_DECLARED }
    return IntSize(scope.width, scope.height)
  }
}

/**
 * Receiver for the placement pass.
 *
 * A child that is never placed occupies no space and will not be mounted. Declining to place is
 * therefore how a container drops a child: measuring one to see whether it fits and then leaving it
 * out costs its subtree nothing.
 */
@ExperimentalLithoApi
class PlaceableScope
internal constructor(
    childCount: Int,
    /** The width the container declared, which a right-to-left placement mirrors against. */
    private val declaredWidth: Int = 0,
    /** The direction the container is laid out in. */
    val layoutDirection: LayoutDirection = LayoutDirection.LTR,
    /** The container placing, whose children's placeables are the only ones it may place. */
    private val owner: Any? = null,
) {

  internal val placements: IntArray = IntArray(childCount * 2)

  /** Which children were placed. What is false here is left out of the mounted output entirely. */
  internal val placed: BooleanArray = BooleanArray(childCount)

  /** The placeable placed for each child, whose layout is the one that mounts. */
  internal val placedWith: Array<Placeable?> = arrayOfNulls(childCount)

  /**
   * Position this child at [start] from this container's start edge and [top] from its top, so a
   * right-to-left container mirrors it: the left edge in left-to-right, the right edge in
   * right-to-left, against the width this container declared.
   */
  fun Placeable.placeRelative(start: Int = 0, top: Int = 0) {
    val x = if (layoutDirection.isRTL) declaredWidth - start - width else start
    place(x, top)
  }

  /**
   * Position this child at [x], [y] relative to this container's top-left.
   *
   * Paint order is declaration order: two children that overlap are drawn in the order the
   * container declared them, not the order they were placed in here.
   *
   * @throws IllegalStateException if this child has already been placed, since one of the two
   *   positions would be discarded
   * @throws IllegalArgumentException if this placeable's child index is out of range for this
   *   container. That is how a placeable belonging to another container is caught, though only when
   *   its index happens to fall outside this one's.
   */
  fun Placeable.place(x: Int = 0, y: Int = 0) {
    val index = childIndex
    require(this.owner === this@PlaceableScope.owner) {
      "Cannot place a child of another container. A placeable belongs to the container whose " +
          "layout function measured it."
    }
    require(index in placed.indices) {
      "Cannot place child $index: this container has ${placed.size} children"
    }
    check(!placed[index]) {
      "Child $index has already been placed. Placing twice means one of the two positions is " +
          "silently discarded."
    }
    placed[index] = true
    placedWith[index] = this
    placements[index * 2] = x
    placements[index * 2 + 1] = y
  }
}

private fun atMostConstraints(maxWidth: Int, maxHeight: Int): SizeConstraints =
    SizeConstraints(maxWidth = maxWidth, maxHeight = maxHeight)

/**
 * What a layout function that never called [LayoutScope.layout] is told.
 *
 * Shared by every place a layout function runs, so a container hears the same thing whether it was
 * asked for an intrinsic size, measured by Yoga, or laid out because its size was already definite.
 */
internal const val NO_SIZE_DECLARED: String =
    "A layout function must finish by calling layout(width, height) { … }. This one returned " +
        "without declaring a size, so the container has none and its children have nowhere to go."
