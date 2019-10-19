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

public class HandlerInstrumenter {

  public interface Instrumenter {
    /**
     * Instrument a {@LithoHandler} or returns the same given {@LithoHandler}. If the
     * {@LithoHandler} given as input is null, the return value will be null too.
     */
    LithoHandler instrumentLithoHandler(LithoHandler lithoHandler);
  }

  private static volatile Instrumenter sInstance;

  public static void provide(Instrumenter instrumenter) {
    sInstance = instrumenter;
  }

  /** {@link Instrumenter#instrumentLithoHandler(com.facebook.litho.LithoHandler)} */
  public static LithoHandler instrumentLithoHandler(LithoHandler lithoHandler) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return lithoHandler;
    }

    return instrumenter.instrumentLithoHandler(lithoHandler);
  }
}
