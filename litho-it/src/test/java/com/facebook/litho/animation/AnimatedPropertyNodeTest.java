/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import android.view.View;

import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedPropertyNode;
import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.GraphBinding;
import com.facebook.litho.dataflow.OutputOnlyNode;
import com.facebook.litho.dataflow.SettableNode;
import com.facebook.litho.dataflow.SimpleNode;
import com.facebook.litho.dataflow.UnitTestTimingSource;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class AnimatedPropertyNodeTest {

  private UnitTestTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new UnitTestTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testViewPropertyNodeWithInput() {
    View view = new View(RuntimeEnvironment.application);
    SettableNode source = new SettableNode();
    SimpleNode middle = new SimpleNode();
    AnimatedPropertyNode destination = new AnimatedPropertyNode(view, AnimatedProperties.X);

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, view.getX());

    source.setValue(37);
    mTestTimingSource.step(1);

    assertEquals(37f, view.getX());
  }

  @Test
  public void testViewPropertyNodeWithOutput() {
    View view = new View(RuntimeEnvironment.application);
    AnimatedPropertyNode source = new AnimatedPropertyNode(view, AnimatedProperties.X);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, destination.getValue());

    view.setX(101);
    mTestTimingSource.step(1);

    assertEquals(101f, destination.getValue());
  }

  @Test
  public void testViewPropertyNodeWithInputAndOutput() {
    View view = new View(RuntimeEnvironment.application);
    SettableNode source = new SettableNode();
    AnimatedPropertyNode viewNode = new AnimatedPropertyNode(view, AnimatedProperties.X);
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = GraphBinding.create(mDataFlowGraph);
    binding.addBinding(source, viewNode);
    binding.addBinding(viewNode, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertEquals(0f, view.getX());
    assertEquals(0f, destination.getValue());

    source.setValue(123);
    mTestTimingSource.step(1);

    assertEquals(123f, view.getX());
    assertEquals(123f, destination.getValue());
  }
}
