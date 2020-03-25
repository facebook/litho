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

package com.facebook.litho.testing;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore wrapper that automatically times out in 5 seconds so that we don't forget to do it and
 * hang tests or not check for whether the lock was successfully acquired.
 */
public class TimeOutSemaphore {

  private final Semaphore mSemaphore;

  public TimeOutSemaphore(int numPermits) {
    mSemaphore = new Semaphore(numPermits, true);
  }

  public void acquire() {
    try {
      if (!mSemaphore.tryAcquire(500, TimeUnit.SECONDS)) {
        throw new RuntimeException("Timed out!");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void release() {
    mSemaphore.release();
  }

  public void drainPermits() {
    mSemaphore.drainPermits();
  }
}
