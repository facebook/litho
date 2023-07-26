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
import android.view.View
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.EventHandler
import com.facebook.litho.MatrixDrawable
import com.facebook.litho.OnInitializeAccessibilityNodeInfoEvent
import com.facebook.litho.Style
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.accessibility.accessibilityRoleDescription
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.accessibility.onInitializeAccessibilityNodeInfo
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Tests for [ExperimentalImage] */
@RunWith(LithoTestRunner::class)
class ImageTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Test
  fun `Image should render`() {
    lithoViewRule
        .render {
          Image(
              drawable = ColorDrawable(Color.RED),
              style = Style.width(100.px).height(100.px),
          )
        }
        .apply {
          // should find an Image in the tree
          findComponent(Image::class)

          // should mount an Image
          assertThat(lithoView.mountItemCount).isEqualTo(1)

          // content of Image should be a MatrixDrawable
          val content = lithoView.getMountItemAt(0).content as MatrixDrawable<*>
          assertThat(content.bounds.width()).isEqualTo(100)
          assertThat(content.bounds.height()).isEqualTo(100)

          // Matrix drawable should host a ColorDrawable
          val drawable = content.mountedDrawable
          assertThat(drawable).isInstanceOf(ColorDrawable::class.java)
        }
  }

  @Test
  fun `when a11y props are set on style it should set them on the rendered content`() {

    val eventHandler: EventHandler<OnInitializeAccessibilityNodeInfoEvent> = mock()

    val node =
        lithoViewRule
            .render {
              Image(
                  drawable = ColorDrawable(Color.RED),
                  style =
                      Style.width(100.px)
                          .height(100.px)
                          .accessibilityRole(AccessibilityRole.IMAGE)
                          .accessibilityRoleDescription("Accessibility Test")
                          .contentDescription("Accessibility Test")
                          .importantForAccessibility(ImportantForAccessibility.YES)
                          .onInitializeAccessibilityNodeInfo { eventHandler },
              )
            }
            .apply {
              // should find an Image in the tree
              findComponent(Image::class)

              // verify a11y properties are correctly set on the View
              assertThat(lithoView.contentDescription).isEqualTo("Accessibility Test")
              assertThat(lithoView.importantForAccessibility)
                  .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
            }
            .currentRootNode
            ?.node

    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.accessibilityRole).isEqualTo(AccessibilityRole.IMAGE)
    assertThat(nodeInfo?.accessibilityRoleDescription).isEqualTo("Accessibility Test")
    assertThat(nodeInfo?.onInitializeAccessibilityNodeInfoHandler).isNotNull
  }
}
