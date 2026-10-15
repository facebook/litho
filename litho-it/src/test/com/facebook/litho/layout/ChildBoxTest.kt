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

import com.facebook.litho.layout.ChildBoxCases.summary
import com.facebook.litho.layout.ChildBoxCases.text
import com.facebook.rendercore.SizeConstraints
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private const val INF = SizeConstraints.Infinity

private fun px(value: Int): BoxLength = BoxLength.Px(value.toFloat())

private fun pct(value: Float): BoxLength = BoxLength.Percent(value)

private fun atMost(width: Int, height: Int = INF) = SizeConstraints(0, width, 0, height)

private fun exact(width: Int, height: Int) = SizeConstraints.exact(width, height)

private fun props(block: BoxLayoutProps.() -> Unit = {}) = BoxLayoutProps().apply(block)

private fun resolve(constraints: SizeConstraints, props: BoxLayoutProps, isRtl: Boolean = false) =
    resolveChildBox(constraints, props, isRtl)

/** The rules a container applies to a child's own box props, within the constraints it asks. */
class ChildBoxTest {

  @Test
  fun `the child's own width wins even over exact constraints`() {
    val b = resolve(exact(200, 50), props { width = px(80) })

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(80)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(80)
  }

  @Test
  fun `an own width wins over a bound`() {
    assertThat(resolve(atMost(300), props { width = px(80) }).borderBoxConstraints.maxWidth)
        .isEqualTo(80)
    val over = resolve(atMost(60), props { width = px(80) })
    assertThat(over.borderBoxConstraints.maxWidth).isEqualTo(80)
  }

  @Test
  fun `a percent width is taken of the constraints, and ignored without a bound`() {
    assertThat(resolve(atMost(300), props { width = pct(50f) }).borderBoxConstraints.maxWidth)
        .isEqualTo(150)
    assertThat(
        resolve(SizeConstraints(), props { width = pct(50f) }).borderBoxConstraints.hasBoundedWidth,
    )
        .isFalse()
  }

  @Test
  fun `margins come out of the constraints and are reported`() {
    val b = resolve(exact(200, 100), props { margin[BoxEdge.ALL] = BoxLength.Px(10f) })

    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(180)
    assertThat(b.borderBoxConstraints.maxHeight).isEqualTo(80)
    assertThat(b.margins.horizontal).isEqualTo(20)
  }

  @Test
  fun `a percent margin is taken of the constraints' width on both axes`() {
    val b = resolve(atMost(200, 1000), props { margin[BoxEdge.VERTICAL] = BoxLength.Percent(10f) })

    assertThat(b.margins.top).isEqualTo(20)
    assertThat(b.margins.bottom).isEqualTo(20)
  }

  @Test
  fun `a percent margin is 0 when the width is unbounded`() {
    val b = resolve(SizeConstraints(), props { margin[BoxEdge.ALL] = BoxLength.Percent(10f) })

    assertThat(b.margins).isSameAs(IntEdges.None)
  }

  @Test
  fun `a negative margin makes the border box's constraints larger`() {
    val b = resolve(exact(100, 100), props { margin[BoxEdge.LEFT] = BoxLength.Px(-10f) })

    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(110)
  }

  @Test
  fun `start and end resolve by direction, as Yoga does`() {
    val declared = props {
      margin[BoxEdge.START] = BoxLength.Px(4f)
      margin[BoxEdge.LEFT] = BoxLength.Px(9f)
    }

    assertThat(resolve(atMost(100), declared).margins.left).isEqualTo(4)
    assertThat(resolve(atMost(100), declared, isRtl = true).margins.left).isEqualTo(9)
    assertThat(resolve(atMost(100), declared, isRtl = true).margins.right).isEqualTo(4)
  }

  @Test
  fun `an own max narrows a bound and an own min raises it`() {
    val b = resolve(
        atMost(300),
        props {
          minWidth = px(40)
          maxWidth = px(120)
        },
    )

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(40)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(120)
  }

  @Test
  fun `an own min wins over the constraints`() {
    val b = resolve(atMost(100), props { minWidth = px(150) })

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(150)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(150)
  }

  @Test
  fun `an own max beats an own min, as in Compose`() {
    val b = resolve(
        atMost(300),
        props {
          minWidth = px(80)
          maxWidth = px(50)
        },
    )

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(50)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(50)
  }

