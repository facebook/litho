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

package com.facebook.litho.animation;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.animation.AnimatedProperties.SCALE;
import static com.facebook.litho.dataflow.GraphBinding.create;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View;
import com.facebook.litho.OutputUnitType;
import com.facebook.litho.OutputUnitsAffinityGroup;
import com.facebook.litho.dataflow.DataFlowGraph;
import com.facebook.litho.dataflow.GraphBinding;
import com.facebook.litho.dataflow.MockTimingSource;
import com.facebook.litho.dataflow.OutputOnlyNode;
import com.facebook.litho.dataflow.SettableNode;
import com.facebook.litho.dataflow.SimpleNode;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class AnimatedPropertyNodeTest {

  private MockTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new MockTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testViewPropertyNodeWithInput() {
    View view = new View(getApplicationContext());
    OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();
    group.add(OutputUnitType.HOST, view);
    SettableNode source = new SettableNode();
    SimpleNode middle = new SimpleNode();
    AnimatedPropertyNode destination = new AnimatedPropertyNode(group, SCALE);

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
  public void testViewPropertyNodeWithInputAndOutput() {
    View view = new View(getApplicationContext());
    OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();
    group.add(OutputUnitType.HOST, view);
    SettableNode source = new SettableNode();
    AnimatedPropertyNode animatedNode = new AnimatedPropertyNode(group, SCALE);
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(source, animatedNode);
    binding.addBinding(animatedNode, destination);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(view.getScaleX()).isEqualTo(0f);
    assertThat(destination.getValue()).isEqualTo(0f);

    source.setValue(123);
    mTestTimingSource.step(1);

    assertThat(view.getScaleX()).isEqualTo(123f);
    assertThat(destination.getValue()).isEqualTo(123f);
  }

  @Test
  public void testSettingMountContentOnNodeWithValue() {
    View view1 = new View(getApplicationContext());
    OutputUnitsAffinityGroup<Object> group1 = new OutputUnitsAffinityGroup<>();
    group1.add(OutputUnitType.HOST, view1);

    View view2 = new View(getApplicationContext());
    OutputUnitsAffinityGroup<Object> group2 = new OutputUnitsAffinityGroup<>();
    group2.add(OutputUnitType.HOST, view2);

    SettableNode source = new SettableNode();
    AnimatedPropertyNode animatedNode = new AnimatedPropertyNode(group1, SCALE);

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(source, animatedNode);
    binding.activate();

    mTestTimingSource.step(1);

    assertThat(view1.getScaleX()).isEqualTo(0f);

    source.setValue(123);
    mTestTimingSource.step(1);

    assertThat(view1.getScaleX()).isEqualTo(123f);

    assertThat(view2.getScaleX()).isEqualTo(1f);

    animatedNode.setMountContentGroup(group2);

    assertThat(view2.getScaleX()).isEqualTo(123f);
  }

  @Test
  public void propertyNode_useRandomObject_failWhenIsNotView() {
    Object view = new Object();
    OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();
    group.add(OutputUnitType.HOST, view);

    SettableNode source = new SettableNode();
    AnimatedPropertyNode animatedNode = new AnimatedPropertyNode(group, SCALE);

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(source, animatedNode);
    binding.activate();

    thrown.expect(RuntimeException.class);
    mTestTimingSource.step(1);
  }
}
