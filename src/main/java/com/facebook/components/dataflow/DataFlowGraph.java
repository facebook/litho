/**
 * Copyright (c) 2014-present, Facebook, Inc.
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
import java.util.Deque;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SimpleArrayMap;
import android.util.ArraySet;
import android.util.MutableInt;

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
  static DataFlowGraph create(TimingSource timingSource) {
    DataFlowGraph instance = new DataFlowGraph(timingSource);
    timingSource.setDataFlowGraph(instance);
    return instance;
  }

  private final TimingSource mTimingSource;
  private final CopyOnWriteArrayList<GraphBinding> mBindings = new CopyOnWriteArrayList<>();
  private final ArrayList<ValueNode> mSortedNodes = new ArrayList<>();
  private final WeakHashMap<ValueNode, Void> mIsInitialized = new WeakHashMap<>();

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
    if (mBindings.isEmpty()) {
      mTimingSource.stop();
    }
    mIsDirty = true;
  }

  void doFrame(long frameTimeNanos) {
    if (mIsDirty) {
      regenerateSortedNodes();
      initializeUninitializedNodes();
    }

    propagate(frameTimeNanos);
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

    final ArraySet<ValueNode> leafNodes = new ArraySet<>();
    final SimpleArrayMap<ValueNode, MutableInt> nodesToOutputsLeft = new SimpleArrayMap<>();

    for (int i = 0; i < mBindings.size(); i++) {
      final GraphBinding graphBinding = mBindings.get(i);
      for (final ValueNode node : graphBinding.getAllNodes()) {
        final int outputCount = node.getOutputCount();
        if (outputCount == 0) {
          leafNodes.add(node);
        } else {
          nodesToOutputsLeft.put(node, new MutableInt(outputCount));
        }
      }
    }

    if (!nodesToOutputsLeft.isEmpty() && leafNodes.isEmpty()) {
      throw new DetectedCycleException(
          "Graph has nodes, but they represent a cycle with no leaf nodes!");
    }

    final Deque<ValueNode> nodesToProcess = new ArrayDeque<>(leafNodes);
    while (!nodesToProcess.isEmpty()) {
      final ValueNode next = nodesToProcess.pollFirst();
      mSortedNodes.add(next);
      for (int i = 0; i < next.getInputCount(); i++) {
        final ValueNode input = next.getInputAt(i);
        final MutableInt outputsLeft = nodesToOutputsLeft.get(input);
        outputsLeft.value--;
        if (outputsLeft.value == 0) {
          nodesToProcess.addLast(input);
        } else if (outputsLeft.value < 0) {
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
  }

  private void initializeUninitializedNodes() {
    for (int i = 0; i < mSortedNodes.size(); i++) {
      final ValueNode node = mSortedNodes.get(i);
      if (!mIsInitialized.containsKey(node)) {
        node.doInitialize();
        mIsInitialized.put(node, null);
      }
    }
  }
}
