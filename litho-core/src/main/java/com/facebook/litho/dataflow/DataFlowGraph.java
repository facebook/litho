/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pools;
import android.support.v4.util.SimpleArrayMap;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.internal.ArraySet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.concurrent.GuardedBy;

/**
 * A directed acyclic graph (DAG) created from one or more {@link GraphBinding}s. These component
 * GraphBindings define how nodes in this graph are connected to each other: GraphBindings can add
 * nodes and connections when they are 'activated' and can remove nodes and connections when they're
 * deactivated.
 *
 * Data flows through the graph on each frame, from input nodes to output nodes.
 */
public class DataFlowGraph {

  private static DataFlowGraph sInstance;

  public static DataFlowGraph getInstance() {
    if (sInstance == null) {
      final ChoreographerTimingSource timingSource = new ChoreographerTimingSource();
      sInstance = new DataFlowGraph(timingSource);
      timingSource.setDataFlowGraph(sInstance);
    }
    return sInstance;
  }

  private static final Pools.SynchronizedPool<NodeState> sNodeStatePool =
      new Pools.SynchronizedPool<>(20);

  private static class NodeState {

    private boolean isFinished = false;
    private int refCount = 0;

    void reset() {
      isFinished = false;
      refCount = 0;
    }
  }

  /**
   * For tests, let's the testing environment explicitly provide a specific DataFlowGraph instance
   * that can, for example, have a mocked TimingSource.
   */
  @VisibleForTesting
  public static void setInstance(DataFlowGraph dataFlowGraph) {
    sInstance = dataFlowGraph;
  }

  @VisibleForTesting
  public static DataFlowGraph create(TimingSource timingSource) {
    DataFlowGraph instance = new DataFlowGraph(timingSource);
    timingSource.setDataFlowGraph(instance);
    return instance;
  }

  @GuardedBy("this")
  private final TimingSource mTimingSource;

  @GuardedBy("this")
  private final ArrayList<GraphBinding> mBindings = new ArrayList<>();

  @GuardedBy("this")
  private final ArrayList<ValueNode> mSortedNodes = new ArrayList<>();

  @GuardedBy("this")
  private final SimpleArrayMap<ValueNode, NodeState> mNodeStates = new SimpleArrayMap<>();

  private boolean mIsDirty = false;

  private DataFlowGraph(TimingSource timingSource) {
    mTimingSource = timingSource;
  }

  /**
   * Adds an activated {@link GraphBinding}. This means that binding's nodes are added to the
   * existing graph and data will flow through them on the next frame.
   */
  public synchronized void register(GraphBinding binding) {
    if (!binding.isActive()) {
      throw new RuntimeException("Expected added GraphBinding to be active: " + binding);
    }
    mBindings.add(binding);
    registerNodes(binding);
    if (mBindings.size() == 1) {
      mTimingSource.start();
    }
    mIsDirty = true;
  }

  /**
   * Removes a {@link GraphBinding}. This means any nodes that only belonged to that binding will
   * be removed from the graph.
   */
  public synchronized void unregister(GraphBinding binding) {
    if (!mBindings.remove(binding)) {
      throw new RuntimeException("Tried to unregister non-existent binding");
    }
    unregisterNodes(binding);
    if (mBindings.isEmpty()) {
      mTimingSource.stop();
      mSortedNodes.clear();
      if (!mNodeStates.isEmpty()) {
        throw new RuntimeException("Failed to clean up all nodes");
      }
    }
    mIsDirty = true;
  }

  synchronized void doFrame(long frameTimeNanos) {
    if (mIsDirty) {
      regenerateSortedNodes();
    }

    propagate(frameTimeNanos);
    updateFinishedStates();
  }

  @GuardedBy("this")
  private void propagate(long frameTimeNanos) {
    final int size = mSortedNodes.size();
    for (int i = 0; i < size; i++) {
      final ValueNode node = mSortedNodes.get(i);
      node.doCalculateValue(frameTimeNanos);
    }
  }

