/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.assertj;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LithoView;
import javax.annotation.CheckReturnValue;
import org.assertj.core.api.Java6Assertions;

/**
 * Common entry point for Litho assertions. Can be statically imported instead of {@link
 * Java6Assertions#assertThat}.
 */
public class LithoAssertions extends Java6Assertions {
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
}
