/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.Diff;

/**
 * ReferenceLifecycle objects which are able retreive resources at runtime without needing to keep
 * them constantly in memory. References should be used any time it's necessary to include a large
 * Object into a {@link com.facebook.litho.Component} in order to limit the amount of
 * retained memory in ComponentTree.
 *
 * ReferenceLifecycle is the base class from which all the Reference types should inherit.
 * A ReferenceLifecycle should take care of both acquiring a resource given its {@link Reference}
 * and releasing/caching it for future use.
 */
@Deprecated
public abstract class ReferenceLifecycle<T> {

  protected abstract T onAcquire(
      ComponentContext context,
      Reference<T> reference);

  protected void onRelease(
      ComponentContext context,
      T value,
      Reference<T> reference) {
  }

  protected final <T> Diff<T> acquireDiff(T previousValue, T nextValue) {
    Diff diff = ComponentsPools.acquireDiff(previousValue, nextValue);

    return diff;
  }

  protected void releaseDiff(Diff diff) {
    ComponentsPools.release(diff);
  }

  protected boolean shouldUpdate(Reference<T> previous, Reference<T> next) {
    return !previous.equals(next);
  }

  public final boolean shouldReferenceUpdate(Reference<T> previous, Reference<T> next) {
    if (previous == null) {
      return next != null;
    } else if (next == null) {
      return true;
    }

    if (previous.getClass() != next.getClass()) {
      return true;
    }
    
    return shouldUpdate(previous, next);
  }
}
