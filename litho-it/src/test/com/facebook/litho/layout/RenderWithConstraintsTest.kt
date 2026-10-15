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

package com.facebook.litho.layout

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EmptyComponent
import com.facebook.litho.Handle
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.handle
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.onCleanup
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useCached
import com.facebook.litho.useEffect
import com.facebook.litho.useHandle
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class RenderWithConstraintsTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  private val defaultAnimationDisabled = ComponentsConfiguration.isAnimationDisabled
  private val defaultLayoutOnlyPassUsesResolveRenderState =
      ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState

  @Before
  fun setup() {
    ComponentsConfiguration.isAnimationDisabled = false
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.isAnimationDisabled = defaultAnimationDisabled
    ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState =
        defaultLayoutOnlyPassUsesResolveRenderState
  }

  @Test
  fun `when component renders with constraints then effect should run`() {
    var effectRunCount = 0
    var effectCleanupCount = 0
    val component = TestComponent(
        runEffects = { effectRunCount++ },
        cleanupEffects = { effectCleanupCount++ },
    )

    lithoTestRule.render { component }

    assertThat(effectRunCount).isEqualTo(1)

    assertThat(effectCleanupCount).isEqualTo(0)

    lithoTestRule.render { EmptyComponent() }
  }

  @Test
  fun `when component renders with constraints then transitions should be collected`() {
    val component = TestComponent(runEffects = {}, cleanupEffects = {})

    val handle = lithoTestRule.render { component }

    assertThat(checkNotNull(handle.committedLayoutState).transitions).hasSize(1)
  }

  @Test
  fun `when component renders with constraints then state updates should work correctly`() {
    val component = TestComponent(runEffects = {}, cleanupEffects = {})

    val handle = lithoTestRule.render { component }

    lithoTestRule.act(handle) { handle.findViewWithText("count0: 0").performClick() }

    handle.findViewWithText("count0: 1")
    handle.findViewWithText("count1: 0")

    lithoTestRule.act(handle) { handle.findViewWithText("count1: 0").performClick() }

    handle.findViewWithText("count0: 1")
    handle.findViewWithText("count1: 1")
  }

  @Test(expected = AssertionError::class)
  fun `when component renders with constraints then transition in parent should be collected`() {
    val component = TestComponent(
        useTransitionInParent = true,
        runEffects = {},
        cleanupEffects = {},
    )

    val handle = lithoTestRule.render { component }

    assertThat(handle.committedLayoutState!!.transitions).hasSize(2)
  }

  @Test
  fun `when child component is measured then return a valid LayoutResult`() {
    var wasLayoutInvoked = false
    val onLayout: RenderWithConstraintsScope.(LayoutResult?, Handle, Handle) -> Unit =
        { result, handleForRoot, handleForText ->
          wasLayoutInvoked = true

          assertThat(result).isNotNull
          requireNotNull(result)
          assertThat(result.height).isEqualTo(100)

          val root = result.find(handle = handleForRoot)
          assertThat(root).isSameAs(result)

          val textResult = result.find(handle = handleForText)
          assertThat(textResult).isNotNull
          requireNotNull(textResult)

          assertThat(textResult.width).isEqualTo(100)
          assertThat(textResult.layoutData).isNotNull
        }

    val component = TestComponentThatMeasuresChild(onLayout)

    lithoTestRule.render { component }

    assertThat(wasLayoutInvoked).isTrue
  }

  @Test
  fun `when re-measured with new constraints then hooks inside keep their state`() {
    assertHooksInsideKeepStateAcrossReMeasures()
  }

  @Test
  fun `when re-measured with resolve render state disabled then hooks inside keep their state`() {
    ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState = false
    assertHooksInsideKeepStateAcrossReMeasures()
  }

  private fun assertHooksInsideKeepStateAcrossReMeasures() {
    val calls = mutableMapOf<String, Int>()
    val testView =
        lithoTestRule.render(widthPx = 400, heightPx = 200) { HooksInsideComponent(calls) }
    lithoTestRule.idle()
    val afterFirstLayout = calls.toMap()

    // Layout-only passes: new size specs, same resolve.
    testView.setSizePx(320, 200).measure().layout()
    lithoTestRule.idle()
    testView.setSizePx(400, 200).measure().layout()
    lithoTestRule.idle()

    assertThat(afterFirstLayout)
        .isEqualTo(
            mapOf(
                "useState" to 1,
                "useCached" to 1,
                "useEffect" to 1,
                "child useState" to 1,
            ),
        )
    assertThat(calls)
        .describedAs("hook initializations after re-measures")
        .isEqualTo(afterFirstLayout)
  }

  @Test
  fun `when re-measured after a state update then the update is applied once`() {
    val component = TestComponent(runEffects = {}, cleanupEffects = {})
    val testView = lithoTestRule.render(widthPx = 400, heightPx = 400) { component }

    lithoTestRule.act(testView) { testView.findViewWithText("count0: 0").performClick() }
    assertThat(testView.findViewWithTextOrNull("count0: 1")).isNotNull

    testView.setSizePx(320, 400).measure().layout()
    lithoTestRule.idle()
    testView.setSizePx(400, 400).measure().layout()
    lithoTestRule.idle()

    assertThat(testView.findViewWithTextOrNull("count0: 1")).isNotNull
    assertThat(testView.findViewWithTextOrNull("count0: 2")).isNull()
  }

  private class HooksInsideComponent(private val calls: MutableMap<String, Int>) : KComponent() {
    // RenderWithConstraints runs its lambda synchronously during render; the lint does not know it.
    @Suppress("KComponentHookInDeferredLambda")
    override fun ComponentScope.render(): Component = RenderWithConstraints { constraints ->
      useState { calls.increment("useState") }
      useCached(Unit) { calls.increment("useCached") }
      useEffect(Unit) {
        calls.increment("useEffect")
        null
      }
      Column(style = Style.width(constraints.maxWidth.px)) { child(ChildWithState(calls)) }
    }
  }

  private class ChildWithState(private val calls: MutableMap<String, Int>) : KComponent() {
    override fun ComponentScope.render(): Component {
      useState { calls.increment("child useState") }
      return Text(text = "child")
    }
  }

  internal class TestComponent(
      val useTransitionInParent: Boolean = false,
      val runEffects: () -> Unit,
      val cleanupEffects: () -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {

      // FIXME: This transition is lost; NestedTreeHolder does not transfer to the nested node
      useTransition(
          if (useTransitionInParent) {
            Transition.create("parent")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .animator(Transition.timing(100))
          } else {
            null
          },
      )

      return RenderWithConstraints { constraints ->
        val count0 = useState { 0 }
        val count1 = useState { 0 }

        useEffect(Unit) {
          runEffects()
          onCleanup { cleanupEffects() }
        }

        useTransition(
            Transition.create("render-with-constraints")
                .animate(AnimatedProperties.ALPHA)
                .appearFrom(0f)
                .animator(Transition.timing(100)),
        )

        Column {
          child(Text(text = "Hello World"))
          child(
              Text(
                  text = "count0: ${count0.value}",
                  style = Style.viewTag("count0").onClick { count0.update { it + 1 } },
              ),
          )
          child(
              Text(
                  text = "count1: ${count1.value}",
                  style = Style.viewTag("count1").onClick { count1.update { it + 1 } },
              ),
          )
        }
      }
    }
  }

  internal class TestComponentThatMeasuresChild(
      private val onLayout: RenderWithConstraintsScope.(LayoutResult?, Handle, Handle) -> Unit,
  ) : KComponent() {
    override fun ComponentScope.render(): Component = RenderWithConstraints { constraints ->
      val handleForRoot: Handle = useHandle(Unit)
      val handleForText: Handle = useHandle(Unit)

      val component =
          handle(handle = handleForRoot) {
            Column(style = Style.height(100.px)) {
              child(
                  Column {
                    child(
                        Column {
                          child(
                              Column {
                                child(
                                    handle(handle = handleForText) {
                                      Text(text = "Hello World", style = Style.width(100.px))
                                    },
                                )
                              },
                          )
                        },
                    )
                  },
              )
            }
          }

      val result = component.measure(constraints)
      onLayout(result, handleForRoot, handleForText)

      return@RenderWithConstraints component
    }
  }
}

private fun MutableMap<String, Int>.increment(key: String) {
  this[key] = (this[key] ?: 0) + 1
}
