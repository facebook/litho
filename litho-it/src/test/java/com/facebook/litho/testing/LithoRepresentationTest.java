/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing;

import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.assertj.LithoAssertions;
import com.facebook.litho.testing.assertj.LithoRepresentation;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LithoRepresentationTest {

  @Before
  public void assumeInDebugMode() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testLithoRepresentation() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);

    Assertions.useRepresentation(new LithoRepresentation(c));

    final InlineLayoutSpec layout =
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Text.create(c).text("Hello, World!").build();
          }
        };

    try {
      LithoAssertions.assertThat(layout).has(subComponentWith(c, textEquals("Doesn't match.")));
    } catch (final AssertionError assertionError) {
      LithoAssertions.assertThat(assertionError)
          .hasMessageContaining("Text{0, 0 - 100, 100 text=\"Hello, World!\"");
    }

    // Verify that resetting the representation in the same
    Assertions.useDefaultRepresentation();

    try {
      LithoAssertions.assertThat(layout).has(subComponentWith(c, textEquals("Doesn't match.")));
    } catch (final AssertionError assertionError) {
      LithoAssertions.assertThat(assertionError.getMessage())
          .doesNotContain("Text{0, 0 - 100, 100 text=\"Hello, World!\"");
    }
  }
}
