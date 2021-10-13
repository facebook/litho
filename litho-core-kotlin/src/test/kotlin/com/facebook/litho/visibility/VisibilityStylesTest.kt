/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.visibility

import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for visibility event styles. */
@RunWith(AndroidJUnit4::class)
class VisibilityStylesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun onVisible_whenSet_firesWhenVisible() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).onVisible { eventFired.set(true) })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(eventFired.get()).isTrue()
  }

  @Test
  fun onInvisible_whenSet_firesWhenVisibleThenInvisible() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).onInvisible { eventFired.set(true) })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(eventFired.get()).isFalse()

    lithoViewRule.lithoView.setVisibilityHint(false)

    assertThat(eventFired.get()).isTrue()
  }

  @Test
  fun onFocusedVisibleAndUnfocusedVisible_whenSet_firesWhenFocusedAndUnfocusedVisible() {
    val focusFired = AtomicBoolean(false)
    val unfocusFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style =
                  Style.width(200.px)
                      .height(200.px)
                      .onFocusedVisible { focusFired.set(true) }
                      .onUnfocusedVisible { unfocusFired.set(true) })
        }
        .attachToWindow()

    // FocusedVisible requires a measured parent
    val frameLayout = FrameLayout(lithoViewRule.context.androidContext)
    frameLayout.addView(lithoViewRule.lithoView)
    frameLayout.measure(unspecified(), unspecified())
    frameLayout.layout(0, 0, frameLayout.measuredWidth, frameLayout.measuredHeight)

    assertThat(focusFired.get()).isTrue()

    lithoViewRule.lithoView.setVisibilityHint(false)

    assertThat(unfocusFired.get()).isTrue()
  }

  @Test
  fun onFullImpression_whenSet_firesWhenVisible() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).onFullImpression { eventFired.set(true) })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(eventFired.get()).isTrue()
  }

  @Test
  fun onVisibilityChanged_whenSet_firesWhenVisibilityBoundsChange() {
    val eventFired = AtomicBoolean(false)

    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style =
                  Style.width(200.px).height(200.px).onVisibilityChanged { eventFired.set(true) })
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(eventFired.get()).isTrue()
  }
}
