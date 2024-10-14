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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class DataFlowGraphTest {

  private lateinit var testTimingSource: MockTimingSource
  private lateinit var dataFlowGraph: DataFlowGraph

  @Before
  fun setUp() {
    testTimingSource = MockTimingSource()
    dataFlowGraph = DataFlowGraph.create(testTimingSource)
  }

  @Test
  fun testSimpleGraph() {
    val source = SettableNode()
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(source, middle)
    binding.addBinding(middle, destination)
    binding.activate()
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(0f)
    assertThat(source.value).isEqualTo(0f)
    source.value = 37f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(37f)
    assertThat(source.value).isEqualTo(37f)
  }

  @Test
  fun testSimpleUpdatingGraph() {
    val source = NumFramesNode()
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(source, middle)
    binding.addBinding(middle, destination)
    binding.activate()
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(1f)
    assertThat(source.value).isEqualTo(1f)
    testTimingSource.step(39)
    assertThat(destination.value).isEqualTo(40f)
    assertThat(source.value).isEqualTo(40f)
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(41f)
    assertThat(source.value).isEqualTo(41f)
  }

  @Test
  fun testGraphWithMultipleOutputs() {
    val source = NumFramesNode()
    val middle = SimpleNode()
    val dest1 = OutputOnlyNode()
    val dest2 = OutputOnlyNode()
    val dest3 = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(source, middle)
    binding.addBinding(middle, dest1)
    binding.addBinding(middle, dest2)
    binding.addBinding(source, dest3)
    binding.activate()
    testTimingSource.step(1)
    assertThat(dest1.value).isEqualTo(1f)
    assertThat(dest2.value).isEqualTo(1f)
    assertThat(dest3.value).isEqualTo(1f)
    testTimingSource.step(39)
    assertThat(dest1.value).isEqualTo(40f)
    assertThat(dest2.value).isEqualTo(40f)
    assertThat(dest3.value).isEqualTo(40f)
  }

  @Test
  fun testRebindNode() {
    val source = SettableNode()
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(source, middle)
    binding.addBinding(middle, destination)
    binding.activate()
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(0f)
    val newSource = SettableNode()
    val secondBinding = GraphBinding.create(dataFlowGraph)
    secondBinding.addBinding(newSource, destination)
    secondBinding.activate()
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(0f)
    newSource.value = 11f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(11f)
  }

  @Test
  fun testMultipleInputs() {
    val dest = AdditionNode()
    val a = SettableNode()
    val b = SettableNode()
    a.value = 1_776f
    b.value = 1_812f
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(a, dest, "a")
    binding.addBinding(b, dest, "b")
    binding.activate()
    testTimingSource.step(1)
    assertThat(dest.value).isEqualTo(3_588f)
  }

  @Test(expected = DetectedCycleException::class)
  fun testSimpleCycle() {
    val node1 = SimpleNode()
    val node2 = SimpleNode()
    val node3 = SimpleNode()
    val node4 = SimpleNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(node1, node2)
    binding.addBinding(node2, node3)
    binding.addBinding(node3, node1)
    binding.addBinding(node1, node4)
    binding.activate()
    testTimingSource.step(1)
  }

  @Test(expected = DetectedCycleException::class)
  fun testCycleWithoutLeaves() {
    val node1 = SimpleNode()
    val node2 = SimpleNode()
    val node3 = SimpleNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(node1, node2)
    binding.addBinding(node2, node3)
    binding.addBinding(node3, node1)
    binding.activate()
    testTimingSource.step(1)
  }
}
