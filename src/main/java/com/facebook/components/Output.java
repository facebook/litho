// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

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

  public T get() {
    return mT;
  }

  void release() {
    mT = null;
  }
}
