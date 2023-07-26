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

import android.graphics.Color
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.SolidColor
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class VisibilityExtensionTest {

  @JvmField @Rule var lithoViewRule = LithoViewRule()

  @Test
  fun `when litho view is in transient state then visibility event should not be dispatched`() {
    class VisibilityEventCallbackComponent(val callback: () -> Unit) : KComponent() {
      override fun ComponentScope.render(): Component {
        return SolidColor(
            Color.BLACK,
            style = Style.width(100.px).height(100.px).onVisible { callback() },
        )
      }
    }

    var wasCalled = false
    val component = VisibilityEventCallbackComponent(callback = { wasCalled = true })

    val lithoView = LithoView(lithoViewRule.context)
    lithoView.setHasTransientState(true)

    lithoViewRule.render(lithoView = lithoView) { component }

    assertThat(wasCalled).isFalse

    lithoView.setHasTransientState(false)

    assertThat(wasCalled).isTrue
  }
}
