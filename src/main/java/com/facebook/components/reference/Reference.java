/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.reference;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.ResourceResolver;

/**
 * Represents a unique instance of a reference that is driven by its matching
 * {@link ReferenceLifecycle} subclass. Use {@link Reference#acquire(ComponentContext, Reference)}
 * to acquire the underlying resource and
 * {@link Reference#release(ComponentContext, Object, Reference)} to release it when
 * it's not needed anymore.
 */
public abstract class Reference<L> {

  public static abstract class Builder<L> extends ResourceResolver {
    public abstract Reference<L> build();

    public final void init(ComponentContext c, Reference<L> reference) {
      super.init(c, c.getResourceCache());
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
