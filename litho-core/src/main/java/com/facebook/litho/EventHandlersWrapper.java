/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import java.util.ArrayList;
import java.util.List;

/** Used to hold a component's event handlers. */
public class EventHandlersWrapper {

  private final List<EventHandler> eventHandlers = new ArrayList<>();
  boolean boundInCurrentLayout;

  void addEventHandler(EventHandler eventHandler) {
    this.eventHandlers.add(eventHandler);
  }

  void bindToDispatcherComponent(Component dispatcher) {
    for (int i = 0, size = eventHandlers.size(); i < size; i++) {
      final EventHandler eventHandler = eventHandlers.get(i);
      eventHandler.mHasEventDispatcher = dispatcher;

      // Params should only be null for tests
      if (eventHandler.params != null) {
        eventHandler.params[0] = dispatcher.getScopedContext();
      }
    }
  }
}
