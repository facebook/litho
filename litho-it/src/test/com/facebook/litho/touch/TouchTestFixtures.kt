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

package com.facebook.litho.touch

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.view.touchExpansion
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat

/**
 * Layout constants shared by the golden suites. All in px: the traces assert exact integer
 * coordinates, so dp would make them depend on the Robolectric density qualifier.
 */
internal object TouchFixtureSizes {
  const val SIZE: Int = 100
  const val PAD: Int = 10
  const val INNER_SIZE: Int = SIZE - 2 * PAD
  const val SMALL: Int = 20
  const val MARGIN: Int = 20
  const val EXPANSION: Int = 20
  const val CENTER: Float = SIZE / 2f
}

/** A small target inset by a margin, with touch expansion covering part of that margin. */
internal class ExpandedTarget(private val style: Style) : KComponent() {
  override fun ComponentScope.render(): Component = Column {
    child(
        Column(
            style =
                Style.width(TouchFixtureSizes.SMALL.px)
                    .height(TouchFixtureSizes.SMALL.px)
                    .margin(all = TouchFixtureSizes.MARGIN.px)
                    .touchExpansion(all = TouchFixtureSizes.EXPANSION.px) + style,
        ),
    )
  }
}

/**
 * Compares a recorded trace against its golden, printing the observed trace on mismatch since
 * assertion diffs elide long strings.
 */
internal fun TouchContractTrace.assertMatches(expected: String) {
  val actual = asText()
  if (actual != expected) {
    println("===TRACE-BEGIN===\n$actual\n===TRACE-END===")
  }
  assertThat(actual).isEqualTo(expected)
}
