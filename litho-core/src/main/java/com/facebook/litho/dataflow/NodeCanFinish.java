/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

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
   * Called by framework code to determine whether a node is finished. This will only be called on
   * this node when all of its inputs are finished.
   *
   * @return whether this node is finished given that its inputs are finished.
   */
  boolean isFinished();
}
