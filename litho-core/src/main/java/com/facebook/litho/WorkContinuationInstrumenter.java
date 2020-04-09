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

/**
 * Utility class that allows to capture work continuation updates. Client code can specify a custom
 * {@link Instrumenter} that will receive ad-hoc updates when work that has to be executed across
 * threads gets moved around.
 */
public final class WorkContinuationInstrumenter {

  /** Allows to record work being continued across threads. */
  public interface Instrumenter {

    /**
     * Allows to know in advance if the custom instrumenter desires to receive continuation updates.
     *
     * @return true to specify interest in handling the updates, false otherwise.
     */
    boolean isTracing();

    /**
     * Captures when asking for work to be stolen.
     *
     * @param tag name.
     * @return a token object that allows to track the continuation.
     */
    @Nullable
    Object onAskForWorkToContinue(String tag);
    /**
     * Tracks when some work is ready to offered for continuation.
     *
     * @param tag name.
     * @return a token object that allows to track the continuation.
     */
    @Nullable
    Object onOfferWorkForContinuation(String tag);

    /**
     * Tracks when some work is ready to be stolen.
     *
     * @param tag name (optional).
     * @param token returned by {@link Instrumenter#onAskForWorkToContinue}.
     * @return a token object that allows to track the continuation.
     */
    @Nullable
    Object onOfferWorkForContinuation(String tag, Object token);

    /**
     * Captures the beginning of the continuation for stolen work.
     *
     * @param tag name (optional).
     * @param token returned by {@link Instrumenter#onOfferWorkForContinuation}.
     * @return a token object that allows to track the continuation.
     */
    @Nullable
    Object onBeginWorkContinuation(String tag, Object token);

    /**
     * Captures the end of the continuation for stolen work.
     *
     * @param token returned by {@link Instrumenter#onBeginWorkContinuation}.
     */
    void onEndWorkContinuation(Object token);

    /**
     * Reports a failure while executing work.
     *
     * <p><note>{@link Instrumenter#onEndWorkContinuation(Object)} (Object)} still needs to be
     * invoked.
     *
     * @param token returned by {@link Instrumenter#onBeginWorkContinuation(String, Object)}
     *     (Object, String)}.
     * @param th containing the failure.
     */
    void markFailure(Object token, Throwable th);
  }

  @Nullable private static volatile Instrumenter sInstance;

  /**
   * Allows to provide an instrumenter that will receive work continuation updates.
   *
   * @param instrumenter that will receive the updates or null to reset.
   */
  public static void provide(@Nullable Instrumenter instrumenter) {
    sInstance = instrumenter;
  }

  static boolean isTracing() {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return false;
    }
    return instrumenter.isTracing();
  }

  @Nullable
  public static Object onAskForWorkToContinue(String tag) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return null;
    }
    return instrumenter.onAskForWorkToContinue(tag);
  }

  @Nullable
  public static Object onOfferWorkForContinuation(String tag) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return null;
    }
    return instrumenter.onOfferWorkForContinuation(tag);
  }

  @Nullable
  public static Object onOfferWorkForContinuation(String tag, @Nullable Object token) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return null;
    }
    return instrumenter.onOfferWorkForContinuation(tag, token);
  }

  @Nullable
  public static Object onBeginWorkContinuation(String tag, @Nullable Object token) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return null;
    }
    return instrumenter.onBeginWorkContinuation(tag, token);
  }

  public static void onEndWorkContinuation(@Nullable Object token) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return;
    }
    instrumenter.onEndWorkContinuation(token);
  }

  public static void markFailure(@Nullable Object token, Throwable th) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return;
    }
    instrumenter.markFailure(token, th);
  }
}
