/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.testing.specmodels;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.subcomponents.InspectableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.TestText;
import com.facebook.litho.widget.Text;
import org.assertj.core.api.Condition;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class InjectPropMatcherGenerationTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testInjectPropMatching() {
    final ComponentContext c = mComponentsRule.getContext();
    final MyInjectProp component =
        new MyInjectProp(c.getAndroidContext()).create(c).normalString("normal string").build();
    // Faking some DI mechanism doing its thing.
    component.injectedString = "injected string";
    component.injectedKettle = new MyInjectPropSpec.Kettle(92f);
    component.injectedComponent = Text.create(c).text("injected text").build();

    final Condition<InspectableComponent> matcher =
        TestMyInjectProp.matcher(c)
            .normalString("normal string")
            .injectedString("injected string")
            .injectedKettle(
                new CustomTypeSafeMatcher<MyInjectPropSpec.Kettle>("matches temperature") {
                  @Override
                  protected boolean matchesSafely(MyInjectPropSpec.Kettle item) {
                    return Math.abs(item.temperatureCelsius - 92f) < 0.1;
                  }
                })
            .injectedComponent(TestText.matcher(c).text("injected text").build())
            .build();

    assertThat(c, component).has(deepSubComponentWith(c, matcher));
  }
}
