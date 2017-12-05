/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
import com.facebook.litho.testing.assertj.ComponentMatcher;
import com.facebook.litho.testing.specmodels.MyGeneric;
import com.facebook.litho.testing.specmodels.MyGenericSpec;
import com.facebook.litho.testing.specmodels.TestMyGeneric;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
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
        new MyGeneric<>(new MyGenericSpec()).create(c).genericProp(mGenericProp).build();
    final ComponentMatcher matcher = TestMyGeneric.matcher(c).genericProp(mGenericProp).build();

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
