// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.dataflow;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class GraphBindingFinishesTest {

  private UnitTestTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  private static class TrackingBindingListener implements BindingListener {

    private int mNumFinishCalls = 0;

    @Override
    public void onAllNodesFinished(DataFlowBinding binding) {
      mNumFinishCalls++;
    }

    public int getNumFinishCalls() {
      return mNumFinishCalls;
    }
  }

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new UnitTestTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testBindingThatFinishes() {
    int durationMs = 300;
    int numExpectedFrames = durationMs / UnitTestTimingSource.FRAME_TIME_MS + 2;

    TimingNode timingNode = new TimingNode(durationMs);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(timingNode, middle);
    binding.addBinding(middle, destination);
    binding.addBinding(new ConstantNode(0f), timingNode, TimingNode.INITIAL_INPUT);
    binding.addBinding(new ConstantNode(100f), timingNode, TimingNode.END_INPUT);

    TrackingBindingListener testListener = new TrackingBindingListener();
    binding.setListener(testListener);
    binding.activate();

    mTestTimingSource.step(numExpectedFrames / 2);

    assertTrue(destination.getValue() < 100);
    assertTrue(destination.getValue() > 0);
    assertEquals(0, testListener.getNumFinishCalls());

    mTestTimingSource.step(numExpectedFrames / 2 + 1);

    assertEquals(100f, destination.getValue());
    assertEquals(1, testListener.getNumFinishCalls());
  }
}
