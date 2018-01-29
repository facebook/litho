/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