  @Test
  fun `an own min and an own max replace the bound of exact constraints, the max over the min`() {
    val both = resolve(
        exact(200, 50),
        props {
          minWidth = px(250)
          maxWidth = px(100)
        },
    )
    assertThat(both.borderBoxConstraints.minWidth).isEqualTo(100)
    assertThat(both.borderBoxConstraints.maxWidth).isEqualTo(100)

    // The own max narrows even an exact question, as in a Column that stretches its children.
    val max = resolve(exact(200, 50), props { maxWidth = px(100) })
    assertThat(max.borderBoxConstraints.minWidth).isEqualTo(100)
    assertThat(max.borderBoxConstraints.maxWidth).isEqualTo(100)

    // An own min replaces the exact minimum, as `requiredWidthIn` does: the content chooses.
    val min = resolve(exact(200, 50), props { minWidth = px(120) })
    assertThat(min.borderBoxConstraints.minWidth).isEqualTo(120)
    assertThat(min.borderBoxConstraints.maxWidth).isEqualTo(200)

    // So declaring a min of 0 is not the same as leaving it unset.
    val zero = resolve(exact(200, 50), props { minWidth = px(0) })
    assertThat(zero.borderBoxConstraints.minWidth).isEqualTo(0)
    assertThat(zero.borderBoxConstraints.maxWidth).isEqualTo(200)
  }

  @Test
  fun `a range far past any screen stays one constraints can encode`() {
    // A range with a minimum above 0 encodes a minimum up to 8190 and a maximum up to 262142.
    val bigMin = resolve(exact(20_000, 50), props { minWidth = px(9_000) })
    assertThat(bigMin.borderBoxConstraints.minWidth)
        .describedAs("fixed at its min")
        .isEqualTo(9_000)
    assertThat(bigMin.borderBoxConstraints.maxWidth).isEqualTo(9_000)

    val bigMax = resolve(atMost(300_000), props { minWidth = px(100) })
    assertThat(bigMax.borderBoxConstraints.minWidth).isEqualTo(100)
    assertThat(bigMax.borderBoxConstraints.maxWidth).describedAs("cut to 262142").isEqualTo(262_142)
  }

  @Test
  fun `a percentage size fills as fillMaxWidth does`() {
    // Within an exact room it fills the room, as `fillMaxWidth(0.5f)` does.
    val exactRoom = resolve(exact(200, 50), props { width = pct(50f) })
    assertThat(exactRoom.borderBoxConstraints.maxWidth).isEqualTo(200)
    assertThat(exactRoom.borderBoxConstraints.minWidth).isEqualTo(200)
    // Above 100% it is all of the room.
    assertThat(resolve(atMost(300), props { width = pct(150f) }).borderBoxConstraints.maxWidth)
        .isEqualTo(300)
    // Taken of the room before margins, then kept inside them.
    val margined = resolve(
        atMost(300),
        props {
          width = pct(100f)
          margin[BoxEdge.HORIZONTAL] = BoxLength.Px(10f)
        },
    )
    assertThat(margined.borderBoxConstraints.maxWidth).isEqualTo(280)
  }

  @Test
  fun `an aspect ratio derives the open axis from the fixed one`() {
    val b = resolve(
        SizeConstraints(0, 200, 0, 1000),
        props {
          width = px(120)
          aspectRatio = 2f
        },
    )

    assertThat(b.borderBoxConstraints.maxHeight).isEqualTo(60)
    assertThat(b.borderBoxConstraints.minHeight).isEqualTo(60)
  }

  @Test
  fun `a derived axis is limited only by its own min and max`() {
    val b = resolve(
        SizeConstraints(0, 200, 0, 40),
        props {
          width = px(120)
          aspectRatio = 2f
        },
    )
    assertThat(b.borderBoxConstraints.maxHeight).describedAs("past the 40 bound").isEqualTo(60)

    val capped = resolve(
        SizeConstraints(0, 200, 0, 40),
        props {
          width = px(120)
          maxHeight = px(50)
          aspectRatio = 2f
        },
    )
    assertThat(capped.borderBoxConstraints.maxHeight).isEqualTo(50)
  }

  @Test
  fun `a percent padding is taken of the constraints' width`() {
    val b = resolve(atMost(200), props { padding[BoxEdge.ALL] = BoxLength.Percent(5f) })

    assertThat(b.padding.left).isEqualTo(10)
    assertThat(b.padding.top).isEqualTo(10)
    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(20)
  }

  @Test
  fun `padding that does not fit answers the smallest box`() {
    val b = resolve(exact(10, 10), props { padding[BoxEdge.ALL] = BoxLength.Px(8f) })

    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(16)
  }

