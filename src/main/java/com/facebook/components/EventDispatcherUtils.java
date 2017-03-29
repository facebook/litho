/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.os.Bundle;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import static com.facebook.litho.ThreadUtils.assertMainThread;

/**
 * This class contains utility methods to send pre-defined events
 * (click, touch, accessibility, etc.) to {@link EventHandler} instances' {@link Component}s
 */
class EventDispatcherUtils {

  private static ClickEvent sClickEvent;
  private static LongClickEvent sLongClickEvent;
  private static TouchEvent sTouchEvent;
  private static VisibleEvent sVisibleEvent;
  private static InvisibleEvent sInvisibleEvent;
  private static FocusedVisibleEvent sFocusedVisibleEvent;
  private static FullImpressionVisibleEvent sFullImpressionVisibleEvent;
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

  static void dispatchOnVisible(EventHandler<VisibleEvent> visibleHandler) {
    assertMainThread();

    if (sVisibleEvent == null) {
      sVisibleEvent = new VisibleEvent();
    }

    final EventDispatcher eventDispatcher = visibleHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(visibleHandler, sVisibleEvent);
  }

  static void dispatchOnFocused(EventHandler<FocusedVisibleEvent> focusedHandler) {
    assertMainThread();

    if (sFocusedVisibleEvent == null) {
      sFocusedVisibleEvent = new FocusedVisibleEvent();
    }

    final EventDispatcher eventDispatcher = focusedHandler.mHasEventDispatcher.getEventDispatcher();
    eventDispatcher.dispatchOnEvent(focusedHandler, sFocusedVisibleEvent);
