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

package com.facebook.litho.stats;

import java.util.concurrent.atomic.AtomicLong;

/** Provides global counters about Litho internals. Useful for performance analyses. */
public final class LithoStats {
  private static final AtomicLong sStateUpdates = new AtomicLong(0);
  private static final AtomicLong sStateUpdatesSync = new AtomicLong(0);
  private static final AtomicLong sStateUpdatesAsync = new AtomicLong(0);
  private static final AtomicLong sStateUpdatesLazy = new AtomicLong(0);

  /**
   * @return the global count of all applied state updates (async, lazy and sync) that have happened
   *     in the process.
   */
  public static long getAppliedStateUpdates() {
    return sStateUpdates.get();
  }

  /**
   * @return the global count of triggered synchronous state updates that have happened in the
   *     process.
   */
  public static long getStateUpdatesSync() {
    return sStateUpdatesSync.get();
  }

  /**
   * @return the global count of triggered asynchronous state updates that have happened in the
   *     process.
   */
  public static long getStateUpdatesAsync() {
    return sStateUpdatesAsync.get();
  }

  /** @return the global count of triggered lazy state updates that have happened in the process. */
  public static long getStateUpdatesLazy() {
    return sStateUpdatesLazy.get();
  }

  /**
   * Increment the count of all applied state updates by {@param num}.
   *
   * @return The new total number of all state updates recorded.
   */
  public static long incrementAppliedStateUpdatesBy(final long num) {
    return sStateUpdates.addAndGet(num);
  }

  /**
   * Increment the count of triggered synchronous state updates (by one).
   *
   * @return The new total number of synchronous state updates recorded.
   */
  public static long incrementStateUpdateSync() {
    return sStateUpdatesSync.addAndGet(1);
  }

  /**
   * Increment the count of triggered asynchronous state updates (by one).
   *
   * @return The new total number of asynchronous state updates recorded.
   */
  public static long incrementStateUpdateAsync() {
    return sStateUpdatesAsync.addAndGet(1);
  }

  /**
   * Increment the count of triggered lazy state updates (by one).
   *
   * @return The new total number of lazy state updates recorded.
   */
  public static long incrementStateUpdateLazy() {
    return sStateUpdatesLazy.addAndGet(1);
  }
}
