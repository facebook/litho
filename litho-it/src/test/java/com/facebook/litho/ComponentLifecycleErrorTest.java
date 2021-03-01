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
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayoutWithSizeSpec;
import com.facebook.litho.testing.error.TestCrasherOnMount;
import com.facebook.litho.testing.error.TestErrorBoundary;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.OnErrorNotPresentChild;
import com.facebook.litho.widget.OnErrorPassUpChildTester;
import com.facebook.litho.widget.OnErrorPassUpParentTester;
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethod;
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodSpec;
import com.facebook.litho.widget.ThrowExceptionGrandChildTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/**
 * Tests error handling in {@link ComponentLifecycle} using the {@link
 * com.facebook.litho.testing.error.TestErrorBoundarySpec} against components crashing in various
 * different lifecycle methods.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class ComponentLifecycleErrorTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();
  @Rule public LithoViewRule mLithoViewRule = new LithoViewRule();
  @Rule public ExpectedException mExpectedException = ExpectedException.none();
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testOnCreateLayoutErrorWithoutBoundaryWhenEnabled() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectCause(
        ThrowableMatcher.forClassWithMessage(Exception.class, "onCreateLayout crash"));

    ComponentTestHelper.mountComponent(
        TestCrasherOnCreateLayout.create(mComponentsRule.getContext()));
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecErrorWithoutBoundaryWhenEnabled() {
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
    final ComponentContext c = mComponentsRule.getContext();

    final Component component =
        TestErrorBoundary.create(c).child(TestCrasherOnMount.create(c).build()).build();
    assertThat(c, component).afterStateUpdate().hasVisibleTextMatching("onMount crash");
  }

  @Test
  public void testOnMountErrorWithoutBoundaryWhenEnabled() {
    mExpectedException.expect(LithoMetadataExceptionWrapper.class);
    mExpectedException.expectCause(
        ThrowableMatcher.forClassWithMessage(Exception.class, "onMount crash"));

    ComponentTestHelper.mountComponent(TestCrasherOnMount.create(mComponentsRule.getContext()));
  }

  @Test
  public void lifecycleOnErrorIndirectlyPassError() {
    final List<String> info = new ArrayList<>();
    Exception exception = null;
    final ComponentContext context = mLithoViewRule.getContext();
    try {
      final Component component =
          OnErrorPassUpParentTester.create(context)
              .child(OnErrorPassUpChildTester.create(context).info(info).build())
              .info(info)
              .build();
      mLithoViewRule.setRoot(component);
      mLithoViewRule.attachToWindow().measure().layout();
    } catch (Exception error) {
      exception = error;
    }
    assertThat(exception).isNotNull();

    assertThat(info)
        .containsExactly(
            "OnErrorPassUpChildTesterSpec->onError", "OnErrorPassUpParentTesterSpec->onError");
  }

  @Test
  public void lifecycleOnErrorDirectlyPassError() {
    final List<String> info = new ArrayList<>();
    Exception exception = null;
    final ComponentContext context = mLithoViewRule.getContext();
    try {
      final Component component =
          OnErrorPassUpParentTester.create(context)
              .child(OnErrorNotPresentChild.create(context).build())
              .info(info)
              .build();
      mLithoViewRule.setRoot(component);

      mLithoViewRule.attachToWindow().measure().layout();
    } catch (Exception error) {
      exception = error;
    }

    assertThat(exception).isNotNull();

    assertThat(info).containsExactly("OnErrorPassUpParentTesterSpec->onError");
  }

  @Test
  public void testOwnErrorHandler() {
    ErrorEventHandler errorEventHandler = mock(ErrorEventHandler.class);
    final ComponentContext context = mLithoViewRule.getContext();

    Component component = ThrowExceptionGrandChildTester.create(context).build();
    ComponentTree.Builder componentTreeBuilder = ComponentTree.create(context);

    ComponentTree componentTree = componentTreeBuilder.errorHandler(errorEventHandler).build();

    componentTree.setRoot(component);
    mLithoViewRule.useComponentTree(componentTree);
    mLithoViewRule.attachToWindow().measure().layout();

    verify(errorEventHandler).onError(any());
  }

  @Test
  public void testOnCreateLayoutCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_LAYOUT, "onCreateLayout crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnCreateTreePropCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_TREE_PROP, "onCreateTreeProp crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnCreateInitialStateCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_INITIAL_STATE, "onCreateInitialState crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnCalculateCachedValueCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;
    ComponentsConfiguration.isAnimationDisabled = false;

    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CALCULATE_CACHED_VALUE, "onCalculateCachedValue crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
    ComponentsConfiguration.isAnimationDisabled = true;
  }

  @Test
  public void testOnCreateTransitionCrashWithTestErrorBoundary() {
    // TODO(T85657700): add onError coverage and remove expected exception
    // RuntimeException we throw is wrapped, so we need to expect that one
    mExpectedException.expect(com.facebook.litho.LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("onCreateTransition crash");

    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;
    ComponentsConfiguration.isAnimationDisabled = false;

    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_TRANSITION, "onCreateTransition crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
    ComponentsConfiguration.isAnimationDisabled = true;
  }

  @Test
  public void testOnAttachedCrashWithTestErrorBoundary() {
    // TODO(T85584869): add onError coverage and remove expected exception
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage(is("onAttached crash"));

    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    crashingScenarioLayoutHelper(LifecycleStep.ON_ATTACHED, "onAttached crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnUpdateStateCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    final ComponentContext context = mLithoViewRule.getContext();

    TestCrashFromEachLayoutLifecycleMethodSpec.Caller caller =
        new TestCrashFromEachLayoutLifecycleMethodSpec.Caller();
    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE)
            .caller(caller)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();
    caller.updateStateSync();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onUpdateState crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnUpdateStateWithTransitionCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    final ComponentContext context = mLithoViewRule.getContext();

    TestCrashFromEachLayoutLifecycleMethodSpec.Caller caller =
        new TestCrashFromEachLayoutLifecycleMethodSpec.Caller();
    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION)
            .caller(caller)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();
    caller.updateStateWithTransition();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onUpdateStateWithTransition crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecCrashWithTestErrorBoundary() {
    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        TestCrasherOnCreateLayoutWithSizeSpec.create(context).widthPx(1).heightPx(1).build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onCreateLayoutWithSizeSpec crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnEventVisibleCrashWithTestErrorBoundary() {
    // TODO(T85672862): add onError coverage and remove expected exception
    // RuntimeException we throw is wrapped, so we need to expect that one
    mExpectedException.expect(com.facebook.litho.LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("onEventVisible crash");

    final boolean currentValue = ComponentsConfiguration.isReconciliationEnabled;

    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;

    final ComponentContext context = mLithoViewRule.getContext();

    TestCrashFromEachLayoutLifecycleMethodSpec.Caller caller =
        new TestCrashFromEachLayoutLifecycleMethodSpec.Caller();
    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context)
            .crashFromStep(LifecycleStep.ON_EVENT_VISIBLE)
            .caller(caller)
            .widthPx(10)
            .heightPx(5)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();
    mLithoViewRule.getLithoView().notifyVisibleBoundsChanged(new Rect(0, 0, 20, 20), true);

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onEventVisible crash");

    // Reset the the value of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentValue;
  }

  @Test
  public void testOnDetachedCrashWithTestErrorBoundary() {
    // TODO(T85584749): add onError coverage and remove expected exception
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage(is("onDetached crash"));

    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context)
            .crashFromStep(LifecycleStep.ON_DETACHED)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();
    mLithoViewRule.release();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onDetached crash");
  }

  private void crashingScenarioLayoutHelper(LifecycleStep crashFromStep, String expectedMessage) {
    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context).crashFromStep(crashFromStep).build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage(expectedMessage);
  }
}
