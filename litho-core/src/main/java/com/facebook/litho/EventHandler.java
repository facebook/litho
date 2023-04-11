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

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.rendercore.Function;
import com.facebook.rendercore.primitives.Equivalence;

public class EventHandler<E> implements Function<Void>, Equivalence<EventHandler<E>> {

  public final int id;
  public EventDispatchInfo dispatchInfo;
  public final @Nullable Object[] params;

  protected EventHandler(@Nullable HasEventDispatcher hasEventDispatcher, int id) {
    this(hasEventDispatcher, id, null);
  }

  public EventHandler(
      @Nullable HasEventDispatcher hasEventDispatcher, int id, @Nullable Object[] params) {
    this.id = id;
    this.dispatchInfo = new EventDispatchInfo(hasEventDispatcher, null);
    this.params = params;
  }

  /**
   * The EventDispatchInfo ctors are used to construct EventHandlers from generated code. The
   * HasEventDispatcher ones above are mostly from manual construction of EventHandlers, e.g. in
   * tests.
   */
  public EventHandler(int id, EventDispatchInfo dispatchInfo) {
    this.id = id;
    this.dispatchInfo = dispatchInfo;
    this.params = null;
  }

  public EventHandler(int id, EventDispatchInfo dispatchInfo, @Nullable Object[] params) {
    this.id = id;
    this.dispatchInfo = dispatchInfo;
    this.params = params;
  }

  @Override
  public @Nullable Void call(Object... arguments) {
    dispatchEvent((E) arguments[0]);
    return null;
  }

  public void dispatchEvent(E event) {
    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onEvent:" + this);
    }
    Preconditions.checkNotNull(dispatchInfo.hasEventDispatcher)
        .getEventDispatcher()
        .dispatchOnEvent(this, event);
    if (isTracing) {
      ComponentsSystrace.endSection();
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

    for (int i = 0; i < params.length; i++) {
      final Object object1 = params[i];
      final Object object2 = other.params[i];

      if (!(object1 == null ? object2 == null : object1.equals(object2))) {
        return false;
      }
    }

    // Deliberately don't check ComponentContext which will change between EventHandlers.
    return true;
  }

  @Override
  public String toString() {
    final HasEventDispatcher hasEventDispatcher = dispatchInfo.hasEventDispatcher;
    return hasEventDispatcher != null && hasEventDispatcher != this
        ? hasEventDispatcher.toString()
        : super.toString();
  }
}
