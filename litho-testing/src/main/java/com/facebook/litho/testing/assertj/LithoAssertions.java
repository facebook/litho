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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.StateValue;
import javax.annotation.CheckReturnValue;

/** Common entry point for Litho assertions. */
public class LithoAssertions {
  @CheckReturnValue
  public static ComponentAssert assertThat(ComponentContext c, Component component) {
    return ComponentAssert.assertThat(c, component);
  }

  @CheckReturnValue
  public static ComponentAssert assertThat(Component.Builder<?> builder) {
    return ComponentAssert.assertThat(builder);
  }

  @CheckReturnValue
  public static LithoViewAssert assertThat(LithoView lithoView) {
    return LithoViewAssert.assertThat(lithoView);
  }

  @CheckReturnValue
  public static <T> StateValueAssert<T> assertThat(StateValue<T> stateValue) {
    return StateValueAssert.assertThat(stateValue);
  }
}
