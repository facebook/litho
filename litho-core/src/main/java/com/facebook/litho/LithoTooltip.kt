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

package com.facebook.litho

import android.graphics.Rect
import android.view.View

fun interface LithoTooltip {
  /**
   * **Do not call this method directly!** It should only be called by the Litho framework to ensure
   * that the correct parameters are passed. Use the methods exposed by [LithoTooltipController]
   * instead.
   *
   * Display the tooltip on an anchor component with the given bounds relative to the view that
   * contains it.
   *
   * [container] is the view that wraps the Component used as anchor, while [anchorBounds] specifies
   * the bounds of the component relative to the host view. [xOffset] and [yOffset] moves the
   * tooltip horizontally and vertically, respectively, from the default position
   */
  fun showLithoTooltip(container: View?, anchorBounds: Rect, xOffset: Int, yOffset: Int)
}
