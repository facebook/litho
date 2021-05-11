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

import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.RunnableFuture;

/**
 * Provides common instrumentation for {@link java.util.concurrent.Future}(s) and related
 * implementations.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public final class FutureInstrumenter {

  public interface Instrumenter {

    /**
     * Hook that allows to instrument a {@RunnableFuture}.
     *
     * @param future that has to be instrumented
     * @param tag used to mark the task for debugging purposes.
     * @param <V>
     * @return an instrumented {@link RunnableFuture} or returns the given input one.
     */
    <V> RunnableFuture<V> instrument(RunnableFuture<V> future, String tag);
  }

  private static volatile FutureInstrumenter.Instrumenter sInstance;

  public static void provide(FutureInstrumenter.Instrumenter instrumenter) {
    sInstance = instrumenter;
  }

  public static <V> RunnableFuture<V> instrument(RunnableFuture<V> future, String tag) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return future;
    }

    return instrumenter.instrument(future, tag);
  }
}
