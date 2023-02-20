/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
package com.facebook.litho.animation

import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.OutputUnitType
import com.facebook.litho.OutputUnitsAffinityGroup
import com.facebook.litho.dataflow.DataFlowGraph
import com.facebook.litho.dataflow.GraphBinding
import com.facebook.litho.dataflow.MockTimingSource
import com.facebook.litho.dataflow.OutputOnlyNode
import com.facebook.litho.dataflow.SettableNode
import com.facebook.litho.dataflow.SimpleNode
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class AnimatedPropertyNodeTest {
  private lateinit var testTimingSource: MockTimingSource
  private lateinit var dataFlowGraph: DataFlowGraph

  @JvmField @Rule var thrown = ExpectedException.none()

  @Before
  fun setUp() {
    testTimingSource = MockTimingSource()
    dataFlowGraph = DataFlowGraph.create(testTimingSource)
  }

  @Test
  fun testViewPropertyNodeWithInput() {
    val view = View(ApplicationProvider.getApplicationContext())
    val group = OutputUnitsAffinityGroup<Any>().apply { add(OutputUnitType.HOST, view) }
    val source = SettableNode()
    val middle = SimpleNode()
    val destination = AnimatedPropertyNode(group, AnimatedProperties.SCALE)
    val binding =
        GraphBinding.create(dataFlowGraph).apply {
          addBinding(source, middle)
          addBinding(middle, destination)
          activate()
        }

    testTimingSource.step(1)
    Assertions.assertThat(view.scaleX).isEqualTo(0f)

    source.value = 37f
    testTimingSource.step(1)
    Assertions.assertThat(view.scaleX).isEqualTo(37f)
  }

  @Test
  fun testViewPropertyNodeWithInputAndOutput() {
    val view = View(ApplicationProvider.getApplicationContext())
    val group = OutputUnitsAffinityGroup<Any>().apply { add(OutputUnitType.HOST, view) }
    val source = SettableNode()
    val animatedNode = AnimatedPropertyNode(group, AnimatedProperties.SCALE)
    val destination = OutputOnlyNode()
    val binding =
        GraphBinding.create(dataFlowGraph).apply {
          addBinding(source, animatedNode)
          addBinding(animatedNode, destination)
          activate()
        }

    testTimingSource.step(1)
    Assertions.assertThat(view.scaleX).isEqualTo(0f)
    Assertions.assertThat(destination.value).isEqualTo(0f)

    source.value = 123f
    testTimingSource.step(1)
    Assertions.assertThat(view.scaleX).isEqualTo(123f)
    Assertions.assertThat(destination.value).isEqualTo(123f)
  }

  @Test
  fun testSettingMountContentOnNodeWithValue() {
    val view1 = View(ApplicationProvider.getApplicationContext())
    val group1 = OutputUnitsAffinityGroup<Any>().apply { add(OutputUnitType.HOST, view1) }
    val view2 = View(ApplicationProvider.getApplicationContext())
    val group2 = OutputUnitsAffinityGroup<Any>().apply { add(OutputUnitType.HOST, view2) }
    val source = SettableNode()
    val animatedNode = AnimatedPropertyNode(group1, AnimatedProperties.SCALE)
    val binding =
        GraphBinding.create(dataFlowGraph).apply {
          addBinding(source, animatedNode)
          activate()
        }

    testTimingSource.step(1)
    Assertions.assertThat(view1.scaleX).isEqualTo(0f)

    source.value = 123f
    testTimingSource.step(1)
    Assertions.assertThat(view1.scaleX).isEqualTo(123f)
    Assertions.assertThat(view2.scaleX).isEqualTo(1f)

    animatedNode.setMountContentGroup(group2)
    Assertions.assertThat(view2.scaleX).isEqualTo(123f)
  }

  @Test
  fun propertyNode_useRandomObject_failWhenIsNotView() {
    val view = Any()
    val group = OutputUnitsAffinityGroup<Any>().apply { add(OutputUnitType.HOST, view) }
    val source = SettableNode()
    val animatedNode = AnimatedPropertyNode(group, AnimatedProperties.SCALE)
    val binding =
        GraphBinding.create(dataFlowGraph).apply {
          addBinding(source, animatedNode)
          activate()
        }

    thrown.expect(RuntimeException::class.java)
    testTimingSource.step(1)
  }
}
