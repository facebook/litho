// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;

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
