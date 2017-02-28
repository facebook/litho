// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.reference;

import android.support.v4.util.Pools;

import com.facebook.components.ComponentContext;
import com.facebook.components.Diff;

/**
 * ReferenceLifecycle objects which are able retreive resources at runtime without needing to keep
 * them constantly in memory. References should be used any time it's necessary to include a large
 * Object into a {@link com.facebook.components.Component} in order to limit the amount of
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
