/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
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
