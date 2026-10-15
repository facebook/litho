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

package com.facebook.litho.widget

import android.text.style.ClickableSpan
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.litho.AccessibilityRole
import com.facebook.rendercore.text.ClickableSpanAccessibilityProvider
import com.facebook.widget.accessibility.delegates.AccessibleClickableSpan

/**
 * Describes a clickable span's screen-reader node from Litho's [AccessibleClickableSpan], the same
 * way for legacy Text and primitive Text.
 */
internal object AccessibleClickableSpanNodes : ClickableSpanAccessibilityProvider {

  /** Applies the span's description, role (Button by default) and role description to [node]. */
  @JvmStatic
  fun applyRoleAndLabel(span: ClickableSpan?, node: AccessibilityNodeInfoCompat) {
    if (span !is AccessibleClickableSpan) {
      node.className = AccessibilityRole.BUTTON
      return
    }
    span.accessibilityDescription?.let { node.contentDescription = it }
    span.roleDescription?.let { node.roleDescription = it }
    node.className = span.accessibilityRole ?: AccessibilityRole.BUTTON
  }

  // Other spans keep RCTextView's own handling, which already defaults them to a Button.
  override fun onPopulateSpanNode(span: ClickableSpan, node: AccessibilityNodeInfoCompat): Boolean {
    if (span !is AccessibleClickableSpan) {
      return false
    }
    applyRoleAndLabel(span, node)
    return true
  }
}
