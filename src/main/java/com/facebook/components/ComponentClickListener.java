// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.View;

import static com.facebook.components.EventDispatcherUtils.dispatchOnClick;

/**
 * Click listener that triggers its underlying event handler.
 */
class ComponentClickListener implements View.OnClickListener {

  private EventHandler mEventHandler;

  @Override
  public void onClick(View view) {
    if (mEventHandler != null) {
      dispatchOnClick(mEventHandler, view);
    }
  }

  EventHandler getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler eventHandler) {
    mEventHandler = eventHandler;
  }
}
