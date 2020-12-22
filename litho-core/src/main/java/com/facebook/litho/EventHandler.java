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
import com.facebook.rendercore.Function;

public class EventHandler<E> implements Function<Void> {

  public HasEventDispatcher mHasEventDispatcher;
  public final int id;
  public final @Nullable Object[] params;

  /* TODO: (T81557408) Fix @Nullable issue */
  protected EventHandler(@Nullable HasEventDispatcher hasEventDispatcher, int id) {
    this(hasEventDispatcher, id, null);
  }

  public EventHandler(
      @Nullable HasEventDispatcher hasEventDispatcher, int id, @Nullable Object[] params) {
    this.mHasEventDispatcher = hasEventDispatcher;
    this.id = id;
    this.params = params;
  }

  @Override
  public @Nullable Void call(Object... arguments) {
    dispatchEvent((E) arguments[0]);
    return null;
  }

  public void dispatchEvent(E event) {
    mHasEventDispatcher.getEventDispatcher().dispatchOnEvent(this, event);
  }

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

    if (id != other.id) {
      return false;
    }

    if (params == other.params) {
      return true;
    }

    if (params == null || other.params == null) {
      return false;
    }

    if (params.length != other.params.length) {
      return false;
    }

    // Deliberately skip the first param as it is a ComponentContext which will change between
    // EventHandlers.
    for (int i = 1; i < params.length; i++) {
      final Object object1 = params[i];
      final Object object2 = other.params[i];

      if (!(object1 == null ? object2 == null : object1.equals(object2))) {
        return false;
      }
    }

    return true;
  }
}