  @Test
  fun `margins that do not fit leave the border box no room`() {
    val b = resolve(exact(10, 10), props { margin[BoxEdge.LEFT] = BoxLength.Px(20f) })

    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(0)
  }

  @Test
  fun `a child with no size of its own stays inside the constraints when its edges fit`() {
    val constraintsToTry = buildList {
      for (w in listOf(0, 40, 200)) {
        for (h in listOf(0, 40, 200)) {
          add(exact(w, h))
          add(atMost(w, h))
          add(SizeConstraints(w / 2, w, 0, h))
        }
      }
      add(SizeConstraints())
      add(atMost(100))
    }
    val edges = listOf(0f to false, 12f to false, 10f to true)
    var checked = 0
    for (q in constraintsToTry) {
      for ((edge, isPercent) in edges) {
        val length = if (isPercent) BoxLength.Percent(edge) else BoxLength.Px(edge)
        val b = resolve(
            q,
            props {
              margin[BoxEdge.ALL] = length
              padding[BoxEdge.ALL] = length
            },
        )
        val edgesWidth = b.margins.horizontal + b.padding.horizontal + b.border.horizontal
        val edgesHeight = b.margins.vertical + b.padding.vertical + b.border.vertical
        if (
            (q.hasBoundedWidth && edgesWidth > q.maxWidth) ||
                (q.hasBoundedHeight && edgesHeight > q.maxHeight)
        ) {
          continue
        }
        checked++
        assertThat(b.borderBoxConstraints.minWidth + b.margins.horizontal)
            .describedAs("min width in $q")
            .isGreaterThanOrEqualTo(q.minWidth)
        assertThat(b.borderBoxConstraints.minHeight + b.margins.vertical)
            .isGreaterThanOrEqualTo(q.minHeight)
        if (q.hasBoundedWidth) {
          assertThat(b.borderBoxConstraints.maxWidth + b.margins.horizontal)
              .describedAs("max width in $q")
              .isLessThanOrEqualTo(q.maxWidth)
        }
        if (q.hasBoundedHeight) {
          assertThat(b.borderBoxConstraints.maxHeight + b.margins.vertical)
              .describedAs("max height in $q")
              .isLessThanOrEqualTo(q.maxHeight)
        }
        assertThat(b.borderBoxConstraints.minWidth)
            .isLessThanOrEqualTo(b.borderBoxConstraints.maxWidth)
        assertThat(b.borderBoxConstraints.minHeight)
            .isLessThanOrEqualTo(b.borderBoxConstraints.maxHeight)
      }
    }
    assertThat(checked).isGreaterThan(40)
  }

  @Test
  fun `an own height fixes the height, and a percentage height fills its share`() {
    val fixed = resolve(SizeConstraints(0, 300, 0, 300), props { height = px(80) })
    assertThat(fixed.borderBoxConstraints.minHeight).isEqualTo(80)
    assertThat(fixed.borderBoxConstraints.maxHeight).isEqualTo(80)

    val share = resolve(SizeConstraints(0, 300, 0, 200), props { height = pct(50f) })
    assertThat(share.borderBoxConstraints.minHeight).isEqualTo(100)
    assertThat(share.borderBoxConstraints.maxHeight).isEqualTo(100)
  }

  @Test
  fun `an own min and an own max replace the bounds of the height too`() {
    val both = resolve(
        SizeConstraints(0, 300, 0, 300),
        props {
          minHeight = px(40)
          maxHeight = px(120)
        },
    )
    assertThat(both.borderBoxConstraints.minHeight).isEqualTo(40)
    assertThat(both.borderBoxConstraints.maxHeight).isEqualTo(120)

    val max = resolve(exact(50, 200), props { maxHeight = px(100) })
    assertThat(max.borderBoxConstraints.minHeight).isEqualTo(100)
    assertThat(max.borderBoxConstraints.maxHeight).isEqualTo(100)
  }

  @Test
  fun `an own max only narrows the room, as maxWidth does in a Column`() {
    val above = resolve(atMost(250), props { maxWidth = px(280) })
    assertThat(above.borderBoxConstraints.maxWidth).isEqualTo(250)

    val aboveExact = resolve(exact(200, 50), props { maxWidth = px(280) })
    assertThat(aboveExact.borderBoxConstraints.minWidth).isEqualTo(200)
    assertThat(aboveExact.borderBoxConstraints.maxWidth).isEqualTo(200)

    val margined = resolve(
        atMost(300),
        props {
          maxWidth = pct(100f)
          margin[BoxEdge.HORIZONTAL] = BoxLength.Px(20f)
        },
    )
    assertThat(margined.borderBoxConstraints.maxWidth)
        .describedAs("inside the margins")
        .isEqualTo(260)
  }

