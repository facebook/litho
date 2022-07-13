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

package com.facebook.litho.kotlin.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.facebook.litho.Column
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.px
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.MountItemsPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ContentPoolingTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `different components should use different pools`() {

    // Mount an Image component
    lithoViewRule.render {
      Column {
        child(
            ExperimentalImage(
                drawable = ColorDrawable(Color.RED),
                style = Style.width(100.px).height(100.px),
            ))
      }
    }

    // Should create 1 Pool for Image
    assertThat(MountItemsPool.getMountItemPools().size).isEqualTo(1)

    // Mount multiple Image components, and a Vertical Scroll component
    val lithoView =
        lithoViewRule
            .render {
              Column {
                child(
                    ExperimentalImage(
                        drawable = ColorDrawable(Color.RED),
                        style = Style.width(100.px).height(100.px),
                    ))
                child(
                    ExperimentalVerticalScroll(
                        style = Style.width(100.px).height(100.px),
                    ) {
                      ExperimentalImage(
                          drawable = ColorDrawable(Color.RED),
                          style = Style.width(100.px).height(500.px),
                      )
                    })
              }
            }
            .lithoView

    // Should now have 2 Pools; one for the Image, and one for the Vertical Scroll component.
    assertThat(MountItemsPool.getMountItemPools().size).isEqualTo(2)

    // Unmount all content to release all the content to the pools
    lithoView.unmountAllItems()
  }

  @Test
  fun `should use pool size from mountable`() {

    // Mount 40 Image components, and then unmount them all
    lithoViewRule
        .render {
          Column {
            for (i in 1..40) {
              child(
                  ExperimentalImage(
                      drawable = ColorDrawable(Color.RED),
                      style = Style.width(100.px).height(100.px),
                  ))
            }
          }
        }
        .lithoView
        .unmountAllItems()

    assertThat(MountItemsPool.getMountItemPools().size).isEqualTo(1)
  }
}
