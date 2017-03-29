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

import android.support.v4.util.SimpleArrayMap;

/**
 * A single node in a {@link DataFlowGraph}. Nodes are added to a {@link DataFlowGraph} using
 * by creating a {@link GraphBinding} and calling {@link GraphBinding#addBinding}. The nodes will
 * actually be added to the graph when {@link GraphBinding#activate} is called.
 *
 * The nodes in an active graph are visited on each frame in dependency order and given a chance to
 * compute a new value based on frame time and their parent (dependency) nodes.
 *
 * A ValueNode should be able to return its latest value at any point in time.
 *
 * Sub-classes are expected to implement {@link #calculateValue}, which handles calculating
 * the new value for this frame based on the node's parents (i.e. nodes it depends on) and the
 * current frame time, and {@link #initialize}, which handles calculating and returning
 * an initial value for this node.
 */
public abstract class ValueNode<T> {

  public static final String DEFAULT_INPUT = "default_input";

  private SimpleArrayMap<String, ValueNode> mInputs = null;
  private ArrayList<ValueNode> mOutputs = null;
  private T mValue;
  private long mTimeNs = 0;

  /**
   * @return the most recently calculated value from {@link #calculateValue}.
   */
  public T getValue() {
    return mValue;
  }

  /**
   * This node should calculate and set a new value based on frame time and its parents (the nodes
   * it depends on). When this is called, it's guaranteed that the parent nodes have already been
   * updated for this frame.
   */
  protected abstract T calculateValue(long frameTimeNanos);

  /**
   * Called when this node is first added to the graph. inputSpec defines what this node's
   * outputs expects as its initial input, if anything. If this node has multiple outputs, its
   * inputSpec will be the most restrictive of these outputs, assuming they don't conflict.
   * (If they do conflict, the graph is trying to initialize an illegal state and will throw).
   */
  protected abstract T initialize();

  /**
   * @return the input node for the given input name
   */
  protected <T> ValueNode<T> getInput(String name) {
    final ValueNode<T> input = getInputUnsafe(name);
    if (input == null) {
      throw new RuntimeException(
          "Tried to get non-existent input '" + name + "'. Node only has these inputs: " +
              buildDebugInputsString());
    }
    return input;
  }

  /**
   * @return the default input node. This should only be used for nodes that expect a single input.
   */
  protected <T> ValueNode<T> getInput() {
    if (getInputCount() > 1) {
      throw new RuntimeException("Trying to get single input of node with multiple inputs!");
    }
    return getInput(DEFAULT_INPUT);
  }

  /**
   * @return whether this node has an input with the given name
   */
  protected boolean hasInput(String name) {
    if (mInputs != null) {
      return false;
    }
    return mInputs.containsKey(name);
  }

  /**
   * @return whether this node has a default input node. This should only be used for nodes that
   * expect a single input.
   */
  protected boolean hasInput() {
    if (getInputCount() > 1) {
      throw new RuntimeException("Trying to check for single input of node with multiple inputs!");
    }
    return hasInput(DEFAULT_INPUT);
  }

  private String buildDebugInputsString() {
    if (mInputs == null) {
      return "[]";
    }
    String inputNames = "";
    for (int i = 0; i < mInputs.size(); i++) {
      inputNames += "'" + mInputs.keyAt(i) + "'";
      if (i != mInputs.size() - 1) {
        inputNames += ", ";
      }
    }
    return "[" + inputNames + "]";
  }

  <T> ValueNode<T> getInputUnsafe(String name) {
    if (mInputs == null) {
      return null;
    }
    return mInputs.get(name);
  }

  final void doCalculateValue(long frameTimeNanos) {
    final T value = calculateValue(frameTimeNanos);
    if (frameTimeNanos == mTimeNs) {
      throw new RuntimeException(
          "Got a calculate value call multiple times in the same frame. This isn't expected.");
    }

    mTimeNs = frameTimeNanos;
    mValue = value;
  }

  final void doInitialize() {
    mValue = initialize();
  }

  void addOutput(ValueNode node) {
    if (mOutputs == null) {
      mOutputs = new ArrayList<>();
    }
    mOutputs.add(node);
  }

  int getOutputCount() {
    return mOutputs == null ? 0 : mOutputs.size();
  }

  ValueNode getOutputAt(int i) {
    return mOutputs.get(i);
  }

  ValueNode getOutput() {
    if (getOutputCount() != 1) {
      throw new RuntimeException("Node does not have inputs of size 1: " + getOutputCount());
    }
    return getOutputAt(0);
  }

  void removeOutputAt(int i) {
    if (i >= getOutputCount()) {
      throw new RuntimeException("Bad index: " + i + " >= " + getOutputCount());
    }
    mOutputs.remove(i);
  }

  void removeOutput(ValueNode output) {
    if (!mOutputs.remove(output)) {
      throw new RuntimeException("Tried to remove non-existent input!");
    }
  }

  int getInputCount() {
    return mInputs == null ? 0 : mInputs.size();
  }

  ValueNode getInputAt(int i) {
    if (getInputCount() <= i) {
      throw new RuntimeException("Bad index: " + i + " > " + getInputCount());
    }
    return mInputs.valueAt(i);
  }

  void setInput(String name, ValueNode input) {
    if (mInputs == null) {
      mInputs = new SimpleArrayMap<>();
    }
    mInputs.put(name, input);
  }

  void removeInput(String name) {
    if (mInputs == null || mInputs.remove(name) == null) {
      throw new RuntimeException("Tried to remove non-existent input with name: " + name);
    }
  }
}
