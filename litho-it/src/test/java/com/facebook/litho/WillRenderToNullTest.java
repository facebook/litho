/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.Layout.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.View;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.testing.util.InlineLayoutWithSizeSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WillRenderToNullTest {

  private static final InlineLayoutSpec NULL_SPEC =
      new InlineLayoutSpec() {

        @Override
        protected ComponentLayout onCreateLayout(ComponentContext c) {
          return null;
        }
      };

  private static final InlineLayoutSpec NONNULL_SPEC =
      new InlineLayoutSpec() {

        @Override
        protected ComponentLayout onCreateLayout(ComponentContext c) {
          return Row.create(c).build();
        }
      };

  private static final InlineLayoutWithSizeSpec LAYOUT_WITH_SIZE_SPEC =
      new InlineLayoutWithSizeSpec() {

        @Override
        protected ComponentLayout onCreateLayoutWithSizeSpec(
            ComponentContext c, int widthSpec, int heightSpec) {
          return Row.create(c)
              .widthDip(View.MeasureSpec.getSize(widthSpec))
              .heightDip(View.MeasureSpec.getSize(heightSpec))
              .build();
        }
      };

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testWillRenderToNullForComponentThatReturnsNull() {
    ComponentContext c = new ComponentContext(application);
    assertThat(Component.willRenderToNull(create(c, NULL_SPEC).build())).isTrue();
  }

  @Test
  public void testWillRenderToNullForComponentThatReturnsNonNull() {
    ComponentContext c = new ComponentContext(application);
    assertThat(Component.willRenderToNull(create(c, NONNULL_SPEC).build())).isFalse();
  }

  @Test
  public void testWillRenderToNullForComponentWithSizeSpecThrowsException() {
    mExpectedException.expect(IllegalArgumentException.class);
    mExpectedException.expectMessage("@OnCreateLayoutWithSizeSpec");

    ComponentContext c = new ComponentContext(application);
    Component.willRenderToNull(create(c, LAYOUT_WITH_SIZE_SPEC).build());
  }
}
