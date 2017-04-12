/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.ValueNode;

/**
 * Interface for a class that can take a {@link PendingNode} and turn it into a concrete
 * {@link ValueNode}.
 */
public interface PendingNodeResolver {

  /**
   * Implementations are expected to take the given {@link PendingNode} and return an appropriate
   * concrete {@link ValueNode}.
   */
  ValueNode resolve(PendingNode pendingNode);
}
