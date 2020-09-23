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

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OutputUnitsAffinityGroupTest {

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testEmpty() {
    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    assertThat(group.size()).isZero();
    assertThat(group.getContentType(OutputUnitType.CONTENT)).isNull();
    assertThat(group.getContentType(OutputUnitType.BACKGROUND)).isNull();
    assertThat(group.getContentType(OutputUnitType.FOREGROUND)).isNull();
    assertThat(group.getContentType(OutputUnitType.BORDER)).isNull();
    assertThat(group.getContentType(OutputUnitType.HOST)).isNull();
  }

  @Test
  public void testAddingValid() {
    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    final Object content = new Object();
    group.add(OutputUnitType.CONTENT, content);

    assertThat(group.size()).isEqualTo(1);
    assertThat(group.getContentType(OutputUnitType.CONTENT)).isSameAs(content);
    assertThat(group.typeAt(0)).isEqualTo(OutputUnitType.CONTENT);
    assertThat(group.getAt(0)).isSameAs(content);

    final Object background = new Object();
    group.add(OutputUnitType.BACKGROUND, background);

    assertThat(group.size()).isEqualTo(2);
    assertThat(group.getContentType(OutputUnitType.CONTENT)).isSameAs(content);
    assertThat(group.getContentType(OutputUnitType.BACKGROUND)).isSameAs(background);
    final int type0 = group.typeAt(0);
    final int type1 = group.typeAt(1);
    assertThat(type0).isIn(OutputUnitType.CONTENT, OutputUnitType.BACKGROUND);
    assertThat(type1).isIn(OutputUnitType.CONTENT, OutputUnitType.BACKGROUND);
    assertThat(type0).isNotEqualTo(type1);
    final Object value0 = group.getAt(0);
    final Object value1 = group.getAt(1);
    if (type0 == OutputUnitType.CONTENT) {
      assertThat(value0).isSameAs(content);
      assertThat(value1).isSameAs(background);
    } else {
      assertThat(value0).isSameAs(background);
      assertThat(value1).isSameAs(content);
    }

    final Object border = new Object();
    group.add(OutputUnitType.BORDER, border);
    assertThat(group.size()).isEqualTo(3);
    assertThat(group.getContentType(OutputUnitType.CONTENT)).isSameAs(content);
    assertThat(group.getContentType(OutputUnitType.BACKGROUND)).isSameAs(background);
    assertThat(group.getContentType(OutputUnitType.BORDER)).isSameAs(border);
  }

  @Test
  public void testAddingMultipleForSameType() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage("Already contains unit for type CONTENT");

    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    group.add(OutputUnitType.CONTENT, new Object());
    group.add(OutputUnitType.CONTENT, new Object());
  }

  @Test
  public void testAddingNull() {
    mExpectedException.expect(IllegalArgumentException.class);
    mExpectedException.expectMessage("value should not be null");

    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    group.add(OutputUnitType.FOREGROUND, null);
  }

  @Test
  public void testAddingHostToNotEmptyGroup() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage(
        "OutputUnitType.HOST unit should be the only member of an OutputUnitsAffinityGroup");

    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    group.add(OutputUnitType.CONTENT, new Object());
    group.add(OutputUnitType.HOST, new Object());
  }

  @Test
  public void testAddingToGroupThatContainsHost() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage(
        "OutputUnitType.HOST unit should be the only member of an OutputUnitsAffinityGroup");

    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    group.add(OutputUnitType.HOST, new Object());
    group.add(OutputUnitType.BACKGROUND, new Object());
  }

  @Test
  public void testIllegalRange() {
    mExpectedException.expect(IndexOutOfBoundsException.class);
    mExpectedException.expectMessage("index=2, size=2");

    final OutputUnitsAffinityGroup<Object> group = new OutputUnitsAffinityGroup<>();

    group.add(OutputUnitType.BACKGROUND, new Object());
    group.add(OutputUnitType.FOREGROUND, new Object());

    group.getAt(2);
  }
}
