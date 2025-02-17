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

import static com.facebook.rendercore.debug.DebugEventAttribute.Source;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.litho.annotations.EventHandlerRebindMode;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.debug.DebugInfoReporter;
import com.facebook.rendercore.Equivalence;
import com.facebook.rendercore.Function;
import com.facebook.rendercore.thread.utils.ThreadUtils;
import com.facebook.rendercore.utils.CommonUtils;
import kotlin.Unit;

public class EventHandler<E> implements Function<Void>, Equivalence<EventHandler<E>> {

  public static final String UnboundEventHandler = "UnboundEventHandler";

  public final int id;
  public final EventHandlerRebindMode mode;
  public EventDispatchInfo dispatchInfo;
  public final @Nullable Object[] params;

  public EventHandler(
      final int id,
      final EventHandlerRebindMode mode,
      final EventDispatchInfo dispatchInfo,
      final @Nullable Object[] params) {
    this.id = id;
    this.mode = mode;
    this.dispatchInfo = dispatchInfo;
    this.params = params;
  }

  @Override
  public @Nullable Void call(Object... arguments) {
    dispatchEvent((E) arguments[0]);
    return null;
  }

  public @Nullable Object dispatchEvent(E event) {

    // Log unbound event handler dispatches
    if (ComponentsConfiguration.isEventHandlerRebindLoggingEnabled) {
      final EventDispatchInfo info = dispatchInfo;
      final boolean isUnbound = (info == null) || !info.isBound;
      if (isUnbound && mode != EventHandlerRebindMode.NONE) {
        DebugInfoReporter.report(
            UnboundEventHandler,
            (attribute) -> {
              attribute.put("event", CommonUtils.getSectionNameForTracing(event.getClass()));
              attribute.put(Source, this.toString());
              attribute.put("hasDispatchInfo", info != null);
              return Unit.INSTANCE;
            });
      }

      if (mode == EventHandlerRebindMode.REBIND && !ThreadUtils.isMainThread()) {
        DebugInfoReporter.report(
            "EventHandlingViolation",
            (attribute) -> {
              attribute.put("error", "DispatchedOnWrongThread");
              attribute.put("event", CommonUtils.getSectionNameForTracing(event.getClass()));
              attribute.put(Source, this.toString());
              return Unit.INSTANCE;
            });
      }
    }

    boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onEvent:" + this);
    }
    final @Nullable Object result =
        Preconditions.checkNotNull(dispatchInfo.hasEventDispatcher)
            .getEventDispatcher()
            .dispatchOnEvent(this, event);
    if (isTracing) {
      ComponentsSystrace.endSection();
    }

    return result;
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

    if (mode != other.mode) {
      return false;
    }

    final boolean useNonRebindingEventHandlers =
        dispatchInfo != null
            && dispatchInfo.componentContext != null
            && dispatchInfo.componentContext.shouldUseNonRebindingEventHandlers();
    if (useNonRebindingEventHandlers && dispatchInfo != other.dispatchInfo) {
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
    return CommonUtils.getSectionNameForTracing(
        hasEventDispatcher != null && hasEventDispatcher != this
            ? hasEventDispatcher.getClass()
            : getClass());
  }
}
