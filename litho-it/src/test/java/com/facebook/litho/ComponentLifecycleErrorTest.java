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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayoutWithSizeSpec;
import com.facebook.litho.testing.error.TestCrasherOnMount;
import com.facebook.litho.testing.error.TestErrorBoundary;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.CrashingMountable;
import com.facebook.litho.widget.CrashingMountableSpec;
import com.facebook.litho.widget.OnErrorNotPresentChild;
import com.facebook.litho.widget.OnErrorPassUpChildTester;
import com.facebook.litho.widget.OnErrorPassUpParentTester;
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethod;
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodSpec;
import com.facebook.litho.widget.ThrowExceptionGrandChildTester;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
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

  @Rule
  public BackgroundLayoutLooperRule mBackgroundLayoutLooperRule = new BackgroundLayoutLooperRule();

  private boolean currentReconciliationValue;

  @Before
  public void assumeDebugAndChangeConfig() {
    currentReconciliationValue = ComponentsConfiguration.isReconciliationEnabled;
    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false;
    ComponentsConfiguration.isAnimationDisabled = false;

    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @After
  public void adjustConfigs() {
    // Reset the the values of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentReconciliationValue;
    ComponentsConfiguration.isAnimationDisabled = true;
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
    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_LAYOUT, "onCreateLayout crash");
  }

  @Test
  public void testOnCreateTreePropCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_TREE_PROP, "onCreateTreeProp crash");
  }

  @Test
  public void testOnCreateInitialStateCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_INITIAL_STATE, "onCreateInitialState crash");
  }

  @Test
  public void testOnCalculateCachedValueCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CALCULATE_CACHED_VALUE, "onCalculateCachedValue crash");
  }

  @Test
  public void testOnCreateTransitionCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_TRANSITION, "onCreateTransition crash");
  }

  @Test
  public void testOnAttachedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(LifecycleStep.ON_ATTACHED, "onAttached crash");
  }

  @Test
  public void testOnRegisterRangesCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(LifecycleStep.ON_REGISTER_RANGES, "onRegisterRanges crash");
  }

  @Test
  public void testOnEnteredRangeCrashWithTestErrorBoundary() {
    // TODO(T87265593): add onError coverage and remove expected exception
    // RuntimeException we throw is wrapped, so we need to expect that one
    mExpectedException.expect(com.facebook.litho.LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("onEnteredRange crash");

    crashingScenarioLayoutSectionHelper(LifecycleStep.ON_ENTERED_RANGE, "onEnteredRange crash");
  }

  @Test
  public void testOnUpdateStateCrashWithTestErrorBoundary() {
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
  }

  @Test
  public void testOnUpdateStateWithTransitionCrashWithTestErrorBoundary() {
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
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecCrashWithTestErrorBoundary() {
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
  }

  @Test
  public void testOnEventVisibleCrashWithTestErrorBoundary() {
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
  }

  @Test
  public void testOnEventInvisibleCrashWithTestErrorBoundary() {
    final ComponentContext context = mLithoViewRule.getContext();

    TestCrashFromEachLayoutLifecycleMethodSpec.Caller caller =
        new TestCrashFromEachLayoutLifecycleMethodSpec.Caller();
    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context)
            .crashFromStep(LifecycleStep.ON_EVENT_INVISIBLE)
            .caller(caller)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY))
        .measure()
        .layout();

    mLithoViewRule.getLithoView().notifyVisibleBoundsChanged(new Rect(0, 0, 10, 5), true);

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onEventInvisible crash");
  }

  @Test
  public void testOnFocusedEventVisibleCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FOCUSED_EVENT_VISIBLE, "onFocusedEventVisible crash");
  }

  @Test
  public void testOnFullImpressionVisibleEventCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT, "onFullImpressionVisible crash");
  }

  @Test
  public void testOnVisibilityChangedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_VISIBILITY_CHANGED, "onVisibilityChanged crash");
  }

  @Test
  public void testOnDetachedCrashWithTestErrorBoundary() {
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

  @Test
  public void testOnMountCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_MOUNT, "Crashed on ON_MOUNT", false);
  }

  @Test
  public void testOnUnMountCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_UNMOUNT, "Crashed on ON_UNMOUNT", true);
  }

  @Test
  public void testOnBindCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_BIND, "Crashed on ON_BIND", false);
  }

  @Test
  public void testOnUnBindCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_UNBIND, "Crashed on ON_UNBIND", true);
  }

  @Test
  public void testOnPrepareCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_PREPARE, "Crashed on ON_PREPARE", false);
  }

  @Test
  public void testOnMeasureCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_MEASURE, "Crashed on ON_MEASURE", false);
  }

  @Test
  public void testOnBoundsDefinedCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_BOUNDS_DEFINED, "Crashed on ON_BOUNDS_DEFINED", false);
  }

  @Test
  public void testOnCreateMountContentCrashWithTestErrorBoundary() {
    // TODO(T85975360): add onError coverage and remove expected exception
    // RuntimeException we throw is wrapped, so we need to expect that one
    mExpectedException.expect(com.facebook.litho.LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("Crashed on ON_CREATE_MOUNT_CONTENT");

    crashingScenarioMountHelper(
        LifecycleStep.ON_CREATE_MOUNT_CONTENT, "Crashed on ON_CREATE_MOUNT_CONTENT", false);
  }

  @Test
  public void testOnCreateMountContentPoolCrashWithTestErrorBoundary() {
    // TODO(T85975436): add onError coverage and remove expected exception
    // RuntimeException we throw is wrapped, so we need to expect that one
    mExpectedException.expect(com.facebook.litho.LithoMetadataExceptionWrapper.class);
    mExpectedException.expectMessage("Crashed on ON_CREATE_MOUNT_CONTENT_POOL");

    crashingScenarioMountHelper(
        LifecycleStep.ON_CREATE_MOUNT_CONTENT_POOL,
        "Crashed on ON_CREATE_MOUNT_CONTENT_POOL",
        false);
  }

  @Test
  public void testShouldUpdateCrashWithTestErrorBoundary() {
    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        CrashingMountable.create(context)
            .someStringProp("someString")
            .lifecycle(LifecycleStep.SHOULD_UPDATE)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    Component crashingComponent2 =
        CrashingMountable.create(context)
            .someStringProp("someString2")
            .lifecycle(LifecycleStep.SHOULD_UPDATE)
            .build();
    Component component2 =
        TestErrorBoundary.create(context)
            .errorOutput(errorOutput)
            .child(crashingComponent2)
            .build();

    mLithoViewRule.setRoot(component2).measure().layout();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(CrashingMountableSpec.MountPhaseException.class);
    assertThat(error).hasMessage("Crashed on SHOULD_UPDATE");
  }

  private void crashingScenarioMountHelper(
      LifecycleStep crashFromStep, String expectedMessage, boolean unmountAfter) {
    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        CrashingMountable.create(context)
            .someStringProp("someString")
            .lifecycle(crashFromStep)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.attachToWindow().setRoot(component).measure().layout();
    if (unmountAfter) {
      mLithoViewRule.getLithoView().unmountAllItems();
    }
    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(CrashingMountableSpec.MountPhaseException.class);
    assertThat(error).hasMessage(expectedMessage);
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

  private void crashingScenarioLayoutSectionHelper(
      LifecycleStep crashFromStep, String expectedMessage) {
    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context).crashFromStep(crashFromStep).build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context)
            .heightPx(100)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build();

    final RecyclerCollectionComponent rcc =
        RecyclerCollectionComponent.create(context)
            .recyclerConfiguration(ListRecyclerConfiguration.create().build())
            .section(
                SingleComponentSection.create(new SectionContext(context))
                    .component(component)
                    .build())
            .build();

    mLithoViewRule
        .setRoot(rcc)
        .setSizeSpecs(makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));

    mLithoViewRule.attachToWindow().measure().layout();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage(expectedMessage);
  }
}
