/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.annotation.VisibleForTesting;
import com.facebook.litho.annotations.ShouldUpdate;

/**
 * Represents a diff between two values T. It should be used when defining the
 * {@link ShouldUpdate} callback in a ComponentSpec. a Diff
 * holds the previous and next value for a specific Prop for a ComponentSpec
 */
public final class Diff<T> {

  T mPrevious;
  T mNext;

  public Diff() {

  }

  public T getPrevious() {
    return mPrevious;
  }

  public T getNext() {
    return mNext;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void init(T previous, T next) {
    mPrevious = previous;
    mNext = next;
  }

  void release() {
    mPrevious = null;
    mNext = null;
  }

  @Override
  public String toString() {
    return "Diff{" + "mPrevious=" + mPrevious + ", mNext=" + mNext + '}';
  }
}
