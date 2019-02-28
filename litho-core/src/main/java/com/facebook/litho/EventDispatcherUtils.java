/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * This class contains utility methods to send pre-defined events
 * (click, touch, accessibility, etc.) to {@link EventHandler} instances' {@link Component}s
 */
class EventDispatcherUtils {

  private static ClickEvent sClickEvent;
  private static FocusChangedEvent sFocusChangedEvent;
  private static LongClickEvent sLongClickEvent;
  private static TouchEvent sTouchEvent;
  private static InterceptTouchEvent sInterceptTouchEvent;
  private static VisibleEvent sVisibleEvent;
  private static InvisibleEvent sInvisibleEvent;
  private static FocusedVisibleEvent sFocusedVisibleEvent;
  private static UnfocusedVisibleEvent sUnfocusedVisibleEvent;
  private static FullImpressionVisibleEvent sFullImpressionVisibleEvent;
  private static VisibilityChangedEvent sVisibleRectChangedEvent;
  private static DispatchPopulateAccessibilityEventEvent sDispatchPopulateAccessibilityEventEvent;
  private static OnInitializeAccessibilityEventEvent sOnInitializeAccessibilityEventEvent;
  private static OnInitializeAccessibilityNodeInfoEvent sOnInitializeAccessibilityNodeInfoEvent;
  private static OnPopulateAccessibilityEventEvent sOnPopulateAccessibilityEventEvent;
  private static OnRequestSendAccessibilityEventEvent sOnRequestSendAccessibilityEventEvent;
  private static PerformAccessibilityActionEvent sPerformAccessibilityActionEvent;
  private static SendAccessibilityEventEvent sSendAccessibilityEventEvent;
  private static SendAccessibilityEventUncheckedEvent sSendAccessibilityEventUncheckedEvent;

  static void dispatchOnClick(EventHandler<ClickEvent> clickHandler, View view) {
    assertMainThread();

    if (sClickEvent == null) {
      sClickEvent = new ClickEvent();
    }

    sClickEvent.view = view;

    final EventDispatcher eventDispatcher = clickHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(clickHandler, sClickEvent);

    sClickEvent.view = null;
  }

  static void dispatchOnFocusChanged(
      EventHandler<FocusChangedEvent> focusChangeHandler,
      View view,
      boolean hasFocus) {
    assertMainThread();

    if (sFocusChangedEvent == null) {
      sFocusChangedEvent = new FocusChangedEvent();
    }

    sFocusChangedEvent.view = view;
    sFocusChangedEvent.hasFocus = hasFocus;

    final EventDispatcher eventDispatcher =
        focusChangeHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(focusChangeHandler, sFocusChangedEvent);

    sFocusChangedEvent.view = null;
  }

  static void dispatchOnVisible(EventHandler<VisibleEvent> visibleHandler) {
    assertMainThread();

    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("EventDispatcherUtils.dispatchOnVisible");
    }

    if (sVisibleEvent == null) {
      sVisibleEvent = new VisibleEvent();
    }

    visibleHandler.dispatchEvent(sVisibleEvent);

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  static void dispatchOnFocused(EventHandler<FocusedVisibleEvent> focusedHandler) {
    assertMainThread();

    if (sFocusedVisibleEvent == null) {
      sFocusedVisibleEvent = new FocusedVisibleEvent();
    }

    focusedHandler.dispatchEvent(sFocusedVisibleEvent);
  }

  static void dispatchOnUnfocused(EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    assertMainThread();

    if (sUnfocusedVisibleEvent == null) {
      sUnfocusedVisibleEvent = new UnfocusedVisibleEvent();
    }

    unfocusedHandler.dispatchEvent(sUnfocusedVisibleEvent);
  }

  static void dispatchOnFullImpression(
      EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    assertMainThread();

    if (sFullImpressionVisibleEvent == null) {
      sFullImpressionVisibleEvent = new FullImpressionVisibleEvent();
    }

    fullImpressionHandler.dispatchEvent(sFullImpressionVisibleEvent);
  }

  static void dispatchOnInvisible(EventHandler<InvisibleEvent> invisibleHandler) {
    assertMainThread();

    if (sInvisibleEvent == null) {
      sInvisibleEvent = new InvisibleEvent();
    }

    invisibleHandler.dispatchEvent(sInvisibleEvent);
  }

