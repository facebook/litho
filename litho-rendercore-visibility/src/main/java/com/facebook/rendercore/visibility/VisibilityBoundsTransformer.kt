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

package com.facebook.rendercore.visibility

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

interface VisibilityBoundsTransformer {

  /**
   * Transforms the local visible rect of the host view to account for occlusions.
   *
   * @param host The host ViewGroup whose visibility bounds should be transformed.
   * @param outRect The output Rect to populate with transformed bounds. Implementations should
   *   modify this Rect in-place. If the host is fully occluded, the Rect should be set to empty via
   *   [Rect.setEmpty].
   */
  fun getTransformedLocalVisibleRect(host: ViewGroup, outRect: Rect)

  /**
   * Gets the viewport rect of the view.
   *
   * @param view The view to get viewport for.
   * @param outRect The output Rect to populate with viewport bounds. Implementations should modify
   *   this Rect in-place.
   */
  fun getViewportRect(view: View, outRect: Rect)

  fun shouldUseTransformedVisibleRect(visibilityOutput: VisibilityOutput): Boolean
}