  @Test
  fun `an own size stays within its own min and max, the max over the min`() {
    fun box(block: BoxLayoutProps.() -> Unit) =
        resolve(atMost(300), props(block)).borderBoxConstraints

    assertThat(
        box {
          width = px(200)
          maxWidth = px(120)
        }
            .maxWidth,
    )
        .isEqualTo(120)
    assertThat(
        box {
          width = px(50)
          minWidth = px(80)
        }
            .minWidth,
    )
        .isEqualTo(80)
    val conflicting = box {
      width = px(100)
      minWidth = px(80)
      maxWidth = px(50)
    }
    assertThat(conflicting.minWidth).isEqualTo(50)
    assertThat(conflicting.maxWidth).isEqualTo(50)
  }

  @Test
  fun `an aspect ratio derives the width from a fixed height`() {
    val b = resolve(
        SizeConstraints(0, 1000, 0, 200),
        props {
          height = px(60)
          aspectRatio = 2f
        },
    )

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(120)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(120)
  }

  @Test
  fun `a derived axis is raised to its own min`() {
    val b = resolve(
        SizeConstraints(0, 1000, 0, 1000),
        props {
          width = px(120)
          aspectRatio = 2f
          minHeight = px(80)
        },
    )

    assertThat(b.borderBoxConstraints.minHeight).isEqualTo(80)
    assertThat(b.borderBoxConstraints.maxHeight).isEqualTo(80)
  }

  @Test
  fun `an aspect ratio counts only with one axis fixed, and only when finite`() {
    val bothFixed = resolve(
        SizeConstraints(0, 1000, 0, 1000),
        props {
          width = px(120)
          height = px(30)
          aspectRatio = 2f
        },
    )
    assertThat(bothFixed.borderBoxConstraints.maxHeight).isEqualTo(30)

    val infinite =
        resolve(SizeConstraints.fixedWidth(200), props { aspectRatio = Float.POSITIVE_INFINITY })
    assertThat(infinite.borderBoxConstraints.minHeight).isEqualTo(0)
    assertThat(infinite.borderBoxConstraints.hasBoundedHeight).isFalse()
    val fromHeight = resolve(
        atMost(300),
        props {
          height = px(50)
          aspectRatio = Float.POSITIVE_INFINITY
        },
    )
    assertThat(fromHeight.borderBoxConstraints.minWidth).isEqualTo(0)
    assertThat(fromHeight.borderBoxConstraints.maxWidth).isEqualTo(300)
  }

  @Test
  fun `a percentage min, max or size has no effect when its axis is unbounded`() {
    val max = resolve(SizeConstraints(), props { maxWidth = pct(50f) })
    assertThat(max.borderBoxConstraints.hasBoundedWidth).isFalse()

    val height = resolve(atMost(300), props { height = pct(50f) })
    assertThat(height.borderBoxConstraints.minHeight).isEqualTo(0)
    assertThat(height.borderBoxConstraints.hasBoundedHeight).isFalse()
  }

  @Test
  fun `a percentage size is at most the room, even where negative margins widen the range`() {
    val b = resolve(
        atMost(300),
        props {
          width = pct(150f)
          margin[BoxEdge.HORIZONTAL] = BoxLength.Px(-20f)
        },
    )

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(300)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(300)
  }

  @Test
  fun `padding and border set the smallest the border box can be, after the aspect ratio`() {
    val bordered = resolve(exact(4, 4), props { border[BoxEdge.HORIZONTAL] = BoxLength.Px(5f) })
    assertThat(bordered.borderBoxConstraints.minWidth).isEqualTo(10)

    val padded = resolve(SizeConstraints(), props { padding[BoxEdge.VERTICAL] = BoxLength.Px(8f) })
    assertThat(padded.borderBoxConstraints.minHeight).isEqualTo(16)
    assertThat(padded.borderBoxConstraints.hasBoundedHeight).isFalse()

    val derived = resolve(
        SizeConstraints(0, 1000, 0, 1000),
        props {
          width = px(120)
          aspectRatio = 2f
          padding[BoxEdge.VERTICAL] = BoxLength.Px(40f)
        },
    )
    assertThat(derived.borderBoxConstraints.minHeight)
        .describedAs("60 from the ratio, raised to the padding")
        .isEqualTo(80)
  }

