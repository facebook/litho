/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

import java.util.ArrayList;

import android.support.annotation.VisibleForTesting;
import android.util.ArraySet;

/**
 * The core {@link DataFlowBinding} implementation: it specifies a set of {@link ValueNode}s and how
 * they're connected within a {@link DataFlowGraph}. When a GraphBinding is activated, it adds its
 * nodes (if they don't already exist) to the {@link DataFlowGraph} and connects them together. When
 * the binding is deactivated, these connections and nodes are removed. You can think of a
 * GraphBinding as a sub-graph of a DAG that can be added and removed in isolation.
 *
 * NB: ValueNodes can be referenced by multiple GraphBindings (e.g. a view property).
 */
public final class GraphBinding implements DataFlowBinding {

  private final DataFlowGraph mDataFlowGraph;
  private final BindingSpecs mBindingSpecs = new BindingSpecs();
  private final ArraySet<ValueNode> mAllNodes = new ArraySet<>();
  private boolean mIsActive = false;
  private boolean mHasBeenActivated = false;

  /**
   * Creates a {@link GraphBinding} associated with the default {@link DataFlowGraph} instance.
   */
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
    mBindingSpecs.addBinding(fromNode, toNode, name);
    mAllNodes.add(fromNode);
    mAllNodes.add(toNode);
  }

  public void addBinding(ValueNode fromNode, ValueNode toNode) {
    addBinding(fromNode, toNode, ValueNode.DEFAULT_INPUT);
  }

  /**
   * @return all nodes that have a binding defined in this {@link GraphBinding}.
   */
  ArraySet<ValueNode> getAllNodes() {
    return mAllNodes;
  }

  @Override
  public void activate() {
    mBindingSpecs.applyBindings();
    mHasBeenActivated = true;
    mIsActive = true;

    mDataFlowGraph.register(this);
  }

  @Override
  public void deactivate() {
    mIsActive = false;
    mDataFlowGraph.unregister(this);
    mBindingSpecs.removeBindings();
  }

  @Override
  public boolean isActive() {
    return mIsActive;
  }

  private static class BindingSpecs {

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
