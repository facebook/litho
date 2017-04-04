// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.dataflow;

/**
 * Interface for a ValueNode that has logic that determines when its finished. A node should be
 * considered finished when 1) its inputs are also finished (i.e. won't change) and 2) it won't
 * output any new values of its own.
 *
 * For example, a {@link SpringNode} is considered finished when its inputs are finished and the
 * spring is at rest. A {@link TimingNode} is considered finished when its inputs are finished and
 * it's reached the end of its duration.
 */
public interface NodeCanFinish {

  /**
   * Called by framework code to determine whether a node is finished.
   * @return whether this node is finished, which should factor in whether its parents are finished.
   */
  boolean isFinished();

  /**
   * Called once the first time all of this nodes inputs have finished.
   */
  void onInputsFinished();
}
