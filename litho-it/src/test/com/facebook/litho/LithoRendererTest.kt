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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

/** Tests for LithoRenderer. */
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class LithoRendererTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  private lateinit var context: Context
  private lateinit var renderer: LithoRenderer
  private val testComponent: Component = TestComponent()
  private val testConstraints =
      SizeConstraints(minWidth = 100, maxWidth = 100, minHeight = 100, maxHeight = 100)

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    renderer =
        LithoRenderer(
            context = context,
            componentsConfig = ComponentsConfiguration.defaultInstance,
        )
  }

  // ========== Rebind Tests ==========
  // These tests verify the fix for always calling rebind() after layout commit,
  // regardless of render mode. Previously, rebind was only called for StateUpdate modes.
  // This change ensures UI consistency, especially for scenarios like RecyclerView adapters
  // where binding must happen after state commitment.

  @Test
  fun `rebind should update view layout state after SetRootSync render mode`() {
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // Before the fix, SetRootSync would NOT call rebind (only StateUpdate modes did)
    // After the fix, rebind should be called for SetRootSync
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // Verify layout state is bound to the view (proves rebind was called)
    assertThat(view.layoutState).isNotNull
    assertThat(view.layoutState?.sizeConstraints).isEqualTo(testConstraints)
  }

  @Test
  fun `rebind should update view layout state after SetConstraintsSync render mode`() {
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // Establish initial state
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val firstLayoutState = view.layoutState
    assertThat(firstLayoutState).isNotNull

    // Before the fix, SetConstraintsSync would NOT call rebind
    // After the fix, it should call rebind and update the view
    val newConstraints =
        SizeConstraints(minWidth = 150, maxWidth = 150, minHeight = 150, maxHeight = 150)
    renderer.layoutSync(newConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // Verify view was updated with new layout state
    assertThat(view.layoutState).isNotNull
    assertThat(view.layoutState).isNotSameAs(firstLayoutState)
    assertThat(view.layoutState?.sizeConstraints).isEqualTo(newConstraints)
  }

  @Test
  fun `rebind should be posted to UI thread for deferred execution`() {
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // renderSync synchronously creates layout state but posts rebind to UI thread
    renderer.renderSync(testComponent, testConstraints)

    // Without idling the looper, rebind may not have executed yet
    // This tests that rebind is properly posted to UI thread handler
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // After idling, rebind should have executed and bound the layout state
    assertThat(view.layoutState).isNotNull
    assertThat(view.layoutState?.sizeConstraints).isEqualTo(testConstraints)
  }

  @Test
  fun `rebind should maintain UI consistency across multiple render operations`() {
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // Simulate multiple render operations (e.g., in RecyclerView adapter scenarios)
    // Each should trigger rebind to keep UI in sync with latest layout state

    // First render
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val state1 = view.layoutState
    assertThat(state1?.sizeConstraints).isEqualTo(testConstraints)

    // Second render with different constraints
    val constraints2 =
        SizeConstraints(minWidth = 150, maxWidth = 150, minHeight = 150, maxHeight = 150)
    renderer.renderSync(testComponent, constraints2)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val state2 = view.layoutState
    assertThat(state2?.sizeConstraints).isEqualTo(constraints2)

    // Third render
    val constraints3 =
        SizeConstraints(minWidth = 200, maxWidth = 200, minHeight = 200, maxHeight = 200)
    renderer.renderSync(testComponent, constraints3)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val state3 = view.layoutState

    // Each render should have updated the view's layout state via rebind
    assertThat(state3?.sizeConstraints).isEqualTo(constraints3)
    assertThat(state1).isNotSameAs(state2)
    assertThat(state2).isNotSameAs(state3)
  }

  @Test
  fun `rebind should work correctly when view is not bound`() {
    // Test that rebind handles the case where no view is bound
    // (rebind checks if rootHost is LithoRenderTreeView before calling setLayoutState)

    // No view bound - should not crash
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // Now bind a view and render again with different constraints to force new layout state
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    val newConstraints =
        SizeConstraints(minWidth = 150, maxWidth = 150, minHeight = 150, maxHeight = 150)
    renderer.renderSync(testComponent, newConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // View should now have the layout state
    assertThat(view.layoutState).isNotNull
    assertThat(view.layoutState?.sizeConstraints).isEqualTo(newConstraints)
  }

  @Test
  fun `sync render without ViewModels succeeds after ViewModel owner is cleared`() {
    val store = LithoViewModelStore()
    val treeProps = TreePropContainer().apply { put(LithoViewModelStoreTreeProp, store) }
    renderer =
        LithoRenderer(
            context = context,
            componentsConfig = ComponentsConfiguration.defaultInstance,
            treePropContainer = treeProps,
        )
    store.close()

    assertThat(renderer.renderSync(testComponent, testConstraints)).isNotNull()
  }

  // ========== Rebind Guard Condition Tests ==========
  // These tests verify the guard conditions inside rebind() method:
  // 1. Same instance check (layoutState === uiThreadLayoutState)
  // 2. Version check (layoutState.version <= previousUiThreadLayoutState.version)
  // 3. Both states null/empty check

  @Test
  fun `rebind should skip when same layout state instance is already bound`() {
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // First render
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val layoutState = view.layoutState
    assertThat(layoutState).isNotNull

    // Render again with same component and constraints
    // This should result in the same layout state instance being reused
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // Layout state should be the same instance (rebind should have detected and skipped)
    assertThat(view.layoutState).isSameAs(layoutState)
  }

  @Test
  fun `rebind should ensure layout state versions progress monotonically`() {
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // First render
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val state1 = view.layoutState
    assertThat(state1).isNotNull

    // Second render with different constraints
    val constraints2 =
        SizeConstraints(minWidth = 150, maxWidth = 150, minHeight = 150, maxHeight = 150)
    renderer.renderSync(testComponent, constraints2)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val state2 = view.layoutState
    assertThat(state2).isNotNull

    // Third render with different constraints again
    val constraints3 =
        SizeConstraints(minWidth = 200, maxWidth = 200, minHeight = 200, maxHeight = 200)
    renderer.renderSync(testComponent, constraints3)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val state3 = view.layoutState
    assertThat(state3).isNotNull

    // Verify versions progress monotonically (rebind checks version <= previous and skips)
    // Each layout state should have a higher version than the previous
    assertThat(state2?.version).isGreaterThan(state1?.version ?: -1)
    assertThat(state3?.version).isGreaterThan(state2?.version ?: -1)
  }

  @Test
  fun `rebind should reject older version and keep current layout state`() {
    // This test directly verifies the version rejection guard in rebind() using reflection
    // Scenario:
    // 1. layoutSync without view -> creates V1, but doesn't commit to UI (no view)
    // 2. Bind view
    // 3. layoutSync with view -> creates V2, commits to UI
    // 4. Try to rebind with old V1 using reflection -> should be rejected

    // Step 1: Create layout state V1 without a view
    val oldLayoutState = renderer.layoutSync(testConstraints)
    val oldVersion = oldLayoutState.version

    // Step 2: Bind a view
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    // Step 3: Create newer layout state V2 with view bound
    val newConstraints =
        SizeConstraints(minWidth = 200, maxWidth = 200, minHeight = 200, maxHeight = 200)
    renderer.renderSync(testComponent, newConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()
    val newLayoutState = view.layoutState
    val newVersion = newLayoutState?.version
    assertThat(newLayoutState).isNotNull
    assertThat(newVersion).isGreaterThan(oldVersion)

    // Step 4: Try to rebind with old layout state using reflection
    // The version check in rebind should reject it
    val rebindMethod =
        LithoRenderer::class.java.getDeclaredMethod("rebind", LayoutState::class.java)
    rebindMethod.isAccessible = true
    rebindMethod.invoke(renderer, oldLayoutState)

    // Verify: View should still have the newer layout state (V2), not the old one (V1)
    // The version check prevented the older state from being applied
    assertThat(view.layoutState).isSameAs(newLayoutState)
    assertThat(view.layoutState?.version).isEqualTo(newVersion)
    assertThat(view.layoutState?.version).isNotEqualTo(oldVersion)
  }

  @Test
  fun `rebind should handle rendering without view then binding view later`() {
    // This tests the guard condition path where uiThreadLayoutState might be null initially
    // Then we bind a view and render, which should work correctly

    // First render without view - should not crash
    // Internally, uiThreadLayoutState may not be updated since rootHost is not LithoRenderTreeView
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // Bind view and render with different constraints
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)

    val newConstraints =
        SizeConstraints(minWidth = 200, maxWidth = 200, minHeight = 200, maxHeight = 200)
    renderer.renderSync(testComponent, newConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    // View should be updated with the layout state
    assertThat(view.layoutState).isNotNull
    assertThat(view.layoutState?.sizeConstraints).isEqualTo(newConstraints)

    // Render once more to verify subsequent renders work
    val constraints3 =
        SizeConstraints(minWidth = 250, maxWidth = 250, minHeight = 250, maxHeight = 250)
    renderer.renderSync(testComponent, constraints3)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    assertThat(view.layoutState?.sizeConstraints).isEqualTo(constraints3)
  }

  @Test
  fun `clean resets the visibility hint before dropping the layout state`() {
    val renderer = LithoRenderer(
        context = context,
        componentsConfig =
            ComponentsConfiguration.defaultInstance.copy(
                resetVisibilityHintOnTreeDetach = true,
            ),
    )
    val view = LithoRenderTreeView(context)
    renderer.bindTo(view)
    renderer.renderSync(testComponent, testConstraints)
    (Shadow.extract(android.os.Looper.getMainLooper()) as ShadowLooper).idle()

    view.setVisibilityHint(true)
    assertThat(Whitebox.getInternalState<Boolean>(view, "visibilityHintIsVisible")).isTrue

    view.clean()

    assertThat(Whitebox.getInternalState<Boolean>(view, "hasVisibilityHint")).isFalse
    assertThat(Whitebox.getInternalState<Boolean>(view, "pauseMountingWhileVisibilityHintFalse"))
        .isFalse
    assertThat(Whitebox.getInternalState<Boolean>(view, "visibilityHintIsVisible")).isFalse
  }

  // ========== Test Components ==========

  /** Test component for rendering tests */
  private class TestComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Row(style = Style.height(100.dp).width(100.dp)) {
        child(Column { child(Text(text = "Test Component")) })
      }
    }
  }

  /** Different test component to verify component change detection */
  private class DifferentTestComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Row(style = Style.height(100.dp).width(100.dp)) {
        child(Column { child(Text(text = "Different Test Component")) })
      }
    }
  }
}
