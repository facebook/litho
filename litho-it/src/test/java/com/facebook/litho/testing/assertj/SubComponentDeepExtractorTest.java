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

import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class SubComponentDeepExtractorTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private Component mComponent;

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    mComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(Card.create(c).content(Text.create(c).text("test")))
                .build();
          }
        };
  }

  @Test
  public void testDeep() {
    final ComponentContext c = mComponentsRule.getContext();
    assertThat(c, mComponent)
        // We don't have a shallow Text component ...
        .doesNotHave(
            SubComponentExtractor.subComponentWith(
                mComponentsRule.getContext(), typeIs(Text.class)))
        // ... but we do have one deep down.
        .has(
            SubComponentDeepExtractor.deepSubComponentWith(
                mComponentsRule.getContext(), typeIs(Text.class)));
  }
}
