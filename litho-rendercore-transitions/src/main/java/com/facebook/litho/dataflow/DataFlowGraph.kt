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

import androidx.annotation.VisibleForTesting
import androidx.collection.ArraySet
import androidx.collection.SimpleArrayMap
import java.util.ArrayDeque
import javax.annotation.concurrent.GuardedBy

/**
 * A directed acyclic graph (DAG) created from one or more [GraphBinding]s. These component
 * GraphBindings define how nodes in this graph are connected to each other: GraphBindings can add
 * nodes and connections when they are 'activated' and can remove nodes and connections when they're
 * deactivated.
 *
 * Data flows through the graph on each frame, from input nodes to output nodes.
 */
class DataFlowGraph private constructor(@GuardedBy("this") private val timingSource: TimingSource) {

  private class NodeState {
    var isFinished: Boolean = false
    var refCount: Int = 0
  }

  @GuardedBy("this") private val bindings: MutableSet<GraphBinding> = LinkedHashSet()

  @GuardedBy("this") private val sortedNodes: ArrayList<ValueNode> = ArrayList()

  @GuardedBy("this") private val nodeStates: MutableMap<ValueNode, NodeState> = HashMap()

  @GuardedBy("this") private var isFinishingBindings: Boolean = false

  @GuardedBy("this") private val bindingsToUnregister: MutableList<GraphBinding> = ArrayList()

  @GuardedBy("this") private val bindingsToRegister: MutableList<GraphBinding> = ArrayList()

  private var isDirty: Boolean = false

  /**
   * Adds an activated [GraphBinding]. This means that binding's nodes are added to the existing
   * graph and data will flow through them on the next frame.
   */
  @Synchronized
  fun register(binding: GraphBinding) {
    if (!binding.isActive) {
      throw RuntimeException("Expected added GraphBinding to be active: $binding")
    }
    if (isFinishingBindings) {
      bindingsToRegister.add(binding)
      return
    }
    bindings.add(binding)
    registerNodes(binding)
    if (bindings.size == 1) {
      timingSource.start()
    }
    isDirty = true
  }

  /**
   * Removes a [GraphBinding]. This means any nodes that only belonged to that binding will be
   * removed from the graph.
   */
  @Synchronized
  fun unregister(binding: GraphBinding) {
    if (isFinishingBindings) {
      bindingsToUnregister.add(binding)
      return
    }
    if (!bindings.remove(binding)) {
      throw RuntimeException("Tried to unregister non-existent binding")
    }
    unregisterNodes(binding)
    if (bindings.isEmpty()) {
      timingSource.stop()
      sortedNodes.clear()
      if (nodeStates.isNotEmpty()) {
        throw RuntimeException("Failed to clean up all nodes")
      }
    }
    isDirty = true
  }

  @Synchronized
  fun doFrame(frameTimeNanos: Long) {
    if (isDirty) {
      regenerateSortedNodes()
    }

    propagate(frameTimeNanos)
    updateFinishedStates()
  }

  @GuardedBy("this")
  private fun propagate(frameTimeNanos: Long) {
    for (node in sortedNodes) {
      node.doCalculateValue(frameTimeNanos)
    }
  }

  @GuardedBy("this")
  private fun regenerateSortedNodes() {
    sortedNodes.clear()

    if (bindings.isEmpty()) {
      return
    }

    val leafNodes = ArraySet<ValueNode>()
    val nodesToOutputsLeft = SimpleArrayMap<ValueNode, Int>()
    val nodesToProcess = ArrayDeque<ValueNode>()

    for (binding in bindings) {
      val nodes = binding.getAllNodes()
      for (node in nodes) {
        val outputCount = node.getOutputCount()
        if (outputCount == 0) {
          if (!leafNodes.contains(node)) {
            nodesToProcess.add(node)
            leafNodes.add(node)
          }
        } else {
          nodesToOutputsLeft.put(node, outputCount)
        }
      }
    }

    if (!nodesToOutputsLeft.isEmpty() && leafNodes.isEmpty()) {
      throw DetectedCycleException(
          "Graph has nodes, but they represent a cycle with no leaf nodes!",
      )
    }

    while (!nodesToProcess.isEmpty()) {
      val next = nodesToProcess.pollFirst()
      if (next != null) {
        sortedNodes.add(next)
        for (input in next.getAllInputs()) {
          val outputsLeftValue = nodesToOutputsLeft.get(input)
          if (outputsLeftValue != null) {
            val outputsLeft = outputsLeftValue - 1
            nodesToOutputsLeft.put(input, outputsLeft)
            if (outputsLeft == 0) {
              nodesToProcess.addLast(input)
            } else if (outputsLeft < 0) {
              throw DetectedCycleException("Detected cycle.")
            }
          }
        }
      }
    }

    val expectedTotalNodes = nodesToOutputsLeft.size() + leafNodes.size
    if (sortedNodes.size != expectedTotalNodes) {
      throw DetectedCycleException(
          "Had unreachable nodes in graph -- this likely means there was a cycle",
      )
    }

    sortedNodes.reverse()
    isDirty = false
  }

