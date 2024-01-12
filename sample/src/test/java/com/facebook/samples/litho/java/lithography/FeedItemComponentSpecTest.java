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

import static com.facebook.litho.testing.assertj.ComponentConditions.textEquals;
import static com.facebook.litho.testing.assertj.LegacyLithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.LithoViewSubComponentDeepExtractor.deepSubComponentWith;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static com.facebook.litho.testing.subcomponents.SubComponent.legacySubComponent;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.subcomponents.SubComponent;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class FeedItemComponentSpecTest {
  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  private Component mComponent;

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    mComponent =
        FeedItemComponent.create(c)
            .artist(new Artist("Sindre Sorhus", "Rockstar Developer", 2010))
            .build();
  }

  @Test
  public void recursiveSubComponentExists() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    assertThat(c, mComponent).extractingSubComponentAt(0).extractingSubComponents(c).hasSize(2);
  }

  @Test
  public void testLithoViewSubComponentMatching() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final LithoView lithoView = ComponentTestHelper.mountComponent(c, mComponent);

    assertThat(lithoView).has(deepSubComponentWith(textEquals("Sindre Sorhus")));
  }

  @Test
  public void subComponentLegacyBridge() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    assertThat(c, mComponent)
        .extractingSubComponentAt(0)
        .has(
            subComponentWith(
                c,
                legacySubComponent(
                    SubComponent.of(
                        FooterComponent.create(c).text("Rockstar Developer").build()))));
  }
}
