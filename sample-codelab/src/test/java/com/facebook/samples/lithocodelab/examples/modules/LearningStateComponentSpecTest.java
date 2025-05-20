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

import static com.facebook.litho.testing.assertj.StateValueAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.StateValue;
import com.facebook.litho.config.LithoDebugConfigurations;
import com.facebook.litho.testing.LithoTestRule;
import com.facebook.litho.testing.TestLithoView;
import com.facebook.litho.testing.assertj.LithoAssertions;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.TextComponent;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LearningStateComponentSpecTest {
  @Rule public LithoTestRule lithoTestRule = new LithoTestRule();

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        is(true));
  }

  @Test
  public void testComponentOnClick() {
    final TestLithoView testLithoView =
        lithoTestRule.render(
            componentScope ->
                LearningStateComponent.create(lithoTestRule.context).canClick(true).build());

    LithoAssertions.assertThat(testLithoView)
        .hasAnyMatchingComponent(
            new Condition<InspectableComponent>() {

              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == TextComponent.class
                    && value.getClickHandler() != null;
              }
            });
  }

  @Test
  public void testNoComponentOnClick() {
    final TestLithoView testLithoView =
        lithoTestRule.render(
            componentScope ->
                LearningStateComponent.create(lithoTestRule.context).canClick(false).build());

    LithoAssertions.assertThat(testLithoView)
        .hasAnyMatchingComponent(
            new Condition<InspectableComponent>() {

              @Override
              public boolean matches(InspectableComponent value) {
                return value.getComponentClass() == TextComponent.class
                    && value.getClickHandler() == null;
              }
            });
  }

  @Test
  public void testIncrementClickCount() {
    final StateValue<Integer> count = new StateValue<>();
    count.set(0);
    LearningStateComponentSpec.incrementClickCount(count);

    assertThat(count).valueEqualTo(1);
  }
}
