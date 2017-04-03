/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.annotations.ShouldUpdate;

/**
 * Represents a diff between two values T. It should be used when defining the
 * {@link ShouldUpdate} callback in a ComponentSpec. a Diff
 * holds the previous and next value for a specific Prop for a ComponentSpec
 */
public class Diff<T> {

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

  public void setNext(T next) {
    mNext = next;
  }

  public void setPrevious(T previous) {
    mPrevious = previous;
  }

  void release() {
    mPrevious = null;
    mNext = null;
  }
}
