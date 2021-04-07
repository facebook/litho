// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

package com.facebook.litho.flexbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.LithoView
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.kotlinStyle
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertMatches
import com.facebook.litho.testing.match
import com.facebook.litho.testing.setRoot
import com.facebook.litho.testing.unspecified
import com.facebook.litho.view.wrapInView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests interop between Spec Components and flexbox [Style] properties. */
@RunWith(AndroidJUnit4::class)
class FlexboxStyleCompatibilityTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun specComponent_whenKotlinStyleSetOnBuilder_commonPropsAreApplied() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row.create(lithoViewRule.context)
              .wrapInView()
              .kotlinStyle(Style.width(100.px).height(100.px))
              .build()
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 100, 100) })
  }

  @Test
  fun specComponent_whenKotlinStyleSetOnBuilderThenOverriden_overriddenValuesApply() {
    lithoViewRule
        .setSizeSpecs(unspecified(), unspecified())
        .setRoot {
          Row.create(lithoViewRule.context)
              .wrapInView()
              .kotlinStyle(Style.width(100.px).height(100.px))
              .widthPx(50)
              .heightPx(50)
              .build()
        }
        .assertMatches(match<LithoView> { bounds(0, 0, 50, 50) })
  }
}
