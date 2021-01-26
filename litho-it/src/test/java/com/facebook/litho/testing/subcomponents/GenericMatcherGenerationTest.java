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

package com.facebook.litho.testing.subcomponents;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.specmodels.MyGeneric;
import com.facebook.litho.testing.specmodels.TestMyGeneric;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class GenericMatcherGenerationTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  private final GenericProp mGenericProp = new GenericProp();

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testGenericPropMatching() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component =
        new MyGeneric<>(c.getAndroidContext()).create(c).genericProp(mGenericProp).build();
    final Condition<InspectableComponent> matcher =
        TestMyGeneric.matcher(c).genericProp(mGenericProp).build();

    assertThat(c, component).has(deepSubComponentWith(c, matcher));
  }

  // This is just to fulfill the prop requirements, reusing an existing interface we've got lying
  // around.
  public static class GenericProp implements HasEventDispatcher {
    @Override
    public EventDispatcher getEventDispatcher() {
      return null;
    }
  }
}
