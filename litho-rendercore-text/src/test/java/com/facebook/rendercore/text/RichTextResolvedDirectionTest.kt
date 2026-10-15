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

import android.text.Layout
import com.facebook.rendercore.HostView
import com.facebook.rendercore.testing.RenderCoreTestRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Laying out with [RichTextPrimitive]'s `useResolvedLayoutDirection` keeps per-layout state local.
 */
@RunWith(RobolectricTestRunner::class)
class RichTextResolvedDirectionTest {

  @JvmField @Rule val renderCoreTestRule = RenderCoreTestRule()

  @Test
  fun `measuring without a resolved direction leaves the caller's style unchanged`() {
    val style = TextStyle().apply { textSize = 40 }

    renderCoreTestRule
        .useRootNode(
            RichTextPrimitive(
                id = 1,
                text = "1234 5678 90",
                style = style,
                useResolvedLayoutDirection = true,
            ),
        )
        .setSizePx(300, 100)
        .render()

    // A default direction written here would override a resolved direction in a later layout.
    assertThat(style.textDirection).isNull()
  }

  @Test
  fun `a direction set on the style applies when the host has none`() {
    val style =
        TextStyle().apply {
          textSize = 40
          resolvedLayoutDirectionRtl = true
        }

    renderCoreTestRule
        .useRootNode(
            RichTextPrimitive(
                id = 1,
                text = "1234 5678 90",
                style = style,
                useResolvedLayoutDirection = true,
            ),
        )
        .setSizePx(300, 100)
        .render()

    // Neutral digits take their paragraph direction from the style's RTL.
    val textView = (renderCoreTestRule.rootHost as HostView).getChildAt(0) as RCTextView
    assertThat(checkNotNull(textView.layout).getParagraphDirection(0))
        .isEqualTo(Layout.DIR_RIGHT_TO_LEFT)
  }
}
