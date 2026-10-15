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

package com.facebook.litho.testing

import android.os.Looper
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.State
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
class LithoTestRuleIdleTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `idle runs a state update enqueued after the main clock moved ahead`() {
    var label: State<String>? = null
    val view = lithoTestRule.render {
      object : KComponent() {
        override fun ComponentScope.render(): Component {
          val state = useState { "before" }
          label = state
          return Text(state.value)
        }
      }
    }

    // A test (or a debounce) idling the main looper moves its clock ahead of the layout thread's.
    shadowOf(Looper.getMainLooper()).idleFor(500, TimeUnit.MILLISECONDS)
    checkNotNull(label).update("after")
    lithoTestRule.idle()

    assertThat(view.findViewWithTextOrNull("after")).isNotNull()
  }
}
