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

import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.logging.TestComponentsLogger
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.TextInput
import java.util.concurrent.CountDownLatch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LithoStatsTest {

  private lateinit var resolveThreadShadowLooper: ShadowLooper
  private lateinit var layoutThreadShadowLooper: ShadowLooper
  private lateinit var context: ComponentContext
  private lateinit var testComponent: StateUpdateTestComponent
  private lateinit var componentTree: ComponentTree
  private lateinit var componentsLogger: ComponentsLogger
  private lateinit var lithoView: LithoView
  private lateinit var testComponentKey: String

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    componentsLogger = TestComponentsLogger()
    context =
        ComponentContext(ApplicationProvider.getApplicationContext(), LOG_TAG, componentsLogger)
    resolveThreadShadowLooper = ComponentTestHelper.getDefaultResolveThreadShadowLooper()
    layoutThreadShadowLooper = ComponentTestHelper.getDefaultLayoutThreadShadowLooper()
    testComponent = StateUpdateTestComponent()
    testComponentKey = testComponent.key
    componentTree = ComponentTree.create(context, testComponent).build()
    lithoView = LithoView(context)
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    ComponentTestHelper.measureAndLayout(lithoView)
  }

  private fun runToEndOfTasks() {
    resolveThreadShadowLooper.runToEndOfTasks()
    layoutThreadShadowLooper.runToEndOfTasks()
  }

  private fun runOneTask() {
    if (this::resolveThreadShadowLooper.isInitialized) {
      resolveThreadShadowLooper.runOneTask()
    }
    layoutThreadShadowLooper.runOneTask()
  }

  @Test
  fun updateStateAsync_incrementsAsyncCountAndTotalCount() {
    val beforeSync = LithoStats.getComponentTriggeredSyncStateUpdateCount()
    val beforeAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount()
    val beforeTotal = LithoStats.getComponentAppliedStateUpdateCount()
    componentTree.updateStateAsync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    runToEndOfTasks()
    val afterSync = LithoStats.getComponentTriggeredSyncStateUpdateCount()
    val afterAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount()
    val afterTotal = LithoStats.getComponentAppliedStateUpdateCount()
    assertThat(afterSync - beforeSync).isEqualTo(0)
    assertThat(afterAsync - beforeAsync).isEqualTo(1)
    assertThat(afterTotal - beforeTotal).isEqualTo(1)
  }

  @Test
  fun updateStateSync_incrementsSyncAndTotalCount() {
    val beforeSync = LithoStats.getComponentTriggeredSyncStateUpdateCount()
    val beforeAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount()
    val beforeTotal = LithoStats.getComponentAppliedStateUpdateCount()
    componentTree.updateStateSync(
        testComponentKey, StateUpdateTestComponent.createIncrementStateUpdate(), "test", false)
    runToEndOfTasks()
    val afterSync = LithoStats.getComponentTriggeredSyncStateUpdateCount()
    val afterAsync = LithoStats.getComponentTriggeredAsyncStateUpdateCount()
    val afterTotal = LithoStats.getComponentAppliedStateUpdateCount()
    assertThat(afterSync - beforeSync).isEqualTo(1)
    assertThat(afterAsync - beforeAsync).isEqualTo(0)
    assertThat(afterTotal - beforeTotal).isEqualTo(1)
  }

  @Test
  fun setRoot_incrementsCalculateLayoutOnUIAndTotalCount() {
    val beforeLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount()
    val beforeLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount()
    componentTree.root = StateUpdateTestComponent()
    val afterLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount()
    val afterLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount()
    assertThat(afterLayoutCalculationCount - beforeLayoutCalculationCount).isEqualTo(1)
    assertThat(afterLayoutCalculationOnUICount - beforeLayoutCalculationOnUICount).isEqualTo(1)
  }

  @Test
  fun setRootAsync_incrementsCalculateLayoutTotalCount() {
    val beforeLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount()
    val beforeLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount()
    componentTree.setRootAsync(StateUpdateTestComponent())
    val latch = CountDownLatch(1)
    Thread {
          // We have to do this inside another thread otherwise the execution of
          // mChangeSetThreadShadowLooper will happen on Main Thread
          runOneTask()
          if (!ComponentsConfiguration.useSeparateThreadHandlersForResolveAndLayout) {
            runOneTask()
          }
          latch.countDown()
        }
        .start()
    latch.await()
    val afterLayoutCalculationCount = LithoStats.getComponentCalculateLayoutCount()
    val afterLayoutCalculationOnUICount = LithoStats.getComponentCalculateLayoutOnUICount()
    assertThat(afterLayoutCalculationCount - beforeLayoutCalculationCount).isEqualTo(1)
    assertThat(afterLayoutCalculationOnUICount - beforeLayoutCalculationOnUICount).isEqualTo(0)
  }

  @Test
  fun mount_incrementsMountCount() {
    val beforeMountCount = LithoStats.getComponentMountCount()
    val component = TextInput.create(legacyLithoViewRule.context).build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val afterMountCount = LithoStats.getComponentMountCount()
    assertThat(afterMountCount - beforeMountCount).isEqualTo(1)
  }

  companion object {
    private const val LOG_TAG = "logTag"
  }
}
