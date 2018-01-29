/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.testing.assertj;

import static com.facebook.litho.testing.assertj.CharSequenceContains.containsCharSequence;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Test;

public class CharSequenceContainsTest {
  @Test
  public void testContainsCharSequence() {
    final Matcher<CharSequence> angeryMatcher = containsCharSequence("angery");

    assertThat(angeryMatcher.matches("angery reaccs only")).isTrue();
    assertThat(angeryMatcher.matches("delet dis")).isFalse();

    final StringDescription description = new StringDescription();
    angeryMatcher.describeMismatch("delet", description);

    assertThat(description.toString()).isEqualTo("was \"delet\"");
  }
}
