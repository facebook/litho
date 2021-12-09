// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

package com.facebook.samples.litho.java.lithography;

import static com.facebook.litho.testing.assertj.ComponentConditions.inspectedTypeIs;
import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.assertj.core.api.Java6Assertions.allOf;
import static org.assertj.core.data.Index.atIndex;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Card;
import com.facebook.litho.widget.TestCard;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class FeedItemCardSpecTest {
  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  private Component mComponent;
  private static final Artist ARTIST = new Artist("Sindre Sorhus", "JavaScript Rockstar", 2010);

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));

    final ComponentContext c = mLegacyLithoViewRule.getContext();

    mComponent = FeedItemCard.create(c).artist(ARTIST).build();
  }

  @Test
  public void testShallowSubComponents() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    assertThat(c, mComponent)
        .extractingSubComponentAt(0)
        .extractingSubComponents(c)
        .hasSize(1)
        .has(
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == Card.class;
              }
            },
            atIndex(0));
  }

  @Test
  public void testDeepSubComponents() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    // N.B. This manual way of testing is not recommended and will be replaced by more high-level
    // matchers, but illustrates how it can be used in case more fine-grained assertions are
    // required.
    assertThat(c, mComponent)
        .extractingSubComponentAt(0)
        .extractingSubComponentsDeeply(c)
        .hasSize(23) // TODO: T53372437 Remove or rewrite test.
        .has(
            new Condition<InspectableComponent>() {
              @Override
              public boolean matches(InspectableComponent value) {
                describedAs(value.getComponentClass() + " with text " + value.getTextContent());
                return value.getComponentClass() == Text.class
                    && "JavaScript Rockstar".equals(value.getTextContent());
              }
            },
            atIndex(10));
  }

  @Test
  public void testDeepSubComponentText() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    assertThat(c, mComponent)
        .has(
            allOf(
                deepSubComponentWith(c, textEquals("JavaScript Rockstar")),
                deepSubComponentWith(c, textEquals("Sindre Sorhus"))));
  }

  @Test
  public void testDeepSubComponentTextType() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    assertThat(c, mComponent).has(deepSubComponentWith(c, inspectedTypeIs(Text.class)));
  }

  @Test
  public void testDeepMatcherMatching() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    // You can also test nested sub-components by passing in another Matcher where
    // you would normally provide a Component. In this case we provide a Matcher
    // of the FeedItemComponent to the Card Matcher's content prop.
    assertThat(c, mComponent)
        .has(
            deepSubComponentWith(
                c,
                TestCard.matcher(c)
                    .content(TestFeedItemComponent.matcher(c).artist(ARTIST).build())
                    .build()));
  }
}
