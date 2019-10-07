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
