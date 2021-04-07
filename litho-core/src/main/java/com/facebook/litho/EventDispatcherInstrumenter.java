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
import com.facebook.infer.annotation.Nullsafe;

/**
 * Plugin style that can be used for instrument before & after the event is dispatched by {@link
 * EventDispatcher#dispatchOnEvent(EventHandler, Object)}
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class EventDispatcherInstrumenter {
  public interface Instrumenter {

    boolean isTracing();

    @Nullable
    Object onBeginWork(EventHandler eventHandler, Object eventState);

    void onEndWork(@Nullable Object token);
  }

  @Nullable private static volatile Instrumenter sInstance;

  public static void provide(@Nullable Instrumenter instrumenter) {
    sInstance = instrumenter;
  }

  public static boolean isTracing() {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return false;
    }
    return instrumenter.isTracing();
  }

  @Nullable
  public static Object onBeginWork(EventHandler eventHandler, Object eventState) {
    final Instrumenter instrumenter = sInstance;
    if (!isTracing() || instrumenter == null) {
      return null;
    }
    return instrumenter.onBeginWork(eventHandler, eventState);
  }

  public static void onEndWork(@Nullable Object token) {
    final Instrumenter instrumenter = sInstance;
    if (!isTracing() || instrumenter == null || token == null) {
      return;
    }
    instrumenter.onEndWork(token);
  }
}
