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

package com.facebook.rendercore.zoomable

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.facebook.rendercore.extensions.HostVisibilityNotifier
import com.facebook.rendercore.extensions.VisibilityBoundChangeForwardingHostView
import com.facebook.rendercore.extensions.VisibilityBoundsChangeNotifier

/**
 * A [VisibilityBoundChangeForwardingHostView] wrapper for remote container views that need to
 * support visibility bounds change forwarding for incremental mount.
 *
 * This view extends [VisibilityBoundChangeForwardingHostView] to automatically detect and forward
 * scroll, translation, and visibility changes to nested component views (e.g., LithoView).
 *
 * The protected [attachViewToParent] and [detachViewFromParent] methods are exposed publicly to
 * support the zoomable view controller's remote zoom functionality.
 *
 * @param context The Android context.
 * @param notifier The [VisibilityBoundsChangeNotifier] to forward visibility events to. Defaults to
 *   [HostVisibilityNotifier] for managing host view subscribers and forwarding visibility events
 *   when incremental mount is enabled.
 * @param isHostResumed Optional callback that returns whether the host (e.g. Fragment) is in a
 *   resumed state. When provided, the view will post a deferred visible-bounds notification on
 *   attach if the host is not resumed. This works around cases where onDestroyView may not be
 *   called when a Fragment is replaced.
 * @see VisibilityBoundChangeForwardingHostView
 * @see VisibilityBoundsChangeNotifier
 */
open class ZoomableContainerView(
    context: Context,
    notifier: VisibilityBoundsChangeNotifier = HostVisibilityNotifier(),
    isHostResumed: (() -> Boolean)? = null,
    forwardScrollChanges: Boolean = true,
) :
    VisibilityBoundChangeForwardingHostView(
        context,
        notifier,
        isHostResumed,
        forwardScrollChanges = forwardScrollChanges,
    ) {

  public override fun attachViewToParent(
      child: View?,
      index: Int,
      params: ViewGroup.LayoutParams?,
  ) {
    super.attachViewToParent(child, index, params)
  }

  public override fun detachViewFromParent(child: View?) {
    val indexOfChild: Int = indexOfChild(child)
    if (indexOfChild != NO_ID) {
      super.detachViewFromParent(child)
    }
  }
}
