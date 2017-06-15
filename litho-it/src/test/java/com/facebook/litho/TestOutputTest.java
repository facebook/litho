/*
 * Copyright (c) 2017-present, Facebook, Inc.
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

import static org.assertj.core.api.Java6Assertions.assertThat;

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

    assertThat(mTestOutput.getBounds().left).isEqualTo(0);
    assertThat(mTestOutput.getBounds().top).isEqualTo(1);
    assertThat(mTestOutput.getBounds().right).isEqualTo(3);
    assertThat(mTestOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testRectBoundsSet() {
    final Rect bounds = new Rect(0, 1, 3, 4);
    mTestOutput.setBounds(bounds);
    assertThat(mTestOutput.getBounds().left).isEqualTo(0);
    assertThat(mTestOutput.getBounds().top).isEqualTo(1);
    assertThat(mTestOutput.getBounds().right).isEqualTo(3);
    assertThat(mTestOutput.getBounds().bottom).isEqualTo(4);
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
    assertThat(testOutput.getBounds()).isEqualTo(new Rect());
    assertThat(testOutput.getTestKey()).isNull();
    assertThat(testOutput.getHostMarker()).isEqualTo(-1);
    assertThat(testOutput.getLayoutOutputId()).isEqualTo(-1);
  }
}
