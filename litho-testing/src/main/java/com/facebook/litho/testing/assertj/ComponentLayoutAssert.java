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
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Java6Assertions;

/**
 * Assertion methods for {@link com.facebook.litho.ComponentLayout}s.
 *
 * <p>
 *  To create an instance of this class, invoke
 * <code>{@link ComponentLayoutAssert#assertThat(ComponentLayout)}
 */
public final class ComponentLayoutAssert
    extends AbstractAssert<ComponentLayoutAssert, ComponentLayout> {

  public static ComponentLayoutAssert assertThat(ComponentLayout componentLayout) {
    return new ComponentLayoutAssert(componentLayout);
  }

  private ComponentLayoutAssert(ComponentLayout actual) {
    super(actual, ComponentLayoutAssert.class);
  }

  /**
   * Assert that a given {@link ComponentLayout} renders to null, i.e. its <code>onCreateLayout
   * </code> method returns a {@link ComponentContext#NULL_LAYOUT}.
   */
  public ComponentLayoutAssert willRenderToNull() {
    Java6Assertions.assertThat(Component.willRenderToNull(actual))
        .overridingErrorMessage("Expected Component to render to null, but it did not.")
        .isTrue();

    return this;
  }

  public ComponentLayoutAssert wontRenderToNull() {
    Java6Assertions.assertThat(Component.willRenderToNull(actual))
        .overridingErrorMessage("Expected Component to not render to null, but it did.")
        .isFalse();

    return this;
  }

  /** @see #wontRenderToNull() */
  public ComponentLayoutAssert willNotRenderToNull() {
    return wontRenderToNull();
  }
}
