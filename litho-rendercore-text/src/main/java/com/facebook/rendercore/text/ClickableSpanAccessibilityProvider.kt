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

package com.facebook.rendercore.text

import android.text.style.ClickableSpan
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * Describes a clickable span's screen-reader node, for span types [RCTextView] doesn't know about
 * (e.g. a framework's own accessible span class).
 */
fun interface ClickableSpanAccessibilityProvider {
  /**
   * Called after [RCTextView] has set the span's bounds, text and default role on [node].
   *
   * @return true if [node] is fully described; false to fall back to [RCTextView]'s own handling.
   */
  fun onPopulateSpanNode(span: ClickableSpan, node: AccessibilityNodeInfoCompat): Boolean
}
