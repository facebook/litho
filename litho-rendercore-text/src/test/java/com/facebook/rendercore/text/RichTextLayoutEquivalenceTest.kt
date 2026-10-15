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

package com.facebook.rendercore.text

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.facebook.rendercore.primitives.LayoutBehavior
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Whether a re-render of [RichTextPrimitive] can keep its last measurement. */
@RunWith(RobolectricTestRunner::class)
class RichTextLayoutEquivalenceTest {

  @Test
  fun `a style built again with the same values is equivalent`() {
    assertThat(behavior(style = style()).isEquivalentTo(behavior(style = style()))).isTrue()
  }

  @Test
  fun `a style with a different value is not equivalent`() {
    val larger = style().apply { textSize = 50 }

    assertThat(behavior(style = style()).isEquivalentTo(behavior(style = larger))).isFalse()
  }

  @Test
  fun `a style with a different color is not equivalent`() {
    val blue = style().apply { textColor = Color.BLUE }

    assertThat(behavior(style = style()).isEquivalentTo(behavior(style = blue))).isFalse()
  }

  @Test
  fun `different text is not equivalent`() {
    assertThat(behavior(text = "hello").isEquivalentTo(behavior(text = "world"))).isFalse()
  }

  @Test
  fun `a custom ellipsis with different spans is not equivalent`() {
    val plain = style().apply { customEllipsisText = "more" }
    val colored = style().apply { customEllipsisText = coloredMore() }

    assertThat(behavior(style = plain).isEquivalentTo(behavior(style = colored))).isFalse()
  }

  @Test
  fun `a different truncation mode is not equivalent`() {
    assertThat(
        behavior(usePerformantTruncation = false)
            .isEquivalentTo(behavior(usePerformantTruncation = true)),
    )
        .isFalse()
  }

  private fun style(): TextStyle = TextStyle().apply { textSize = 40 }

  private fun coloredMore(): SpannableString =
      SpannableString("more").apply {
        setSpan(ForegroundColorSpan(Color.RED), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }

  private fun behavior(
      text: CharSequence = "hello",
      style: TextStyle = style(),
      usePerformantTruncation: Boolean = false,
  ): LayoutBehavior = RichTextPrimitive(
      id = 1L,
      text = text,
      style = style,
      usePerformantTruncation = usePerformantTruncation,
      useResolvedLayoutDirection = true,
  )
      .layoutBehavior
}
