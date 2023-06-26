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

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * This class contains utility methods to send pre-defined events (click, touch, accessibility,
 * etc.) to [EventHandler] instances' [Component]s
 */
internal object EventDispatcherUtils {
  @JvmStatic
  fun dispatchOnClick(clickHandler: EventHandler<ClickEvent>, view: View) {
    ThreadUtils.assertMainThread()
    val clickEvent = ClickEvent()
    clickEvent.view = view
    val eventDispatcher = checkNotNull(clickHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(clickHandler, clickEvent)
  }

  @JvmStatic
  fun dispatchOnFocusChanged(
      focusChangeHandler: EventHandler<FocusChangedEvent>,
      view: View,
      hasFocus: Boolean
  ) {
    ThreadUtils.assertMainThread()
    val focusChangedEvent = FocusChangedEvent()
    focusChangedEvent.view = view
    focusChangedEvent.hasFocus = hasFocus
    val eventDispatcher =
        checkNotNull(focusChangeHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(focusChangeHandler, focusChangedEvent)
  }

  @JvmStatic
  fun dispatchOnLongClick(longClickHandler: EventHandler<LongClickEvent>, view: View): Boolean {
    ThreadUtils.assertMainThread()
    val longClickEvent = LongClickEvent()
    longClickEvent.view = view
    val eventDispatcher =
        checkNotNull(longClickHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    val returnValue = eventDispatcher.dispatchOnEvent(longClickHandler, longClickEvent)
    return returnValue != null && returnValue as Boolean
  }

  @JvmStatic
  fun dispatchOnTouch(
      touchHandler: EventHandler<TouchEvent>,
      view: View,
      event: MotionEvent
  ): Boolean {
    ThreadUtils.assertMainThread()
    val touchEvent = TouchEvent()
    touchEvent.view = view
    touchEvent.motionEvent = event
    val eventDispatcher = checkNotNull(touchHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    val returnValue = eventDispatcher.dispatchOnEvent(touchHandler, touchEvent)
    return returnValue != null && returnValue as Boolean
  }

  @JvmStatic
  fun dispatchOnInterceptTouch(
      interceptTouchHandler: EventHandler<InterceptTouchEvent>,
      view: View,
      event: MotionEvent
  ): Boolean {
    ThreadUtils.assertMainThread()
    val interceptTouchEvent = InterceptTouchEvent()
    interceptTouchEvent.motionEvent = event
    interceptTouchEvent.view = view
    val eventDispatcher =
        checkNotNull(interceptTouchHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    val returnValue = eventDispatcher.dispatchOnEvent(interceptTouchHandler, interceptTouchEvent)
    return returnValue != null && returnValue as Boolean
  }

  @JvmStatic
  fun dispatchDispatchPopulateAccessibilityEvent(
      eventHandler: EventHandler<DispatchPopulateAccessibilityEventEvent>,
      host: View,
      event: AccessibilityEvent,
      superDelegate: AccessibilityDelegateCompat?
  ): Boolean {
    ThreadUtils.assertMainThread()
    val dispatchPopulateAccessibilityEventEvent = DispatchPopulateAccessibilityEventEvent()
    dispatchPopulateAccessibilityEventEvent.host = host
    dispatchPopulateAccessibilityEventEvent.event = event
    dispatchPopulateAccessibilityEventEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    val returnValue =
        eventDispatcher.dispatchOnEvent(eventHandler, dispatchPopulateAccessibilityEventEvent)
    return returnValue != null && returnValue as Boolean
  }

  @JvmStatic
  fun dispatchOnInitializeAccessibilityEvent(
      eventHandler: EventHandler<OnInitializeAccessibilityEventEvent>,
      host: View,
      event: AccessibilityEvent,
      superDelegate: AccessibilityDelegateCompat?
  ) {
    ThreadUtils.assertMainThread()
    val onInitializeAccessibilityEventEvent = OnInitializeAccessibilityEventEvent()
    onInitializeAccessibilityEventEvent.host = host
    onInitializeAccessibilityEventEvent.event = event
    onInitializeAccessibilityEventEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(eventHandler, onInitializeAccessibilityEventEvent)
  }

  @JvmStatic
  fun dispatchOnInitializeAccessibilityNodeInfoEvent(
      eventHandler: EventHandler<OnInitializeAccessibilityNodeInfoEvent>,
      host: View,
      info: AccessibilityNodeInfoCompat,
      superDelegate: AccessibilityDelegateCompat?
  ) {
    ThreadUtils.assertMainThread()
    val onInitializeAccessibilityNodeInfoEvent = OnInitializeAccessibilityNodeInfoEvent()
    onInitializeAccessibilityNodeInfoEvent.host = host
    onInitializeAccessibilityNodeInfoEvent.info = info
    onInitializeAccessibilityNodeInfoEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(eventHandler, onInitializeAccessibilityNodeInfoEvent)
  }

  @JvmStatic
  fun dispatchOnPopulateAccessibilityEvent(
      eventHandler: EventHandler<OnPopulateAccessibilityEventEvent>,
      host: View,
      event: AccessibilityEvent,
      superDelegate: AccessibilityDelegateCompat?
  ) {
    ThreadUtils.assertMainThread()
    val onPopulateAccessibilityEventEvent = OnPopulateAccessibilityEventEvent()
    onPopulateAccessibilityEventEvent.host = host
    onPopulateAccessibilityEventEvent.event = event
    onPopulateAccessibilityEventEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(eventHandler, onPopulateAccessibilityEventEvent)
  }

  @JvmStatic
  fun dispatchOnPopulateAccessibilityNode(
      eventHandler: EventHandler<OnPopulateAccessibilityNodeEvent>,
      host: View,
      info: AccessibilityNodeInfoCompat
  ) {
    ThreadUtils.assertMainThread()
    val onPopulateAccessibilityNodeEvent = OnPopulateAccessibilityNodeEvent()
    onPopulateAccessibilityNodeEvent.host = host
    onPopulateAccessibilityNodeEvent.accessibilityNode = info
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(eventHandler, onPopulateAccessibilityNodeEvent)
  }

  @JvmStatic
  fun dispatchOnRequestSendAccessibilityEvent(
      eventHandler: EventHandler<OnRequestSendAccessibilityEventEvent>,
      host: ViewGroup,
      child: View,
      event: AccessibilityEvent,
      superDelegate: AccessibilityDelegateCompat?
  ): Boolean {
    ThreadUtils.assertMainThread()
    val onRequestSendAccessibilityEventEvent = OnRequestSendAccessibilityEventEvent()
    onRequestSendAccessibilityEventEvent.host = host
    onRequestSendAccessibilityEventEvent.child = child
    onRequestSendAccessibilityEventEvent.event = event
    onRequestSendAccessibilityEventEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    val returnValue =
        eventDispatcher.dispatchOnEvent(eventHandler, onRequestSendAccessibilityEventEvent)
    return returnValue != null && returnValue as Boolean
  }

  @JvmStatic
  fun dispatchPerformAccessibilityActionEvent(
      eventHandler: EventHandler<PerformAccessibilityActionEvent>,
      host: View,
      action: Int,
      args: Bundle?,
      superDelegate: AccessibilityDelegateCompat?
  ): Boolean {
    ThreadUtils.assertMainThread()
    val performAccessibilityActionEvent = PerformAccessibilityActionEvent()
    performAccessibilityActionEvent.host = host
    performAccessibilityActionEvent.action = action
    performAccessibilityActionEvent.args = args
    performAccessibilityActionEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    val returnValue = eventDispatcher.dispatchOnEvent(eventHandler, performAccessibilityActionEvent)
    return returnValue != null && returnValue as Boolean
  }

  @JvmStatic
  fun dispatchSendAccessibilityEvent(
      eventHandler: EventHandler<SendAccessibilityEventEvent>,
      host: View,
      eventType: Int,
      superDelegate: AccessibilityDelegateCompat?
  ) {
    ThreadUtils.assertMainThread()
    val sendAccessibilityEventEvent = SendAccessibilityEventEvent()
    sendAccessibilityEventEvent.host = host
    sendAccessibilityEventEvent.eventType = eventType
    sendAccessibilityEventEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(eventHandler, sendAccessibilityEventEvent)
  }

  @JvmStatic
  fun dispatchSendAccessibilityEventUnchecked(
      eventHandler: EventHandler<SendAccessibilityEventUncheckedEvent>,
      host: View,
      event: AccessibilityEvent,
      superDelegate: AccessibilityDelegateCompat?
  ) {
    ThreadUtils.assertMainThread()
    val sendAccessibilityEventUncheckedEvent = SendAccessibilityEventUncheckedEvent()
    sendAccessibilityEventUncheckedEvent.host = host
    sendAccessibilityEventUncheckedEvent.event = event
    sendAccessibilityEventUncheckedEvent.superDelegate = superDelegate
    val eventDispatcher = checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher).eventDispatcher
    eventDispatcher.dispatchOnEvent(eventHandler, sendAccessibilityEventUncheckedEvent)
  }
}
