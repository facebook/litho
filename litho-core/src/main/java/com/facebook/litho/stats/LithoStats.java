/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
  private static final AtomicLong mStateUpdates = new AtomicLong(0);

  /** @return the global count of state updates that have happened in the process. */
  public static long getStateUpdates() {
    return mStateUpdates.get();
  }

  /**
   * Increment the count of performed state updates by {@param num}.
   *
   * @return The new total number of state updates recorded.
   */
  public static long incStateUpdate(final long num) {
    return mStateUpdates.addAndGet(num);
  }
}
