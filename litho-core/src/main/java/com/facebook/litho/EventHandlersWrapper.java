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

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SparseArrayCompat;

/** Used to hold a component's event handlers. */
public class EventHandlersWrapper {

  @VisibleForTesting
  final SparseArrayCompat<EventHandler> eventHandlers = new SparseArrayCompat<>();

  boolean boundInCurrentLayout;

  void addEventHandler(EventHandler eventHandler) {
    this.eventHandlers.put(eventHandler.id, eventHandler);
  }

  void bindToDispatcherComponent(Component dispatcher) {
    for (int i = 0, size = eventHandlers.size(); i < size; i++) {
      final EventHandler eventHandler = eventHandlers.valueAt(i);
      eventHandler.mHasEventDispatcher = dispatcher;

      // Params should only be null for tests
      if (eventHandler.params != null) {
        eventHandler.params[0] = dispatcher.getScopedContext();
      }
    }
  }
}
