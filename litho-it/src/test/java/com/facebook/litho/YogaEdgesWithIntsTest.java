/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
    YogaEdgesWithInts edgesOld = new CommonProps.YogaEdgesWithIntsImpl();
    YogaEdgesWithInts edgesNew = new CommonProps.YogaEdgesWithIntsImplOptimized();

    YogaEdgesWithInts[] bothEdges = {edgesOld, edgesNew};

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
    YogaEdgesWithInts edgesOld = new CommonProps.YogaEdgesWithIntsImpl();
    YogaEdgesWithInts edgesNew = new CommonProps.YogaEdgesWithIntsImplOptimized();

    YogaEdgesWithInts[] bothEdges = {edgesOld, edgesNew};

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
