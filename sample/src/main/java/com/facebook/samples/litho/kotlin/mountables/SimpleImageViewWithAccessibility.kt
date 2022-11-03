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

package com.facebook.samples.litho.kotlin.mountables

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.Style
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.accessibility.accessibilityRoleDescription
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.accessibility.onInitializeAccessibilityNodeInfo

// start_simple_mountable_component_with_a11y_example
class SimpleImageViewWithAccessibility(private val style: Style? = null) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    val a11y =
        Style.accessibilityRole(AccessibilityRole.IMAGE)
            .accessibilityRoleDescription("Image View")
            .importantForAccessibility(ImportantForAccessibility.YES)
            .onInitializeAccessibilityNodeInfo {
              it.superDelegate.onInitializeAccessibilityNodeInfo(it.host, it.info)
              it.info.addAction(
                  AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                      AccessibilityNodeInfoCompat.ACTION_CLICK, "actionDescriptionText"))
            }

    return MountableRenderResult(SimpleImageViewMountable(), a11y + style)
  }
}
// end_simple_mountable_component_with_a11y_example
