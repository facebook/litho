/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import android.support.v4.util.Pools;

import com.facebook.litho.ComponentContext;
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
public abstract class ReferenceLifecycle<T> {

  private static final Pools.SynchronizedPool<Diff<?>> sDiffPool =
      new Pools.SynchronizedPool<>(20);

  protected abstract T onAcquire(
      ComponentContext context,
      Reference<T> reference);

  protected void onRelease(
      ComponentContext context,
      T value,
      Reference<T> reference) {
  }

  protected final <T> Diff<T> acquireDiff(T previousValue, T nextValue) {
    Diff diff =  sDiffPool.acquire();
    if (diff == null) {
      diff = new Diff();
    }

    diff.setPrevious(previousValue);
    diff.setNext(nextValue);

    return diff;
  }

  protected void releaseDiff(Diff diff) {
    sDiffPool.release(diff);
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
