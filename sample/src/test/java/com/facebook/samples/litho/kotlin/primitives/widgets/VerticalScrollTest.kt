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
import com.facebook.litho.widget.LithoScrollView
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [VerticalScroll] */
@RunWith(LithoTestRunner::class)
class VerticalScrollTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `VerticalScroll Component should render`() {
    lithoViewRule
        .render {
          VerticalScroll(
              style = Style.width(100.px).height(100.px),
          ) {
            Image(
                drawable = ColorDrawable(Color.RED),
                style = Style.width(100.px).height(500.px),
            )
          }
        }
        .apply {

          // should find an VerticalScroll Component in the tree
          findComponent(VerticalScroll::class)

          // should mount an VerticalScroll Component
          assertThat(lithoView.mountItemCount).isEqualTo(1)

          // content of VerticalScroll Component should be a MatrixDrawable
          val content = lithoView.getMountItemAt(0).content as LithoScrollView
          assertThat(content.measuredWidth).isEqualTo(100)
          assertThat(content.measuredHeight).isEqualTo(100)
          assertThat(content.childCount).isEqualTo(1)

          val innerLithoView = content.getChildAt(0) as LithoView

          // The inner LithoView's height should be 500
          assertThat(innerLithoView.measuredWidth).isEqualTo(100)
          assertThat(innerLithoView.measuredHeight).isEqualTo(500)

          // The content of Image Component should be a MatrixDrawable
          val innerContent = innerLithoView.getMountItemAt(0).content as MatrixDrawable<*>
          assertThat(innerContent.bounds.width()).isEqualTo(100)
          assertThat(innerContent.bounds.height()).isEqualTo(500)
        }
  }

  @Test
  fun `same instance should be equivalent`() {
    val component =
        VerticalScroll(style = Style.width(100.px).height(100.px)) {
          Image(
              drawable = ColorDrawable(Color.RED),
              style = Style.width(100.px).height(500.px),
          )
        }

    assertThat(component).isEquivalentTo(component)
    assertThat(component).isEquivalentTo(component, true)
  }

  @Test
  fun `components with same prop values should be equivalent`() {
    val color = ColorDrawable(Color.RED)
    val a =
        VerticalScroll(style = Style.width(100.px).height(100.px)) {
          Image(
              drawable = color,
              style = Style.width(100.px).height(500.px),
          )
        }

    val b =
        VerticalScroll(style = Style.width(100.px).height(100.px)) {
          Image(
              drawable = color,
              style = Style.width(100.px).height(500.px),
          )
        }

    assertThat(a).isEquivalentTo(b)
    assertThat(a).isEquivalentTo(b, true)
  }

  @Test
  fun `components with different prop values should not be equivalent`() {
    val a =
        VerticalScroll(style = Style.width(100.px).height(100.px)) {
          Image(
              drawable = ColorDrawable(Color.RED), // red here
              style = Style.width(100.px).height(500.px),
          )
        }

    val b =
        VerticalScroll(style = Style.width(100.px).height(100.px)) {
          Image(
              drawable = ColorDrawable(Color.BLUE), // blue here
              style = Style.width(100.px).height(500.px),
          )
        }

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
    assertThatThrownBy { assertThat(a).isEquivalentTo(b, true) }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `components with different style values should not be equivalent`() {
    val color = ColorDrawable(Color.RED)
    val a =
        VerticalScroll(style = Style.width(100.px).height(100.px) /* 100 here */) {
          Image(
              drawable = color,
              style = Style.width(100.px).height(500.px),
          )
        }

    val b =
        VerticalScroll(style = Style.width(200.px).height(200.px) /* 200 here */) {
          Image(
              drawable = color,
              style = Style.width(100.px).height(500.px),
          )
        }

    assertThatThrownBy { assertThat(a).isEquivalentTo(b, true) }
        .isInstanceOf(AssertionError::class.java)
  }
}