  @GuardedBy("this")
  private fun updateFinishedStates() {
    updateFinishedNodes()
    notifyFinishedBindings()
  }

  @GuardedBy("this")
  private fun updateFinishedNodes() {
    for (node in sortedNodes) {
      val nodeState = nodeStates[node]
      if (nodeState == null || nodeState.isFinished || !areInputsFinished(node)) {
        continue
      }

      val nodeIsNowFinished = node !is NodeCanFinish || node.isFinished()
      if (nodeIsNowFinished) {
        nodeState.isFinished = true
      }
    }
  }

  @GuardedBy("this")
  private fun areInputsFinished(node: ValueNode): Boolean {
    for (input in node.getAllInputs()) {
      val nodeState = nodeStates[input]
      if (nodeState != null && !nodeState.isFinished) {
        return false
      }
    }
    return true
  }

  @GuardedBy("this")
  private fun notifyFinishedBindings() {
    // We need loop the graph bindings and flag the ones we need to add/remove so that we don't
    // change the list while iterating.
    isFinishingBindings = true
    for (binding in bindings) {
      var allAreFinished = true
      val nodesToCheck = binding.getAllNodes()
      for (node in nodesToCheck) {
        val nodeState = nodeStates[node]
        if (nodeState == null || !nodeState.isFinished) {
          allAreFinished = false
          break
        }
      }
      if (allAreFinished) {
        binding.notifyNodesHaveFinished()
      }
    }
    isFinishingBindings = false
    for (binding in bindingsToRegister) {
      register(binding)
    }
    for (binding in bindingsToUnregister) {
      unregister(binding)
    }
    bindingsToRegister.clear()
    bindingsToUnregister.clear()
  }

  @GuardedBy("this")
  private fun registerNodes(binding: GraphBinding) {
    val nodes = binding.getAllNodes()
    for (node in nodes) {
      val nodeState = nodeStates[node]
      if (nodeState != null) {
        nodeState.refCount++
      } else {
        val newState = NodeState()
        newState.refCount = 1
        nodeStates[node] = newState
      }
    }
  }

  @GuardedBy("this")
  private fun unregisterNodes(binding: GraphBinding) {
    val nodes = binding.getAllNodes()
    for (node in nodes) {
      val nodeState = nodeStates[node]
      if (nodeState != null) {
        nodeState.refCount--
        if (nodeState.refCount == 0) {
          nodeStates.remove(node)
        }
      }
    }
  }

  @VisibleForTesting
  @GuardedBy("this")
  fun hasReferencesToNodes(): Boolean =
      bindings.isNotEmpty() || sortedNodes.isNotEmpty() || nodeStates.isNotEmpty()

  companion object {
    private var instance: DataFlowGraph? = null

    @JvmStatic
    fun getInstance(): DataFlowGraph {
      var result = instance
      if (result == null) {
        val timingSource = ChoreographerTimingSource()
        result = DataFlowGraph(timingSource)
        instance = result
        timingSource.setDataFlowGraph(result)
      }
      return result
    }

    /**
     * For tests, lets the testing environment explicitly provide a specific DataFlowGraph instance
     * that can, for example, have a mocked TimingSource.
     */
    @JvmStatic
    @VisibleForTesting
    fun setInstance(dataFlowGraph: DataFlowGraph?) {
      instance = dataFlowGraph
    }

    @JvmStatic
    @VisibleForTesting
    fun create(timingSource: TimingSource): DataFlowGraph {
      val dfGraph = DataFlowGraph(timingSource)
      timingSource.setDataFlowGraph(dfGraph)
      return dfGraph
    }
  }
}
