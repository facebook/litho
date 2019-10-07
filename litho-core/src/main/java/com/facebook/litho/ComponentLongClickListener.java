/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import static com.facebook.litho.EventDispatcherUtils.dispatchOnLongClick;

import android.view.View;

/** Long click listener that triggers its underlying event handler. */
class ComponentLongClickListener implements View.OnLongClickListener {

  private EventHandler<LongClickEvent> mEventHandler;

  @Override
  public boolean onLongClick(View view) {
    if (mEventHandler != null) {
      return dispatchOnLongClick(mEventHandler, view);
    }

    return false;
  }

  EventHandler<LongClickEvent> getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(EventHandler<LongClickEvent> eventHandler) {
    mEventHandler = eventHandler;
  }
}
