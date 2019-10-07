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
