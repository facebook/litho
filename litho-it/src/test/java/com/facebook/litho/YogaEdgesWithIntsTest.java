/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class YogaEdgesWithIntsTest {

  @Test
  public void testDefault() {
    CommonPropsHolder.YogaEdgesWithInts edge = new CommonPropsHolder.YogaEdgesWithInts();

    assertThat(edge.size()).isEqualTo(0);
    edge.add(com.facebook.yoga.YogaEdge.ALL, 0);
    assertThat(edge.size()).isEqualTo(1);
    assertThat(edge.getEdge(0)).isEqualTo(com.facebook.yoga.YogaEdge.ALL);
    assertThat(edge.getValue(0)).isEqualTo(0);

    edge.add(com.facebook.yoga.YogaEdge.LEFT, 1);
    edge.add(com.facebook.yoga.YogaEdge.RIGHT, 0);
    assertThat(edge.size()).isEqualTo(3);
    assertThat(edge.getEdge(1)).isEqualTo(com.facebook.yoga.YogaEdge.LEFT);
    assertThat(edge.getValue(1)).isEqualTo(1);
    assertThat(edge.getEdge(2)).isEqualTo(com.facebook.yoga.YogaEdge.RIGHT);
    assertThat(edge.getValue(2)).isEqualTo(0);
  }

  @Test
  public void testDefaultZeros() {
    CommonPropsHolder.YogaEdgesWithInts edge = new CommonPropsHolder.YogaEdgesWithInts();

    assertThat(edge.size()).isEqualTo(0);
    edge.add(com.facebook.yoga.YogaEdge.ALL, 0);
    edge.add(com.facebook.yoga.YogaEdge.LEFT, 0);
    edge.add(com.facebook.yoga.YogaEdge.TOP, 0);
    edge.add(com.facebook.yoga.YogaEdge.BOTTOM, 0);

    assertThat(edge.size()).isEqualTo(4);
    assertThat(edge.getEdge(3)).isEqualTo(com.facebook.yoga.YogaEdge.BOTTOM);
    assertThat(edge.getValue(0)).isEqualTo(0);
    assertThat(edge.getValue(1)).isEqualTo(0);
    assertThat(edge.getValue(2)).isEqualTo(0);
    assertThat(edge.getValue(3)).isEqualTo(0);

    edge.add(com.facebook.yoga.YogaEdge.RIGHT, 1);
    assertThat(edge.size()).isEqualTo(5);
    assertThat(edge.getEdge(4)).isEqualTo(com.facebook.yoga.YogaEdge.RIGHT);
    assertThat(edge.getValue(4)).isEqualTo(1);
  }

  @Test
  public void testSettingMoreValuesThanTheSizeOfTheYogaEdges() {
    CommonPropsHolder.YogaEdgesWithInts edge = new CommonPropsHolder.YogaEdgesWithInts();
    edge.add(com.facebook.yoga.YogaEdge.LEFT, 0);
    edge.add(com.facebook.yoga.YogaEdge.TOP, 0);
    edge.add(com.facebook.yoga.YogaEdge.RIGHT, 0);
    edge.add(com.facebook.yoga.YogaEdge.BOTTOM, 0);
    edge.add(com.facebook.yoga.YogaEdge.START, 0);
    edge.add(com.facebook.yoga.YogaEdge.END, 0);
    edge.add(com.facebook.yoga.YogaEdge.HORIZONTAL, 0);
    edge.add(com.facebook.yoga.YogaEdge.VERTICAL, 0);
    edge.add(com.facebook.yoga.YogaEdge.ALL, 5);

    assertThat(edge.size()).isEqualTo(9);
    assertThat(edge.getEdge(8)).isEqualTo(com.facebook.yoga.YogaEdge.ALL);
    assertThat(edge.getValue(8)).isEqualTo(5);

    edge.add(com.facebook.yoga.YogaEdge.ALL, 10);
    assertThat(edge.size()).isEqualTo(10);
    assertThat(edge.getEdge(9)).isEqualTo(com.facebook.yoga.YogaEdge.ALL);
    assertThat(edge.getValue(9)).isEqualTo(10);
  }
}
