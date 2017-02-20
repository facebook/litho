// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.annotations.ShouldUpdate;

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
