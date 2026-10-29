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

import android.view.View
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectableTextViewTest {

  @Test
  @Config(sdk = [27, 33])
  fun `mount applies the measured line height`() {
    val context = RuntimeEnvironment.getApplication()
    val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    val style =
        TextStyle().apply {
          textSize = 14
          lineHeight = 22f
        }
    val measured =
        TextMeasurementUtils.layout(context, unspecified, unspecified, "a\nb\nc", style).second

    val view =
        SelectableTextView(context).apply {
          mount(measured)
          measure(unspecified, unspecified)
        }

    assertThat(view.lineHeight).isEqualTo(22)
    assertThat(view.layout.height).isEqualTo(measured.layout.height)
  }
}
