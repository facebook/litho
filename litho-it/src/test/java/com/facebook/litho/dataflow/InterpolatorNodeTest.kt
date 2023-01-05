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

import android.view.animation.Interpolator
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class InterpolatorNodeTest {

  private lateinit var testTimingSource: MockTimingSource
  private lateinit var dataFlowGraph: DataFlowGraph

  @Before
  fun setUp() {
    testTimingSource = MockTimingSource()
    dataFlowGraph = DataFlowGraph.create(testTimingSource)
  }

  @Test
  fun testInterpolatorNode() {
    val settableNode = SettableNode()
    val interpolatorNode = InterpolatorNode(Interpolator { input -> input })
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(settableNode, interpolatorNode)
    binding.addBinding(interpolatorNode, middle)
    binding.addBinding(middle, destination)
    binding.activate()
    settableNode.value = 0f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(0f)
    settableNode.value = 0.5f
    testTimingSource.step(1)
    assertThat(destination.value < 1).isTrue
    assertThat(destination.value > 0).isTrue
    settableNode.value = 1.0f
    testTimingSource.step(1)
    assertThat(destination.value).isEqualTo(1f)
  }
}
