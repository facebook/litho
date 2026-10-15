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

import android.util.Log
import com.facebook.rendercore.Function
import com.facebook.rendercore.RenderCoreSystrace

object VisibilityUtils {
  private val visibleEvent: VisibleEvent = VisibleEvent()
  private val focusedVisibleEvent: FocusedVisibleEvent = FocusedVisibleEvent()
  private val unfocusedVisibleEvent: UnfocusedVisibleEvent = UnfocusedVisibleEvent()
  private val fullImpressionVisibleEvent: FullImpressionVisibleEvent = FullImpressionVisibleEvent()
  private val invisibleEvent: InvisibleEvent = InvisibleEvent()
  private val visibilityChangedEvent: VisibilityChangedEvent = VisibilityChangedEvent()

  @JvmStatic
  fun dispatchOnVisible(
      visibleHandler: Function<Void?>,
      content: Any?,
      reason: VisibilityChangeReason,
  ) {
    RenderCoreSystrace.beginSection("VisibilityUtils.dispatchOnVisible")

    visibleEvent.content = content
    visibleEvent.reason = reason
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(VisibilityExtensionConfigs.DEBUG_TAG, "Dispatch:VisibleEvent to: $visibleHandler")
    }
    visibleHandler.call(visibleEvent)
    visibleEvent.content = null
    visibleEvent.reason = VisibilityChangeReason.UNKNOWN
    RenderCoreSystrace.endSection()
  }

  @JvmStatic
  fun dispatchOnFocused(
      focusedHandler: Function<Void?>,
      reason: VisibilityChangeReason,
  ) {
    focusedVisibleEvent.reason = reason
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG,
          "Dispatch:FocusedVisibleEvent to: $focusedHandler",
      )
    }
    focusedHandler.call(focusedVisibleEvent)
    focusedVisibleEvent.reason = VisibilityChangeReason.UNKNOWN
  }

  @JvmStatic
  fun dispatchOnUnfocused(
      unfocusedHandler: Function<Void?>,
      reason: VisibilityChangeReason,
  ) {
    unfocusedVisibleEvent.reason = reason
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG,
          "Dispatch:UnfocusedVisibleEvent to: $unfocusedHandler",
      )
    }
    unfocusedHandler.call(unfocusedVisibleEvent)
    unfocusedVisibleEvent.reason = VisibilityChangeReason.UNKNOWN
  }

  @JvmStatic
  fun dispatchOnFullImpression(
      fullImpressionHandler: Function<Void?>,
      content: Any?,
      reason: VisibilityChangeReason,
  ) {
    fullImpressionVisibleEvent.content = content
    fullImpressionVisibleEvent.reason = reason
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG,
          "Dispatch:FullImpressionVisibleEvent to: $fullImpressionHandler",
      )
    }
    fullImpressionHandler.call(fullImpressionVisibleEvent)
    fullImpressionVisibleEvent.content = null
    fullImpressionVisibleEvent.reason = VisibilityChangeReason.UNKNOWN
  }

  @JvmStatic
  fun dispatchOnInvisible(
      invisibleHandler: Function<Void?>,
      reason: VisibilityChangeReason,
  ) {
    invisibleEvent.reason = reason
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(VisibilityExtensionConfigs.DEBUG_TAG, "Dispatch:InvisibleEvent to: $invisibleHandler")
    }
    invisibleHandler.call(invisibleEvent)
    invisibleEvent.reason = VisibilityChangeReason.UNKNOWN
  }

  @JvmStatic
  fun dispatchOnVisibilityChanged(
      visibilityChangedHandler: Function<Void?>?,
      visibleTop: Int,
      visibleLeft: Int,
      visibleWidth: Int,
      visibleHeight: Int,
      rootHostViewWidth: Int,
      rootHostViewHeight: Int,
      percentVisibleWidth: Float,
      percentVisibleHeight: Float,
      reason: VisibilityChangeReason,
  ) {
    if (visibilityChangedHandler == null) {
      return
    }
    visibilityChangedEvent.reason = reason
    visibilityChangedEvent.visibleTop = visibleTop
    visibilityChangedEvent.visibleLeft = visibleLeft
    visibilityChangedEvent.visibleHeight = visibleHeight
    visibilityChangedEvent.visibleWidth = visibleWidth
    visibilityChangedEvent.rootHostViewHeight = rootHostViewHeight
    visibilityChangedEvent.rootHostViewWidth = rootHostViewWidth
    visibilityChangedEvent.percentVisibleHeight = percentVisibleHeight
    visibilityChangedEvent.percentVisibleWidth = percentVisibleWidth
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG,
          "Dispatch:VisibilityChangedEvent to: $visibilityChangedHandler",
      )
    }
    visibilityChangedHandler.call(visibilityChangedEvent)
    visibilityChangedEvent.reason = VisibilityChangeReason.UNKNOWN
  }
}
