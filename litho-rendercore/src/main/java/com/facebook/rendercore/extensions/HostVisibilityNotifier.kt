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
import androidx.annotation.UiThread
import com.facebook.rendercore.RenderCoreExtensionHost
import com.facebook.rendercore.thread.utils.ThreadUtils.assertMainThread

/**
 * Implementation of [VisibilityBoundsChangeNotifier] that maintains a set of
 * [RenderCoreExtensionHost] subscribers for visibility bounds change notifications.
 *
 * This notifier allows RenderCore host views (e.g., LithoView, RenderTreeHostView) to subscribe and
 * receive visibility bounds change notifications when the container scrolls, translates, or changes
 * visibility. This enables incremental mount to work correctly for content inside containers that
 * forward visibility events.
 *
 * **Thread Safety**: All methods must be called on the UI thread. This class is not thread-safe and
 * relies on the Android UI threading model where all UI operations happen on a single thread.
 *
 * Usage:
 * ```
 * val notifier = HostVisibilityNotifier()
 * val container = VisibilityBoundChangeForwardingHostView(context, notifier)
 *
 * // Subscribe a host view (e.g., in bind)
 * notifier.subscribe(hostView)
 *
 * // Unsubscribe (e.g., in unbind)
 * notifier.unsubscribe(hostView)
 * ```
 *
 * @see VisibilityBoundChangeForwardingHostView
 * @see VisibilityBoundsChangeNotifier
 */
class HostVisibilityNotifier : VisibilityBoundsChangeNotifier {
  private val hostSubscribers = mutableSetOf<RenderCoreExtensionHost>()

  /**
   * Subscribe a [RenderCoreExtensionHost] view to receive visibility bounds change notifications.
   *
   * Must be called on the UI thread.
   */
  @UiThread
  fun subscribe(host: RenderCoreExtensionHost) {
    assertMainThread()
    hostSubscribers.add(host)
  }

  /**
   * Unsubscribe a [RenderCoreExtensionHost] view from receiving visibility bounds change
   * notifications.
   *
   * Must be called on the UI thread.
   */
  @UiThread
  fun unsubscribe(host: RenderCoreExtensionHost) {
    assertMainThread()
    hostSubscribers.remove(host)
  }

  @UiThread
  override fun notifyVisibleBoundsChanged(hostView: View, reason: VisibilityChangeReason) {
    assertMainThread()
    for (host in hostSubscribers) {
      host.notifyVisibleBoundsChanged(reason)
    }
  }

  @UiThread
  override fun notifyVisibilityChanged(
      hostView: View,
      visibility: Int,
      reason: VisibilityChangeReason,
  ) {
    assertMainThread()
    for (host in hostSubscribers) {
      host.notifyVisibleBoundsChanged(reason)
    }
  }
}
