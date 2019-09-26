/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.dataflow;

/**
 * Interface for a ValueNode that has logic that determines when its finished. A node should be
 * considered finished when 1) its inputs are also finished (i.e. won't change) and 2) it won't
 * output any new values of its own.
 *
 * <p>For example, a {@link SpringNode} is considered finished when its inputs are finished and the
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
