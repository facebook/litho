/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.content.Context;

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
  T acquire(Context c, Component component);

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
  void maybePreallocateContent(Context c, Component component);
}
