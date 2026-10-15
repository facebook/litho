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
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityManager

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ResolverTest {

  @JvmField @Rule var lithoViewRule = LithoTestRule()
  private lateinit var accessibilityManager: AccessibilityManager
  private lateinit var shadowAccessibilityManager: ShadowAccessibilityManager

  @Before
  fun setup() {
    accessibilityManager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowAccessibilityManager = Shadow.extract(accessibilityManager)
    setAccessibilityState(isEnabled = false, touchExploration = false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    setAccessibilityState(isEnabled = false, touchExploration = false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  /**
   * A component with a stateful child and a lifecycle-tracked sibling. Used to test whether
   * reconciliation reuses the sibling (ON_CREATE_LAYOUT not called) or re-resolves it
   * (ON_CREATE_LAYOUT called).
   */
  private class ComponentWithStatefulChildAndTrackedSibling(
      private val stateUpdater: SimpleStateUpdateEmulatorSpec.Caller,
      private val lifecycleSteps: MutableList<StepInfo>,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      return Column {
        child(LayoutSpecLifecycleTester.create(context).steps(lifecycleSteps).build())
        child(SimpleStateUpdateEmulator.create(context).caller(stateUpdater).build())
      }
    }
  }

  /**
   * When a sync state update triggers reconciliation and the accessibility state has NOT changed,
   * sibling components should be reused (not re-resolved).
   */
  @Test
  fun `should reconcile siblings when accessibility state has not changed`() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()

    lithoViewRule.render {
      ComponentWithStatefulChildAndTrackedSibling(stateUpdater, lifecycleSteps)
    }
    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    // Trigger state update without changing accessibility state
    stateUpdater.increment()

    // Sibling should be reconciled (not re-resolved)
    assertThat(LifecycleStep.getSteps(lifecycleSteps))
        .doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)
  }

  /**
   * When a sync state update triggers reconciliation but the accessibility state has changed since
   * the previous resolve, reconciliation should be skipped and all components should be
   * re-resolved. This ensures needsHostView decisions are re-evaluated with the new accessibility
   * state.
   */
  @Test
  fun `should not reconcile when accessibility state changes`() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val lifecycleSteps: MutableList<StepInfo> = mutableListOf()

    // Initial render with accessibility disabled
    lithoViewRule.render {
      ComponentWithStatefulChildAndTrackedSibling(stateUpdater, lifecycleSteps)
    }
    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
    lifecycleSteps.clear()

    // Simulate a UiAutomation client connecting while the app is running.
    setAccessibilityState(isEnabled = true, touchExploration = true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()

    stateUpdater.increment()

    // Sibling should NOT be reconciled — full re-resolve needed
    assertThat(LifecycleStep.getSteps(lifecycleSteps)).contains(LifecycleStep.ON_CREATE_LAYOUT)
  }

  /**
   * Regression test: a SpecGeneratedComponent's common props (e.g. widthPx/heightPx) must be
   * applied to its resolved node so the measured layout reflects them. The Resolver previously had
   * a redundant fallback that pulled commonProps off the SpecGeneratedComponent itself if the
   * result didn't carry them; this verifies common-prop application still works after that fallback
   * was removed.
   */
  @Test
  fun `spec generated component common props are applied to resolved node`() {
    val steps: MutableList<StepInfo> = mutableListOf()
    val c = lithoViewRule.context
    val component =
        Column.create(c)
            .child(
                LayoutSpecLifecycleTester.create(c).widthPx(123).heightPx(45).steps(steps).build(),
            )
            .build()

    val testView = lithoViewRule.render { component }
    val rootResult = checkNotNull(testView.currentRootNode)
    val childResult = checkNotNull(rootResult.getChildAt(0))

    assertThat(childResult.width).isEqualTo(123)
    assertThat(childResult.height).isEqualTo(45)
  }

  /**
   * Regression test for the OCLWSS / deferred-resolution path: an `@OnCreateLayoutWithSizeSpec`
   * SpecGeneratedComponent goes through the deferred resolve flow. Common props set on it must
   * still be applied to the resolved node so the measured layout reflects them.
   */
  @Test
  fun `OCLWSS spec generated component common props are applied to resolved node`() {
    val steps: MutableList<StepInfo> = mutableListOf()
    val c = lithoViewRule.context
    val component =
        Column.create(c)
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .widthPx(77)
                    .heightPx(88)
                    .steps(steps)
                    .build(),
            )
            .build()

    val testView = lithoViewRule.render { component }
    val rootResult = checkNotNull(testView.currentRootNode)
    val childResult = checkNotNull(rootResult.getChildAt(0))

    assertThat(childResult.width).isEqualTo(77)
    assertThat(childResult.height).isEqualTo(88)
  }

  private fun setAccessibilityState(isEnabled: Boolean, touchExploration: Boolean) {
    shadowAccessibilityManager.setEnabled(isEnabled)
    shadowAccessibilityManager.setTouchExplorationEnabled(touchExploration)
  }
}
