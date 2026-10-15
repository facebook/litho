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

import com.facebook.rendercore.SizeConstraints
import kotlin.random.Random

/**
 * Generated container children for the child box's tests: a child's declared box props as data, so
 * one case can be built, mirrored or rewritten, and the constraints to try each under.
 */
internal object ChildBoxCases {

  /** One declared edge of a box prop, in pixels or, with [isPercent], a percentage. */
  data class EdgeDecl(val edge: BoxEdge, val value: Float, val isPercent: Boolean = false)

  data class Declared(
      val width: BoxLength = BoxLength.Unset,
      val height: BoxLength = BoxLength.Unset,
      val minWidth: BoxLength = BoxLength.Unset,
      val maxWidth: BoxLength = BoxLength.Unset,
      val minHeight: BoxLength = BoxLength.Unset,
      val maxHeight: BoxLength = BoxLength.Unset,
      val aspectRatio: Float = Float.NaN,
      val margins: List<EdgeDecl> = emptyList(),
      val paddings: List<EdgeDecl> = emptyList(),
      val borders: List<EdgeDecl> = emptyList(),
  ) {
    fun build(): BoxLayoutProps =
        BoxLayoutProps().also { props ->
          props.width = width
          props.height = height
          props.minWidth = minWidth
          props.maxWidth = maxWidth
          props.minHeight = minHeight
          props.maxHeight = maxHeight
          props.aspectRatio = aspectRatio
          margins.forEach { props.margin[it.edge] = it.length }
          paddings.forEach { props.padding[it.edge] = it.length }
          borders.forEach { props.border[it.edge] = it.length }
        }

    /** The same child with every LEFT and RIGHT declaration swapped; logical edges stay. */
    fun leftRightSwapped(): Declared = copy(
        margins = margins.map(::swapLeftRight),
        paddings = paddings.map(::swapLeftRight),
        borders = borders.map(::swapLeftRight),
    )

    /**
     * Variants that each declare one more prop at its default, where this child leaves it unset.
     */
    fun withDefaults(): List<Declared> = buildList {
      // Not an own min of 0: an own min replaces the room's minimum, so `minWidth(0)` lets a child
      // be narrower than an exact room, which leaving it unset does not.
      if (aspectRatio.isNaN()) add(copy(aspectRatio = 0f))
      val zero = EdgeDecl(BoxEdge.ALL, 0f)
      if (margins.none { it.edge == BoxEdge.ALL }) add(copy(margins = margins + zero))
      if (paddings.none { it.edge == BoxEdge.ALL }) add(copy(paddings = paddings + zero))
      if (borders.none { it.edge == BoxEdge.ALL }) add(copy(borders = borders + zero))
    }

    /**
     * Every percentage as the points it comes to under a [width] by [height] bound. Edges take the
     * width on both axes, as in CSS; heights take the height.
     */
    fun percentsAsPoints(width: Int, height: Int): Declared = copy(
        width = this.width.asPoints(width),
        height = this.height.asPoints(height),
        minWidth = minWidth.asPoints(width),
        maxWidth = maxWidth.asPoints(width),
        minHeight = minHeight.asPoints(height),
        maxHeight = maxHeight.asPoints(height),
        margins = margins.map { it.asPoints(width) },
        paddings = paddings.map { it.asPoints(width) },
    )

    /** Whether a size or a padding is a percentage, which resolves against the constraints. */
    val hasPercentSizeOrPadding: Boolean
      get() =
          listOf(width, height, minWidth, maxWidth, minHeight, maxHeight).any {
            it is BoxLength.Percent
          } || paddings.any { it.isPercent }
  }

  /** Constraints of every shape a container asks under: exact, bounded, ranged, unbounded. */
  val constraints: List<SizeConstraints> = buildList {
    for (w in listOf(0, 40, 200, 333)) {
      for (h in listOf(0, 40, 200)) {
        add(SizeConstraints.exact(w, h))
        add(SizeConstraints(0, w, 0, h))
        add(SizeConstraints(w / 2, w, h / 3, h))
      }
    }
    add(SizeConstraints())
    add(SizeConstraints(0, 100, 0, SizeConstraints.Infinity))
    add(SizeConstraints(0, SizeConstraints.Infinity, 0, 90))
  }

  /**
   * [count] children with random box props: sizes in pixels, percent or unset; an aspect ratio or
   * none; up to three declarations per edge prop on any [BoxEdge], in pixels or percent, some
   * negative. The same [seed] gives the same children.
   */
  fun generated(count: Int, seed: Int = 20260928): List<Declared> {
    val random = Random(seed)
    fun <T> pick(options: List<T>): T = options[random.nextInt(options.size)]
    val sizes = listOf(
        BoxLength.Unset,
        BoxLength.Unset,
        points(0f),
        points(60f),
        points(500f),
        percent(50f),
        percent(150f),
    )
    fun edges(negative: Float, allowPercent: Boolean): List<EdgeDecl> =
        List(random.nextInt(4)) {
          val asPercent = allowPercent && random.nextBoolean()
          EdgeDecl(
              edge = pick(BoxEdge.values().toList()),
              value =
                  if (asPercent) pick(listOf(5f, 10f, 12.5f, 25f))
                  else pick(listOf(0f, 5f, 12f, negative)),
              isPercent = asPercent,
          )
        }
    return List(count) {
      Declared(
          width = pick(sizes),
          height = pick(sizes),
          minWidth = pick(sizes),
          maxWidth = pick(sizes),
          minHeight = pick(sizes),
          maxHeight = pick(sizes),
          aspectRatio = pick(listOf(Float.NaN, Float.NaN, 0.5f, 3f)),
          margins = edges(negative = -8f, allowPercent = true),
          paddings = edges(negative = -4f, allowPercent = true),
          borders = edges(negative = -2f, allowPercent = false),
      )
    }
  }

  /** Everything a [ChildBox] says, with left and right swapped when [mirrored]. */
  fun summary(box: ChildBox, mirrored: Boolean = false): String =
      "margins ${text(box.margins, mirrored)} padding ${text(box.padding, mirrored)} " +
          "border ${text(box.border, mirrored)} constraints ${box.borderBoxConstraints}"

  fun text(edges: IntEdges, mirrored: Boolean = false): String =
      if (mirrored) "(${edges.right}, ${edges.top}, ${edges.left}, ${edges.bottom})"
      else "(${edges.left}, ${edges.top}, ${edges.right}, ${edges.bottom})"

  fun points(value: Float): BoxLength = BoxLength.Px(value)

  fun percent(value: Float): BoxLength = BoxLength.Percent(value)

  /** The declaration as the [BoxLength] it sets. */
  val EdgeDecl.length: BoxLength
    get() = if (isPercent) BoxLength.Percent(value) else BoxLength.Px(value)

  private fun swapLeftRight(declared: EdgeDecl): EdgeDecl =
      when (declared.edge) {
        BoxEdge.LEFT -> declared.copy(edge = BoxEdge.RIGHT)
        BoxEdge.RIGHT -> declared.copy(edge = BoxEdge.LEFT)
        else -> declared
      }

  private fun BoxLength.asPoints(base: Int): BoxLength =
      if (this is BoxLength.Percent) points(value * base / 100f) else this

  private fun EdgeDecl.asPoints(base: Int): EdgeDecl =
      if (isPercent) copy(value = value * base / 100f, isPercent = false) else this
}
