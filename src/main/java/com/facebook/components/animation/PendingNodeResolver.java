// Copyright 2004-present Facebook. All Rights Reserved.

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
