// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

/**
 * The component responsible for driving the progress of a {@link DataFlowBinding} on each frame
 * once it's been activated. This is generally just an Android Choreographer but is abstracted out
 * for testing.
 */
public interface TimingSource {

  void setDataFlowGraph(DataFlowGraph dataFlowGraph);

  /**
   * Registers the {@link DataFlowGraph} to receive frame callbacks until it calls {@link #stop()}.
   */
  void start();

  /**
   * Stops the {@link DataFlowGraph} from receiving frame callbacks.
   */
  void stop();
}
