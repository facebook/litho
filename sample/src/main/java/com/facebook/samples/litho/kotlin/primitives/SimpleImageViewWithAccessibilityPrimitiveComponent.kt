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

package com.facebook.samples.litho.kotlin.primitives

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.accessibility.accessibilityRoleDescription
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.accessibility.onInitializeAccessibilityNodeInfo

// start_simple_primitive_component_with_a11y_example
class SimpleImageViewWithAccessibilityPrimitiveComponent(private val style: Style? = null) :
    PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
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

    return LithoPrimitive(primitive = SimpleImageViewPrimitive, style = a11y + style)
  }
}
// end_simple_primitive_component_with_a11y_example
