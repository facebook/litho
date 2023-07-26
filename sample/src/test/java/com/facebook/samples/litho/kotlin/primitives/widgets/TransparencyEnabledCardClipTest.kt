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

package com.facebook.samples.litho.kotlin.primitives.widgets

import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions.assertThat
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [TransparencyEnabledCardClip] */
@RunWith(LithoTestRunner::class)
class TransparencyEnabledCardClipTest {

  @Rule @JvmField val lithoViewRule: LithoViewRule = LithoViewRule()

  @Test
  fun `TransparencyEnabledCardClip should render`() {
    val component = TransparencyEnabledCardClip(style = Style.width(100.px).height(100.px))

    val testLithoView = lithoViewRule.render { component }

    assertThat(testLithoView).willRenderContent()
    assertThat(testLithoView).containsExactlyOne(TransparencyEnabledCardClip::class)
    assertThat(testLithoView.lithoView.mountItemCount).isEqualTo(1)
  }
}
