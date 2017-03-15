// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.dataflow;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class InputSpecTest {

  @Test
  public void testStandardFloatValue() {
    float value = 3.141592654f;
    long spec = InputSpec.create(false, value);
    assertEquals(value, InputSpec.getValue(spec));
  }

  @Test
  public void testNegativeFloatValue() {
    float value = -3.141592654f;
    long spec = InputSpec.create(false, value);
    assertEquals(value, InputSpec.getValue(spec));
  }

  @Test
  public void testNaNValue() {
    float value = Float.NaN;
    long spec = InputSpec.create(false, value);
    assertTrue(Float.isNaN(InputSpec.getValue(spec)));
  }

  @Test
  public void testPosInfinityValue() {
    float value = Float.POSITIVE_INFINITY;
    long spec = InputSpec.create(false, value);
    assertEquals(value, InputSpec.getValue(spec));
  }

  @Test
  public void testNegInfinityValue() {
    float value = Float.NEGATIVE_INFINITY;
    long spec = InputSpec.create(false, value);
    assertEquals(value, InputSpec.getValue(spec));
  }

  @Test
  public void testNeedsSpecificValueFlag() {
    long trueSpec = InputSpec.create(true, 0);
    long falseSpec = InputSpec.create(false, 0);
    assertEquals(true, InputSpec.getNeedsSpecificValue(trueSpec));
    assertEquals(false, InputSpec.getNeedsSpecificValue(falseSpec));
  }
}
