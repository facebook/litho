/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import com.facebook.litho.testing.InspectableComponent;
import org.assertj.core.api.Condition;
import org.hamcrest.Matcher;

/**
 * Provides various helpers to match against {@link
 * com.facebook.litho.testing.InspectableComponent}s.
 */
// TODO(T21048805): Provide examples in the javadoc.
public final class ComponentConditions {
  private ComponentConditions() {}

  /**
   * Matcher that succeeds if a {@link InspectableComponent} has a text content that exactly matches
   * the provided string.
   */
  public static Condition<InspectableComponent> textEquals(final CharSequence text) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        return text.equals(value.getTextContent());
      }
    };
  }

  /** @see #textEquals(CharSequence) */
  public static Condition<InspectableComponent> textEquals(final String text) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        return text.equals(value.getTextContent());
      }
    };
  }

  /**
   * Matcher that succeeds if a {@link InspectableComponent} has text content that matches the
   * provided condition.
   *
   * <p>N.B. We are implicitly casting the {@link CharSequence} to a {@link String} when matching so
   * that more powerful matchers can be applied like sub-string matching.
   */
  public static Condition<InspectableComponent> text(final Condition<String> condition) {
    return new Condition<InspectableComponent>() {
      @Override
      public boolean matches(InspectableComponent value) {
        return condition.matches(value.getTextContent());
      }
    };
  }

  /**
   * Matcher that succeeds if a {@link InspectableComponent} has text content that matches the
   * provided hamcrest matcher.
   *
   * @return Wrapper around {@link #text(Condition)}
   */
  public static Condition<InspectableComponent> text(final Matcher<String> matcher) {
    return text(new HamcrestCondition<>(matcher));
  }
}