  @Test
  fun `a negative margin raises both bounds of the room`() {
    val b = resolve(atMost(200), props { margin[BoxEdge.HORIZONTAL] = BoxLength.Px(-20f) })

    assertThat(b.borderBoxConstraints.minWidth).isEqualTo(40)
    assertThat(b.borderBoxConstraints.maxWidth).isEqualTo(240)
  }

  @Test
  fun `the height stays encodable too, and no size is past what an axis stores`() {
    val bigMin = resolve(exact(50, 20_000), props { minHeight = px(9_000) })
    assertThat(bigMin.borderBoxConstraints.minHeight).isEqualTo(9_000)
    assertThat(bigMin.borderBoxConstraints.maxHeight).isEqualTo(9_000)

    val wide = resolve(atMost(1080), props { width = px(1_200_000_000) })
    assertThat(wide.borderBoxConstraints.maxWidth).isEqualTo(1_073_741_822)
    val loose = resolve(SizeConstraints(), props { maxWidth = BoxLength.Px(1.5e9f) })
    assertThat(loose.borderBoxConstraints.maxWidth).isEqualTo(1_073_741_822)
  }

  @Test
  fun `a size or an edge that is not a finite number counts as unset, as in Yoga`() {
    val nan = resolve(atMost(300), props { width = BoxLength.Px(Float.NaN) })
    assertThat(nan.borderBoxConstraints.minWidth).isEqualTo(0)
    assertThat(nan.borderBoxConstraints.maxWidth).isEqualTo(300)

    val infiniteMin =
        resolve(atMost(1080), props { minWidth = BoxLength.Px(Float.POSITIVE_INFINITY) })
    assertThat(infiniteMin.borderBoxConstraints.minWidth).isEqualTo(0)
    assertThat(infiniteMin.borderBoxConstraints.maxWidth).isEqualTo(1080)

    val infiniteShare =
        resolve(atMost(300), props { width = BoxLength.Percent(Float.POSITIVE_INFINITY) })
    assertThat(infiniteShare.borderBoxConstraints.minWidth).isEqualTo(0)

    val nanMax = resolve(atMost(300), props { maxWidth = BoxLength.Percent(Float.NaN) })
    assertThat(nanMax.borderBoxConstraints.maxWidth).isEqualTo(300)

    val edge = resolve(
        atMost(300),
        props {
          margin[BoxEdge.ALL] = BoxLength.Px(10f)
          margin[BoxEdge.LEFT] = BoxLength.Px(Float.NaN)
        },
    )
    assertThat(edge.margins.left).describedAs("the edges behind it apply").isEqualTo(10)
  }

  // ───────────────────────── properties every child meets, over generated children and constraints

  @Test
  fun `RTL resolves as LTR mirrored, with left and right declarations swapped`() {
    forEachCase { constraints, declared ->
      val rtl = resolve(constraints, declared.build(), isRtl = true)
      val ltr = resolve(constraints, declared.leftRightSwapped().build())

      assertThat(summary(rtl))
          .describedAs("%s in %s", declared, constraints)
          .isEqualTo(summary(ltr, mirrored = true))
    }
  }

  @Test
  fun `declaring a prop at its default changes nothing`() {
    forEachCase { constraints, declared ->
      val unset = summary(resolve(constraints, declared.build()))
      for (variant in declared.withDefaults()) {
        assertThat(summary(resolve(constraints, variant.build())))
            .describedAs("%s in %s", variant, constraints)
            .isEqualTo(unset)
      }
    }
  }

  @Test
  fun `exact constraints are met by a child with no size of its own whose edges fit`() {
    forEachCase { constraints, generated ->
      val declared =
          generated.copy(
              width = BoxLength.Unset,
              height = BoxLength.Unset,
              minWidth = BoxLength.Unset,
              maxWidth = BoxLength.Unset,
              minHeight = BoxLength.Unset,
              maxHeight = BoxLength.Unset,
              aspectRatio = Float.NaN,
          )
      val b = resolve(constraints, declared.build())
      val edgesWidth = b.margins.horizontal + b.padding.horizontal + b.border.horizontal
      val edgesHeight = b.margins.vertical + b.padding.vertical + b.border.vertical
      if (constraints.hasExactWidth && edgesWidth <= constraints.maxWidth) {
        assertThat(b.borderBoxConstraints.minWidth)
            .describedAs("%s in %s", declared, constraints)
            .isEqualTo(b.borderBoxConstraints.maxWidth)
            .isEqualTo(constraints.maxWidth - b.margins.horizontal)
      }
      if (constraints.hasExactHeight && edgesHeight <= constraints.maxHeight) {
        assertThat(b.borderBoxConstraints.minHeight)
            .describedAs("%s in %s", declared, constraints)
            .isEqualTo(b.borderBoxConstraints.maxHeight)
            .isEqualTo(constraints.maxHeight - b.margins.vertical)
      }
    }
  }

