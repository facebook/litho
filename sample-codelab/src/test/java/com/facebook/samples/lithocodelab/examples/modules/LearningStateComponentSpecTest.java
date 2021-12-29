/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.samples.lithocodelab.examples.modules;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.StateValue;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.assertj.LegacyLithoAssertions;
import com.facebook.litho.testing.assertj.SubComponentExtractor;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TestText;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LearningStateComponentSpecTest {
  @Rule public LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testComponentOnClick() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component = LearningStateComponent.create(c).canClick(true).build();

    LegacyLithoAssertions.assertThat(c, component)
        .has(
            SubComponentExtractor.subComponentWith(
                c,
                TestText.matcher(c)
                    .clickHandler(IsNull.<EventHandler<ClickEvent>>notNullValue(null))
                    .build()));
  }

  @Test
  public void testNoComponentOnClick() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component = LearningStateComponent.create(c).canClick(false).build();

    LegacyLithoAssertions.assertThat(c, component)
        .has(
            SubComponentExtractor.subComponentWith(
                c,
                TestText.matcher(c)
                    .clickHandler(IsNull.<EventHandler<ClickEvent>>nullValue(null))
                    .build()));
  }

  @Test
  public void testIncrementClickCount() {
    final StateValue<Integer> count = new StateValue<>();
    count.set(0);
    LearningStateComponentSpec.incrementClickCount(count);

    LegacyLithoAssertions.assertThat(count).valueEqualTo(1);
  }
}
