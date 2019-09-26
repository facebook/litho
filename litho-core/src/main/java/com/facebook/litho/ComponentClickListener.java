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

import static com.facebook.litho.EventDispatcherUtils.dispatchOnClick;

import android.view.View;

/** Click listener that triggers its underlying event handler. */
class ComponentClickListener implements View.OnClickListener {

  private EventHandler<ClickEvent> mEventHandler;

  @Override
  public void onClick(View view) {
    if (mEventHandler != null) {
      dispatchOnClick(mEventHandler, view);
    }
  }

  EventHandler<ClickEvent> getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler<ClickEvent> eventHandler) {
    mEventHandler = eventHandler;
  }
}
