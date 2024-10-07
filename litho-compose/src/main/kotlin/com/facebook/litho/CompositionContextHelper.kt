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

@file:Suppress(
    "CANNOT_OVERRIDE_INVISIBLE_MEMBER",
    "INVISIBLE_MEMBER",
    "INVISIBLE_REFERENCE",
)

package com.facebook.litho

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.platform.windowRecomposer
import com.facebook.compose.view.MetaComposeView

/**
 * Compose requires CompositionContext to be present when initial composition is executed, otherwise
 * it'll throw an exception. Compose looks for CompositionContext by walking up the View hierarchy
 * so it requires MetaComposeView to be attached when initial composition is performed. Litho
 * sometimes measures the content view before it's attached to the window and
 * MetaComposeView.measure will perform initial composition in case it hasn't been performed before.
 *
 * In order to prevent that crash from happening, we need to ensure that CompositionContext is
 * always present during initial composition, even if Litho measures MetaComposeView without
 * attaching it to the window.
 */
internal object CompositionContextHelper {

  /**
   * Ensures that [MetaComposeView] has CompositionContext set.
   *
   * @return returns true if custom CompositionContext was set, false otherwise.
   */
  fun bind(content: MetaComposeView, activity: Activity): Boolean {
    return if (!content.isAttachedToWindow) {
      val ancestorViewInWindow =
          activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
      var parentContext = ancestorViewInWindow.findViewTreeCompositionContext()
      if (parentContext == null) {
        parentContext = ancestorViewInWindow.windowRecomposer
      }
      content.setParentCompositionContext(parentContext)
      true
    } else {
      false
    }
  }

  /** Unsets the custom CompositionContext set by [bind]. */
  fun unbind(content: MetaComposeView, shouldReset: Boolean) {
    if (shouldReset) {
      content.setParentCompositionContext(null)
    }
  }
}
