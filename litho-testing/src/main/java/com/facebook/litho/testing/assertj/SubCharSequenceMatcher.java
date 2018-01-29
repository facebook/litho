/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.testing.assertj;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.SubstringMatcher;

/**
 * An implementation like {@link SubstringMatcher} but generalized to {@link CharSequence} instead
 * of {@link CharSequence}.
 */
public abstract class SubCharSequenceMatcher extends TypeSafeMatcher<CharSequence> {
  protected final CharSequence mSubstring;

  protected SubCharSequenceMatcher(final CharSequence substring) {
    this.mSubstring = substring;
  }

  @Override
  public boolean matchesSafely(CharSequence item) {
    return evalSubstringOf(item);
  }

  @Override
  public void describeMismatchSafely(CharSequence item, Description mismatchDescription) {
    mismatchDescription.appendText("was \"").appendText(item.toString()).appendText("\"");
  }

  @Override
  public void describeTo(Description description) {
    description
        .appendText("a string ")
        .appendText(relationship().toString())
        .appendText(" ")
        .appendValue(mSubstring);
  }

  protected abstract boolean evalSubstringOf(CharSequence string);

  protected abstract CharSequence relationship();
}