  @GuardedBy("this")
  private void regenerateSortedNodes() {
    mSortedNodes.clear();

    if (mBindings.size() == 0) {
      return;
    }

    final ArraySet<ValueNode> leafNodes = ComponentsPools.acquireArraySet();
    final SimpleArrayMap<ValueNode, Integer> nodesToOutputsLeft = new SimpleArrayMap<>();

    for (int i = 0, bindingsSize = mBindings.size(); i < bindingsSize; i++) {
      final ArraySet<ValueNode> nodes = mBindings.get(i).getAllNodes();
      for (int j = 0, nodesSize = nodes.size(); j < nodesSize; j++) {
        final ValueNode node = nodes.valueAt(j);
        final int outputCount = node.getOutputCount();
        if (outputCount == 0) {
          leafNodes.add(node);
        } else {
          nodesToOutputsLeft.put(node, outputCount);
        }
      }
    }

    if (!nodesToOutputsLeft.isEmpty() && leafNodes.isEmpty()) {
      throw new DetectedCycleException(
          "Graph has nodes, but they represent a cycle with no leaf nodes!");
    }

    final ArrayDeque<ValueNode> nodesToProcess = ComponentsPools.acquireArrayDeque();
    nodesToProcess.addAll(leafNodes);

    while (!nodesToProcess.isEmpty()) {
      final ValueNode next = nodesToProcess.pollFirst();
      mSortedNodes.add(next);
      for (int i = 0, count = next.getInputCount(); i < count; i++) {
        final ValueNode input = next.getInputAt(i);
        final int outputsLeft = nodesToOutputsLeft.get(input) - 1;
        nodesToOutputsLeft.put(input, outputsLeft);
        if (outputsLeft == 0) {
          nodesToProcess.addLast(input);
        } else if (outputsLeft < 0) {
          throw new DetectedCycleException("Detected cycle.");
        }
      }
    }

    int expectedTotalNodes = nodesToOutputsLeft.size() + leafNodes.size();
    if (mSortedNodes.size() != expectedTotalNodes) {
      throw new DetectedCycleException(
          "Had unreachable nodes in graph -- this likely means there was a cycle");
    }

    Collections.reverse(mSortedNodes);
    mIsDirty = false;

    ComponentsPools.release(nodesToProcess);
    ComponentsPools.release(leafNodes);
  }

  @GuardedBy("this")
  private void updateFinishedStates() {
    updateFinishedNodes();
    notifyFinishedBindings();
  }

  @GuardedBy("this")
  private void updateFinishedNodes() {
    for (int i = 0, size = mSortedNodes.size(); i < size; i++) {
      final ValueNode node = mSortedNodes.get(i);
      final NodeState nodeState = mNodeStates.get(node);
      if (nodeState.isFinished || !areInputsFinished(node)) {
        continue;
      }

      final boolean nodeIsNowFinished =
          !(node instanceof NodeCanFinish) ||
              ((NodeCanFinish) node).isFinished();
      if (nodeIsNowFinished) {
        nodeState.isFinished = true;
      }
    }
  }

  @GuardedBy("this")
  private boolean areInputsFinished(ValueNode node) {
    for (int i = 0, inputCount = node.getInputCount(); i < inputCount; i++) {
      final NodeState nodeState = mNodeStates.get(node.getInputAt(i));
      if (!nodeState.isFinished) {
        return false;
      }
    }
    return true;
  }

  @GuardedBy("this")
  private void notifyFinishedBindings() {
    // Iterate in reverse order since notifying that a binding is finished results in removing
    // that binding.
    for (int i = mBindings.size() - 1; i >= 0; i--) {
      final GraphBinding binding = mBindings.get(i);
      boolean allAreFinished = true;
      final ArraySet<ValueNode> nodesToCheck = binding.getAllNodes();
      for (int j = 0, nodesSize = nodesToCheck.size(); j < nodesSize; j++) {
        final NodeState nodeState = mNodeStates.get(nodesToCheck.valueAt(j));
        if (!nodeState.isFinished) {
          allAreFinished = false;
          break;
        }
      }
      if (allAreFinished) {
        binding.notifyNodesHaveFinished();
      }
    }
  }

  @GuardedBy("this")
  private void registerNodes(GraphBinding binding) {
    final ArraySet<ValueNode> nodes = binding.getAllNodes();
    for (int i = 0, size = nodes.size(); i < size; i++) {
      final ValueNode node = nodes.valueAt(i);
      final NodeState nodeState = mNodeStates.get(node);
      if (nodeState != null) {
        nodeState.refCount++;
      } else {
        final NodeState newState = acquireNodeState();
        newState.refCount = 1;
        mNodeStates.put(node, newState);
      }
    }
  }

  @GuardedBy("this")
  private void unregisterNodes(GraphBinding binding) {
    final ArraySet<ValueNode> nodes = binding.getAllNodes();
    for (int i = 0, size = nodes.size(); i < size; i++) {
      final ValueNode node = nodes.valueAt(i);
      final NodeState nodeState = mNodeStates.get(node);
      nodeState.refCount--;
      if (nodeState.refCount == 0) {
        release(mNodeStates.remove(node));
      }
    }
  }

  private static NodeState acquireNodeState() {
    final NodeState fromPool = sNodeStatePool.acquire();
    if (fromPool != null) {
      return fromPool;
    }
    return new NodeState();
  }

  private static void release(NodeState nodeState) {
    nodeState.reset();
    sNodeStatePool.release(nodeState);
  }

  @VisibleForTesting
  @GuardedBy("this")
  boolean hasReferencesToNodes() {
    return !mBindings.isEmpty() || !mSortedNodes.isEmpty() || !mNodeStates.isEmpty();
  }
}
