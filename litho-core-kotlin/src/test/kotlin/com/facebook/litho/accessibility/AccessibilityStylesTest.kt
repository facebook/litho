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

package com.facebook.litho.accessibility

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for accessibility styles. */
@RunWith(AndroidJUnit4::class)
class AccessibilityStylesTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun contentDescription_whenSet_isSetOnView() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(style = Style.width(200.px).height(200.px).contentDescription("Accessibility Test"))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.contentDescription).isEqualTo("Accessibility Test")
  }

  @Test
  fun importantForAccessibility_whenSet_isSetOnView() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row(
              style =
                  Style.width(200.px)
                      .height(200.px)
                      .importantForAccessibility(ImportantForAccessibility.YES))
        }
        .measure()
        .layout()
        .attachToWindow()

    assertThat(lithoViewRule.lithoView.importantForAccessibility)
        .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
  }
}
