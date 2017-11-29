/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

/**
 * A pool dedicated to recycling mount content.
 *
 * <p>Note! This class MUST be implemented in a thread safe manner! See info in the javadocs below.
 */
public interface MountContentPool<T> extends PoolWithDebugInfo {

  /**
   * Return a recycled mount content, or a newly acquired mount content if there isn't one to
   * recycle. Should not return null.
   *
   * <p>NB: This can be called from multiple threads, possibly at the same time!
   */
  T acquire(ComponentContext c, ComponentLifecycle lifecycle);

  /**
   * Release the given mount content into the pool.
   *
   * <p>NB: This can be called from multiple threads, possibly at the same time!
   */
  void release(T item);

  /**
   * Called when a LayoutState that uses this mount content type may be used in the near future. The
   * pool is given a chance to create mount content and put it in the pool in anticipation of a
   * acquire call.
   *
   * <p>NB: This can be called from multiple threads, possibly at the same time!
   */
  void maybePreallocateContent(ComponentContext c, ComponentLifecycle lifecycle);
}
