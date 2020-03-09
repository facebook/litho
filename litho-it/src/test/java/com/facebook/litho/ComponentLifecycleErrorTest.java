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

package com.facebook.litho;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayoutWithSizeSpec;
import com.facebook.litho.testing.error.TestCrasherOnMount;
import com.facebook.litho.testing.error.TestErrorBoundary;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests error handling in {@link ComponentLifecycle} using the {@link
 * com.facebook.litho.testing.error.TestErrorBoundarySpec} against components crashing in various
 * different lifecycle methods.
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
  public void testOnCreateLayoutErrorBoundary() throws Exception {
    ComponentsConfiguration.enableOnErrorHandling = true;
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is
    // called for TestCrasherOnCreateLayout is called.
    ComponentsConfiguration.isReconciliationEnabled = false;

    final ComponentContext c = mComponentsRule.getContext();

    final Component component =
        TestErrorBoundary.create(c).child(TestCrasherOnCreateLayout.create(c).build()).build();

    assertThat(c, component).afterStateUpdate().hasVisibleTextMatching("onCreateLayout crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnCreateLayoutErrorBoundaryWhenDisabled() {
    ComponentsConfiguration.enableOnErrorHandling = false;

    final ComponentContext c = mComponentsRule.getContext();

    final TestErrorBoundary.Builder builder =
        TestErrorBoundary.create(c).child(TestCrasherOnCreateLayout.create(c).build());

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasStackTraceContaining("onCreateLayout crash");
  }

  @Test
  public void testOnCreateLayoutErrorWithoutBoundaryWhenEnabled() {
    ComponentsConfiguration.enableOnErrorHandling = true;

    final ComponentContext c = mComponentsRule.getContext();

    final TestCrasherOnCreateLayout.Builder builder = TestCrasherOnCreateLayout.create(c);

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasStackTraceContaining("onCreateLayout crash");
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecErrorBoundary() throws Exception {
    ComponentsConfiguration.enableOnErrorHandling = true;

    final ComponentContext c = mComponentsRule.getContext();

    final Component component =
        TestErrorBoundary.create(c)
            .child(TestCrasherOnCreateLayoutWithSizeSpec.create(c).build())
            .build();

    assertThat(c, component)
        .afterStateUpdate()
        .hasVisibleTextMatching("onCreateLayoutWithSizeSpec crash");
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecErrorBoundaryWhenDisabled() {
    ComponentsConfiguration.enableOnErrorHandling = false;

    final ComponentContext c = mComponentsRule.getContext();

    final TestErrorBoundary.Builder builder =
        TestErrorBoundary.create(c).child(TestCrasherOnCreateLayoutWithSizeSpec.create(c).build());

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasStackTraceContaining("onCreateLayoutWithSizeSpec crash");
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecErrorWithoutBoundaryWhenEnabled() {
    ComponentsConfiguration.enableOnErrorHandling = true;

    final ComponentContext c = mComponentsRule.getContext();

    final TestCrasherOnCreateLayoutWithSizeSpec.Builder builder =
        TestCrasherOnCreateLayoutWithSizeSpec.create(c);

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasStackTraceContaining("onCreateLayoutWithSizeSpec crash");
  }

  @Test
  public void testOnMountErrorBoundary() throws Exception {
    ComponentsConfiguration.enableOnErrorHandling = true;

    final ComponentContext c = mComponentsRule.getContext();

    final Component component =
        TestErrorBoundary.create(c).child(TestCrasherOnMount.create(c).build()).build();
    assertThat(c, component).afterStateUpdate().hasVisibleTextMatching("onMount crash");
  }

  @Test
  public void testOnMountErrorBoundaryWhenDisabled() throws Exception {
    ComponentsConfiguration.enableOnErrorHandling = false;

    final ComponentContext c = mComponentsRule.getContext();

    final TestErrorBoundary.Builder builder =
        TestErrorBoundary.create(c).child(TestCrasherOnMount.create(c).build());

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasMessageContaining("onMount crash");
  }

  @Test
  public void testOnMountErrorWithoutBoundaryWhenEnabled() throws Exception {
    ComponentsConfiguration.enableOnErrorHandling = true;

    final ComponentContext c = mComponentsRule.getContext();

    final TestCrasherOnMount.Builder builder = TestCrasherOnMount.create(c);

    RuntimeException exception = null;
    try {
      ComponentTestHelper.mountComponent(builder);
    } catch (RuntimeException e) {
      exception = e;
    }

    assertThat(exception).isNotNull().hasMessageContaining("onMount crash");
  }
}
