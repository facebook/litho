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
import static com.facebook.litho.dataflow.MappingNode.END_INPUT;
import static com.facebook.litho.dataflow.MappingNode.INITIAL_INPUT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class MappingNodeTest {

  private MockTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new MockTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testMappingNodeWithinRange() {
    SettableNode settableNode = new SettableNode();
    MappingNode mappingNode = new MappingNode();
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(settableNode, mappingNode);
    binding.addBinding(mappingNode, middle);
    binding.addBinding(new ConstantNode(0f), mappingNode, INITIAL_INPUT);
    binding.addBinding(new ConstantNode(100f), mappingNode, END_INPUT);
    binding.addBinding(middle, destination);
    binding.activate();

    settableNode.setValue(0);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(0f);

    settableNode.setValue(0.2f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(20f);

    settableNode.setValue(0.7f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(70f);

    settableNode.setValue(1.0f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(100f);
  }

  @Test
  public void testMappingNodeOutsideRange() {
    SettableNode settableNode = new SettableNode();
    MappingNode mappingNode = new MappingNode();
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(settableNode, mappingNode);
    binding.addBinding(mappingNode, middle);
    binding.addBinding(new ConstantNode(0f), mappingNode, INITIAL_INPUT);
    binding.addBinding(new ConstantNode(100f), mappingNode, END_INPUT);
    binding.addBinding(middle, destination);
    binding.activate();

    settableNode.setValue(-0.1f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(-10f);

    settableNode.setValue(0f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(0f);

    settableNode.setValue(0.5f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(50f);

    settableNode.setValue(1.5f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(150f);
  }
}
