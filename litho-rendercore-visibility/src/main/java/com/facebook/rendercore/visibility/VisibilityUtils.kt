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
import com.facebook.litho.FocusedVisibleEvent
import com.facebook.litho.FullImpressionVisibleEvent
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.UnfocusedVisibleEvent
import com.facebook.litho.VisibilityChangedEvent
import com.facebook.litho.VisibleEvent
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
  fun dispatchOnVisible(visibleHandler: Function<Void?>, content: Any?) {
    RenderCoreSystrace.beginSection("VisibilityUtils.dispatchOnVisible")

    visibleEvent.content = content
    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(VisibilityExtensionConfigs.DEBUG_TAG, "Dispatch:VisibleEvent to: $visibleHandler")
    }
    visibleHandler.call(visibleEvent)
    visibleEvent.content = null
    RenderCoreSystrace.endSection()
  }

  @JvmStatic
  fun dispatchOnFocused(focusedHandler: Function<Void?>) {

    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG, "Dispatch:FocusedVisibleEvent to: $focusedHandler")
    }
    focusedHandler.call(focusedVisibleEvent)
  }

  @JvmStatic
  fun dispatchOnUnfocused(unfocusedHandler: Function<Void?>) {

    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG,
          "Dispatch:UnfocusedVisibleEvent to: $unfocusedHandler")
    }
    unfocusedHandler.call(unfocusedVisibleEvent)
  }

  @JvmStatic
  fun dispatchOnFullImpression(fullImpressionHandler: Function<Void?>) {

    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(
          VisibilityExtensionConfigs.DEBUG_TAG,
          "Dispatch:FullImpressionVisibleEvent to: $fullImpressionHandler")
    }
    fullImpressionHandler.call(fullImpressionVisibleEvent)
  }

  @JvmStatic
  fun dispatchOnInvisible(invisibleHandler: Function<Void?>) {

    if (VisibilityExtensionConfigs.isDebugLoggingEnabled) {
      Log.d(VisibilityExtensionConfigs.DEBUG_TAG, "Dispatch:InvisibleEvent to: $invisibleHandler")
    }
    invisibleHandler.call(invisibleEvent)
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
      percentVisibleHeight: Float
  ) {
    if (visibilityChangedHandler == null) {
      return
    }
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
          "Dispatch:VisibilityChangedEvent to: $visibilityChangedHandler")
    }
    visibilityChangedHandler.call(visibilityChangedEvent)
  }
}
