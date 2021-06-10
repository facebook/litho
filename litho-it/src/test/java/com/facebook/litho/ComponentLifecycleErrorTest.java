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
import android.util.Pair;
import android.view.ViewGroup;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout;
import com.facebook.litho.testing.error.TestCrasherOnCreateLayoutWithSizeSpec;
import com.facebook.litho.testing.error.TestCrasherOnMount;
import com.facebook.litho.testing.error.TestErrorBoundary;
import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.CrashFromLayoutFromStyle;
import com.facebook.litho.widget.CrashingMountable;
import com.facebook.litho.widget.CrashingMountableSpec;
import com.facebook.litho.widget.DynamicPropCrasher;
import com.facebook.litho.widget.OnErrorNotPresentChild;
import com.facebook.litho.widget.OnErrorPassUpChildTester;
import com.facebook.litho.widget.OnErrorPassUpParentTester;
import com.facebook.litho.widget.RootComponentWithTreeProps;
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethod;
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodSpec;
import com.facebook.litho.widget.ThrowExceptionGrandChildTester;
import com.facebook.rendercore.LogLevel;
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

    Exception exception = null;
    final ComponentContext context = mLithoViewRule.getContext();
    try {
      final Component component = TestCrasherOnCreateLayout.create(context).build();
      mLithoViewRule.setRoot(component);
      mLithoViewRule.attachToWindow().measure().layout();
    } catch (Exception error) {
      exception = error;
    }
    assertThat(exception).isNotNull();
    assertThat(exception).hasStackTraceContaining("onCreateLayout crash");
  }

  @Test
  public void testOnCreateLayoutWithSizeSpecErrorWithoutBoundaryWhenEnabled() {

    Exception exception = null;
    final ComponentContext context = mLithoViewRule.getContext();
    try {
      final Component component = TestCrasherOnCreateLayoutWithSizeSpec.create(context).build();
      mLithoViewRule.setRoot(component);
      mLithoViewRule.attachToWindow().measure().layout();
    } catch (Exception error) {
      exception = error;
    }
    assertThat(exception).isNotNull();
    assertThat(exception).hasStackTraceContaining("onCreateLayoutWithSizeSpec crash");
  }

  @Test
  public void testOnMountErrorWithoutBoundaryWhenEnabled() {

    Exception exception = null;
    final ComponentContext context = mLithoViewRule.getContext();
    try {
      final Component component = TestCrasherOnMount.create(context).build();
      mLithoViewRule.setRoot(component);
      mLithoViewRule.attachToWindow().setSizePx(100, 100);
      mLithoViewRule.measure().layout();
    } catch (Exception error) {
      exception = error;
    }
    assertThat(exception).isNotNull();
    assertThat(exception).hasStackTraceContaining("onMount crash");
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

    verify(errorEventHandler).onError(any(), any());
  }

  @Test
  public void testDefaultErrorHandlerLoggingWhenUnhandledExceptionsSwallowed() {
    ComponentsConfiguration.swallowUnhandledExceptions = true;
    TestComponentsReporter componentsReporter = new TestComponentsReporter();
    ComponentsReporter.provide(componentsReporter);

    final ComponentContext context = mLithoViewRule.getContext();

    final Component component =
        OnErrorPassUpParentTester.create(context)
            .child(OnErrorNotPresentChild.create(context).build())
            .info(new ArrayList<>())
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    // the component hierarchy is OnErrorPassUpParentTester -> OnErrorNotPresentChild ->
    // ThrowExceptionGrandChildTester (exception thrown here) and we want to verify that both root
    // and the crashing component were logged in the categoryKey
    assertThat(componentsReporter.getLoggedCategoryKeys())
        .contains(
            new Pair<>(
                LogLevel.ERROR,
                "DefaultErrorEventHandler:OnErrorPassUpParentTester:ThrowExceptionGrandChildTester"));

    ComponentsReporter.provide(null);
    ComponentsConfiguration.swallowUnhandledExceptions = false;
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
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_REGISTER_RANGES, "onRegisterRanges crash", false);
  }

  @Test
  public void testOnEnteredRangeCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_ENTERED_RANGE, "onEnteredRange crash", false);
  }

  @Test
  public void testOnExitedRangeCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(LifecycleStep.ON_EXITED_RANGE, "onExitedRange crash", true);
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
  public void testOnShouldCreateLayoutWithNewSizeSpecCrashWithTestErrorBoundary() {
    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent =
        RootComponentWithTreeProps.create(context)
            .crashFromStep(LifecycleStep.ON_SHOULD_CREATE_LAYOUT_WITH_NEW_SIZE_SPEC)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.attachToWindow().setSizePx(100, 100).measure().setRoot(component).layout();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onShouldCreateLayoutWithSizeSpec crash");
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
        LifecycleStep.ON_FOCUSED_EVENT_VISIBLE, "onFocusedEventVisible crash", false);
  }

  @Test
  public void testOnFullImpressionVisibleEventCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT, "onFullImpressionVisible crash", false);
  }

  @Test
  public void testOnVisibilityChangedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_VISIBILITY_CHANGED, "onVisibilityChanged crash", false);
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
  public void testOnTriggerCrashWithTestErrorBoundary() {
    final ComponentContext context = mLithoViewRule.getContext();
    final Handle triggerHandle = new Handle();

    Component crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(context)
            .crashFromStep(LifecycleStep.ON_TRIGGER)
            .handle(triggerHandle)
            .build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    final Object bazObject = new Object();

    // We need to use a ComponentContext with a ComponentTree on it
    TestCrashFromEachLayoutLifecycleMethod.triggerTestEvent(
        mLithoViewRule.getComponentTree().getContext(), triggerHandle, bazObject);

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onTrigger crash");
  }

  @Test
  public void testOnLoadStyleCrashWithTestErrorBoundary() {
    // note: this test has a slightly different flow than the rest, since
    // TestCrashFromEachLayoutLifecycleMethod.create(c, 0, android.R.style.Animation) crashes at
    // component initialization and not at layout, so we need to wrap it in another spec that will
    // rethrow from onCreateLayout()
    final ComponentContext context = mLithoViewRule.getContext();

    Component crashingComponent = CrashFromLayoutFromStyle.create(context).build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onLoadStyle crash");
  }

  @Test
  public void testOnBindDynamicValueCrashWithTestErrorBoundary() {
    final ComponentContext context = mLithoViewRule.getContext();

    DynamicValue<String> dynamicStringProp = new DynamicValue<>("dynamic_prop_test");
    Component crashingComponent =
        DynamicPropCrasher.create(context).someStringProp(dynamicStringProp).build();
    final List<Exception> errorOutput = new ArrayList<>();
    Component component =
        TestErrorBoundary.create(context).errorOutput(errorOutput).child(crashingComponent).build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    dynamicStringProp.set("change_dynamic_prop_test");

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage("onBindDynamicValue crash");
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
      LifecycleStep crashFromStep, String expectedMessage, boolean releaseAfter) {
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
    if (releaseAfter) {
      mLithoViewRule.release();
    }

    Exception error = errorOutput.size() == 1 ? errorOutput.get(0) : null;
    assertThat(error).isInstanceOf(RuntimeException.class);
    assertThat(error).hasMessage(expectedMessage);
  }
}
