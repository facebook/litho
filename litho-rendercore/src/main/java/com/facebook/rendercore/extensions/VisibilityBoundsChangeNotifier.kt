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

package com.facebook.rendercore.extensions

import android.view.View

/**
 * Interface for publishing visibility-related events to subscribers.
 *
 * Implementations of this interface are responsible for notifying subscribers about
 * visibility-related changes in a host view, such as visible bounds changes (due to scrolling or
 * translation) and visibility state changes (VISIBLE, INVISIBLE, GONE).
 */
interface VisibilityBoundsChangeNotifier {

  /**
   * Notifies subscribers that the visible bounds of the host view have changed.
   *
   * This is typically called when the host view is scrolled, translated, or its layout changes in a
   * way that affects which portions of its children are visible.
   *
   * @param hostView The view whose visible bounds have changed
   * @param reason The reason for the visibility change
   */
  fun notifyVisibleBoundsChanged(
      hostView: View,
      reason: VisibilityChangeReason,
  )

  /**
   * Notifies subscribers that the visibility state of the host view has changed.
   *
   * @param hostView The view whose visibility has changed
   * @param visibility The new visibility state (e.g., [View.VISIBLE], [View.INVISIBLE],
   *   [View.GONE])
   * @param reason The reason for the visibility change
   */
  fun notifyVisibilityChanged(
      hostView: View,
      visibility: Int,
      reason: VisibilityChangeReason,
  )
}
