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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.facebook.litho.LithoView
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions.assertThat
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.HorizontalScrollLithoView
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [HorizontalScroll] */
@RunWith(LithoTestRunner::class)
class HorizontalScrollTest {
  @Rule @JvmField val lithoViewRule: LithoViewRule = LithoViewRule()

  @Test
  fun `HorizontalScroll Component should render`() {
    val component =
        HorizontalScroll(style = Style.width(100.px).height(500.px)) {
          Image(
              drawable = ColorDrawable(Color.RED),
              style = Style.width(100.px).height(500.px),
          )
        }

    val testLithoView = lithoViewRule.render { component }

    assertThat(testLithoView).willRenderContent()
    assertThat(testLithoView).containsExactlyOne(HorizontalScroll::class.java)
    assertThat(testLithoView.lithoView.mountItemCount).isEqualTo(1)
    val scrollView = testLithoView.lithoView.getMountItemAt(0).content as HorizontalScrollLithoView
    assertThat(scrollView.measuredWidth).isEqualTo(100)
    assertThat(scrollView.measuredHeight).isEqualTo(500)
    assertThat(scrollView.childCount).isEqualTo(1)
    val innerLithoView = scrollView.getChildAt(0) as LithoView
    assertThat(innerLithoView.measuredWidth).isEqualTo(100)
    assertThat(innerLithoView.measuredHeight).isEqualTo(500)
    val innerContent = innerLithoView.getMountItemAt(0).content as MatrixDrawable<*>
    assertThat(innerContent.bounds.width()).isEqualTo(100)
    assertThat(innerContent.bounds.height()).isEqualTo(500)
  }
}
