/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.EventDispatcherUtils.dispatchOnFocusChanged;

import android.view.View;

/**
 * Focus change listener that triggers its underlying event handler.
 */
class ComponentFocusChangeListener implements View.OnFocusChangeListener {

  private EventHandler<FocusChangedEvent> mEventHandler;

  @Override
  public void onFocusChange(View view, boolean b) {
    if (mEventHandler != null) {
      dispatchOnFocusChanged(mEventHandler, view, b);
    }
  }

  EventHandler<FocusChangedEvent> getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler<FocusChangedEvent> eventHandler) {
    mEventHandler = eventHandler;
  }
}
