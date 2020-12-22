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

import static com.facebook.litho.EventDispatcherUtils.dispatchOnTouch;

import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

/** Touch listener that triggers its underlying event handler. */
class ComponentTouchListener implements View.OnTouchListener {
  private @Nullable EventHandler<TouchEvent> mEventHandler;

  EventHandler<TouchEvent> getEventHandler() {
    return mEventHandler;
  }

  void setEventHandler(@Nullable EventHandler<TouchEvent> eventHandler) {
    mEventHandler = eventHandler;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return mEventHandler != null && dispatchOnTouch(mEventHandler, v, event);
  }
}