  static void dispatchOnVisibilityChanged(
      EventHandler<VisibilityChangedEvent> visibilityChangedHandler,
      int visibleWidth,
      int visibleHeight,
      float percentVisibleWidth,
      float percentVisibleHeight) {
    assertMainThread();

    if (sVisibleRectChangedEvent == null) {
      sVisibleRectChangedEvent = new VisibilityChangedEvent();
    }

    sVisibleRectChangedEvent.visibleHeight = visibleHeight;
    sVisibleRectChangedEvent.visibleWidth = visibleWidth;
    sVisibleRectChangedEvent.percentVisibleHeight = percentVisibleHeight;
    sVisibleRectChangedEvent.percentVisibleWidth = percentVisibleWidth;

    visibilityChangedHandler.dispatchEvent(sVisibleRectChangedEvent);
  }

  static boolean dispatchOnLongClick(EventHandler<LongClickEvent> longClickHandler, View view) {
    assertMainThread();

    if (sLongClickEvent == null) {
      sLongClickEvent = new LongClickEvent();
    }

    sLongClickEvent.view = view;

    final EventDispatcher eventDispatcher =
        longClickHandler.mHasEventDispatcher.getEventDispatcher();
    final boolean returnValue =
        (boolean) eventDispatcher.dispatchOnEvent(longClickHandler, sLongClickEvent);

    sLongClickEvent.view = null;

    return returnValue;
  }

  static boolean dispatchOnTouch(
      EventHandler<TouchEvent> touchHandler,
      View view,
      MotionEvent event) {
    assertMainThread();

    if (sTouchEvent == null) {
      sTouchEvent = new TouchEvent();
    }

    sTouchEvent.view = view;
    sTouchEvent.motionEvent = event;

    final EventDispatcher eventDispatcher = touchHandler.mHasEventDispatcher.getEventDispatcher();
    final boolean returnValue =
        (boolean) eventDispatcher.dispatchOnEvent(touchHandler, sTouchEvent);

    sTouchEvent.view = null;
    sTouchEvent.motionEvent = null;

    return returnValue;
  }

  static boolean dispatchOnInterceptTouch(
      EventHandler<InterceptTouchEvent> interceptTouchHandler, View view, MotionEvent event) {
    assertMainThread();

    if (sInterceptTouchEvent == null) {
      sInterceptTouchEvent = new InterceptTouchEvent();
    }

    sInterceptTouchEvent.motionEvent = event;
    sInterceptTouchEvent.view = view;

    final EventDispatcher eventDispatcher =
        interceptTouchHandler.mHasEventDispatcher.getEventDispatcher();
    final boolean returnValue =
        (boolean) eventDispatcher.dispatchOnEvent(interceptTouchHandler, sInterceptTouchEvent);

    sInterceptTouchEvent.motionEvent = null;
    sInterceptTouchEvent.view = null;

    return returnValue;
  }

  static boolean dispatchDispatchPopulateAccessibilityEvent(
      EventHandler<DispatchPopulateAccessibilityEventEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sDispatchPopulateAccessibilityEventEvent == null) {
      sDispatchPopulateAccessibilityEventEvent = new DispatchPopulateAccessibilityEventEvent();
    }

    sDispatchPopulateAccessibilityEventEvent.host = host;
    sDispatchPopulateAccessibilityEventEvent.event = event;
    sDispatchPopulateAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    final boolean returnValue =  (boolean) eventDispatcher.dispatchOnEvent(
        eventHandler,
        sDispatchPopulateAccessibilityEventEvent);

    sDispatchPopulateAccessibilityEventEvent.host = null;
    sDispatchPopulateAccessibilityEventEvent.event = null;
    sDispatchPopulateAccessibilityEventEvent.superDelegate = null;

