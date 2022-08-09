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
import com.facebook.infer.annotation.Nullsafe;

/**
 * This class acts as wrapper to any kind of {@link EventHandler} which allows a {@link
 * ComponentContext} to understand when a specific event handling starts and finishes.
 *
 * <p>This is a core piece to batched state updates, since it allow us to delay any layout
 * calculation until the moment we know the event was handled.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class ComponentCallbackLayoutEventHandler<E> extends EventHandler<E>
    implements HasEventDispatcher, EventDispatcher {

  protected final EventHandler<E> mDelegateEventHandler;
  private final ComponentContext mContext;
  private final ComponentCallbackType mComponentCallbackType;

  public ComponentCallbackLayoutEventHandler(
      ComponentCallbackType componentCallbackType,
      EventHandler<E> delegateEventHandler,
      ComponentContext context) {
    super(null, delegateEventHandler.id);
    mDelegateEventHandler = delegateEventHandler;
    dispatchInfo.hasEventDispatcher = this;
    mContext = context;
    mComponentCallbackType = componentCallbackType;
  }

  @Nullable
  @Override
  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    try {
      mContext.registerComponentCallbackStart(mComponentCallbackType);

      EventDispatcher mDelegateEventDispatcher =
          Preconditions.checkNotNull(mDelegateEventHandler.dispatchInfo.hasEventDispatcher)
              .getEventDispatcher();
      return mDelegateEventDispatcher.dispatchOnEvent(eventHandler, eventState);
    } finally {
      mContext.registerComponentCallbackEnd(mComponentCallbackType);
    }
  }

  @Override
  public EventDispatcher getEventDispatcher() {
    return this;
  }

  @Override
  public boolean isEquivalentTo(@Nullable EventHandler other) {
    if (other instanceof ComponentCallbackLayoutEventHandler) {
      return mDelegateEventHandler.isEquivalentTo(
          ((ComponentCallbackLayoutEventHandler<?>) other).mDelegateEventHandler);
    }

    return mDelegateEventHandler.isEquivalentTo(other);
  }
}
