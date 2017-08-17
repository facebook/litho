/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import org.assertj.core.api.Condition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/** Wraps a Hamcrest {@link Matcher<T>} in an AssertJ Condition. */
public class HamcrestCondition<T> extends Condition<T> {

  private final Matcher<T> matcher;

  public HamcrestCondition(Matcher<T> matcher) {
    this.matcher = matcher;
    as(describeMatcher());
  }

  @Override
  public boolean matches(T value) {
    return matcher.matches(value);
  }

  private String describeMatcher() {
    final Description d = new StringDescription();
    matcher.describeTo(d);
    return d.toString();
  }
}
