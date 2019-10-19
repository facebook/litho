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

import javax.annotation.Nullable;

/**
 * An event handler that takes two event handlers and calls both of them when an event is
 * dispatched.
 */
public class DelegatingEventHandler<E> extends EventHandler<E> {
  private final EventHandler<E> mEventHandler1;
  private final EventHandler<E> mEventHandler2;

  protected DelegatingEventHandler(EventHandler<E> eventHandler1, EventHandler<E> eventHandler2) {
    super(null, -1);

    mEventHandler1 = eventHandler1;
    mEventHandler2 = eventHandler2;
  }

  @Override
  public void dispatchEvent(E event) {
    mEventHandler1.dispatchEvent(event);
    mEventHandler2.dispatchEvent(event);
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

    DelegatingEventHandler otherDelegating = (DelegatingEventHandler) other;

    return mEventHandler1.isEquivalentTo(otherDelegating.mEventHandler1)
        && mEventHandler2.isEquivalentTo(otherDelegating.mEventHandler2);
  }
}
