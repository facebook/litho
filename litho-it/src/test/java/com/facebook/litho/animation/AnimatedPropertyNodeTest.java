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

import static com.facebook.litho.animation.AnimatedProperties.SCALE;
import static com.facebook.litho.dataflow.GraphBinding.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

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
    View view = new View(application);
    SettableNode source = new SettableNode();
    SimpleNode middle = new SimpleNode();
    AnimatedPropertyNode destination = new AnimatedPropertyNode(view, SCALE);

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(view.getScaleX()).isEqualTo(0f);

    source.setValue(37);
    mTestTimingSource.step(1);

    assertThat(view.getScaleX()).isEqualTo(37f);
  }

  @Test
  public void testViewPropertyNodeWithOutput() {
    View view = new View(application);
    AnimatedPropertyNode source = new AnimatedPropertyNode(view, SCALE);
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(source, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(1f);

    view.setScaleX(101);
    view.setScaleY(101);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(101f);
  }

  @Test
  public void testViewPropertyNodeWithInputAndOutput() {
    View view = new View(application);
    SettableNode source = new SettableNode();
    AnimatedPropertyNode viewNode = new AnimatedPropertyNode(view, SCALE);
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(source, viewNode);
    binding.addBinding(viewNode, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(view.getScaleX()).isEqualTo(0f);
    assertThat(destination.getValue()).isEqualTo(0f);

    source.setValue(123);
    mTestTimingSource.step(1);

    assertThat(view.getScaleX()).isEqualTo(123f);
    assertThat(destination.getValue()).isEqualTo(123f);
  }
}
