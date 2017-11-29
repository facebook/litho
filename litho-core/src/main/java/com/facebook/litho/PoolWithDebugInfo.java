/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

/** A object pool that has debug info for display in tools like Stetho. */
public interface PoolWithDebugInfo {

  /** @return a human-readable name for the pool. */
  String getName();

  /** @return the max number of objects this pool will hold. */
  int getMaxSize();

  /** @return the number of objects currently in the pool. */
  int getCurrentSize();
}
