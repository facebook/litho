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

package com.facebook.litho.testing;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.assertj.LithoRepresentation;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
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
    final ComponentContext c = new ComponentContext(getApplicationContext());

    Assertions.useRepresentation(new LithoRepresentation(c));

    final InlineLayoutSpec layout =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Text.create(c).text("Hello, World!").build();
          }
        };

    try {
      assertThat(layout).has(subComponentWith(c, textEquals("Doesn't match.")));
    } catch (final AssertionError assertionError) {
      assertThat(assertionError).hasMessageContaining(" 0,0-100,100 text=\"Hello, World!\"");
    }

    // Verify that resetting the representation in the same
    Assertions.useDefaultRepresentation();

    try {
      assertThat(layout).has(subComponentWith(c, textEquals("Doesn't match.")));
    } catch (final AssertionError assertionError) {
      assertThat(assertionError.getMessage()).doesNotContain(" 0,0-100,100 text=\"Hello, World!\"");
    }
  }
}
