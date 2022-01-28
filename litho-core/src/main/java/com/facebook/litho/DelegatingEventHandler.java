/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An event handler that takes two event handlers and calls both of them when an event is
 * dispatched.
 */
public class DelegatingEventHandler<E> extends EventHandler<E> {
  private final List<EventHandler<E>> mEventHandlers;

  protected DelegatingEventHandler(EventHandler<E> eventHandler1, EventHandler<E> eventHandler2) {
    super(null, -1);

    mEventHandlers = new ArrayList<>();
    mEventHandlers.add(eventHandler1);
    mEventHandlers.add(eventHandler2);
  }

  @Override
  public void dispatchEvent(E event) {
    for (int i = 0, length = mEventHandlers.size(); i < length; i++) {
      mEventHandlers.get(i).dispatchEvent(event);
    }
  }

  @Override
  public boolean isEquivalentTo(@Nullable EventHandler other) {
    if (this == other) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if (other.getClass() != getClass()) {
      return false;
    }

    List<EventHandler<E>> list = ((DelegatingEventHandler) other).mEventHandlers;
    int size = mEventHandlers.size();
    if (size != list.size()) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!mEventHandlers.get(i).isEquivalentTo(list.get(i))) {
        return false;
      }
    }
    return true;
  }

  public DelegatingEventHandler<E> addEventHandler(EventHandler<E> eventHandler) {
    mEventHandlers.add(eventHandler);
    return this;
  }
}
