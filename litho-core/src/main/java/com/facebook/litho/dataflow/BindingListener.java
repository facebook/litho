// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.dataflow;

/**
 * Listener that receives events when a {@link GraphBinding} is activated or is finished.
 */
public interface BindingListener {

  /**
   * Called when a {@link GraphBinding} is finished, meaning all of its nodes are finished.
   */
  void onAllNodesFinished(GraphBinding binding);
}
