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

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

/** Tests if the argument is a string that contains a substring. */
public class CharSequenceContains extends SubCharSequenceMatcher {
  CharSequenceContains(CharSequence substring) {
    super(substring);
  }

  @Override
  protected boolean evalSubstringOf(CharSequence s) {
    return s.toString().contains(mSubstring);
  }

  @Override
  protected CharSequence relationship() {
    return "containing";
  }

  /** Alias for {@link #containsCharSequence(CharSequence)} for better discoverability. */
  @Factory
  public static Matcher<CharSequence> containsString(CharSequence substring) {
    return new CharSequenceContains(substring);
  }

  /**
   * Creates a matcher that matches if the examined {@link CharSequence} contains the specified
   * {@link CharSequence} anywhere.
   *
   * <p>For example:
   *
   * <pre>assertThat("myStringOfNote", containsCharSequence("ring"))</pre>
   *
   * @param substring the substring that the returned matcher will expect to find within any
   *     examined string
   */
  @Factory
  public static Matcher<CharSequence> containsCharSequence(CharSequence substring) {
    return new CharSequenceContains(substring);
  }
}
