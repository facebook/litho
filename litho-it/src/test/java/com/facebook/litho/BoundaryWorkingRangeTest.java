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
public class BoundaryWorkingRangeTest {

  @Test
  public void tearShouldEnterRange() {
    BoundaryWorkingRange workingRange = new BoundaryWorkingRange(1);
    assertThat(workingRange.shouldEnterRange(0, 0, 2, 1, 3)).isEqualTo(true);
    assertThat(workingRange.shouldEnterRange(4, 0, 2, 1, 3)).isEqualTo(false);
  }

  @Test
  public void testShouldExitRange() {
    BoundaryWorkingRange workingRange = new BoundaryWorkingRange(1);
    assertThat(workingRange.shouldExitRange(4, 0, 2, 1, 3)).isEqualTo(true);
    assertThat(workingRange.shouldExitRange(0, 0, 2, 1, 3)).isEqualTo(false);
  }
}
