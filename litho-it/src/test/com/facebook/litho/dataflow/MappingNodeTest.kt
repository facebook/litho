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
class MappingNodeTest {

  private lateinit var testTimingSource: MockTimingSource
  private lateinit var dataFlowGraph: DataFlowGraph

  @Before
  fun setUp() {
    testTimingSource = MockTimingSource()
    dataFlowGraph = DataFlowGraph.create(testTimingSource)
  }

  @Test
  fun testMappingNodeWithinRange() {
    val settableNode = SettableNode()
    val mappingNode = MappingNode()
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(settableNode, mappingNode)
    binding.addBinding(mappingNode, middle)
    binding.addBinding(ConstantNode(0f), mappingNode, MappingNode.INITIAL_INPUT)
    binding.addBinding(ConstantNode(100f), mappingNode, MappingNode.END_INPUT)
    binding.addBinding(middle, destination)
    binding.activate()
    settableNode.value = 0f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(0f)
    settableNode.value = 0.2f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(20f)
    settableNode.value = 0.7f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(70f)
    settableNode.value = 1.0f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(100f)
  }

  @Test
  fun testMappingNodeOutsideRange() {
    val settableNode = SettableNode()
    val mappingNode = MappingNode()
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(settableNode, mappingNode)
    binding.addBinding(mappingNode, middle)
    binding.addBinding(ConstantNode(0f), mappingNode, MappingNode.INITIAL_INPUT)
    binding.addBinding(ConstantNode(100f), mappingNode, MappingNode.END_INPUT)
    binding.addBinding(middle, destination)
    binding.activate()
    settableNode.value = -0.1f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(-10f)
    settableNode.value = 0f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(0f)
    settableNode.value = 0.5f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(50f)
    settableNode.value = 1.5f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(150f)
  }
}
