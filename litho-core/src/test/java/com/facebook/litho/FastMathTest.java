/**
 * Copyright (c) 2014-present, Facebook, Inc.
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

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class FastMathTest {

  @Test
  public void testRoundPositiveUp() {
    assertEquals(FastMath.round(1.6f), 2);
  }

  @Test
  public void testRoundPositiveDown() {
    assertEquals(FastMath.round(1.3f), 1);
  }

  @Test
  public void testRoundZero() {
    assertEquals(FastMath.round(0f), 0);
  }

  @Test
  public void testRoundNegativeUp() {
    assertEquals(FastMath.round(-1.3f), -1);
  }

  @Test
  public void testRoundNegativeDown() {
    assertEquals(FastMath.round(-1.6f), -2);
  }

}
