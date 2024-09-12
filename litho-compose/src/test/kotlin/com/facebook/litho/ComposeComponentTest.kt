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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComposeComponentTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  lateinit var lithoView: LithoView

  @Before
  fun setUp() {
    val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get().apply {
          setContentView(FrameLayout(this))
        }
    lithoViewRule.context = ComponentContext(activity)
    lithoView = LithoView(lithoViewRule.context)
    activity.findViewById<ViewGroup>(android.R.id.content).addView(lithoView)
  }

  @Test
  fun `should render compose component`() {
    var rendered = false
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return ComposeComponent(
            composable = useComposable(Unit) { Box { rendered = true } },
            contentType = TEST_COMPONENT_CONTENT_TYPE)
      }
    }

    val testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent()
        }

    assertThat(rendered).isTrue()

    assertThat(testView.lithoView.getChildAt(0)).isInstanceOf(ComposeView::class.java)
    val composeView = testView.lithoView.getChildAt(0) as ComposeView

    assertThat(composeView.hasComposition).isTrue()

    testView.lithoView.unmountAllItems()

    assertThat(composeView.hasComposition).isFalse()
  }

  @Test
  fun `should update compose component with new content only when deps are changed`() {
    var boxRendered = false
    var rowRendered = false
    class TestComponent(private val useBoxComposable: Boolean) : KComponent() {
      override fun ComponentScope.render(): Component {
        val composable =
            useComposable(useBoxComposable) {
              if (useBoxComposable) {
                Box { boxRendered = true }
              } else {
                Row { rowRendered = true }
              }
            }

        return ComposeComponent(composable = composable, contentType = TEST_COMPONENT_CONTENT_TYPE)
      }
    }

    // Initial render
    var testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(useBoxComposable = true)
        }

    assertThat(boxRendered).isTrue()
    assertThat(rowRendered).isFalse()

    // Update with changed deps
    boxRendered = false
    rowRendered = false

    testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(useBoxComposable = false)
        }

    // deps are changed so box is not rendered and row is rendered
    assertThat(boxRendered).isFalse()
    assertThat(rowRendered).isTrue()

    // Update without changing deps
    boxRendered = false
    rowRendered = false

    testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(useBoxComposable = false)
        }

    assertThat(boxRendered).isFalse()
    // deps aren't changed so the Row shouldn't be re-rendered
    assertThat(rowRendered).isFalse()

    testView.lithoView.unmountAllItems()
  }

  @Test
  fun `should run effects cleanup when component is removed from the hierarchy`() {
    var launchedEffectRan = false
    var sideEffectRan = false
    var disposableEffectRan = false
    var disposableEffectDisposed = false
    var onRememberRan = false
    var onForgottenRan = false
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return ComposeComponent(
            composable =
                useComposable(Unit) {
                  LaunchedEffect(Unit) { launchedEffectRan = true }
                  SideEffect { sideEffectRan = true }
                  DisposableEffect(Unit) {
                    disposableEffectRan = true
                    onDispose { disposableEffectDisposed = true }
                  }
                  val remembered = remember {
                    object : RememberObserver {
                      override fun onRemembered() {
                        onRememberRan = true
                      }

                      override fun onForgotten() {
                        onForgottenRan = true
                      }

                      override fun onAbandoned() = Unit
                    }
                  }
                },
            contentType = TEST_COMPONENT_CONTENT_TYPE)
      }
    }

    val testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent()
        }

    assertThat(launchedEffectRan).isTrue()
    assertThat(sideEffectRan).isTrue()

    assertThat(disposableEffectRan).isTrue()
    assertThat(disposableEffectDisposed).isFalse()

    assertThat(onRememberRan).isTrue()
    assertThat(onForgottenRan).isFalse()

    testView.lithoView.unmountAllItems()

    assertThat(launchedEffectRan).isTrue()
    assertThat(sideEffectRan).isTrue()

    assertThat(disposableEffectRan).isTrue()
    assertThat(disposableEffectDisposed).isTrue()

    assertThat(onRememberRan).isTrue()
    assertThat(onForgottenRan).isTrue()
  }

  @Test
  fun `should run effects cleanup when component is updated with new composable content`() {
    var launchedEffectRan = false
    var sideEffectRan = false
    var disposableEffectRan = false
    var disposableEffectDisposed = false
    var onRememberRan = false
    var onForgottenRan = false
    class TestComponent(private val useComposableWithEffects: Boolean) : KComponent() {
      override fun ComponentScope.render(): Component {
        val composable =
            useComposable(useComposableWithEffects) {
              if (useComposableWithEffects) {
                LaunchedEffect(Unit) { launchedEffectRan = true }
                SideEffect { sideEffectRan = true }
                DisposableEffect(Unit) {
                  disposableEffectRan = true
                  onDispose { disposableEffectDisposed = true }
                }
                val remembered = remember {
                  object : RememberObserver {
                    override fun onRemembered() {
                      onRememberRan = true
                    }

                    override fun onForgotten() {
                      onForgottenRan = true
                    }

                    override fun onAbandoned() = Unit
                  }
                }
              } else {
                Row {}
              }
            }

        return ComposeComponent(composable = composable, contentType = TEST_COMPONENT_CONTENT_TYPE)
      }
    }

    // Initial render
    var testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(useComposableWithEffects = true)
        }

    assertThat(launchedEffectRan).isTrue()
    assertThat(sideEffectRan).isTrue()

    assertThat(disposableEffectRan).isTrue()
    assertThat(disposableEffectDisposed).isFalse()

    assertThat(onRememberRan).isTrue()
    assertThat(onForgottenRan).isFalse()

    // Update with changed deps
    testView =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(useComposableWithEffects = false)
        }

    assertThat(launchedEffectRan).isTrue()
    assertThat(sideEffectRan).isTrue()

    assertThat(disposableEffectRan).isTrue()
    assertThat(disposableEffectDisposed).isTrue()

    assertThat(onRememberRan).isTrue()
    assertThat(onForgottenRan).isTrue()

    testView.lithoView.unmountAllItems()
  }

  @Test
  fun `should use the same pool for components with the same content types`() {
    class TestComponent(private val contentType: Any) : KComponent() {
      override fun ComponentScope.render(): Component {
        return ComposeComponent(
            composable = useComposable(Unit) { Box {} }, contentType = contentType)
      }
    }

    val testViewFirst =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(contentType = TEST_COMPONENT_CONTENT_TYPE)
        }

    assertThat(testViewFirst.lithoView.getChildAt(0)).isInstanceOf(ComposeView::class.java)
    val composeViewFirst = testViewFirst.lithoView.getChildAt(0) as ComposeView

    // unmount and return the view to the pool
    testViewFirst.lithoView.unmountAllItems()

    // render component with the same content type
    val testViewSecond =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(contentType = TEST_COMPONENT_CONTENT_TYPE)
        }

    assertThat(testViewSecond.lithoView.getChildAt(0)).isInstanceOf(ComposeView::class.java)
    val composeViewSecond = testViewSecond.lithoView.getChildAt(0) as ComposeView

    // instances should be the same because content type is the same
    assertThat(composeViewFirst).isSameAs(composeViewSecond)

    testViewSecond.lithoView.unmountAllItems()
  }

  @Test
  fun `should use separate pools for components with different content types`() {
    class TestComponent(private val contentType: Any) : KComponent() {
      override fun ComponentScope.render(): Component {
        return ComposeComponent(
            composable = useComposable(Unit) { Box {} }, contentType = contentType)
      }
    }

    val testViewFirst =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(contentType = TEST_COMPONENT_CONTENT_TYPE)
        }

    assertThat(testViewFirst.lithoView.getChildAt(0)).isInstanceOf(ComposeView::class.java)
    val composeViewFirst = testViewFirst.lithoView.getChildAt(0) as ComposeView

    // unmount and return the view to the pool
    testViewFirst.lithoView.unmountAllItems()

    // render component with a different content type
    val testViewSecond =
        lithoViewRule.render(lithoView = lithoView, widthPx = 100, heightPx = 100) {
          TestComponent(contentType = OTHER_TEST_COMPONENT_CONTENT_TYPE)
        }

    assertThat(testViewSecond.lithoView.getChildAt(0)).isInstanceOf(ComposeView::class.java)
    val composeViewSecond = testViewSecond.lithoView.getChildAt(0) as ComposeView

    // instances should be different because content type is different
    assertThat(composeViewFirst).isNotSameAs(composeViewSecond)

    testViewSecond.lithoView.unmountAllItems()
  }

  @Test
  fun `should not crash when LithoView isn't attached to the window`() {
    val unattachedLithoView = LithoView(lithoViewRule.context)

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return ComposeComponent(
            composable = useComposable(Unit) { Box {} }, contentType = TEST_COMPONENT_CONTENT_TYPE)
      }
    }

    // This would crash without CompositionContext fix
    val testView =
        lithoViewRule.render(lithoView = unattachedLithoView, widthPx = 100, heightPx = 100) {
          TestComponent()
        }

    testView.lithoView.unmountAllItems()
  }
}

private val TEST_COMPONENT_CONTENT_TYPE = "com.facebook.litho.TestComponent"
private val OTHER_TEST_COMPONENT_CONTENT_TYPE = "com.facebook.litho.OtherTestComponent"
