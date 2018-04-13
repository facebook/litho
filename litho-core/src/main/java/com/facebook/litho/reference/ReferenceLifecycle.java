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

  protected final <R> Diff<R> acquireDiff(R previousValue, R nextValue) {
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
