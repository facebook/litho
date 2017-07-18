/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.litho.dataflow.GraphBinding.create;
import static com.facebook.litho.dataflow.TimingNode.END_INPUT;
import static com.facebook.litho.dataflow.TimingNode.INITIAL_INPUT;
import static com.facebook.litho.dataflow.MockTimingSource.FRAME_TIME_MS;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
    binding.addBinding(new ConstantNode(0f), timingNode, INITIAL_INPUT);
    binding.addBinding(new ConstantNode(100f), timingNode, END_INPUT);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(0f);

    mTestTimingSource.step(numExpectedFrames / 2);

    assertThat(destination.getValue() < 100).isTrue();
    assertThat(destination.getValue() > 0).isTrue();

    mTestTimingSource.step(numExpectedFrames / 2 + 1);

    assertThat(destination.getValue()).isEqualTo(100f);
  }

  @Test
  public void testTimingNodeWithUpdatingEndValue() {
    int durationMs = 300;
    int numExpectedFrames = 300 / FRAME_TIME_MS + 1;

    SettableNode end = new SettableNode();
    TimingNode timingNode = new TimingNode(durationMs);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();
    end.setValue(100);

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(timingNode, middle);
    binding.addBinding(middle, destination);
    binding.addBinding(new ConstantNode(0f), timingNode, INITIAL_INPUT);
    binding.addBinding(end, timingNode, END_INPUT);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(0f);

    mTestTimingSource.step(numExpectedFrames / 2);

    assertThat(destination.getValue() < 100).isTrue();
    assertThat(destination.getValue() > 0).isTrue();

    end.setValue(200);

    // Move to 3/4 done
    mTestTimingSource.step(numExpectedFrames / 4);

    assertThat(destination.getValue() > 100).isTrue();
    assertThat(destination.getValue() < 200).isTrue();

    mTestTimingSource.step(numExpectedFrames / 4 + 3);

    assertThat(destination.getValue()).isEqualTo(200f);
  }
}