    return returnValue;
  }

  static void dispatchOnInitializeAccessibilityEvent(
      EventHandler<OnInitializeAccessibilityEventEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sOnInitializeAccessibilityEventEvent == null) {
      sOnInitializeAccessibilityEventEvent = new OnInitializeAccessibilityEventEvent();
    }

    sOnInitializeAccessibilityEventEvent.host = host;
    sOnInitializeAccessibilityEventEvent.event = event;
    sOnInitializeAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sOnInitializeAccessibilityEventEvent);

    sOnInitializeAccessibilityEventEvent.host = null;
    sOnInitializeAccessibilityEventEvent.event = null;
    sOnInitializeAccessibilityEventEvent.superDelegate = null;
  }

  static void dispatchOnInitializeAccessibilityNodeInfoEvent(
      EventHandler<OnInitializeAccessibilityNodeInfoEvent> eventHandler,
      View host,
      AccessibilityNodeInfoCompat info,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sOnInitializeAccessibilityNodeInfoEvent == null) {
      sOnInitializeAccessibilityNodeInfoEvent = new OnInitializeAccessibilityNodeInfoEvent();
    }

    sOnInitializeAccessibilityNodeInfoEvent.host = host;
    sOnInitializeAccessibilityNodeInfoEvent.info = info;
    sOnInitializeAccessibilityNodeInfoEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sOnInitializeAccessibilityNodeInfoEvent);

    sOnInitializeAccessibilityNodeInfoEvent.host = null;
    sOnInitializeAccessibilityNodeInfoEvent.info = null;
    sOnInitializeAccessibilityNodeInfoEvent.superDelegate = null;
  }

  static void dispatchOnPopulateAccessibilityEvent(
      EventHandler<OnPopulateAccessibilityEventEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sOnPopulateAccessibilityEventEvent == null) {
      sOnPopulateAccessibilityEventEvent = new OnPopulateAccessibilityEventEvent();
    }

    sOnPopulateAccessibilityEventEvent.host = host;
    sOnPopulateAccessibilityEventEvent.event = event;
    sOnPopulateAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sOnPopulateAccessibilityEventEvent);

    sOnPopulateAccessibilityEventEvent.host = null;
    sOnPopulateAccessibilityEventEvent.event = null;
    sOnPopulateAccessibilityEventEvent.superDelegate = null;
  }

  static boolean dispatchOnRequestSendAccessibilityEvent(
      EventHandler<OnRequestSendAccessibilityEventEvent> eventHandler,
      ViewGroup host,
      View child,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sOnRequestSendAccessibilityEventEvent == null) {
      sOnRequestSendAccessibilityEventEvent = new OnRequestSendAccessibilityEventEvent();
    }

    sOnRequestSendAccessibilityEventEvent.host = host;
    sOnRequestSendAccessibilityEventEvent.child = child;
    sOnRequestSendAccessibilityEventEvent.event = event;
    sOnRequestSendAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();

    final boolean returnValue = (boolean) eventDispatcher.dispatchOnEvent(
        eventHandler,
        sOnRequestSendAccessibilityEventEvent);

    sOnRequestSendAccessibilityEventEvent.host = null;
    sOnRequestSendAccessibilityEventEvent.child = null;
    sOnRequestSendAccessibilityEventEvent.event = null;
    sOnRequestSendAccessibilityEventEvent.superDelegate = null;

    return returnValue;
  }

  static boolean dispatchPerformAccessibilityActionEvent(
      EventHandler<PerformAccessibilityActionEvent> eventHandler,
      View host,
      int action,
      Bundle args,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sPerformAccessibilityActionEvent == null) {
      sPerformAccessibilityActionEvent = new PerformAccessibilityActionEvent();
    }

    sPerformAccessibilityActionEvent.host = host;
    sPerformAccessibilityActionEvent.action = action;
    sPerformAccessibilityActionEvent.args = args;
    sPerformAccessibilityActionEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    final boolean returnValue = (boolean) eventDispatcher.dispatchOnEvent(
        eventHandler,
        sPerformAccessibilityActionEvent);

    sPerformAccessibilityActionEvent.host = null;
    sPerformAccessibilityActionEvent.action = 0;
    sPerformAccessibilityActionEvent.args = null;
    sPerformAccessibilityActionEvent.superDelegate = null;

    return returnValue;
  }

  static void dispatchSendAccessibilityEvent(
      EventHandler<SendAccessibilityEventEvent> eventHandler,
      View host,
      int eventType,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sSendAccessibilityEventEvent == null) {
      sSendAccessibilityEventEvent = new SendAccessibilityEventEvent();
    }

    sSendAccessibilityEventEvent.host = host;
    sSendAccessibilityEventEvent.eventType = eventType;
    sSendAccessibilityEventEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sSendAccessibilityEventEvent);

    sSendAccessibilityEventEvent.host = null;
    sSendAccessibilityEventEvent.eventType = 0;
    sSendAccessibilityEventEvent.superDelegate = null;
  }

  static void dispatchSendAccessibilityEventUnchecked(
      EventHandler<SendAccessibilityEventUncheckedEvent> eventHandler,
      View host,
      AccessibilityEvent event,
      AccessibilityDelegateCompat superDelegate) {
    assertMainThread();

    if (sSendAccessibilityEventUncheckedEvent == null) {
      sSendAccessibilityEventUncheckedEvent = new SendAccessibilityEventUncheckedEvent();
    }

    sSendAccessibilityEventUncheckedEvent.host = host;
    sSendAccessibilityEventUncheckedEvent.event = event;
    sSendAccessibilityEventUncheckedEvent.superDelegate = superDelegate;

    final EventDispatcher eventDispatcher = eventHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(eventHandler, sSendAccessibilityEventUncheckedEvent);

    sSendAccessibilityEventUncheckedEvent.host = null;
    sSendAccessibilityEventUncheckedEvent.event = null;
    sSendAccessibilityEventUncheckedEvent.superDelegate = null;
  }
}
