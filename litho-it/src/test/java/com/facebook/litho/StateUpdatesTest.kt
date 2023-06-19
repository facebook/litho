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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.components.StateUpdateTestLayout
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.ThreadTestingUtils
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.logging.TestComponentsLogger
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
open class StateUpdatesTest {

  private var widthSpec = 0
  private var heightSpec = 0
  private lateinit var context: ComponentContext
  private lateinit var testComponent: StateUpdateTestComponent
  private lateinit var componentTree: ComponentTree
  private lateinit var componentsLogger: ComponentsLogger
  private lateinit var testComponentKey: String

  @JvmField
  @Rule
  var backgroundLayoutLooperRule: BackgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    setup(false)
  }

  fun setup(enableComponentTreeSpy: Boolean) {
    componentsLogger = TestComponentsLogger()
    context =
        ComponentContext(
            ApplicationProvider.getApplicationContext<Context>(), LOG_TAG, componentsLogger)
    widthSpec = exactly(39)
    heightSpec = exactly(41)
    testComponent = StateUpdateTestComponent()
    testComponentKey = testComponent.key
    legacyLithoViewRule.setRoot(testComponent)
    componentTree = legacyLithoViewRule.componentTree
    if (enableComponentTreeSpy) {
      componentTree = spy(componentTree)
    }
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(widthSpec, heightSpec)
  }

  @After
  fun tearDown() {
    // Empty all pending runnables.
    backgroundLayoutLooperRule.runToEndOfTasksSync()
  }

  @Test
  fun testNoCrashOnSameComponentKey() {
    val child1 = StateUpdateTestComponent()
    child1.key = "key"
    val child2 = StateUpdateTestComponent()
    child2.key = "key"
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c).child(child1).child(child2).build()
        }
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule
        .attachToWindow()
        .measure()
        .layout()
        .setSizeSpecs(exactly(1_000), unspecified())
  }

  @Test
  fun testNoCrashOnSameComponentKeyNestedContainers() {
    val child1 = StateUpdateTestComponent()
    child1.key = "key"
    val child2 = StateUpdateTestComponent()
    child2.key = "key"
    val component: Component =
        object : InlineLayoutSpec() {
          protected override fun onCreateLayout(c: ComponentContext): Component {
            return Column.create(c)
                .child(Column.create(c).child(child1))
                .child(Column.create(c).child(child2))
                .build()
          }
        }
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule
        .attachToWindow()
        .measure()
        .layout()
        .setSizeSpecs(exactly(1_000), unspecified())
  }

  @Test
  fun testKeepInitialStateValues() {
    val previousStateContainer =
        stateContainersMap?.get(testComponentKey) as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE)
    assertThat(resolveState?.initialStateContainer?.initialStates?.isEmpty()).isTrue
  }

  @Test
  fun testKeepUpdatedStateValue() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    val previousStateContainer =
        stateContainersMap?.get(testComponentKey) as StateUpdateTestComponent.TestStateContainer?
    assertThat(previousStateContainer).isNotNull
    assertThat(previousStateContainer?.count)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1)
  }

  @Test
  fun testClearUnusedStateContainers() {
    componentTree.updateStateSync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    assertThat(stateContainersMap?.keys?.size).isEqualTo(1)
    assertThat(stateContainersMap?.keys?.contains(testComponentKey)).isTrue
    val child1 = StateUpdateTestComponent()
    child1.key = "key"
    legacyLithoViewRule.setRoot(child1)
    legacyLithoViewRule
        .attachToWindow()
        .measure()
        .layout()
        .setSizeSpecs(exactly(1_000), unspecified())
    componentTree.updateStateSync(
        "\$key", StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    assertThat(stateContainersMap?.keys?.size).isEqualTo(1)
    assertThat(stateContainersMap?.keys?.contains("\$key")).isTrue
  }

  @Test
  fun testClearAppliedStateUpdates() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    assertThat(getPendingStateUpdatesForComponent(testComponentKey)).hasSize(1)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(getPendingStateUpdatesForComponent(testComponentKey)).isNull()
  }

  @Test
  fun testEnqueueStateUpdate() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    assertThat(getPendingStateUpdatesForComponent(testComponentKey)).hasSize(1)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    assertThat(
            (stateContainersMap?.get(testComponentKey)
                    as StateUpdateTestComponent.TestStateContainer?)
                ?.count)
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1)
    assertThat(getPendingStateUpdatesForComponent(testComponentKey)).hasSize(1)
  }

  @Test(expected = RuntimeException::class)
  fun testUpdateStateFromOnCreateLayout_throwsRuntimeExceptionWhenThresholdExceeds() {
    for (i in 0 until ComponentTree.STATE_UPDATES_IN_LOOP_THRESHOLD) {
      componentTree.updateStateInternal(false, "test", true)
    }
  }

  @Test
  fun testEnqueueStateUpdate_checkAppliedStateUpdate() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    assertThat(getPendingStateUpdatesForComponent(testComponentKey)).hasSize(1)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(getPendingStateUpdatesForComponent(testComponentKey)).isNullOrEmpty()
  }

  @Test
  fun testSetInitialStateValue() {
    assertThat(testComponent.getCount(context))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE)
  }

  @Test
  fun testUpdateState() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1)
  }

  @Test
  fun testLazyUpdateState_doesNotTriggerRelayout() {
    setup(true)
    componentTree.addMeasureListener { _, _, _, _ ->
      throw RuntimeException("Should not have computed a new layout!")
    }
    componentTree.updateStateLazy(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate())
    backgroundLayoutLooperRule.runToEndOfTasksSync()
  }

  @Test
  fun testLazyUpdateState_isCommittedOnlyOnRelayout() {
    componentTree.updateStateLazy(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate())
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE)
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createNoopStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1)
  }

  @Test
  fun testLazyUpdateState_isCommittedInCorrectOrder() {
    componentTree.updateStateLazy(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate())
    componentTree.updateStateLazy(
        testComponentKey, StateUpdateTestComponent.createMultiplyStateUpdate())
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo((StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1)
  }

  @Test
  fun testLazyUpdateState_everyLazyUpdateThatManagesToBeEnqueuedBeforeActualRelayoutGetsCommitted() {
    componentTree.updateStateLazy(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate())
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createMultiplyStateUpdate(), "test", false)
    componentTree.updateStateLazy(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate())
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo((StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1) * 2 + 1)
  }

  @Test
  fun testTransferState() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    componentTree.setSizeSpec(widthSpec, heightSpec)
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 1)
  }

  @Test
  fun testTransferAndUpdateState() {
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(testComponent.componentForStateUpdate.getCount(context))
        .isEqualTo(StateUpdateTestComponent.INITIAL_COUNT_STATE_VALUE + 2)
  }

  @Test
  fun testStateContainerDrained() {
    val componentTree = ComponentTree.create(context).build()
    val check = CountDownLatch(2)
    val waitForFirstLayoutToStart = CountDownLatch(1)
    val waitForFirstThreadToStart = CountDownLatch(1)
    val stateUpdateCalled = AtomicInteger(0)
    val stateValue = AtomicInteger(0)
    val secondStateValue = AtomicInteger(0)

    // The threading here is a bit tricky. The idea is that the first thread unlocks the second
    // thread from its onCreateLayout execution and blocks immediately. At this point the second
    // thread's onCreateLayout unblocks the first thread. We are now in the situation that we want
    // to test where both threads are executing a layout and the state for the inner component
    // has not been created yet.
    val thread1: Thread =
        object : Thread() {
          override fun run() {
            componentTree.setRootAndSizeSpecSync(
                StateUpdateTestLayout.create(context)
                    .awaitable(waitForFirstLayoutToStart)
                    .countDownLatch(waitForFirstThreadToStart)
                    .createStateCount(stateUpdateCalled)
                    .outStateValue(stateValue)
                    .build(),
                exactly(100),
                exactly(100))
            check.countDown()
          }
        }
    val thread2: Thread =
        object : Thread() {
          override fun run() {
            ThreadTestingUtils.failSilentlyIfInterrupted { waitForFirstThreadToStart.await() }
            componentTree.setRootAndSizeSpecSync(
                StateUpdateTestLayout.create(context)
                    .awaitable(null)
                    .countDownLatch(waitForFirstLayoutToStart)
                    .createStateCount(stateUpdateCalled)
                    .outStateValue(secondStateValue)
                    .build(),
                exactly(200),
                exactly(200))
            check.countDown()
          }
        }
    thread1.start()
    thread2.start()
    ThreadTestingUtils.failSilentlyIfInterrupted { check.await(5_000, TimeUnit.MILLISECONDS) }
    assertThat(resolveState?.initialStateContainer?.initialStates?.isEmpty()).isTrue
    assertThat(resolveState?.initialStateContainer?.pendingStateHandlers?.isEmpty()).isTrue
    assertThat(stateUpdateCalled.toInt()).isEqualTo(1)
    assertThat(stateValue.toInt()).isEqualTo(secondStateValue.toInt())
    assertThat(stateValue.toInt()).isEqualTo(10)
  }

  private val resolveState: StateHandler?
    get() = componentTree.treeState?.resolveState

  private val stateContainersMap: Map<String, StateContainer>?
    get() = resolveState?.stateContainers

  private val pendingStateUpdates: Map<String, List<StateContainer.StateUpdate>>?
    get() = resolveState?.pendingStateUpdates

  private fun getPendingStateUpdatesForComponent(
      globalKey: String?
  ): List<StateContainer.StateUpdate>? = pendingStateUpdates?.get(globalKey)

  companion object {
    private const val LOG_TAG = "logTag"
  }
}
