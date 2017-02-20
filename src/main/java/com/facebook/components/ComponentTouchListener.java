// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.MotionEvent;
import android.view.View;

import static com.facebook.components.EventDispatcherUtils.dispatchOnTouch;

/**
 * Touch listener that triggers its underlying event handler.
 */
class ComponentTouchListener implements View.OnTouchListener {
  private EventHandler mEventHandler;

  EventHandler getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler eventHandler) {
    mEventHandler = eventHandler;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return mEventHandler != null && dispatchOnTouch(mEventHandler, v, event);
  }
}
