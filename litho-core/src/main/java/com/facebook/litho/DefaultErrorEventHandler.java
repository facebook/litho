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

import androidx.annotation.Nullable;

/** Default implementation of ErrorEvent handler. */
public class DefaultErrorEventHandler extends ErrorEventHandler {

  static final DefaultErrorEventHandler INSTANCE = new DefaultErrorEventHandler();

  @Override
  public void onError(Exception e) {
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    } else {
      throw new RuntimeException(e);
    }
  }

  /** An event handler to be used for the root of the hierarchy that reraises error events. */
  private static class DefaultErrorEventDispatcher implements HasEventDispatcher {

    private static final DefaultErrorEventDispatcher INSTANCE = new DefaultErrorEventDispatcher();

    private static final EventDispatcher DISPATCHER_INSTANCE =
        new EventDispatcher() {
          @Override
          public @Nullable Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
            if (eventHandler.id == ComponentLifecycle.ERROR_EVENT_HANDLER_ID) {
              final Exception e = ((ErrorEvent) eventState).exception;
              if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
              } else {
                throw new RuntimeException(e);
              }
            }
            return null;
          }
        };

    private DefaultErrorEventDispatcher() {}

    @Override
    public EventDispatcher getEventDispatcher() {
      return DISPATCHER_INSTANCE;
    }
  }
}
