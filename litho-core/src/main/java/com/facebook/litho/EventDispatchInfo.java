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
import com.facebook.infer.annotation.Nullsafe;

/**
 * Holds onto the mutable data used to dispatch an event. This data is kept up-to-date with the
 * latest version of the owning component or section by {@link EventHandlersController}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class EventDispatchInfo {

  public @Nullable HasEventDispatcher hasEventDispatcher;
  public @Nullable ComponentContext componentContext;

  // These should only be null for manually created EventHandlers (i.e. not using
  // Component.newEventHandler)
  public EventDispatchInfo(
      @Nullable HasEventDispatcher hasEventDispatcher,
      @Nullable ComponentContext componentContext) {
    this.hasEventDispatcher = hasEventDispatcher;
    this.componentContext = componentContext;
  }
}
