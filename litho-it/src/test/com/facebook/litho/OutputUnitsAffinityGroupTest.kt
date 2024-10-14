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

package com.facebook.litho

import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.lang.RuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OutputUnitsAffinityGroupTest {

  @JvmField @Rule var expectedException = ExpectedException.none()

  @Test
  fun testEmpty() {
    val group = OutputUnitsAffinityGroup<Any>()
    assertThat(group.size()).isZero
    assertThat(group[OutputUnitType.CONTENT]).isNull()
    assertThat(group[OutputUnitType.BACKGROUND]).isNull()
    assertThat(group[OutputUnitType.FOREGROUND]).isNull()
    assertThat(group[OutputUnitType.BORDER]).isNull()
    assertThat(group[OutputUnitType.HOST]).isNull()
  }

  @Test
  fun testAddingValid() {
    val group = OutputUnitsAffinityGroup<Any>()
    val content = Any()
    group.add(OutputUnitType.CONTENT, content)
    assertThat(group.size()).isEqualTo(1)
    assertThat(group[OutputUnitType.CONTENT]).isSameAs(content)
    assertThat(group.typeAt(0)).isEqualTo(OutputUnitType.CONTENT)
    assertThat(group.getAt(0)).isSameAs(content)
    val background = Any()
    group.add(OutputUnitType.BACKGROUND, background)
    assertThat(group.size()).isEqualTo(2)
    assertThat(group[OutputUnitType.CONTENT]).isSameAs(content)
    assertThat(group[OutputUnitType.BACKGROUND]).isSameAs(background)
    val type0 = group.typeAt(0)
    val type1 = group.typeAt(1)
    assertThat(type0).isIn(OutputUnitType.CONTENT, OutputUnitType.BACKGROUND)
    assertThat(type1).isIn(OutputUnitType.CONTENT, OutputUnitType.BACKGROUND)
    assertThat(type0).isNotEqualTo(type1)
    val value0 = group.getAt(0)
    val value1 = group.getAt(1)
    if (type0 == OutputUnitType.CONTENT) {
      assertThat(value0).isSameAs(content)
      assertThat(value1).isSameAs(background)
    } else {
      assertThat(value0).isSameAs(background)
      assertThat(value1).isSameAs(content)
    }
    val border = Any()
    group.add(OutputUnitType.BORDER, border)
    assertThat(group.size()).isEqualTo(3)
    assertThat(group[OutputUnitType.CONTENT]).isSameAs(content)
    assertThat(group[OutputUnitType.BACKGROUND]).isSameAs(background)
    assertThat(group[OutputUnitType.BORDER]).isSameAs(border)
  }

  @Test
  fun testAddingMultipleForSameType() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage("Already contains unit for type CONTENT")
    val group = OutputUnitsAffinityGroup<Any>()
    group.add(OutputUnitType.CONTENT, Any())
    group.add(OutputUnitType.CONTENT, Any())
  }

  @Test
  fun testAddingNull() {
    expectedException.expect(IllegalArgumentException::class.java)
    expectedException.expectMessage("value should not be null")
    val group = OutputUnitsAffinityGroup<Any?>()
    group.add(OutputUnitType.FOREGROUND, null)
  }

  @Test
  fun testAddingHostToNotEmptyGroup() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage(
        "OutputUnitType.HOST unit should be the only member of an OutputUnitsAffinityGroup")
    val group = OutputUnitsAffinityGroup<Any>()
    group.add(OutputUnitType.CONTENT, Any())
    group.add(OutputUnitType.HOST, Any())
  }

  @Test
  fun testAddingToGroupThatContainsHost() {
    expectedException.expect(RuntimeException::class.java)
    expectedException.expectMessage(
        "OutputUnitType.HOST unit should be the only member of an OutputUnitsAffinityGroup")
    val group = OutputUnitsAffinityGroup<Any>()
    group.add(OutputUnitType.HOST, Any())
    group.add(OutputUnitType.BACKGROUND, Any())
  }

  @Test
  fun testIllegalRange() {
    expectedException.expect(IndexOutOfBoundsException::class.java)
    expectedException.expectMessage("index=2, size=2")
    val group = OutputUnitsAffinityGroup<Any>()
    group.add(OutputUnitType.BACKGROUND, Any())
    group.add(OutputUnitType.FOREGROUND, Any())
    group.getAt(2)
  }
}
