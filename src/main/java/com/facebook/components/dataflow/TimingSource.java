/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

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
