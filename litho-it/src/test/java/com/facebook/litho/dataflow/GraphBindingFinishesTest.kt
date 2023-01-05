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
class GraphBindingFinishesTest {

  private lateinit var testTimingSource: MockTimingSource
  private lateinit var dataFlowGraph: DataFlowGraph

  private class TrackingBindingListener : BindingListener {
    var numFinishCalls: Int = 0
      private set

    override fun onAllNodesFinished(binding: GraphBinding) {
      numFinishCalls++
    }
  }

  @Before
  fun setUp() {
    testTimingSource = MockTimingSource()
    dataFlowGraph = DataFlowGraph.create(testTimingSource)
  }

  @Test
  fun testBindingThatFinishes() {
    val durationMs = 300
    val numExpectedFrames = durationMs / MockTimingSource.FRAME_TIME_MS + 2
    val timingNode = TimingNode(durationMs)
    val middle = SimpleNode()
    val destination = OutputOnlyNode()
    val binding = GraphBinding.create(dataFlowGraph)
    binding.addBinding(timingNode, middle)
    binding.addBinding(middle, destination)
    val testListener = TrackingBindingListener()
    binding.setListener(testListener)
    binding.activate()
    testTimingSource.step(numExpectedFrames / 2)
    assertThat(destination.value < 1).isTrue
    assertThat(destination.value > 0).isTrue
    assertThat(testListener.numFinishCalls).isEqualTo(0)
    testTimingSource.step(numExpectedFrames / 2 + 1)
    assertThat(destination.value).isEqualTo(1f)
    assertThat(testListener.numFinishCalls).isEqualTo(1)
    assertThat(dataFlowGraph.hasReferencesToNodes()).isFalse
  }
}
