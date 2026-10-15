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

import android.annotation.SuppressLint
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the legacy [Row.Builder] / [Column.Builder] `children(List<Component>)` API: children are
 * appended in order and compose with `child(...)`. Asserts via [isEquivalentTo] against the
 * equivalent all-`child()` chain (Row/Column compare children element-wise and in order).
 */
@Suppress("DEPRECATION")
@SuppressLint("KComponentJavaApiDetector")
@RunWith(LithoTestRunner::class)
class LegacyContainerBuilderChildrenTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  /** Distinguishable leaf component — equivalence compares the [id] field. */
  private class Child(val id: Int) : KComponent() {
    override fun ComponentScope.render(): Component = Row.create(context).build()
  }

  @Test
  fun `Row children() appends in order and composes with child()`() {
    val c = lithoTestRule.context
    val actual =
        Row.create(c).child(Child(0)).children(listOf(Child(1), Child(2))).child(Child(3)).build()
    val expected =
        Row.create(c).child(Child(0)).child(Child(1)).child(Child(2)).child(Child(3)).build()
    assertThat(actual.isEquivalentTo(expected)).isTrue()
  }

  @Test
  fun `Row children() order is significant`() {
    val c = lithoTestRule.context
    val actual = Row.create(c).children(listOf(Child(1), Child(2))).build()
    val wrongOrder = Row.create(c).children(listOf(Child(2), Child(1))).build()
    assertThat(actual.isEquivalentTo(wrongOrder)).isFalse()
  }

  @Test
  fun `Column children() appends in order and composes with child()`() {
    val c = lithoTestRule.context
    val actual =
        Column.create(c)
            .child(Child(0))
            .children(listOf(Child(1), Child(2)))
            .child(Child(3))
            .build()
    val expected =
        Column.create(c).child(Child(0)).child(Child(1)).child(Child(2)).child(Child(3)).build()
    assertThat(actual.isEquivalentTo(expected)).isTrue()
  }
}
