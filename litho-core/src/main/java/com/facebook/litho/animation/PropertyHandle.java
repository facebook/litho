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
package com.facebook.litho.animation;

import com.facebook.litho.TransitionId;

/**
 * A pair of transition key and {@link AnimatedProperty} which can be used to identify a single
 * animating component property at runtime.
 */
final public class PropertyHandle {

  private final TransitionId mTransitionId;
  private final AnimatedProperty mProperty;

  public PropertyHandle(TransitionId transitionId, AnimatedProperty property) {
    mTransitionId = transitionId;
    mProperty = property;
  }

  public TransitionId getTransitionId() {
    return mTransitionId;
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
    return mTransitionId.equals(other.mTransitionId) && mProperty.equals(other.mProperty);
  }

  @Override
  public int hashCode() {
    return 31 * mTransitionId.hashCode() + mProperty.hashCode();
  }

  @Override
  public String toString() {
    return "PropertyHandle{ mTransitionId='" + mTransitionId + "', mProperty=" + mProperty + "}";
  }
}
