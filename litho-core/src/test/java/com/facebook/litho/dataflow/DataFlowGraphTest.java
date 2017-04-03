/**
 * Copyright (c) 2014-present, Facebook, Inc.
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

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class DataFlowGraphTest {

  private UnitTestTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new UnitTestTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testSimpleGraph() {
    SettableNode source = new SettableNode();
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode(123);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, destination.getValue());
    assertEquals(0f, source.getValue());

    source.setValue(37);
    mTestTimingSource.step(1);

    assertEquals(37f, destination.getValue());
    assertEquals(37f, source.getValue());
  }

  @Test
  public void testSimpleUpdatingGraph() {
    NumFramesNode source = new NumFramesNode();
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode(123);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(1f, destination.getValue());
    assertEquals(1f, source.getValue());

    mTestTimingSource.step(39);

    assertEquals(40f, destination.getValue());
    assertEquals(40f, source.getValue());

    mTestTimingSource.step(1);

    assertEquals(41f, destination.getValue());
    assertEquals(41f, source.getValue());
  }

  @Test
  public void testGraphWithMultipleOutputs() {
    NumFramesNode source = new NumFramesNode();
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode dest1 = new OutputOnlyNode(123);
    OutputOnlyNode dest2 = new OutputOnlyNode(456);
    OutputOnlyNode dest3 = new OutputOnlyNode(456);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, dest1);
    binding.addBinding(middle, dest2);
    binding.addBinding(source, dest3);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(1f, dest1.getValue());
    assertEquals(1f, dest2.getValue());
    assertEquals(1f, dest3.getValue());

    mTestTimingSource.step(39);

    assertEquals(40f, dest1.getValue());
    assertEquals(40f, dest2.getValue());
    assertEquals(40f, dest3.getValue());
  }

  @Test
  public void testRebindNode() {
    SettableNode source = new SettableNode();
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode(123);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, destination.getValue());

    SettableNode newSource = new SettableNode();
    GraphBinding secondBinding = GraphBinding.create(mDataFlowGraph);
    secondBinding.addBinding(newSource, destination);
    secondBinding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, destination.getValue());

    newSource.setValue(11);
    mTestTimingSource.step(1);

    assertEquals(11f, destination.getValue());
  }

  @Test
  public void testMultipleInputs() {
    AdditionNode dest = new AdditionNode();
    SettableNode a = new SettableNode();
    SettableNode b = new SettableNode();
    a.setValue(1776);
    b.setValue(1812);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(a, dest, "a");
    binding.addBinding(b, dest, "b");
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(3588f, dest.getValue());
  }

  @Test(expected = DetectedCycleException.class)
  public void testSimpleCycle() {
    SimpleNode node1 = new SimpleNode();
    SimpleNode node2 = new SimpleNode();
    SimpleNode node3 = new SimpleNode();
    SimpleNode node4 = new SimpleNode();

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(node1, node2);
    binding.addBinding(node2, node3);
    binding.addBinding(node3, node1);
    binding.addBinding(node1, node4);
    binding.activate();

    mTestTimingSource.step(1);
  }

  @Test(expected = DetectedCycleException.class)
  public void testCycleWithoutLeaves() {
    SimpleNode node1 = new SimpleNode();
    SimpleNode node2 = new SimpleNode();
    SimpleNode node3 = new SimpleNode();

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(node1, node2);
    binding.addBinding(node2, node3);
    binding.addBinding(node3, node1);
    binding.activate();

    mTestTimingSource.step(1);
  }
}
