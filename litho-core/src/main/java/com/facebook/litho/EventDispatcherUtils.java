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

package com.facebook.litho;

import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.util.Preconditions;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * This class contains utility methods to send pre-defined events (click, touch, accessibility,
 * etc.) to {@link EventHandler} instances' {@link Component}s
 */
class EventDispatcherUtils {

  static void dispatchOnClick(EventHandler<ClickEvent> clickHandler, View view) {
    assertMainThread();

    final ClickEvent clickEvent = new ClickEvent();
    clickEvent.view = view;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(clickHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(clickHandler, clickEvent);
  }

  static void dispatchOnFocusChanged(
      EventHandler<FocusChangedEvent> focusChangeHandler, View view, boolean hasFocus) {
    assertMainThread();

    final FocusChangedEvent focusChangedEvent = new FocusChangedEvent();
    focusChangedEvent.view = view;
    focusChangedEvent.hasFocus = hasFocus;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(focusChangeHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(focusChangeHandler, focusChangedEvent);
  }

  static boolean dispatchOnLongClick(EventHandler<LongClickEvent> longClickHandler, View view) {
    assertMainThread();

    final LongClickEvent longClickEvent = new LongClickEvent();
    longClickEvent.view = view;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(longClickHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    final Object returnValue = eventDispatcher.dispatchOnEvent(longClickHandler, longClickEvent);

    return returnValue != null && (boolean) returnValue;
  }

  static boolean dispatchOnTouch(
      EventHandler<TouchEvent> touchHandler, View view, MotionEvent event) {
    assertMainThread();

    final TouchEvent touchEvent = new TouchEvent();
    touchEvent.view = view;
    touchEvent.motionEvent = event;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(touchHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    final Object returnValue = eventDispatcher.dispatchOnEvent(touchHandler, touchEvent);

    return returnValue != null && (boolean) returnValue;
  }

  static boolean dispatchOnInterceptTouch(
      EventHandler<InterceptTouchEvent> interceptTouchHandler, View view, MotionEvent event) {
    assertMainThread();

    final InterceptTouchEvent interceptTouchEvent = new InterceptTouchEvent();
    interceptTouchEvent.motionEvent = event;
    interceptTouchEvent.view = view;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(interceptTouchHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    final Object returnValue =
        eventDispatcher.dispatchOnEvent(interceptTouchHandler, interceptTouchEvent);

    return returnValue != null && (boolean) returnValue;
  }

  static boolean dispatchDispatchPopulateAccessibilityEvent(
      EventHandler<DispatchPopulateAccessibilityEventEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final DispatchPopulateAccessibilityEventEvent dispatchPopulateAccessibilityEventEvent =
        new DispatchPopulateAccessibilityEventEvent();
    dispatchPopulateAccessibilityEventEvent.host = host;
    dispatchPopulateAccessibilityEventEvent.event = event;
    dispatchPopulateAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    final Object returnValue =
        eventDispatcher.dispatchOnEvent(eventHandler, dispatchPopulateAccessibilityEventEvent);

    return returnValue != null && (boolean) returnValue;
  }

  static void dispatchOnInitializeAccessibilityEvent(
      EventHandler<OnInitializeAccessibilityEventEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final OnInitializeAccessibilityEventEvent onInitializeAccessibilityEventEvent =
        new OnInitializeAccessibilityEventEvent();
    onInitializeAccessibilityEventEvent.host = host;
    onInitializeAccessibilityEventEvent.event = event;
    onInitializeAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, onInitializeAccessibilityEventEvent);
  }

  static void dispatchOnInitializeAccessibilityNodeInfoEvent(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent> eventHandler,
      View host,
      AccessibilityNodeInfoCompat info,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final OnInitializeAccessibilityNodeInfoEvent onInitializeAccessibilityNodeInfoEvent =
        new OnInitializeAccessibilityNodeInfoEvent();
    onInitializeAccessibilityNodeInfoEvent.host = host;
    onInitializeAccessibilityNodeInfoEvent.info = info;
    onInitializeAccessibilityNodeInfoEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, onInitializeAccessibilityNodeInfoEvent);
  }

  static void dispatchOnPopulateAccessibilityEvent(
      EventHandler<OnPopulateAccessibilityEventEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final OnPopulateAccessibilityEventEvent onPopulateAccessibilityEventEvent =
        new OnPopulateAccessibilityEventEvent();
    onPopulateAccessibilityEventEvent.host = host;
    onPopulateAccessibilityEventEvent.event = event;
    onPopulateAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, onPopulateAccessibilityEventEvent);
  }

  static void dispatchOnPopulateAccessibilityNode(
      EventHandler<OnPopulateAccessibilityNodeEvent> eventHandler,
      View host,
      AccessibilityNodeInfoCompat info) {
    assertMainThread();

    final OnPopulateAccessibilityNodeEvent onPopulateAccessibilityNodeEvent =
        new OnPopulateAccessibilityNodeEvent();
    onPopulateAccessibilityNodeEvent.host = host;
    onPopulateAccessibilityNodeEvent.accessibilityNode = info;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, onPopulateAccessibilityNodeEvent);
  }

  static boolean dispatchOnRequestSendAccessibilityEvent(
      EventHandler<OnRequestSendAccessibilityEventEvent> eventHandler,
      ViewGroup host,
      View child,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final OnRequestSendAccessibilityEventEvent onRequestSendAccessibilityEventEvent =
        new OnRequestSendAccessibilityEventEvent();
    onRequestSendAccessibilityEventEvent.host = host;
    onRequestSendAccessibilityEventEvent.child = child;
    onRequestSendAccessibilityEventEvent.event = event;
    onRequestSendAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();

    final Object returnValue =
        eventDispatcher.dispatchOnEvent(eventHandler, onRequestSendAccessibilityEventEvent);

    return returnValue != null && (boolean) returnValue;
  }

  static boolean dispatchPerformAccessibilityActionEvent(
      EventHandler<PerformAccessibilityActionEvent> eventHandler,
      View host,
      int action,
      Bundle args,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final PerformAccessibilityActionEvent performAccessibilityActionEvent =
        new PerformAccessibilityActionEvent();
    performAccessibilityActionEvent.host = host;
    performAccessibilityActionEvent.action = action;
    performAccessibilityActionEvent.args = args;
    performAccessibilityActionEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    final Object returnValue =
        eventDispatcher.dispatchOnEvent(eventHandler, performAccessibilityActionEvent);

    return returnValue != null && (boolean) returnValue;
  }

  static void dispatchSendAccessibilityEvent(
      EventHandler<SendAccessibilityEventEvent> eventHandler,
      View host,
      int eventType,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final SendAccessibilityEventEvent sendAccessibilityEventEvent =
        new SendAccessibilityEventEvent();
    sendAccessibilityEventEvent.host = host;
    sendAccessibilityEventEvent.eventType = eventType;
    sendAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sendAccessibilityEventEvent);
  }

  static void dispatchSendAccessibilityEventUnchecked(
      EventHandler<SendAccessibilityEventUncheckedEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    final SendAccessibilityEventUncheckedEvent sendAccessibilityEventUncheckedEvent =
        new SendAccessibilityEventUncheckedEvent();
    sendAccessibilityEventUncheckedEvent.host = host;
    sendAccessibilityEventUncheckedEvent.event = event;
    sendAccessibilityEventUncheckedEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher =
        Preconditions.checkNotNull(eventHandler.dispatchInfo.hasEventDispatcher)
            .getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sendAccessibilityEventUncheckedEvent);
  }
}
