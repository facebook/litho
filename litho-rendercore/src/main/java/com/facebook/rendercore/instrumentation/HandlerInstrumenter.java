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

package com.facebook.rendercore.instrumentation;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.RunnableHandler;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class HandlerInstrumenter {

  public interface Instrumenter {
    /**
     * Instrument a {@link RunnableHandler} or return the same given {@link RunnableHandler}. If the
     * {@link RunnableHandler} given as input is null, the return value will be null too.
     */
    RunnableHandler instrumentHandler(RunnableHandler handler);
  }

  private static volatile @Nullable Instrumenter sInstrumenter;

  public static void provide(Instrumenter instrumenter) {
    sInstrumenter = instrumenter;
  }

  /** {@link Instrumenter#instrumentHandler} */
  public static RunnableHandler instrumentHandler(RunnableHandler handler) {
    final Instrumenter instrumenter = sInstrumenter;
    if (instrumenter == null) {
      return handler;
    }

    return instrumenter.instrumentHandler(handler);
  }
}
