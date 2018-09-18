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
    YogaEdgesWithInts edgesNew = new CommonPropsHolder.YogaEdgesWithIntsImplOptimized();

    YogaEdgesWithInts[] bothEdges = {edgesNew};

    for (YogaEdgesWithInts edge : bothEdges) {
      assertThat(edge.size() == 0);
      edge.add(com.facebook.yoga.YogaEdge.ALL, 0);
      assertThat(edge.size() == 1);
      assertThat(edge.getEdge(0) == com.facebook.yoga.YogaEdge.ALL);
      assertThat(edge.getValue(0) == 0);

      edge.add(com.facebook.yoga.YogaEdge.LEFT, 1);
      edge.add(com.facebook.yoga.YogaEdge.RIGHT, 0);
      assertThat(edge.size() == 3);
      assertThat(edge.getEdge(1) == com.facebook.yoga.YogaEdge.LEFT);
      assertThat(edge.getValue(1) == 1);
      assertThat(edge.getEdge(2) == com.facebook.yoga.YogaEdge.RIGHT);
      assertThat(edge.getValue(2) == 0);
    }
  }

  @Test
  public void testDefaultZeros() {
    YogaEdgesWithInts edgesNew = new CommonPropsHolder.YogaEdgesWithIntsImplOptimized();

    YogaEdgesWithInts[] bothEdges = {edgesNew};

    for (YogaEdgesWithInts edge : bothEdges) {
      assertThat(edge.size() == 0);
      edge.add(com.facebook.yoga.YogaEdge.ALL, 0);
      edge.add(com.facebook.yoga.YogaEdge.LEFT, 0);
      edge.add(com.facebook.yoga.YogaEdge.TOP, 0);
      edge.add(com.facebook.yoga.YogaEdge.BOTTOM, 0);

      assertThat(edge.size() == 4);
      assertThat(edge.getEdge(3) == com.facebook.yoga.YogaEdge.BOTTOM);
      assertThat(edge.getValue(0) == 0);
      assertThat(edge.getValue(1) == 0);
      assertThat(edge.getValue(2) == 0);
      assertThat(edge.getValue(3) == 0);

      edge.add(com.facebook.yoga.YogaEdge.RIGHT, 1);
      assertThat(edge.size() == 5);
      assertThat(edge.getEdge(5) == com.facebook.yoga.YogaEdge.RIGHT);
      assertThat(edge.getValue(5) == 1);
    }
  }
}
