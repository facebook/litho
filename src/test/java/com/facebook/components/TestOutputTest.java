/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(ComponentsTestRunner.class)
public class TestOutputTest {
  private TestOutput mTestOutput;

  @Before
  public void setup() {
    mTestOutput = new TestOutput();
  }

  @Test
  public void testPositionAndSizeSet() {
    mTestOutput.setBounds(0, 1, 3, 4);

    assertEquals(0, mTestOutput.getBounds().left);
    assertEquals(1, mTestOutput.getBounds().top);
    assertEquals(3, mTestOutput.getBounds().right);
    assertEquals(4, mTestOutput.getBounds().bottom);
  }

  @Test
  public void testRectBoundsSet() {
    final Rect bounds = new Rect(0, 1, 3, 4);
    mTestOutput.setBounds(bounds);
    assertEquals(0, mTestOutput.getBounds().left);
    assertEquals(1, mTestOutput.getBounds().top);
    assertEquals(3, mTestOutput.getBounds().right);
    assertEquals(4, mTestOutput.getBounds().bottom);
  }

  @Test
  public void testRelease() {
    mTestOutput.setBounds(0, 1, 2, 3);
    mTestOutput.setTestKey("testkey");
    mTestOutput.setHostMarker(1337);

    mTestOutput.release();

    assertDefaultValues(mTestOutput);
  }

  @Test
  public void testPoolRelease() {
    final TestOutput testOutput = ComponentsPools.acquireTestOutput();
    testOutput.setBounds(0, 1, 2, 3);
    testOutput.setTestKey("testkey");
    testOutput.setHostMarker(1337);
    testOutput.setLayoutOutputId(42);

    ComponentsPools.release(testOutput);

    assertDefaultValues(testOutput);
  }

  private static void assertDefaultValues(TestOutput testOutput) {
    assertEquals(new Rect(), testOutput.getBounds());
    assertNull(testOutput.getTestKey());
    assertEquals(-1, testOutput.getHostMarker());
    assertEquals(-1, testOutput.getLayoutOutputId());
  }
}
