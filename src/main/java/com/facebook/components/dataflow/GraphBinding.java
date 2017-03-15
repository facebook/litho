// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

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
   * {@link #activate()} is called, and will be removed once {@link #deactivate()} is called.
   */
  public void addBinding(ValueNode fromNode, ValueNode toNode) {
    if (mHasBeenActivated) {
      throw new RuntimeException(
          "Trying to add binding after DataFlowGraph has already been activated.");
    }
    mBindingSpecs.addBinding(fromNode, toNode);
    mAllNodes.add(fromNode);
    mAllNodes.add(toNode);
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

    public void addBinding(ValueNode fromNode, ValueNode toNode) {
      mFromNodes.add(fromNode);
      mToNodes.add(toNode);
    }

    public void applyBindings() {
      for (int i = 0; i < mFromNodes.size(); i++) {
        final ValueNode fromNode = mFromNodes.get(i);
        final ValueNode toNode = mToNodes.get(i);

        fromNode.addOutput(toNode);
        toNode.setInput(fromNode);
      }
    }

    public void removeBindings() {
      for (int i = 0; i < mFromNodes.size(); i++) {
        final ValueNode fromNode = mFromNodes.get(i);
        final ValueNode toNode = mToNodes.get(i);

        fromNode.removeOutput(toNode);
        toNode.setInput(null);
      }
    }
  }
}
