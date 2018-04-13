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

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
