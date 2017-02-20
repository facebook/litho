// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.infer.annotation.AssumeThreadSafe;

/**
 * Type for parameters that are logical outputs.
 */
public class Output<T> {
  private T mT;

  @AssumeThreadSafe(because = "the one write is before all reads.")
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
