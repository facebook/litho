/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
public class GraphBindingFinishesTest {

  private MockTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  private static class TrackingBindingListener implements BindingListener {

    private int mNumFinishCalls = 0;

    @Override
    public void onAllNodesFinished(GraphBinding binding) {
      mNumFinishCalls++;
    }

    public int getNumFinishCalls() {
      return mNumFinishCalls;
    }
  }

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new MockTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testBindingThatFinishes() {
    int durationMs = 300;
    int numExpectedFrames = durationMs / FRAME_TIME_MS + 2;

    TimingNode timingNode = new TimingNode(durationMs);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(timingNode, middle);
    binding.addBinding(middle, destination);

    TrackingBindingListener testListener = new TrackingBindingListener();
    binding.setListener(testListener);
    binding.activate();

    mTestTimingSource.step(numExpectedFrames / 2);

    assertThat(destination.getValue() < 1).isTrue();
    assertThat(destination.getValue() > 0).isTrue();
    assertThat(testListener.getNumFinishCalls()).isEqualTo(0);

    mTestTimingSource.step(numExpectedFrames / 2 + 1);

    assertThat(destination.getValue()).isEqualTo(1f);
    assertThat(testListener.getNumFinishCalls()).isEqualTo(1);
  }
}
