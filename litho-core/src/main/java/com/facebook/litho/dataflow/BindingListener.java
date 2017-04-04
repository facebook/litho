// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.dataflow;

/**
 * Listener that receives events when a {@link DataFlowBinding} is activated or is finished.
 */
public interface BindingListener {

  /**
   * Called when a {@link DataFlowBinding} is finished, meaning all of its nodes are finished.
   */
  void onAllNodesFinished(DataFlowBinding binding);
}
