/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import static com.facebook.litho.testing.assertj.ComponentConditions.text;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static com.facebook.litho.testing.assertj.SubComponentExtractor.subComponentWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.error.TestCrasher;
import com.facebook.litho.testing.error.TestErrorBoundary;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests error handling in {@link ComponentLifecycle} using the components {@link
 * com.facebook.litho.testing.error.TestErrorBoundarySpec} and {@link
 * com.facebook.litho.testing.error.TestCrasherSpec}.
 */
@RunWith(ComponentsTestRunner.class)
public class ComponentLifecycleErrorTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();
  private boolean mPreviousOnErrorConfig;

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Before
  public void saveConfiguration() {
    mPreviousOnErrorConfig = ComponentsConfiguration.enableOnErrorHandling;
  }

  @After
  public void restoreConfiguration() {
    ComponentsConfiguration.enableOnErrorHandling = mPreviousOnErrorConfig;
  }

  @Test
  public void testErrorBoundary() {
    ComponentsConfiguration.enableOnErrorHandling = true;

    final ComponentContext c = mComponentsRule.getContext();

    assertThat(TestErrorBoundary.create(c).child(TestCrasher.create(c).build()))
        .has(subComponentWith(c, text(containsString("A WILD ERROR APPEARS"))));
  }

  @Test
  public void testErrorBoundaryWhenDisabled() {
    ComponentsConfiguration.enableOnErrorHandling = false;

    final ComponentContext c = mComponentsRule.getContext();

    final TestErrorBoundary.Builder builder =
        TestErrorBoundary.create(c).child(TestCrasher.create(c).build());

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasMessage("Boom!");
  }
}
