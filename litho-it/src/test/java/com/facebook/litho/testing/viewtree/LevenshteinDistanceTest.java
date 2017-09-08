/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.testing.viewtree;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import org.junit.Test;

/**
 * Tests for {@link LevenshteinDistance}.
 */
public class LevenshteinDistanceTest {

  @Test
  public void testGetLevenshteinDistance() {
    String s = "sunday";
    String t = "saturday";
    int distance;

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 0);
    assertThat(distance).isEqualTo(1);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 1);
    assertThat(distance).isEqualTo(2);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 2);
    assertThat(distance).isEqualTo(3);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 3);
    assertThat(distance).isEqualTo(3);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 4);
    assertThat(distance).isEqualTo(3);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 5);
    assertThat(distance).isEqualTo(3);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 5);
    assertThat(distance).isEqualTo(3);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 1);
    assertThat(distance).isEqualTo(2);

    s = "abcdef";
    t = "a";

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 0);
    assertThat(distance).isEqualTo(1);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 1);
    assertThat(distance).isEqualTo(2);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 10);
    assertThat(distance).isEqualTo(5);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 0);
    assertThat(distance).isEqualTo(1);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 1);
    assertThat(distance).isEqualTo(2);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 10);
    assertThat(distance).isEqualTo(5);

    s = "abcdef";
    t = "xxxdef";

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 0);
    assertThat(distance).isEqualTo(1);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 1);
    assertThat(distance).isEqualTo(2);

    distance = LevenshteinDistance.getLevenshteinDistance(s, t, 10);
    assertThat(distance).isEqualTo(3);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 0);
    assertThat(distance).isEqualTo(1);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 1);
    assertThat(distance).isEqualTo(2);

    distance = LevenshteinDistance.getLevenshteinDistance(t, s, 10);
    assertThat(distance).isEqualTo(3);
  }
}
