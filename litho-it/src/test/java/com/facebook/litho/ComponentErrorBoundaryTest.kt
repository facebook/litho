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

package com.facebook.litho

import android.graphics.Rect
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.error.TestCrasherOnCreateLayout
import com.facebook.litho.testing.error.TestCrasherOnCreateLayoutWithSizeSpec
import com.facebook.litho.testing.error.TestCrasherOnMount
import com.facebook.litho.testing.error.TestErrorBoundary
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.CrashFromLayoutFromStyle
import com.facebook.litho.widget.CrashKotlinComponent
import com.facebook.litho.widget.CrashingMountable
import com.facebook.litho.widget.CrashingMountableSpec.MountPhaseException
import com.facebook.litho.widget.DynamicPropCrasher
import com.facebook.litho.widget.OnErrorNotPresentChild
import com.facebook.litho.widget.OnErrorPassUpChildTester
import com.facebook.litho.widget.OnErrorPassUpParentTester
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethod
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodParent
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodSpec
import com.facebook.litho.widget.ThrowExceptionGrandChildTester
import com.facebook.yoga.YogaEdge
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.Is
import org.junit.After
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.annotation.LooperMode

/**
 * Tests error handling in [Component] using the [TestErrorBoundarySpec] against components crashing
 * in various different lifecycle methods.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentErrorBoundaryTest {

  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()

  @Rule @JvmField val expectedException = ExpectedException.none()

  @Rule @JvmField val backgroundLayoutLooperRule = BackgroundLayoutLooperRule()
  private var currentReconciliationValue = false

  @Before
  fun assumeDebugAndChangeConfig() {
    currentReconciliationValue = ComponentsConfiguration.isReconciliationEnabled
    // Disable reconciliation so that the onCreateLayout is called for layout.
    ComponentsConfiguration.isReconciliationEnabled = false
    ComponentsConfiguration.isAnimationDisabled = false
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        Is.`is`(true))
  }

  @After
  fun adjustConfigs() {
    // Reset the the values of the config.
    ComponentsConfiguration.isReconciliationEnabled = currentReconciliationValue
    ComponentsConfiguration.isAnimationDisabled = true
  }

  @Test
  fun testOnCreateLayoutErrorWithoutBoundaryWhenEnabled() {
    var exception: Exception? = null
    try {
      val component = TestCrasherOnCreateLayout.create(lithoViewRule.context).build()
      lithoViewRule.setRoot(component)
      lithoViewRule.attachToWindow().measure().layout()
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(exception).hasStackTraceContaining("onCreateLayout crash")
  }

  @Test
  fun testOnCreateLayoutWithSizeSpecErrorWithoutBoundaryWhenEnabled() {
    var exception: Exception? = null
    try {
      val component = TestCrasherOnCreateLayoutWithSizeSpec.create(lithoViewRule.context).build()
      lithoViewRule.setRoot(component)
      lithoViewRule.attachToWindow().measure().layout()
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(exception).hasStackTraceContaining("onCreateLayoutWithSizeSpec crash")
  }

  @Test
  fun testOnMountErrorWithoutBoundaryWhenEnabled() {
    var exception: Exception? = null
    try {
      val component = TestCrasherOnMount.create(lithoViewRule.context).build()
      lithoViewRule.setRoot(component)
      lithoViewRule.attachToWindow().setSizePx(100, 100)
      lithoViewRule.measure().layout()
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(exception).hasStackTraceContaining("onMount crash")
  }

  @Test
  fun lifecycleOnErrorIndirectlyPassError() {
    val info: List<String> = ArrayList()
    var exception: Exception? = null
    try {
      val component =
          OnErrorPassUpParentTester.create(lithoViewRule.context)
              .child(OnErrorPassUpChildTester.create(lithoViewRule.context).info(info).build())
              .info(info)
              .build()
      lithoViewRule.setRoot(component)
      lithoViewRule.attachToWindow().measure().layout()
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(info)
        .containsExactly(
            "OnErrorPassUpChildTesterSpec->onError", "OnErrorPassUpParentTesterSpec->onError")
  }

  @Test
  fun lifecycleOnErrorDirectlyPassError() {
    val info: List<String> = ArrayList()
    var exception: Exception? = null
    try {
      val component =
          OnErrorPassUpParentTester.create(lithoViewRule.context)
              .child(OnErrorNotPresentChild.create(lithoViewRule.context).build())
              .info(info)
              .build()
      lithoViewRule.setRoot(component)
      lithoViewRule.attachToWindow().measure().layout()
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(info).containsExactly("OnErrorPassUpParentTesterSpec->onError")
  }

  @Test
  fun testOwnErrorHandler() {
    val errorEventHandler = Mockito.mock(ErrorEventHandler::class.java)
    val component = ThrowExceptionGrandChildTester.create(lithoViewRule.context).build()
    val componentTreeBuilder = ComponentTree.create(lithoViewRule.context)
    val componentTree = componentTreeBuilder.errorHandler(errorEventHandler).build()
    componentTree.root = component
    lithoViewRule.useComponentTree(componentTree)
    lithoViewRule.attachToWindow().measure().layout()
    Mockito.verify(errorEventHandler).onError(ArgumentMatchers.any(), ArgumentMatchers.any())
  }

  @Test
  fun testKotlinComponentCrashWithTestErrorBoundary() {
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(CrashKotlinComponent())
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("crash from kotlin component")
  }

  @Test
  fun testOnCreateLayoutCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_LAYOUT, "onCreateLayout crash", false)
  }

  @Test
  fun testOnCreateTreePropCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(LifecycleStep.ON_CREATE_TREE_PROP, "onCreateTreeProp crash", false)
  }

  @Test
  fun testOnCreateInitialStateCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_INITIAL_STATE, "onCreateInitialState crash", false)
  }

  @Test
  fun testOnCalculateCachedValueCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CALCULATE_CACHED_VALUE, "onCalculateCachedValue crash", false)
  }

  @Test
  fun testOnCreateTransitionCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_TRANSITION, "onCreateTransition crash", false)
  }

  @Test
  fun testOnAttachedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(LifecycleStep.ON_ATTACHED, "onAttached crash", true)
  }

  @Test
  fun testOnRegisterRangesCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_REGISTER_RANGES, "onRegisterRanges crash", false, false)
  }

  @Test
  fun testOnEnteredRangeCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_ENTERED_RANGE, "onEnteredRange crash", false, true)
  }

  @Test
  fun testOnExitedRangeCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_EXITED_RANGE, "onExitedRange crash", true, true)
  }

  @Test
  fun testOnUpdateStateCrashWithTestErrorBoundary() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    caller.updateStateSync()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onUpdateState crash")
  }

  @Test
  fun testOnUpdateStateCrashWithTestErrorBoundaryInDeepHierarchy() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(Column.create(lithoViewRule.context).child(crashingComponent).build())
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    caller.updateStateSync()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onUpdateState crash")
  }

  @Test
  fun testOnUpdateStateCrashWithTestErrorBoundaryInDeepHierarchyOther() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(
                Column.create(lithoViewRule.context)
                    .child(
                        TestCrashFromEachLayoutLifecycleMethodParent.create(lithoViewRule.context)
                            .caller(caller))
                    .build())
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    caller.updateStateSync()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onUpdateState crash")
  }

  @Test
  fun testOnUpdateStateWithTransitionCrashWithTestErrorBoundary() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    caller.updateStateWithTransition()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onUpdateStateWithTransition crash")
  }

  @Test
  fun testOnUpdateStateWithTransitionCrashWithTestErrorBoundaryInDeepHierarchy() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(Column.create(lithoViewRule.context).child(crashingComponent).build())
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    caller.updateStateWithTransition()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onUpdateStateWithTransition crash")
  }

  @Test
  fun testOnCreateLayoutWithSizeSpecCrashWithTestErrorBoundary() {
    val crashingComponent =
        TestCrasherOnCreateLayoutWithSizeSpec.create(lithoViewRule.context)
            .widthPx(1)
            .heightPx(1)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onCreateLayoutWithSizeSpec crash")
  }

  @Test
  fun testOnEventVisibleCrashWithTestErrorBoundary() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_EVENT_VISIBLE)
            .caller(caller)
            .widthPx(10)
            .heightPx(5)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    lithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 20, 20), true)
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
    assertThat(errorOutput[0].message).contains("onEventVisible crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:", "TestCrashFromEachLayoutLifecycleMethod", "->", "TestErrorBoundary")
  }

  @Test
  fun testOnEventInvisibleCrashWithTestErrorBoundary() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_EVENT_INVISIBLE)
            .caller(caller)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY))
        .measure()
        .layout()
    lithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 5), true)
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
    assertThat(errorOutput[0].message).contains("onEventInvisible crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:", "TestCrashFromEachLayoutLifecycleMethod", "->", "TestErrorBoundary")
  }

  @Test
  fun testOnFocusedEventVisibleCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FOCUSED_EVENT_VISIBLE, "onFocusedEventVisible crash", false, true)
  }

  @Test
  fun testOnFullImpressionVisibleEventCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
        "onFullImpressionVisible crash",
        false,
        true)
  }

  @Test
  fun testOnVisibilityChangedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_VISIBILITY_CHANGED, "onVisibilityChanged crash", false, true)
  }

  @Test
  fun testOnDetachedCrashWithTestErrorBoundary() {
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_DETACHED)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    lithoViewRule.release()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(RuntimeException::class.java)
    assertThat(errorOutput[0].message).contains("onDetached crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:", "TestCrashFromEachLayoutLifecycleMethod", "->", "TestErrorBoundary")
  }

  @Test
  fun testOnTriggerCrashWithTestErrorBoundary() {
    val triggerHandle = Handle()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_TRIGGER)
            .handle(triggerHandle)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    val bazObject = Any()

    // We need to use a ComponentContext with a ComponentTree on it
    TestCrashFromEachLayoutLifecycleMethod.triggerTestEvent(
        lithoViewRule.componentTree.context, triggerHandle, bazObject)
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(RuntimeException::class.java)
    assertThat(errorOutput[0].message).contains("onTrigger crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:", "TestCrashFromEachLayoutLifecycleMethod", "->", "TestErrorBoundary")
  }

  @Test
  fun testOnLoadStyleCrashWithTestErrorBoundary() {
    // note: this test has a slightly different flow than the rest, since
    // TestCrashFromEachLayoutLifecycleMethod.create(c, 0, android.R.style.Animation) crashes at
    // component initialization and not at layout, so we need to wrap it in another spec that will
    // rethrow from onCreateLayout()
    val crashingComponent = CrashFromLayoutFromStyle.create(lithoViewRule.context).build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onLoadStyle crash")
  }

  @Test
  fun testOnBindDynamicValueCrashWithTestErrorBoundary() {
    val dynamicStringProp = DynamicValue("dynamic_prop_test")
    val crashingComponent =
        DynamicPropCrasher.create(lithoViewRule.context).someStringProp(dynamicStringProp).build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    dynamicStringProp.set("change_dynamic_prop_test")
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
    assertThat(errorOutput[0].message).contains("onBindDynamicValue crash")
    assertThat(errorOutput[0].message)
        .contains("layout_stack:", "DynamicPropCrasher", "->", "TestErrorBoundary")
  }

  @Test
  fun testOnMountCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_MOUNT, "Crashed on ON_MOUNT", false, true)
  }

  @Test
  fun testOnUnMountCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_UNMOUNT, "Crashed on ON_UNMOUNT", true, true)
  }

  @Test
  fun testOnBindCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_BIND, "Crashed on ON_BIND", false, true)
  }

  @Test
  fun testOnUnBindCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_UNBIND, "Crashed on ON_UNBIND", true, true)
  }

  @Test
  fun testOnPrepareCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_PREPARE, "Crashed on ON_PREPARE", false, false)
  }

  @Test
  fun testOnMeasureCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(LifecycleStep.ON_MEASURE, "Crashed on ON_MEASURE", false, true)
  }

  @Test
  fun testOnBoundsDefinedCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_BOUNDS_DEFINED, "Crashed on ON_BOUNDS_DEFINED", false, false)
  }

  @Test
  fun testOnCreateMountContentCrashWithTestErrorBoundary() {
    // TODO(T85975360): add onError coverage and remove expected exception
    // RuntimeException we throw is wrapped, so we need to expect that one
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("Crashed on ON_CREATE_MOUNT_CONTENT")
    crashingScenarioMountHelper(
        LifecycleStep.ON_CREATE_MOUNT_CONTENT, "Crashed on ON_CREATE_MOUNT_CONTENT", false, true)
  }

  @Test
  fun testOnCreateMountContentPoolCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_CREATE_MOUNT_CONTENT_POOL,
        "Crashed on ON_CREATE_MOUNT_CONTENT_POOL",
        false,
        true)
  }

  @Test
  fun testShouldUpdateCrashWithTestErrorBoundary() {
    val crashingComponent =
        CrashingMountable.create(lithoViewRule.context)
            .someStringProp("someString")
            .lifecycle(LifecycleStep.SHOULD_UPDATE)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val crashingComponent2: Component =
        CrashingMountable.create(lithoViewRule.context)
            .someStringProp("someString2")
            .lifecycle(LifecycleStep.SHOULD_UPDATE)
            .build()
    val component2: Component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent2)
            .build()
    lithoViewRule.setRoot(component2).measure().layout()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(MountPhaseException::class.java)
        .hasMessage("Crashed on SHOULD_UPDATE")
  }

  private fun crashingScenarioMountHelper(
      crashFromStep: LifecycleStep,
      expectedMessage: String,
      unmountAfter: Boolean,
      expectHierarchy: Boolean
  ) {
    val crashingComponent =
        CrashingMountable.create(lithoViewRule.context)
            .someStringProp("someString")
            .lifecycle(crashFromStep)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.attachToWindow().setRoot(component).measure().layout()

    // The crashing mountable component should not get added to
    // the RenderTree if the error is thrown before mount.
    if (crashFromStep == LifecycleStep.ON_PREPARE ||
        crashFromStep == LifecycleStep.ON_MEASURE ||
        crashFromStep == LifecycleStep.ON_BOUNDS_DEFINED) {
      val count = lithoViewRule.lithoView.mountDelegateTarget.renderUnitCount
      for (i in 0 until count) {
        val item = lithoViewRule.lithoView.mountDelegateTarget.getMountItemAt(i)
        item?.renderTreeNode?.let {
          assertThat(LithoRenderUnit.getRenderUnit(it).component)
              .isNotInstanceOf(CrashingMountable::class.java)
        }
      }
    }

    if (unmountAfter) {
      lithoViewRule.lithoView.unmountAllItems()
    }
    assertThat(errorOutput).hasSize(1)
    if (expectHierarchy) {
      assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
      assertThat(errorOutput[0].message)
          .contains("layout_stack:", "CrashingMountable", "->", "TestErrorBoundary")
    } else {
      assertThat(errorOutput[0])
          .isInstanceOf(MountPhaseException::class.java)
          .hasMessage(expectedMessage)
    }
  }

  private fun crashingScenarioLayoutHelper(
      crashFromStep: LifecycleStep,
      expectedMessage: String,
      expectHierarchy: Boolean
  ) {
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(crashFromStep)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(errorOutput).hasSize(1)
    if (expectHierarchy) {
      assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
      assertThat(errorOutput[0].message)
          .contains(
              "layout_stack:", "TestCrashFromEachLayoutLifecycleMethod", "->", "TestErrorBoundary")
    } else {
      assertThat(errorOutput[0])
          .isInstanceOf(RuntimeException::class.java)
          .hasMessage(expectedMessage)
    }
  }

  private fun crashingScenarioLayoutSectionHelper(
      crashFromStep: LifecycleStep,
      expectedMessage: String,
      releaseAfter: Boolean,
      expectHierarchy: Boolean
  ) {
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(crashFromStep)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoViewRule.context)
            .heightPx(100)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val rcc =
        RecyclerCollectionComponent.create(lithoViewRule.context)
            .recyclerConfiguration(ListRecyclerConfiguration.create().build())
            .section(
                SingleComponentSection.create(SectionContext(lithoViewRule.context))
                    .component(component)
                    .build())
            .build()
    lithoViewRule.setRoot(rcc).setSizeSpecs(exactly(100), exactly(100))
    lithoViewRule.attachToWindow().measure().layout()
    if (releaseAfter) {
      lithoViewRule.release()
    }
    assertThat(errorOutput).hasSize(1)
    if (expectHierarchy) {
      assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
      assertThat(errorOutput[0].message)
          .contains(
              "layout_stack:", "TestCrashFromEachLayoutLifecycleMethod", "->", "TestErrorBoundary")
    } else {
      assertThat(errorOutput[0]).isInstanceOf(RuntimeException::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
    }
  }

  @Test
  fun testOnCreateLayoutCrashWithKotlinErrorBoundary() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoViewRule.context)
            .crashFromStep(LifecycleStep.ON_CREATE_LAYOUT)
            .build()

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val errorState = useState { listOf<Exception>() }
        stateRef = AtomicReference(errorState.value)
        useErrorBoundary { exception: Exception ->
          errorState.update { prevErrors -> prevErrors + listOf(exception) }
          assertThat(exception.message).contains("onCreateLayout crash")
        }
        return if (errorState.value.isEmpty()) crashingComponent else Text("error caught")
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()

    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(1)
    assertThat(errorList[0].message).isEqualTo("onCreateLayout crash")
  }

  @Test
  fun testOnCreateLayoutWithSizeSpecCrashWithKotlinErrorBoundary() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    val crashingComponent =
        TestCrasherOnCreateLayoutWithSizeSpec.create(lithoViewRule.context).build()

    class UseErrorComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val errorState = useState { listOf<Exception>() }
        stateRef = AtomicReference(errorState.value)
        useErrorBoundary { exception: Exception ->
          errorState.update { prevErrors -> prevErrors + listOf(exception) }
          assertThat(exception.message).contains("onCreateLayoutWithSizeSpec crash")
        }
        return if (errorState.value.isEmpty()) crashingComponent else Text("error caught")
      }
    }

    lithoViewRule.render { UseErrorComponent() }
    lithoViewRule.idle()

    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(2)
    errorList.forEach { error ->
      assertThat(error.message).isEqualTo("onCreateLayoutWithSizeSpec crash")
    }
  }
}
