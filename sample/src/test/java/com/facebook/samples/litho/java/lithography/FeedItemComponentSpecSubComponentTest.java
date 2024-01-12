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

import static com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Demonstrates testing sub components based on {@link FeedItemComponentSpec}'s {@link
 * FooterComponentSpec} use.
 */
@RunWith(LithoTestRunner.class)
public class FeedItemComponentSpecSubComponentTest {
  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void subComponentWithoutProperties() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component = makeComponentWithTextInSubcomponent("Any String");

    // This will match as long as there is a FooterComponent, with any props.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).build()));
  }

  @Test
  public void subComponentWithRawText() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component = makeComponentWithTextInSubcomponent("Raw Text");

    // This will match if the component has exactly the specified text as property.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(subComponentWith(c, TestFooterComponent.matcher(c).text("Raw Text").build()));
  }

  @Test
  public void subComponentWithMatcher() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component =
        makeComponentWithTextInSubcomponent(
            "Long Text That We Don't Want To Match In Its Entirety");

    // We can pass in any of the default hamcrest matchers here.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c, TestFooterComponent.matcher(c).text(containsString("Want To Match")).build()));
  }

  @Test
  public void subComponentWithRes() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    String string = c.getAndroidContext().getResources().getString(android.R.string.cancel);
    final Component component = makeComponentWithTextInSubcomponent(string);

    // You can also reference resources here directly.
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c, TestFooterComponent.matcher(c).textRes(android.R.string.cancel).build()));
  }

  @Test
  public void footerHasNoClickHandler() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component = makeComponentWithTextInSubcomponent("Any Text");

    // Components commonly have conditional handlers assigned. Using the clickHandler matcher
    // we can assert whether or not a given component has a handler attached to them.
    //noinspection unchecked
    assertThat(c, component)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                TestFooterComponent.matcher(c)
                    .clickHandler(IsNull.<EventHandler<ClickEvent>>nullValue(null))
                    .build()));
  }

  private Component makeComponentWithTextInSubcomponent(String value) {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    return FeedItemComponent.create(c).artist(new Artist("Some Name", value, 2001)).build();
  }
}
