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

import static com.facebook.litho.FastMath.round;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
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