  @Test
  fun `the same constraints always give the same answer`() {
    forEachCase { constraints, declared ->
      val props = declared.build()
      val first = summary(resolve(constraints, props))
      resolve(SizeConstraints(0, 97, 0, 41), props)
      resolve(exact(333, 5), props)

      assertThat(summary(resolve(constraints, props))).isEqualTo(first)
    }
  }

  // ───────────────────────── relationships between two resolutions

  @Test
  fun `a child with margins gets the box it would get without them in constraints shrunk by them`() {
    var checked = 0
    forEachCase { constraints, declared ->
      // Percent sizes and padding resolve against the constraints themselves, so they would
      // resolve differently against the shrunk ones.
      if (declared.hasPercentSizeOrPadding) return@forEachCase
      val b = resolve(constraints, declared.build())
      val h = b.margins.horizontal
      val v = b.margins.vertical
      // Margins alone wider than the constraints overflow them, which the shrunk constraints, at
      // 0, cannot show. A child's own size and min overflowing them is covered: that compares
      // equal.
      if (
          (constraints.hasBoundedWidth && h > constraints.maxWidth) ||
              (constraints.hasBoundedHeight && v > constraints.maxHeight)
      ) {
        return@forEachCase
      }
      val shrunk = SizeConstraints(
          minWidth = (constraints.minWidth - h).coerceAtLeast(0),
          maxWidth =
              if (constraints.hasBoundedWidth) (constraints.maxWidth - h).coerceAtLeast(0) else INF,
          minHeight = (constraints.minHeight - v).coerceAtLeast(0),
          maxHeight =
              if (constraints.hasBoundedHeight) (constraints.maxHeight - v).coerceAtLeast(0)
              else INF,
      )
      val without = resolve(shrunk, declared.copy(margins = emptyList()).build())
      checked++

      assertThat(summary(without))
          .describedAs("%s in %s", declared, constraints)
          .isEqualTo(summary(b).replace(text(b.margins), text(IntEdges.None)))
    }
    assertThat(checked).isGreaterThan(1_000)
  }

  @Test
  fun `a percentage gives what the equivalent points give`() {
    forEachCase { constraints, declared ->
      if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) return@forEachCase
      // A percentage size fills the room as `fillMaxWidth` does, while a size in pixels is
      // required, so only the other percentages come to what their pixels give.
      if (declared.width is BoxLength.Percent || declared.height is BoxLength.Percent) {
        return@forEachCase
      }
      val asPoints = declared.percentsAsPoints(constraints.maxWidth, constraints.maxHeight)

      assertThat(summary(resolve(constraints, asPoints.build())))
          .describedAs("%s in %s", declared, constraints)
          .isEqualTo(summary(resolve(constraints, declared.build())))
    }
  }

  @Test
  fun `a later unit replaces an earlier one on the same edge`() {
    val pointsLast = props {
      margin[BoxEdge.LEFT] = BoxLength.Percent(10f)
      margin[BoxEdge.LEFT] = BoxLength.Px(5f)
    }
    val percentLast = props {
      margin[BoxEdge.LEFT] = BoxLength.Px(5f)
      margin[BoxEdge.LEFT] = BoxLength.Percent(10f)
    }

    assertThat(resolve(atMost(200), pointsLast).margins.left).isEqualTo(5)
    assertThat(resolve(atMost(200), percentLast).margins.left).isEqualTo(20)
    assertThat(pointsLast.margin[BoxEdge.LEFT]).isEqualTo(px(5))
    assertThat(percentLast.margin[BoxEdge.LEFT]).isEqualTo(pct(10f))
    assertThat(percentLast.margin[BoxEdge.TOP]).isEqualTo(BoxLength.Unset)
  }

  private fun forEachCase(block: (SizeConstraints, ChildBoxCases.Declared) -> Unit) {
    for (declared in CASES) {
      for (constraints in ChildBoxCases.constraints) {
        block(constraints, declared)
      }
    }
  }

  private companion object {
    val CASES: List<ChildBoxCases.Declared> = ChildBoxCases.generated(count = 600)
  }
}
