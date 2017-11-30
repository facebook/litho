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
 * <code>{@link ComponentLayoutAssert#assertThat(ComponentContext, ComponentLayout)}
 */
public final class ComponentLayoutAssert
    extends AbstractAssert<ComponentLayoutAssert, ComponentLayout> {

  private final ComponentContext mContext;

  public static ComponentLayoutAssert assertThat(
      ComponentContext c, ComponentLayout componentLayout) {
    return new ComponentLayoutAssert(c, componentLayout);
  }

  private ComponentLayoutAssert(ComponentContext c, ComponentLayout actual) {
    super(actual, ComponentLayoutAssert.class);

    mContext = c;
  }

  /**
   * Assert that a given {@link ComponentLayout} renders to null, i.e. its <code>onCreateLayout
   * </code> method returns a {@link ComponentContext#NULL_LAYOUT}.
   */
  public ComponentLayoutAssert wontRender() {
    Java6Assertions.assertThat(Component.willRender(mContext, actual))
        .overridingErrorMessage("Expected Component to render to null, but it did not.")
        .isFalse();

    return this;
  }

  public ComponentLayoutAssert willRender() {
    Java6Assertions.assertThat(Component.willRender(mContext, actual))
        .overridingErrorMessage("Expected Component to not render to null, but it did.")
        .isTrue();

    return this;
  }

  /** @see #wontRender() */
  public ComponentLayoutAssert willNotRender() {
    return wontRender();
  }
}
