/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.dataflow;

import static com.facebook.litho.dataflow.GraphBinding.create;
import static com.facebook.litho.dataflow.MockTimingSource.FRAME_TIME_MS;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class TimingNodeTest {

  private MockTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new MockTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testTimingNode() {
    int durationMs = 300;
    int numExpectedFrames = 300 / FRAME_TIME_MS + 1;

    TimingNode timingNode = new TimingNode(durationMs);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(timingNode, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(0f);

    mTestTimingSource.step(numExpectedFrames / 2);

    assertThat(destination.getValue() < 1).isTrue();
    assertThat(destination.getValue() > 0).isTrue();

    mTestTimingSource.step(numExpectedFrames / 2 + 1);

    assertThat(destination.getValue()).isEqualTo(1f);
  }
}
