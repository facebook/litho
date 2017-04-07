// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.dataflow;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class TimingNodeTest {

  private UnitTestTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new UnitTestTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testTimingNode() {
    int durationMs = 300;
    int numExpectedFrames = 300 / UnitTestTimingSource.FRAME_TIME_MS + 1;

    TimingNode timingNode = new TimingNode(durationMs);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(timingNode, middle);
    binding.addBinding(middle, destination);
    binding.addBinding(new ConstantNode(0f), timingNode, TimingNode.INITIAL_INPUT);
    binding.addBinding(new ConstantNode(100f), timingNode, TimingNode.END_INPUT);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, destination.getValue());

    mTestTimingSource.step(numExpectedFrames / 2);

    assertTrue(destination.getValue() < 100);
    assertTrue(destination.getValue() > 0);

    mTestTimingSource.step(numExpectedFrames / 2 + 1);

    assertEquals(100f, destination.getValue());
  }

  @Test
  public void testTimingNodeWithUpdatingEndValue() {
    int durationMs = 300;
    int numExpectedFrames = 300 / UnitTestTimingSource.FRAME_TIME_MS + 1;

    SettableNode end = new SettableNode();
    TimingNode timingNode = new TimingNode(durationMs);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();
    end.setValue(100);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(timingNode, middle);
    binding.addBinding(middle, destination);
    binding.addBinding(new ConstantNode(0f), timingNode, TimingNode.INITIAL_INPUT);
    binding.addBinding(end, timingNode, TimingNode.END_INPUT);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, destination.getValue());

    mTestTimingSource.step(numExpectedFrames / 2);

    assertTrue(destination.getValue() < 100);
    assertTrue(destination.getValue() > 0);

    end.setValue(200);

    // Move to 3/4 done
    mTestTimingSource.step(numExpectedFrames / 4);

    assertTrue(destination.getValue() > 100);
    assertTrue(destination.getValue() < 200);

    mTestTimingSource.step(numExpectedFrames / 4 + 3);

    assertEquals(200f, destination.getValue());
  }
}
