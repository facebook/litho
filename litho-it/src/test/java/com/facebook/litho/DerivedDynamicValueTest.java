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
public class DerivedDynamicValueTest {

  @Test
  public void testModifierIsApplied() {
    final DynamicValue<Integer> dynamicValue = new DynamicValue<>(5);
    final DerivedDynamicValue<Integer, Integer> multiplyByFiveDynamicValue =
        new DerivedDynamicValue<Integer, Integer>(
            dynamicValue,
            new DerivedDynamicValue.Modifier<Integer, Integer>() {
              @Override
              public Integer modify(Integer in) {
                return 5 * in;
              }
            });
    assertThat(multiplyByFiveDynamicValue.get()).isEqualTo(25);
    dynamicValue.set(2);
    assertThat(multiplyByFiveDynamicValue.get()).isEqualTo(10);
  }
}
