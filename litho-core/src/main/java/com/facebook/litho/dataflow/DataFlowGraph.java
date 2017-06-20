/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;

import com.facebook.litho.ComponentsPools;
import com.facebook.litho.internal.ArraySet;

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

  @VisibleForTesting
  public static DataFlowGraph create(TimingSource timingSource) {
    DataFlowGraph instance = new DataFlowGraph(timingSource);
    timingSource.setDataFlowGraph(instance);
    return instance;
  }

  private final TimingSource mTimingSource;
  private final CopyOnWriteArrayList<GraphBinding> mBindings = new CopyOnWriteArrayList<>();
  private final ArrayList<ValueNode> mSortedNodes = new ArrayList<>();
  private final ArraySet<ValueNode> mFinishedNodes = new ArraySet<>();
  private final ArraySet<ValueNode> mNodesWithFinishedInputs = new ArraySet<>();
  private final SimpleArrayMap<GraphBinding, ArraySet<ValueNode>> mBindingToNodes =
      new SimpleArrayMap<>();
  private final ArraySet<GraphBinding> mFinishedBindings = new ArraySet<>();

  private boolean mIsDirty = false;

  private DataFlowGraph(TimingSource timingSource) {
    mTimingSource = timingSource;
  }

  /**
   * Adds an activated {@link GraphBinding}. This means that binding's nodes are added to the
   * existing graph and data will flow through them on the next frame.
   */
  public void register(GraphBinding binding) {
    if (!binding.isActive()) {
      throw new RuntimeException("Expected added GraphBinding to be active: " + binding);
    }
    mBindings.add(binding);
    mBindingToNodes.put(binding, binding.getAllNodes());
    if (mBindings.size() == 1) {
      mTimingSource.start();
    }
    mIsDirty = true;
  }

  /**
   * Removes a {@link GraphBinding}. This means any nodes that only belonged to that binding will
   * be removed from the graph.
   */
  public void unregister(GraphBinding binding) {
    if (!mBindings.remove(binding)) {
      throw new RuntimeException("Tried to unregister non-existent binding");
    }
    mBindingToNodes.remove(binding);
    mFinishedBindings.remove(binding);
    if (mBindings.isEmpty()) {
      mTimingSource.stop();
    }
    mIsDirty = true;
  }

  void doFrame(long frameTimeNanos) {
    if (mIsDirty) {
      regenerateSortedNodes();
    }

    propagate(frameTimeNanos);
    updateFinishedStates();
  }

  private void propagate(long frameTimeNanos) {
    final int size = mSortedNodes.size();
    for (int i = 0; i < size; i++) {
      final ValueNode node = mSortedNodes.get(i);
      node.doCalculateValue(frameTimeNanos);
    }
  }

  private void regenerateSortedNodes() {
    mSortedNodes.clear();

    if (mBindings.size() == 0) {
      return;
    }

    final ArraySet<ValueNode> leafNodes = ComponentsPools.acquireArraySet();
    final SimpleArrayMap<ValueNode, Integer> nodesToOutputsLeft = new SimpleArrayMap<>();

    for (int i = 0, bindingsSize = mBindingToNodes.size(); i < bindingsSize; i++) {
      final ArraySet<ValueNode> nodes = mBindingToNodes.valueAt(i);
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

  private void updateFinishedStates() {
    updateFinishedNodes();
    notifyFinishedBindings();
  }

  private void updateFinishedNodes() {
    for (int i = 0, size = mSortedNodes.size(); i < size; i++) {
      final ValueNode node = mSortedNodes.get(i);
      if (mFinishedNodes.contains(node)) {
        continue;
      }
      final boolean wereInputsFinished = mNodesWithFinishedInputs.contains(node);
      final boolean areInputsFinished = wereInputsFinished || areInputsFinished(node);
      if (!wereInputsFinished && areInputsFinished) {
        mNodesWithFinishedInputs.add(node);
        if (node instanceof NodeCanFinish) {
          ((NodeCanFinish) node).onInputsFinished();
        }
      }

      final boolean nodeIsNowFinished =
          !(node instanceof NodeCanFinish) ||
              ((NodeCanFinish) node).isFinished();
      if (nodeIsNowFinished) {
        mFinishedNodes.add(node);
      }
    }
  }

  private boolean areInputsFinished(ValueNode node) {
    for (int i = 0, inputCount = node.getInputCount(); i < inputCount; i++) {
      if (!mFinishedNodes.contains(node.getInputAt(i))) {
        return false;
      }
    }
    return true;
  }

  private void notifyFinishedBindings() {
    // Iterate in reverse order since notifying that a binding is finished might result in removing
    // that binding.
    for (int i = mBindingToNodes.size() - 1; i >= 0; i--) {
      final GraphBinding binding = mBindingToNodes.keyAt(i);
      if (mFinishedBindings.contains(binding)) {
        continue;
      }
      boolean allAreFinished = true;
      final ArraySet<ValueNode> nodesToCheck = mBindingToNodes.valueAt(i);
      for (int j = 0, nodesSize = nodesToCheck.size(); j < nodesSize; j++) {
        final ValueNode node = nodesToCheck.valueAt(j);
        if (!mFinishedNodes.contains(node)) {
          allAreFinished = false;
          break;
        }
      }
      if (allAreFinished) {
        binding.notifyNodesHaveFinished();
        mFinishedBindings.add(binding);
      }
    }
  }
}
