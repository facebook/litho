// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.view.View;

import static com.facebook.components.EventDispatcherUtils.dispatchOnLongClick;

/**
 * Long click listener that triggers its underlying event handler.
 */
class ComponentLongClickListener implements View.OnLongClickListener {

  private EventHandler mEventHandler;

  @Override
  public boolean onLongClick(View view) {
    if (mEventHandler != null) {
      return dispatchOnLongClick(mEventHandler, view);
    }

    return false;
  }

  EventHandler getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler eventHandler) {
    mEventHandler = eventHandler;
  }
}
