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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A single node in a {@link DataFlowGraph}. Nodes are added to a {@link DataFlowGraph} using by
 * creating a {@link GraphBinding} and calling {@link GraphBinding#addBinding}. The nodes will
 * actually be added to the graph when {@link GraphBinding#activate} is called.
 *
 * <p>The nodes in an active graph are visited on each frame in dependency order and given a chance
 * to compute a new value based on frame time and their parent (dependency) nodes.
 *
 * <p>A ValueNode should be able to return its latest value at any point in time.
 *
 * <p>Sub-classes are expected to implement {@link #calculateValue}, which handles calculating the
 * new value for this frame based on the node's parents (i.e. nodes it depends on) and the current
 * frame time.
 */
public abstract class ValueNode {

  public static final String DEFAULT_INPUT = "default_input";

  private Map<String, ValueNode> mInputs = null;
  private ArrayList<ValueNode> mOutputs = null;
  private float mValue;
  private long mTimeNs = 0;

  /** @return the most recently calculated value from {@link #calculateValue}. */
  public float getValue() {
    return mValue;
  }

  /** Manually sets the current value. */
  public void setValue(float value) {
    mValue = value;
  }

  /**
   * This node should calculate and set a new value based on frame time and its parents (the nodes
   * it depends on). When this is called, it's guaranteed that the parent nodes have already been
   * updated for this frame.
   */
  protected abstract float calculateValue(long frameTimeNanos);

  /** @return the input node for the given input name */
  protected ValueNode getInput(String name) {
    final ValueNode input = getInputUnsafe(name);
    if (input == null) {
      throw new RuntimeException(
          "Tried to get non-existent input '"
              + name
              + "'. Node only has these inputs: "
              + buildDebugInputsString());
    }
    return input;
  }

  /**
   * @return the default input node. This should only be used for nodes that expect a single input.
   */
  protected ValueNode getInput() {
    if (getInputCount() > 1) {
      throw new RuntimeException("Trying to get single input of node with multiple inputs!");
    }
    return getInput(DEFAULT_INPUT);
  }

  /** @return whether this node has an input with the given name */
  protected boolean hasInput(String name) {
    if (mInputs == null) {
      return false;
    }
    return mInputs.containsKey(name);
  }

  /**
   * @return whether this node has a default input node. This should only be used for nodes that
   *     expect a single input.
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
    final Iterator<String> inputIterator = mInputs.keySet().iterator();
    while (inputIterator.hasNext()) {
      inputNames += "'" + inputIterator.next() + "'";
      if (!inputIterator.hasNext()) {
        inputNames += ", ";
      }
    }
    return "[" + inputNames + "]";
  }

  @Nullable
  ValueNode getInputUnsafe(String name) {
    if (mInputs == null) {
      return null;
    }
    return mInputs.get(name);
  }

  final void doCalculateValue(long frameTimeNanos) {
    final float value = calculateValue(frameTimeNanos);
    if (frameTimeNanos == mTimeNs) {
      throw new RuntimeException(
          "Got a calculate value call multiple times in the same frame. This isn't expected.");
    }

    mTimeNs = frameTimeNanos;
    mValue = value;
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

  void removeOutput(ValueNode output) {
    if (!mOutputs.remove(output)) {
      throw new RuntimeException("Tried to remove non-existent input!");
    }
  }

  int getInputCount() {
    return mInputs == null ? 0 : mInputs.size();
  }

  Collection<ValueNode> getAllInputs() {
    if (mInputs == null) {
      return Collections.emptySet();
    }

    return mInputs.values();
  }

  void setInput(String name, ValueNode input) {
    if (mInputs == null) {
      mInputs = new LinkedHashMap<>();
    }
    mInputs.put(name, input);
  }

  void removeInput(String name) {
    if (mInputs == null || mInputs.remove(name) == null) {
      throw new RuntimeException("Tried to remove non-existent input with name: " + name);
    }
  }
}
