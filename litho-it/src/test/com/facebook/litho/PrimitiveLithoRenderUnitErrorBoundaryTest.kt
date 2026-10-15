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

import android.annotation.SuppressLint
import android.view.View
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.Is
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests that error boundaries correctly catch exceptions thrown from PrimitiveComponent binders.
 * Validates the try-catch wrapping in [PrimitiveLithoRenderUnit] that dispatches exceptions to
 * [ComponentUtils.handle].
 */
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class PrimitiveLithoRenderUnitErrorBoundaryTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Before
  fun assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        LithoDebugConfigurations.isDebugModeEnabled,
        Is.`is`(true),
    )
  }

  @Test
  fun testPrimitiveMountBindersCrashCaughtByTestErrorBoundary() {
    val errorsRef = AtomicReference<List<Exception>>(emptyList())
    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      ErrorBoundaryWrapper(child = CrashOnBindPrimitive(), errorsRef = errorsRef)
    }
    lithoTestRule.idle()
    val errors = errorsRef.get()
    assertThat(errors).isNotEmpty
    assertThat(errors[0].message).contains("crash from primitive bind")
  }

  @Test
  fun testPrimitiveMountBindersCrashWithoutBoundaryThrows() {
    var exception: Exception? = null
    try {
      lithoTestRule.render(widthPx = 100, heightPx = 100) { CrashOnBindPrimitive() }
    } catch (error: Exception) {
      // Expected: verifying exception propagates without error boundary
      exception = error
    }
    assertThat(exception).isNotNull
    assertThat(exception).hasStackTraceContaining("crash from primitive bind")
  }

  @Test
  fun testPrimitiveUnmountBindersCrashCaughtByTestErrorBoundary() {
    val errorsRef = AtomicReference<List<Exception>>(emptyList())
    val testLithoView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          ErrorBoundaryWrapper(child = CrashOnUnbindPrimitive(), errorsRef = errorsRef)
        }
    lithoTestRule.idle()
    assertThat(errorsRef.get()).isEmpty()
    testLithoView.lithoView.unmountAllItems()
    lithoTestRule.idle()
    val errors = errorsRef.get()
    assertThat(errors).isNotEmpty
    assertThat(errors[0].message).contains("crash from primitive unbind")
  }

  @Test
  fun testPrimitiveUpdateBindersCrashCaughtByTestErrorBoundary() {
    val errorsRef = AtomicReference<List<Exception>>(emptyList())
    val testLithoView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          ErrorBoundaryWrapper(
              child = CrashOnBindConditionalPrimitive(shouldCrash = false),
              errorsRef = errorsRef,
          )
        }
    lithoTestRule.idle()
    assertThat(errorsRef.get()).isEmpty()
    testLithoView
        .setRoot(
            ErrorBoundaryWrapper(
                child = CrashOnBindConditionalPrimitive(shouldCrash = true),
                errorsRef = errorsRef,
            ),
        )
        .measure()
        .layout()
    lithoTestRule.idle()
    val errors = errorsRef.get()
    assertThat(errors).isNotEmpty
    assertThat(errors[0].message).contains("crash from primitive bind on update")
  }

  @Test
  fun testPrimitiveMountBindersCrashInDeepHierarchyCaughtByTestErrorBoundary() {
    val errorsRef = AtomicReference<List<Exception>>(emptyList())
    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      ErrorBoundaryWrapper(
          child = Column.create(lithoTestRule.context).child(CrashOnBindPrimitive()).build(),
          errorsRef = errorsRef,
      )
    }
    lithoTestRule.idle()
    val errors = errorsRef.get()
    assertThat(errors).isNotEmpty
    assertThat(errors[0].message).contains("crash from primitive bind")
  }

  @Test
  fun testPrimitiveMountBindersCrashCaughtByInnerNestedErrorBoundary() {
    val outerErrorsRef = AtomicReference<List<Exception>>(emptyList())
    val innerErrorsRef = AtomicReference<List<Exception>>(emptyList())
    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      ErrorBoundaryWrapper(
          child = ErrorBoundaryWrapper(child = CrashOnBindPrimitive(), errorsRef = innerErrorsRef),
          errorsRef = outerErrorsRef,
      )
    }
    lithoTestRule.idle()
    val innerErrors = innerErrorsRef.get()
    assertThat(innerErrors).isNotEmpty
    assertThat(innerErrors[0].message).contains("crash from primitive bind")
    assertThat(outerErrorsRef.get()).isEmpty()
  }

  /**
   * A reusable error boundary KComponent that uses [useErrorBoundary] to capture exceptions. Wraps
   * [child] and exposes captured errors via [errorsRef].
   */
  private class ErrorBoundaryWrapper(
      private val child: Component,
      private val errorsRef: AtomicReference<List<Exception>>,
  ) : KComponent() {
    override fun ComponentScope.render(): Component? {
      val errorState = useState { listOf<Exception>() }
      errorsRef.set(errorState.value)
      useErrorBoundary { exception: Exception ->
        errorState.update { prevErrors -> prevErrors + listOf(exception) }
      }
      return if (errorState.value.isEmpty()) child else Text("error caught")
    }
  }

  /** A [PrimitiveComponent] that always crashes during bind (mountBinders). */
  private class CrashOnBindPrimitive : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      return LithoPrimitive(
          layoutBehavior = FixedSizeLayoutBehavior(100, 100),
          mountBehavior =
              MountBehavior(ViewAllocator { context -> View(context) }) {
                bind(Unit) { _ -> throw RuntimeException("crash from primitive bind") }
              },
          style = null,
      )
    }
  }

  /** A [PrimitiveComponent] that crashes during unbind (unmountBinders). */
  private class CrashOnUnbindPrimitive : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      return LithoPrimitive(
          layoutBehavior = FixedSizeLayoutBehavior(100, 100),
          mountBehavior =
              MountBehavior(ViewAllocator { context -> View(context) }) {
                bind(Unit) { _ ->
                  onUnbind { throw RuntimeException("crash from primitive unbind") }
                }
              },
          style = null,
      )
    }
  }

  /**
   * A [PrimitiveComponent] that conditionally crashes during bind. When [shouldCrash] changes from
   * false to true between renders, the binder re-runs via updateBinders and throws.
   */
  @SuppressLint("PrimitiveEmptyUnbindDetector")
  private class CrashOnBindConditionalPrimitive(
      private val shouldCrash: Boolean,
  ) : PrimitiveComponent() {
    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      return LithoPrimitive(
          layoutBehavior = FixedSizeLayoutBehavior(100, 100),
          mountBehavior =
              MountBehavior(ViewAllocator { context -> View(context) }) {
                bind(shouldCrash) { _ ->
                  if (shouldCrash) {
                    throw RuntimeException("crash from primitive bind on update")
                  }
                  onUnbind { /* no-op: test only verifies bind path */ }
                }
              },
          style = null,
      )
    }
  }
}
