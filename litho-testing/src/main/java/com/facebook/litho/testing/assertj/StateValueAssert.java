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
