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
import com.facebook.rendercore.RenderCoreExtensionHost

/**
 * Extension function to find the nearest [HostVisibilityNotifier] in the view hierarchy.
 *
 * Walks up the parent chain looking for a view with the notifier tag set.
 *
 * @return The notifier if found, null otherwise
 */
fun View.findHostVisibilityNotifier(): HostVisibilityNotifier? {
  var currentView: View? = this
  while (currentView != null) {
    val notifier =
        currentView.getTag(
            VisibilityBoundChangeForwardingHostView.VISIBILITY_BOUNDS_CHANGE_NOTIFIER_TAG_KEY,
        )
    if (notifier is HostVisibilityNotifier) {
      return notifier
    }
    currentView = currentView.parent as? View
  }
  return null
}

/**
 * Extension function to subscribe a [RenderCoreExtensionHost] view to the nearest host visibility
 * notifier in the view hierarchy.
 *
 * This should be called during the bind phase of a component.
 *
 * @return True if successfully subscribed, false if no notifier found
 */
fun RenderCoreExtensionHost.subscribeToVisibilityNotifications(): Boolean {
  val notifier = (this as? View)?.findHostVisibilityNotifier()
  return if (notifier != null) {
    notifier.subscribe(this)
    true
  } else {
    false
  }
}

/**
 * Extension function to unsubscribe a [RenderCoreExtensionHost] view from the nearest host
 * visibility notifier.
 *
 * This should be called during unbind or when the view is detached.
 */
fun RenderCoreExtensionHost.unsubscribeFromVisibilityNotifications() {
  (this as? View)?.findHostVisibilityNotifier()?.unsubscribe(this)
}
