/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.testing.assertj;

import com.facebook.litho.StateValue;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Java6Assertions;

/**
 * Assertion methods for {@link StateValue<T>}.
 *
 * <p>To create an instance of this class, invoke <code>
 * {@link StateValueAssert#assertThat(StateValue)}</code>.
 *
 * <p>Alternatively, use {@link LithoAssertions} which provides entry points to all Litho AssertJ
 * helpers.
 *
 * @param <T> Type of the underlying state value.
 */
public class StateValueAssert<T> extends AbstractAssert<StateValueAssert<T>, StateValue<T>> {

  public static <T> StateValueAssert<T> assertThat(StateValue<T> actual) {
    return new StateValueAssert<>(actual);
  }

  StateValueAssert(StateValue<T> stateValue) {
    super(stateValue, StateValueAssert.class);
  }

  /** Equivalent to calling <code>assertThat(value.get()).isEqualTo(value)</code>. */
  public StateValueAssert<T> valueEqualTo(T value) {
    Java6Assertions.assertThat(actual.get())
        .overridingErrorMessage(
            "Expected state value to equal to <%s>, but was <%s>.", value, actual.get())
        .isEqualTo(value);

    return this;
  }
}
