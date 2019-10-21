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
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.animation.Interpolator;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class InterpolatorNodeTest {

  private MockTimingSource mTestTimingSource;
  private DataFlowGraph mDataFlowGraph;

  @Before
  public void setUp() throws Exception {
    mTestTimingSource = new MockTimingSource();
    mDataFlowGraph = DataFlowGraph.create(mTestTimingSource);
  }

  @Test
  public void testInterpolatorNode() {
    SettableNode settableNode = new SettableNode();
    InterpolatorNode interpolatorNode =
        new InterpolatorNode(
            new Interpolator() {
              @Override
              public float getInterpolation(float input) {
                return input;
              }
            });
    SimpleNode middle = new SimpleNode();
    OutputOnlyNode destination = new OutputOnlyNode();

    GraphBinding binding = create(mDataFlowGraph);
    binding.addBinding(settableNode, interpolatorNode);
    binding.addBinding(interpolatorNode, middle);
    binding.addBinding(middle, destination);
    binding.activate();

    settableNode.setValue(0);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(0f);

    settableNode.setValue(0.5f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue() < 1).isTrue();
    assertThat(destination.getValue() > 0).isTrue();

    settableNode.setValue(1.0f);
    mTestTimingSource.step(1);

    assertThat(destination.getValue()).isEqualTo(1f);
  }
}
