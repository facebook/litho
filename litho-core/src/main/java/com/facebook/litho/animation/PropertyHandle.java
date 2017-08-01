/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.animation;

/**
 * A pair of transition key and {@link AnimatedProperty} which can be used to identify a single
 * animating component property at runtime.
 */
final public class PropertyHandle {

  private final String mTransitionKey;
  private final AnimatedProperty mProperty;

  public PropertyHandle(String transitionKey, AnimatedProperty property) {
    mTransitionKey = transitionKey;
    mProperty = property;
  }

  public String getTransitionKey() {
    return mTransitionKey;
  }

  public AnimatedProperty getProperty() {
    return mProperty;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final PropertyHandle other = (PropertyHandle) o;
    return mTransitionKey.equals(other.mTransitionKey) && mProperty.equals(other.mProperty);
  }

  @Override
  public int hashCode() {
    return 31 * mTransitionKey.hashCode() + mProperty.hashCode();
  }

  @Override
  public String toString() {
    return "PropertyHandle{ mTransitionKey='" + mTransitionKey + "', mProperty=" + mProperty + "}";
  }
}
