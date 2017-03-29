/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

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
