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

import androidx.annotation.VisibleForTesting;
import java.util.ArrayList;

/**
 * Defines the relationship of a set of input values to a set of output values where the values from
 * the input nodes 'flow into' the output nodes. For example, input values could be a touch X/Y or a
 * layout value, and output values could be the X/Y position of a View or its opacity. Input and
 * output values can be connected to each other via intermediate operators like springs or timing.
 *
 * <p>NB: ValueNodes can be referenced by multiple GraphBindings (e.g. a view property).
 */
public final class GraphBinding {

  private final DataFlowGraph mDataFlowGraph;
  private final Bindings mBindings = new Bindings();
  private final ArrayList<ValueNode> mAllNodes = new ArrayList<>();
  private BindingListener mListener;
  private boolean mIsActive = false;
  private boolean mHasBeenActivated = false;

  /** Creates a {@link GraphBinding} associated with the default {@link DataFlowGraph} instance. */
  public static GraphBinding create() {
    return new GraphBinding(DataFlowGraph.getInstance());
  }

  @VisibleForTesting
  public static GraphBinding create(DataFlowGraph dataFlowGraph) {
    return new GraphBinding(dataFlowGraph);
  }

  private GraphBinding(DataFlowGraph dataFlowGraph) {
    mDataFlowGraph = dataFlowGraph;
  }

  /**
   * Adds a connection between two nodes to this graph. The connection will not be actualized until
   * {@link #activate} is called, and will be removed once {@link #deactivate} is called.
   */
  public void addBinding(ValueNode fromNode, ValueNode toNode, String name) {
    if (mHasBeenActivated) {
      throw new RuntimeException(
          "Trying to add binding after DataFlowGraph has already been activated.");
    }
    mBindings.addBinding(fromNode, toNode, name);
    mAllNodes.add(fromNode);
    mAllNodes.add(toNode);
  }

  public void addBinding(ValueNode fromNode, ValueNode toNode) {
    addBinding(fromNode, toNode, ValueNode.DEFAULT_INPUT);
  }

  /** @return all nodes that have a binding defined in this {@link GraphBinding}. */
  ArrayList<ValueNode> getAllNodes() {
    return mAllNodes;
  }

  /**
   * Activates a binding, adding the sub-graph defined by this binding to the main {@link
   * DataFlowGraph} associated with this binding. This is expected to be called from framework code
   * and should not be called by the end developer.
   */
  public void activate() {
    mBindings.applyBindings();
    mHasBeenActivated = true;
    mIsActive = true;

    mDataFlowGraph.register(this);
  }

  /**
   * Deactivates this binding which, as you might guess, is the reverse of activating it: the
   * sub-graph associated with this binding is removed from the main {@link DataFlowGraph}. As with
   * {@link #activate()}, this is expected to only be called by framework code and not the end
   * developer.
   */
  public void deactivate() {
    if (!mIsActive) {
      return;
    }

    mIsActive = false;
    mDataFlowGraph.unregister(this);
    mBindings.removeBindings();
  }

  /** @return whether this binding has been activated and not yet deactivated. */
  public boolean isActive() {
    return mIsActive;
  }

  void notifyNodesHaveFinished() {
    if (mListener != null) {
      mListener.onAllNodesFinished(this);
    }
    deactivate();
  }

  /** Sets the {@link BindingListener}. */
  public void setListener(BindingListener listener) {
    if (mListener != null && listener != null) {
      throw new RuntimeException("Overriding existing listener!");
    }
    mListener = listener;
  }

  private static class Bindings {

    private final ArrayList<ValueNode> mFromNodes = new ArrayList<>();
    private final ArrayList<ValueNode> mToNodes = new ArrayList<>();
    private final ArrayList<String> mInputNames = new ArrayList<>();

    public void addBinding(ValueNode fromNode, ValueNode toNode, String name) {
      mFromNodes.add(fromNode);
      mToNodes.add(toNode);
      mInputNames.add(name);
    }

    public void applyBindings() {
      for (int i = 0; i < mFromNodes.size(); i++) {
        final ValueNode fromNode = mFromNodes.get(i);
        final ValueNode toNode = mToNodes.get(i);
        final String name = mInputNames.get(i);
        final ValueNode currentInput = toNode.getInputUnsafe(name);

        if (currentInput != null) {
          unbindNodes(currentInput, toNode, name);
        }

        fromNode.addOutput(toNode);
        toNode.setInput(name, fromNode);
      }
    }

    public void removeBindings() {
      for (int i = 0; i < mFromNodes.size(); i++) {
        final ValueNode fromNode = mFromNodes.get(i);
        final ValueNode toNode = mToNodes.get(i);
        final String name = mInputNames.get(i);

        // Nodes may have already been re-bound
        if (toNode.getInputUnsafe(name) == fromNode) {
          unbindNodes(fromNode, toNode, name);
        }
      }
    }

    private static void unbindNodes(ValueNode fromNode, ValueNode toNode, String name) {
      fromNode.removeOutput(toNode);
      toNode.removeInput(name);
    }
  }
}
