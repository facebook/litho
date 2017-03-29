/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Type for parameters that are logical outputs.
 */
public class Output<T> {
  private T mT;

  /**
   * Assumed thread-safe because the one write is before all the reads
   */
  @ThreadSafe(enableChecks = false)
  public void set(T t) {
    mT = t;
  }

  @ReturnsOwnership
