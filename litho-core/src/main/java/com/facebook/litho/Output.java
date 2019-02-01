/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho;

import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadSafe;
import javax.annotation.Nullable;

/**
 * Type for parameters that are logical outputs.
 */
public class Output<T> {
  private @Nullable T mT;

  /** Assumed thread-safe because the one write is before all the reads */
  @ThreadSafe(enableChecks = false)
  public void set(@Nullable T t) {
    mT = t;
  }

  @ReturnsOwnership
  public @Nullable T get() {
    return mT;
  }
}
