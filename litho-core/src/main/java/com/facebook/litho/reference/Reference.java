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
import com.facebook.litho.ResourceResolver;

/**
 * Represents a unique instance of a reference that is driven by its matching
 * {@link ReferenceLifecycle} subclass. Use {@link Reference#acquire(ComponentContext, Reference)}
 * to acquire the underlying resource and
 * {@link Reference#release(ComponentContext, Object, Reference)} to release it when
 * it's not needed anymore.
 *
 * @deprecated Just use the object directly instead.
 */
@Deprecated
public abstract class Reference<L> {

  public abstract static class Builder<L> {
    protected ResourceResolver mResourceResolver;

    public abstract Reference<L> build();

    public final void init(ComponentContext c, Reference<L> reference) {
      mResourceResolver = ComponentsPools.acquireResourceResolver(c);
    }

    protected void release() {
      ComponentsPools.release(mResourceResolver);
      mResourceResolver = null;
    }
  }

  private final ReferenceLifecycle<L> mLifecycle;

  protected Reference(ReferenceLifecycle<L> lifecycle) {
    mLifecycle = lifecycle;
  }

  /**
   * Acquires a Reference of type T. It is responsibility of the caller to release the acquired
   * object by calling {@link Reference#release(ComponentContext, Object, Reference)}.
   * Calling acquire twice with the same reference does not guarantee that the same instance will
   * be returned twice.
   */
  public static <T> T acquire(
      ComponentContext context,
      Reference<T> reference) {
    return reference.mLifecycle.onAcquire(context, reference);
  }

  /**
   * Releases the object previously acquired by calling
   * {@link Reference#acquire(ComponentContext, Reference)}.
   * An object that was released calling this function should not be retained or used in any way.
   */
  public static <T> void release(
      ComponentContext context,
      T value,
      Reference<T> reference) {
    reference.mLifecycle.onRelease(context, value, reference);
  }

  public abstract String getSimpleName();

  /**
   * Checks whether acquiring object from two references will produce the same result.
   * This is implemented by default calling {@link Reference#equals(Object)}. When defining a custom
   * reference it's possible to provide custom logic for the comparison implementing a method
   * annotated with the {@link com.facebook.litho.annotations.ShouldUpdate} annotation.
   */
  public static <T> boolean shouldUpdate(Reference<T> previous, Reference<T> next) {
    if (previous != null) {
      return previous.mLifecycle.shouldReferenceUpdate(previous, next);
    } else {
      return next != null;
    }
  }
}
