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

package com.facebook.litho.testing.assertj;

import static com.facebook.litho.testing.assertj.CharSequenceContains.containsCharSequence;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
