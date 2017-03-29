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
 * Defines the relationship of a set of input values to a set of output values where the values from
 * the input nodes 'flow into' the output nodes. For example, input values could be a touch X/Y or a
 * layout value, and output values could be the X/Y position of a View or its opacity. Input and
 * output values can be connected to each other via intermediate operators like springs or timing.
 *
 * A DataFlowBinding may represent a single one of these relationships, or a set of these
 * relationships.
 */
public interface DataFlowBinding {

  /**
   * Activates a binding, adding the sub-graph defined by this binding to the main
   * {@link DataFlowGraph} associated with this binding. This is expected to be called from
   * framework code and should not be called by the end developer.
   */
  void activate();

  /**
   * Deactivates this binding which, as you might guess, is the reverse of activating it: the
   * sub-graph associated with this binding is removed from the main {@link DataFlowGraph}. As with
   * {@link #activate()}, this is expected to only be called by framework code and not the end
   * developer.
   */
  void deactivate();

  /**
   * @return whether this binding has been activated and not yet deactivated.
   */
  boolean isActive();
}
