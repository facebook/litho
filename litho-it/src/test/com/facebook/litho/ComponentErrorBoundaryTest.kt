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
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.error.TestCrasherOnCreateLayoutWithSizeSpec
import com.facebook.litho.testing.error.TestCrasherOnMount
import com.facebook.litho.testing.error.TestErrorBoundary
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.CrashingMountable
import com.facebook.litho.widget.CrashingMountableSpec.MountPhaseException
import com.facebook.litho.widget.CreateMountContentCrashing
import com.facebook.litho.widget.DynamicPropCrasher
import com.facebook.litho.widget.OnErrorPassUpChildTester
import com.facebook.litho.widget.OnErrorPassUpParentTester
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethod
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodParent
import com.facebook.litho.widget.TestCrashFromEachLayoutLifecycleMethodSpec
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.yoga.YogaEdge
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.Is
import org.junit.After
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.robolectric.annotation.LooperMode

/**
 * Tests error handling in [Component] using the [TestErrorBoundarySpec] against components crashing
 * in various different lifecycle methods.
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentErrorBoundaryTest {

  @Rule
  @JvmField
  val lithoTestRule =
      LithoTestRule(componentsConfiguration = ComponentsConfiguration.create().build())

  @Rule @JvmField val expectedException = ExpectedException.none()

  @Rule @JvmField val backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Before
  fun assumeDebugAndChangeConfig() {
    ComponentsConfiguration.isAnimationDisabled = false
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        Is.`is`(true),
    )
  }

  @After
  fun adjustConfigs() {
    ComponentsConfiguration.isAnimationDisabled = true
  }

  @Test
  fun testOnCreateLayoutErrorWithoutBoundaryWhenEnabled() {
    var exception: Exception? = null
    try {
      lithoTestRule.render { TestCrasherOnCreateLayout() }
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
      val component = TestCrasherOnCreateLayoutWithSizeSpec.create(lithoTestRule.context).build()
      lithoTestRule.render { component }
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
      val component = TestCrasherOnMount.create(lithoTestRule.context).build()
      lithoTestRule.render(widthPx = 100, heightPx = 100) { component }
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
          OnErrorPassUpParentTester.create(lithoTestRule.context)
              .child(OnErrorPassUpChildTester.create(lithoTestRule.context).info(info).build())
              .info(info)
              .build()
      lithoTestRule.render { component }
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(info)
        .containsExactly(
            "OnErrorPassUpChildTesterSpec->onError",
            "OnErrorPassUpParentTesterSpec->onError",
        )
  }

  @Test
  fun lifecycleOnErrorDirectlyPassError() {
    val info: List<String> = ArrayList()
    var exception: Exception? = null
    try {
      val component =
          OnErrorPassUpParentTester.create(lithoTestRule.context)
              .child(OnErrorNotPresentChild())
              .info(info)
              .build()
      lithoTestRule.render { component }
    } catch (error: Exception) {
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(info).containsExactly("OnErrorPassUpParentTesterSpec->onError")
  }

  @Test
  fun testOwnErrorHandler() {
    val errorEventHandler = Mockito.mock(ErrorEventHandler::class.java)
    val component = ThrowExceptionGrandChildTester()
    val componentTreeBuilder = ComponentTree.create(lithoTestRule.context)
    val componentTree =
        componentTreeBuilder
            .componentsConfiguration(
                lithoTestRule.context.lithoConfiguration.componentsConfig.copy(
                    errorEventHandler = errorEventHandler,
                ),
            )
            .enableChildClipping(true)
            .build()
    componentTree.root = component
    lithoTestRule.render(componentTree = componentTree) { component }
    Mockito.verify(errorEventHandler).onError(any(), any())
  }

  @Test
  fun testKotlinComponentCrashWithTestErrorBoundary() {
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(CrashKotlinComponent())
            .build()
    lithoTestRule.render { component }
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("crash from kotlin component")
  }

  @Test
  fun testOnCreateLayoutCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_LAYOUT,
        "onCreateLayout crash",
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnCreateTreePropCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_TREE_PROP,
        "onCreateTreeProp crash",
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnCreateInitialStateCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_INITIAL_STATE,
        "onCreateInitialState crash",
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnCalculateCachedValueCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CALCULATE_CACHED_VALUE,
        "onCalculateCachedValue crash",
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnCreateTransitionCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_CREATE_TRANSITION,
        "onCreateTransition crash",
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnAttachedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutHelper(
        LifecycleStep.ON_ATTACHED,
        "onAttached crash",
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnRegisterRangesCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_REGISTER_RANGES,
        "onRegisterRanges crash",
        releaseAfter = false,
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnEnteredRangeCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_ENTERED_RANGE,
        "onEnteredRange crash",
        releaseAfter = false,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnExitedRangeCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_EXITED_RANGE,
        "onExitedRange crash",
        releaseAfter = true,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnUpdateStateCrashWithTestErrorBoundary() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoTestRule.render { component }
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
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(Column.create(lithoTestRule.context).child(crashingComponent).build())
            .build()
    lithoTestRule.render { component }
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
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(
                Column.create(lithoTestRule.context)
                    .child(TestCrashFromEachLayoutLifecycleMethodParent(caller))
                    .build(),
            )
            .build()
    lithoTestRule.render { component }
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
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoTestRule.render { component }
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
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION)
            .caller(caller)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(Column.create(lithoTestRule.context).child(crashingComponent).build())
            .build()
    lithoTestRule.render { component }
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
        TestCrasherOnCreateLayoutWithSizeSpec.create(lithoTestRule.context)
            .widthPx(1)
            .heightPx(1)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoTestRule.render { component }
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onCreateLayoutWithSizeSpec crash")
  }

  @Test
  fun testOnEventVisibleCrashWithTestErrorBoundary() {
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_EVENT_VISIBLE)
            .caller(caller)
            .widthPx(10)
            .heightPx(5)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val testLithoView = lithoTestRule.render { component }
    testLithoView.lithoView.notifyVisibleBoundsChanged(
        Rect(0, 0, 20, 20),
        true,
        VisibilityChangeReason.UNKNOWN,
    )
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
    assertThat(errorOutput[0].message).contains("onEventVisible crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:",
            "TestCrashFromEachLayoutLifecycleMethod",
            "->",
            "TestErrorBoundary",
        )
  }

  @Test
  fun testOnEventInvisibleCrashWithTestErrorBoundary() {
    val c = lithoTestRule.context
    val caller = TestCrashFromEachLayoutLifecycleMethodSpec.Caller()

    val errorOutput: List<Exception> = ArrayList()
    val component =
        Column.create(c)
            .child(
                TestErrorBoundary.create(c)
                    .errorOutput(errorOutput)
                    .child(
                        TestCrashFromEachLayoutLifecycleMethod.create(c)
                            .crashFromStep(LifecycleStep.ON_EVENT_INVISIBLE)
                            .caller(caller)
                            .widthPx(10)
                            .heightPx(5)
                            .marginPx(YogaEdge.TOP, 5),
                    ),
            )
            .build()
    val testLithoView = lithoTestRule.render(widthPx = 10, heightPx = 10) { component }
    testLithoView.lithoView.notifyVisibleBoundsChanged(
        Rect(0, 0, 10, 5),
        true,
        VisibilityChangeReason.UNKNOWN,
    )
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
    assertThat(errorOutput[0].message).contains("onEventInvisible crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:",
            "TestCrashFromEachLayoutLifecycleMethod",
            "->",
            "TestErrorBoundary",
        )
  }

  @Test
  fun testOnFocusedEventVisibleCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FOCUSED_EVENT_VISIBLE,
        "onFocusedEventVisible crash",
        releaseAfter = false,
        true,
    )
  }

  @Test
  fun testOnFullImpressionVisibleEventCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT,
        "onFullImpressionVisible crash",
        false,
        true,
    )
  }

  @Test
  fun testOnVisibilityChangedCrashWithTestErrorBoundary() {
    crashingScenarioLayoutSectionHelper(
        LifecycleStep.ON_VISIBILITY_CHANGED,
        "onVisibilityChanged crash",
        releaseAfter = false,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnDetachedCrashWithTestErrorBoundary() {
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_DETACHED)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val testLithoView = lithoTestRule.render { component }
    testLithoView.release()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(RuntimeException::class.java)
    assertThat(errorOutput[0].message).contains("onDetached crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:",
            "TestCrashFromEachLayoutLifecycleMethod",
            "->",
            "TestErrorBoundary",
        )
  }

  @Test
  fun testOnTriggerCrashWithTestErrorBoundary() {
    val triggerHandle = Handle()
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(LifecycleStep.ON_TRIGGER)
            .handle(triggerHandle)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val testLithoView = lithoTestRule.render { component }
    val bazObject = Any()

    // We need to use a ComponentContext with a ComponentTree on it
    TestCrashFromEachLayoutLifecycleMethod.triggerTestEvent(
        testLithoView.componentTree.context,
        triggerHandle,
        bazObject,
    )
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0]).isInstanceOf(RuntimeException::class.java)
    assertThat(errorOutput[0].message).contains("onTrigger crash")
    assertThat(errorOutput[0].message)
        .contains(
            "layout_stack:",
            "TestCrashFromEachLayoutLifecycleMethod",
            "->",
            "TestErrorBoundary",
        )
  }

  @Test
  fun testOnLoadStyleCrashWithTestErrorBoundary() {
    // note: this test has a slightly different flow than the rest, since
    // TestCrashFromEachLayoutLifecycleMethod.create(c, 0, android.R.style.Animation) crashes at
    // component initialization and not at layout, so we need to wrap it in another spec that will
    // rethrow from onCreateLayout()
    val crashingComponent = CrashFromLayoutFromStyle()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoTestRule.render { component }
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("onLoadStyle crash")
  }

  @Ignore("T243069803")
  @Test
  fun testOnBindDynamicValueCrashWithTestErrorBoundary() {
    val dynamicStringProp = DynamicValue("")
    val crashingComponent = DynamicPropCrasher(dynamicStringProp)
    class KErrorBoundary(private val childComponent: Component) : KComponent() {
      override fun ComponentScope.render(): Component? {
        val errorState = useState<Exception?> { null }
        useErrorBoundary { exception: Exception -> errorState.update(exception) }

        return if (errorState.value == null) childComponent
        else Text("error caught: ${errorState.value?.message}")
      }
    }
    val testLithoView = lithoTestRule.render { KErrorBoundary(crashingComponent) }
    dynamicStringProp.set("change_dynamic_prop_test")
    assertThat(testLithoView.findViewWithText("bindDynamic crash")).isNotNull
  }

  @Test
  fun testOnMountCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_MOUNT,
        "Crashed on ON_MOUNT",
        unmountAfter = false,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnUnMountCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_UNMOUNT,
        "Crashed on ON_UNMOUNT",
        unmountAfter = true,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnBindCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_BIND,
        "Crashed on ON_BIND",
        unmountAfter = false,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnUnBindCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_UNBIND,
        "Crashed on ON_UNBIND",
        unmountAfter = true,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnPrepareCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_PREPARE,
        "Crashed on ON_PREPARE",
        unmountAfter = false,
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnMeasureCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_MEASURE,
        "Crashed on ON_MEASURE",
        unmountAfter = false,
        expectHierarchy = true,
    )
  }

  @Test
  fun testOnBoundsDefinedCrashWithTestErrorBoundary() {
    crashingScenarioMountHelper(
        LifecycleStep.ON_BOUNDS_DEFINED,
        "Crashed on ON_BOUNDS_DEFINED",
        unmountAfter = false,
        expectHierarchy = false,
    )
  }

  @Test
  fun testOnCreateMountContentCrashWithTestErrorBoundary() {
    // RuntimeException we throw is wrapped, so we need to expect that one
    expectedException.expect(LithoMetadataExceptionWrapper::class.java)
    expectedException.expectMessage("Crashed on ON_CREATE_MOUNT_CONTENT")
    crashingScenarioMountHelper(
        LifecycleStep.ON_CREATE_MOUNT_CONTENT,
        "Crashed on ON_CREATE_MOUNT_CONTENT",
        unmountAfter = false,
        expectHierarchy = true,
        crashingComponentClass = CreateMountContentCrashing::class,
    ) { _ ->
      CreateMountContentCrashing()
    }
  }

  @Test
  fun testShouldUpdateCrashWithTestErrorBoundary() {
    val crashingComponent =
        CrashingMountable.create(lithoTestRule.context)
            .someStringProp("someString")
            .lifecycle(LifecycleStep.SHOULD_UPDATE)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val testLithoView = lithoTestRule.render { component }
    val crashingComponent2: Component =
        CrashingMountable.create(lithoTestRule.context)
            .someStringProp("someString2")
            .lifecycle(LifecycleStep.SHOULD_UPDATE)
            .build()
    val component2: Component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent2)
            .build()
    testLithoView.setRoot(component2).measure().layout()
    assertThat(errorOutput).hasSize(1)
    assertThat(errorOutput[0])
        .isInstanceOf(MountPhaseException::class.java)
        .hasMessage("Crashed on SHOULD_UPDATE")
  }

  private fun crashingScenarioMountHelper(
      crashFromStep: LifecycleStep,
      expectedMessage: String,
      unmountAfter: Boolean,
      expectHierarchy: Boolean,
      crashingComponentClass: KClass<*> = CrashingMountable::class,
      crashingComponentCreator: (crashFromStep: LifecycleStep) -> Component = {
        CrashingMountable.create(lithoTestRule.context)
            .someStringProp("someString")
            .lifecycle(crashFromStep)
            .build()
      },
  ) {
    val crashingComponent = crashingComponentCreator(crashFromStep)
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val testLithoView = lithoTestRule.render { component }

    // The crashing mountable component should not get added to
    // the RenderTree if the error is thrown before mount.
    if (
        crashFromStep == LifecycleStep.ON_PREPARE ||
            crashFromStep == LifecycleStep.ON_MEASURE ||
            crashFromStep == LifecycleStep.ON_BOUNDS_DEFINED
    ) {
      val count = testLithoView.lithoView.mountDelegateTarget.getRenderUnitCount()
      for (i in 0 until count) {
        val item = testLithoView.lithoView.mountDelegateTarget.getMountItemAt(i)
        item?.renderTreeNode?.let {
          assertThat(LithoRenderUnit.getRenderUnit(it).component)
              .isNotInstanceOf(crashingComponentClass.java)
        }
      }
    }

    if (unmountAfter) {
      testLithoView.lithoView.unmountAllItems()
    }
    assertThat(errorOutput).hasSize(1)
    if (expectHierarchy) {
      assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
      assertThat(errorOutput[0].message)
          .contains(
              "layout_stack:",
              "${crashingComponentClass.simpleName}",
              "->",
              "TestErrorBoundary",
          )
    } else {
      assertThat(errorOutput[0])
          .isInstanceOf(MountPhaseException::class.java)
          .hasMessage(expectedMessage)
    }
  }

  private fun crashingScenarioLayoutHelper(
      crashFromStep: LifecycleStep,
      expectedMessage: String,
      expectHierarchy: Boolean,
  ) {
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(crashFromStep)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    lithoTestRule.render { component }
    assertThat(errorOutput).hasSize(1)
    if (expectHierarchy) {
      assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
      assertThat(errorOutput[0].message)
          .contains(
              "layout_stack:",
              "TestCrashFromEachLayoutLifecycleMethod",
              "->",
              "TestErrorBoundary",
          )
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
      expectHierarchy: Boolean,
  ) {
    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
            .crashFromStep(crashFromStep)
            .build()
    val errorOutput: List<Exception> = ArrayList()
    val component =
        TestErrorBoundary.create(lithoTestRule.context)
            .heightPx(100)
            .errorOutput(errorOutput)
            .child(crashingComponent)
            .build()
    val rcc =
        RecyclerCollectionComponent.create(lithoTestRule.context)
            .recyclerConfiguration(ListRecyclerConfiguration.create().build())
            .section(
                SingleComponentSection.create(SectionContext(lithoTestRule.context))
                    .component(component)
                    .build(),
            )
            .build()
    val testLithoView = lithoTestRule.render(widthPx = 100, heightPx = 100) { rcc }
    if (releaseAfter) {
      testLithoView.release()
    }
    assertThat(errorOutput).hasSize(1)
    if (expectHierarchy) {
      assertThat(errorOutput[0]).isInstanceOf(LithoMetadataExceptionWrapper::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
      assertThat(errorOutput[0].message)
          .contains(
              "layout_stack:",
              "TestCrashFromEachLayoutLifecycleMethod",
              "->",
              "TestErrorBoundary",
          )
    } else {
      assertThat(errorOutput[0]).isInstanceOf(RuntimeException::class.java)
      assertThat(errorOutput[0].message).contains(expectedMessage)
    }
  }

  @Test
  fun testOnCreateLayoutCrashWithKotlinErrorBoundary() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    val crashingComponent =
        TestCrashFromEachLayoutLifecycleMethod.create(lithoTestRule.context)
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

    lithoTestRule.render { UseErrorComponent() }
    lithoTestRule.idle()

    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(1)
    assertThat(errorList[0].message).isEqualTo("onCreateLayout crash")
  }

  @Test
  fun testOnCreateLayoutWithSizeSpecCrashWithKotlinErrorBoundary() {
    lateinit var stateRef: AtomicReference<List<Exception>>

    val crashingComponent =
        TestCrasherOnCreateLayoutWithSizeSpec.create(lithoTestRule.context).build()

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

    lithoTestRule.render { UseErrorComponent() }
    lithoTestRule.idle()

    val errorList = stateRef.get()
    assertThat(errorList.size).isEqualTo(2)
    errorList.forEach { error ->
      assertThat(error.message).isEqualTo("onCreateLayoutWithSizeSpec crash")
    }
  }

  private class ThrowExceptionGrandChildTester : KComponent() {
    override fun ComponentScope.render(): Component {
      throw RuntimeException("Exception from the ThrowExceptionGrandChildTesterSpec")
    }
  }

  private class OnErrorNotPresentChild : KComponent() {
    override fun ComponentScope.render(): Component {
      return ThrowExceptionGrandChildTester()
    }
  }

  private class TestCrasherOnCreateLayout : KComponent() {
    override fun ComponentScope.render(): Component {
      throw RuntimeException("onCreateLayout crash")
    }
  }

  private class CrashKotlinComponent : KComponent() {
    override fun ComponentScope.render(): Component? {
      throw RuntimeException("crash from kotlin component")
    }
  }

  private class CrashFromLayoutFromStyle : KComponent() {
    override fun ComponentScope.render(): Component {
      return Column {
        child(
            TestCrashFromEachLayoutLifecycleMethod.create(context, 0, android.R.style.Animation)
                .crashFromStep(LifecycleStep.ON_LOAD_STYLE)
                .build(),
        )
      }
    }
  }
}
