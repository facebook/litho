/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.facebook.litho.FastMath.round;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
public class FastMathTest {

  @Test
  public void testRoundPositiveUp() {
    assertThat(2).isEqualTo(round(1.6f));
  }

  @Test
  public void testRoundPositiveDown() {
    assertThat(1).isEqualTo(round(1.3f));
  }

  @Test
  public void testRoundZero() {
    assertThat(0).isEqualTo(round(0f));
  }

  @Test
  public void testRoundNegativeUp() {
    assertThat(-1).isEqualTo(round(-1.3f));
  }

  @Test
  public void testRoundNegativeDown() {
    assertThat(-2).isEqualTo(round(-1.6f));
  }

}
