/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

public class HamcrestConditionTest {
  @Test
  public void testMatcher() {
    final HamcrestCondition<Object> aString =
        new HamcrestCondition<>(IsInstanceOf.instanceOf(String.class));

    assertThat("abc").is(aString);
    assertThat(123).isNot(aString);
  }
}
