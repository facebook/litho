/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.dataflow

/**
 * A single node in a [DataFlowGraph]. Nodes are added to a [DataFlowGraph] by creating a
 * [GraphBinding] and calling [GraphBinding.addBinding]. The nodes will actually be added to the
 * graph when [GraphBinding.activate] is called.
 *
 * The nodes in an active graph are visited on each frame in dependency order and given a chance to
 * compute a new value based on frame time and their parent (dependency) nodes.
 *
 * A ValueNode should be able to return its latest value at any point in time.
 *
 * Sub-classes are expected to implement [calculateValue], which handles calculating the new value
 * for this frame based on the node's parents (i.e. nodes it depends on) and the current frame time.
 */
abstract class ValueNode {

  private var inputs: MutableMap<String, ValueNode>? = null
  private var outputs: ArrayList<ValueNode>? = null
  private var timeNs: Long = 0

  /** The most recently calculated value from [calculateValue]. Can be manually set. */
  open var value: Float = 0f

  /**
   * This node should calculate and set a new value based on frame time and its parents (the nodes
   * it depends on). When this is called, it's guaranteed that the parent nodes have already been
   * updated for this frame.
   */
  protected abstract fun calculateValue(frameTimeNanos: Long): Float

  /** @return the input node for the given input name */
  protected fun getInput(name: String): ValueNode {
    val input = getInputUnsafe(name)
    if (input == null) {
      throw RuntimeException(
          "Tried to get non-existent input '$name'. Node only has these inputs: " +
              buildDebugInputsString(),
      )
    }
    return input
  }

  /**
   * @return the default input node. This should only be used for nodes that expect a single input.
   */
  protected fun getInput(): ValueNode {
    if (getInputCount() > 1) {
      throw RuntimeException("Trying to get single input of node with multiple inputs!")
    }
    return getInput(DEFAULT_INPUT)
  }

  /** @return whether this node has an input with the given name */
  protected fun hasInput(name: String): Boolean {
    val inputs = inputs ?: return false
    return inputs.containsKey(name)
  }

  /**
   * @return whether this node has a default input node. This should only be used for nodes that
   *   expect a single input.
   */
  protected fun hasInput(): Boolean {
    if (getInputCount() > 1) {
      throw RuntimeException("Trying to check for single input of node with multiple inputs!")
    }
    return hasInput(DEFAULT_INPUT)
  }

  fun buildDebugInputsString(): String {
    val inputs = inputs ?: return "[]"
    return inputs.keys.joinToString(", ", "[", "]") { "'$it'" }
  }

  fun getInputUnsafe(name: String): ValueNode? {
    val inputs = inputs ?: return null
    return inputs[name]
  }

  fun doCalculateValue(frameTimeNanos: Long) {
    val value = calculateValue(frameTimeNanos)
    if (frameTimeNanos == timeNs) {
      throw RuntimeException(
          "Got a calculate value call multiple times in the same frame. This isn't expected.",
      )
    }

    timeNs = frameTimeNanos
    this.value = value
  }

  fun addOutput(node: ValueNode) {
    var outputs = outputs
    if (outputs == null) {
      outputs = ArrayList()
      this.outputs = outputs
    }
    outputs.add(node)
  }

  fun getOutputCount(): Int = outputs?.size ?: 0

  fun getOutputAt(i: Int): ValueNode {
    val outputs = requireNotNull(outputs) { "Node has no outputs" }
    return outputs[i]
  }

  fun getOutput(): ValueNode {
    if (getOutputCount() != 1) {
      throw RuntimeException("Node does not have inputs of size 1: ${getOutputCount()}")
    }
    return getOutputAt(0)
  }

  fun removeOutput(output: ValueNode) {
    val outputs = requireNotNull(outputs) { "Tried to remove non-existent output!" }
    if (!outputs.remove(output)) {
      throw RuntimeException("Tried to remove non-existent output!")
    }
  }

  fun getInputCount(): Int = inputs?.size ?: 0

  fun getAllInputs(): Collection<ValueNode> {
    val inputs = inputs ?: return emptySet()
    return inputs.values
  }

  fun setInput(name: String, input: ValueNode) {
    var inputs = inputs
    if (inputs == null) {
      inputs = LinkedHashMap()
      this.inputs = inputs
    }
    inputs[name] = input
  }

  fun removeInput(name: String) {
    val inputs = requireNotNull(inputs) { "Tried to remove non-existent input with name: $name" }
    if (inputs.remove(name) == null) {
      throw RuntimeException("Tried to remove non-existent input with name: $name")
    }
  }

  companion object {
    const val DEFAULT_INPUT: String = "default_input"
  }
}
