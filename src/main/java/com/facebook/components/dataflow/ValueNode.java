// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

import java.util.ArrayList;

/**
 * A single node in a {@link DataFlowGraph}. Nodes are added to a {@link DataFlowGraph} using
 * by creating a {@link GraphBinding} and calling {@link GraphBinding#addBinding}. The nodes will
 * actually be added to the graph when {@link GraphBinding#activate} is called.
 *
 * The nodes in an active graph are visited on each frame in dependency order and given a chance to
 * compute a new value based on frame time and their parent (dependency) nodes.
 *
 * A ValueNode should be able to return its latest value at any point in time, and is responsible
 * for tracking that value's velocity in units/ms.
 *
 * A ValueNode is also given the ability to initialize itself when it is first added to the graph.
 * When initializing, a node can look at time and its InputSpec for its outputs (which specifies
 * what its outputs expect as initial input). This is useful to, for example, set the starting
 * position of a spring based on its outputs' initial values.
 *
 * Sub-classes are expected to implement {@link #calculateValue}, which handles calculating
 * the new value for this frame based on the node's parents (i.e. nodes it depends on) and the
 * current frame time, and {@link #initialize}, which handles calculating and returning
 * an initial value for this node based on its expected initial output.
 */
public abstract class ValueNode {

  private static final int NS_PER_MS = 1000000;

  private ArrayList<ValueNode> mOutputs = null;
  private ValueNode mInput = null;
  private float mPreviousValue = Float.NaN;
  private float mValue = Float.NaN;
  private long mPreviousTimeNs = 0;
  private long mTimeNs = 0;

  /**
   * @return the most recently calculated value from {@link #calculateValue}.
   */
  public final float getValue() {
    return mValue;
  }

  /**
   * This node should calculate and set a new value based on frame time and its parents (the nodes
   * it depends on). When this is called, it's guaranteed that the parent nodes have already been
   * updated for this frame.
   */
  protected abstract float calculateValue(long frameTimeNanos, ValueNode inputNode);

  /**
   * Called when this node is first added to the graph. inputSpec defines what this node's
   * outputs expects as its initial input, if anything. If this node has multiple outputs, its
   * inputSpec will be the most restrictive of these outputs, assuming they don't conflict.
   * (If they do conflict, the graph is trying to initialize an illegal state and will throw).
   */
  protected abstract float initialize(long inputSpec);

  /**
   * If this node has only one output and wants to support having an initial velocity (e.g. a
   * spring) the node can do initialization based on initial velocity here.
   */
  protected void initializeVelocity(float outputVelocity) {
  }

  /**
   * @return the expected input to this node. This will be used as an instruction to this node's
   * input node as to what their initial output to this node should be. For example, a spring would
   * return {@link InputSpec#UNSPECIFIED} since it can take any input, but a view property needs
   * a specific input (its current value) to stay continuous.
   */
  protected long calculateInputSpec(long inputSpecForOutputs) {
    return InputSpec.create(true, mValue);
  }

  final void doCalculateValue(long frameTimeNanos) {
    final float value = calculateValue(frameTimeNanos, getInput());
    if (Float.isNaN(value)) {
      throw new IllegalValueException("Got NaN as a value: " + this);
    }
    if (frameTimeNanos == mTimeNs) {
      throw new RuntimeException(
          "Got a calculate value call multiple times in the same frame. This isn't expected.");
    }

    mPreviousTimeNs = mTimeNs;
    mPreviousValue = mValue;
    mTimeNs = frameTimeNanos;
    mValue = value;
  }

  final void doInitialize(long inputSpec) {
    mValue = initialize(inputSpec);
  }

  final float getVelocity() {
    if (Float.isNaN(mPreviousValue) || Float.isNaN(mValue)) {
      return 0;
    }
    return (mValue - mPreviousValue) / (mTimeNs - mPreviousTimeNs) * NS_PER_MS;
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

  void setInput(ValueNode input) {
    mInput = input;
  }

  ValueNode getInput() {
    return mInput;
  }
}
