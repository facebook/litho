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
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.Text
import com.facebook.rendercore.utils.MeasureSpecUtils.exactly
import com.facebook.rendercore.utils.MeasureSpecUtils.unspecified
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

/** Tests for [EmptyComponent]. */
@RunWith(LithoTestRunner::class)
class EmptyComponentTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `two EmptyComponents should be equivalent and have equivalent props`() {
    val emptyComponent1 = EmptyComponent()
    val emptyComponent2 = EmptyComponent()

    assertThat(emptyComponent1.isEquivalentProps(emptyComponent2, false))
        .describedAs("isEquivalentProps check")
        .isEqualTo(true)
    assertThat(emptyComponent1.isEquivalentTo(emptyComponent2))
        .describedAs("isEquivalent check")
        .isEqualTo(true)
  }

  @Test
  fun `setting a null root renders an empty component`() {
    val lithoView = lithoViewRule.render { Text.create(context).text("Hello World").build() }
    lithoView.setSizeSpecs(exactly(1000), unspecified())

    LithoViewAssert.assertThat(lithoView.lithoView).hasVisibleText("Hello World")

    lithoViewRule.act(lithoView) { lithoView.setRoot(null) }
    lithoView.measure().layout()
    ShadowLooper.idleMainLooper()

    // The assertion below should work, but we don't perform incremental mount when the visible rect
    // is empty!
    // LithoViewAssert.assertThat(lithoView.lithoView).doesNotHaveVisibleText("Hello World")
    assertThat(lithoView.lithoView.height)
        .describedAs("LithoView height with null root")
        .isEqualTo(0)
  }
}
