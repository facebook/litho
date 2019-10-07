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

import androidx.annotation.VisibleForTesting;
import androidx.collection.SparseArrayCompat;
import com.facebook.infer.annotation.ThreadSafe;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Manages the mapping of event handlers to dispatchers. */
@ThreadSafe
public class EventHandlersController {

  private final Map<String, EventHandlersWrapper> mEventHandlers = new HashMap<>();

  /**
   * Update all the known event handlers for a dispatcher with the given key with the new dispacher
   * instance.
   */
  public synchronized void bindEventHandlers(
      ComponentContext c, HasEventDispatcher dispatcher, String globalKey) {
    if (globalKey == null) {
      return;
    }

    final EventHandlersWrapper eventHandlers = mEventHandlers.get(globalKey);

    if (eventHandlers == null) {
      return;
    }

    // Mark that the list of event handlers for this component is still needed.
    eventHandlers.mUsedInCurrentTree = true;
    eventHandlers.bindToDispatcher(c, dispatcher);
  }

  /** Remove entries for dispatchers that are no longer present in the tree. */
  public synchronized void clearUnusedEventHandlers() {
    final Iterator iterator = mEventHandlers.keySet().iterator();
    while (iterator.hasNext()) {
      final EventHandlersWrapper eventHandlersWrapper = mEventHandlers.get(iterator.next());

      if (eventHandlersWrapper == null || !eventHandlersWrapper.mUsedInCurrentTree) {
        iterator.remove();
      } else {
        eventHandlersWrapper.mUsedInCurrentTree = false;
      }
    }
  }

  /** Map the given event handler to a dispatcher with the given global key. */
  public synchronized void recordEventHandler(String globalKey, EventHandler eventHandler) {
    if (globalKey == null) {
      return;
    }

    EventHandlersWrapper eventHandlers = mEventHandlers.get(globalKey);

    if (eventHandlers == null) {
      eventHandlers = new EventHandlersWrapper();
      mEventHandlers.put(globalKey, eventHandlers);
    }

    eventHandlers.addEventHandler(eventHandler);
  }

  @VisibleForTesting
  public synchronized Map<String, EventHandlersWrapper> getEventHandlers() {
    return mEventHandlers;
  }

  /** Used to hold a dispatcher's event handlers. */
  @VisibleForTesting
  public static class EventHandlersWrapper {

    private final SparseArrayCompat<EventHandler> mEventHandlers = new SparseArrayCompat<>();

    boolean mUsedInCurrentTree;

    void addEventHandler(EventHandler eventHandler) {
      mEventHandlers.put(eventHandler.id, eventHandler);
    }

    void bindToDispatcher(ComponentContext c, HasEventDispatcher dispatcher) {
      for (int i = 0, size = mEventHandlers.size(); i < size; i++) {
        final EventHandler eventHandler = mEventHandlers.valueAt(i);
        eventHandler.mHasEventDispatcher = dispatcher;

        // Params should only be null for tests
        if (eventHandler.params != null) {
          eventHandler.params[0] = c;
        }
      }
    }

    @VisibleForTesting
    public SparseArrayCompat<EventHandler> getEventHandlers() {
      return mEventHandlers;
    }
  }
}
