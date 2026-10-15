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

import com.facebook.litho.SizeSpec.AT_MOST
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.sp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Parity tests verifying that NestedLithoTree (resolve + layout) produces equivalent rendering
 * output to ComponentTree for the same input components.
 *
 * The goal is to ensure that NestedLithoTree can replace ComponentTree as the rendering engine for
 * nested Litho trees without observable differences in layout size, visual output, effect
 * lifecycle, and event handler behavior.
 */
@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class NestedLithoTreeParityTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  // ---------------------------------------------------------------------------
  // Layout and visual output parity
  //
  // Litho's Text component renders as a TextDrawable, not a TextView, so we use
  // LithoViewAssert.hasVisibleText to verify rendered content and component-finder
  // APIs to verify component tree structure.
  // ---------------------------------------------------------------------------

  @Test
  fun `component with fixed size measures the same via CT and nested tree`() {
    val ctView =
        lithoTestRule.render(
            widthSpec = makeSizeSpec(500, AT_MOST),
            heightSpec = makeSizeSpec(500, AT_MOST),
        ) {
          FixedSizeComponent()
        }
    val ntView =
        lithoTestRule.render(
            widthSpec = makeSizeSpec(500, AT_MOST),
            heightSpec = makeSizeSpec(500, AT_MOST),
        ) {
          NestedTreeHost { FixedSizeComponent() }
        }

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Fixed")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Fixed")
    assertThat(ctView.committedLayoutState?.width).isEqualTo(120)
    assertThat(ctView.committedLayoutState?.height).isEqualTo(80)
    assertThat(ntView.committedLayoutState?.width).isEqualTo(ctView.committedLayoutState?.width)
    assertThat(ntView.committedLayoutState?.height).isEqualTo(ctView.committedLayoutState?.height)
  }

  @Test
  fun `component with fixed size remeasures the same after size spec change`() {
    val ctView =
        lithoTestRule.render(
            widthSpec = makeSizeSpec(500, AT_MOST),
            heightSpec = makeSizeSpec(500, AT_MOST),
        ) {
          FixedSizeComponent()
        }
    val ntView =
        lithoTestRule.render(
            widthSpec = makeSizeSpec(500, AT_MOST),
            heightSpec = makeSizeSpec(500, AT_MOST),
        ) {
          NestedTreeHost { FixedSizeComponent() }
        }

    ctView.setSizeSpecs(makeSizeSpec(90, AT_MOST), makeSizeSpec(60, AT_MOST)).measure().layout()
    ntView.setSizeSpecs(makeSizeSpec(90, AT_MOST), makeSizeSpec(60, AT_MOST)).measure().layout()

    assertThat(ctView.committedLayoutState?.width).isEqualTo(90)
    assertThat(ctView.committedLayoutState?.height).isEqualTo(60)
    assertThat(ntView.committedLayoutState?.width).isEqualTo(ctView.committedLayoutState?.width)
    assertThat(ntView.committedLayoutState?.height).isEqualTo(ctView.committedLayoutState?.height)
  }

  @Test
  fun `tree props propagate through nested tree host the same as CT`() {
    val ctView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          TreePropProvider(labelTreeProp to "from tree prop") { TreePropReadingComponent() }
        }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          TreePropProvider(labelTreeProp to "from tree prop") {
            NestedTreeHost { TreePropReadingComponent() }
          }
        }

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Label: from tree prop")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Label: from tree prop")
  }

  @Test
  fun `tree prop updates rerender the same through nested tree host`() {
    val ctView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          TreePropProvider(labelTreeProp to "v1") { TreePropReadingComponent() }
        }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          TreePropProvider(labelTreeProp to "v1") { NestedTreeHost { TreePropReadingComponent() } }
        }

    ctView.setRoot(TreePropProvider(labelTreeProp to "v2") { TreePropReadingComponent() })
    ntView.setRoot(
        TreePropProvider(labelTreeProp to "v2") { NestedTreeHost { TreePropReadingComponent() } },
    )

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Label: v2")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Label: v2")
  }

  @Test
  fun `render crashes route to the same error handler via CT and nested tree`() {
    val ctErrors = mutableListOf<String>()
    val ntErrors = mutableListOf<String>()

    lithoTestRule.render(
        componentTree = createErrorBoundaryComponentTree(ctErrors),
        widthPx = 500,
        heightPx = 500,
    ) {
      CrashingComponent()
    }
    lithoTestRule.render(
        componentTree = createErrorBoundaryComponentTree(ntErrors),
        widthPx = 500,
        heightPx = 500,
    ) {
      NestedTreeHost { CrashingComponent() }
    }

    assertThat(ctErrors).hasSize(1)
    assertThat(ntErrors).hasSize(1)
    assertThat(ctErrors[0]).contains("boom")
    assertThat(ntErrors[0]).contains("boom")
  }

  // ---------------------------------------------------------------------------
  // Effect lifecycle parity
  // ---------------------------------------------------------------------------

  @Test
  fun `useEffect attach fires on initial render for both CT and nested tree`() {
    val ctEffects = mutableListOf<String>()
    val ntEffects = mutableListOf<String>()

    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      EffectTrackingComponent(ctEffects, dep = 0)
    }
    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      NestedTreeHost { EffectTrackingComponent(ntEffects, dep = 0) }
    }

    assertThat(ctEffects).containsExactly("attach 0")
    assertThat(ntEffects).containsExactly("attach 0")
  }

  @Test
  fun `useEffect cleanup fires on release for both CT and nested tree`() {
    val ctEffects = mutableListOf<String>()
    val ntEffects = mutableListOf<String>()

    val ctView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          EffectTrackingComponent(ctEffects, dep = 0)
        }
    val ntView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          NestedTreeHost { EffectTrackingComponent(ntEffects, dep = 0) }
        }

    ctEffects.clear()
    ntEffects.clear()

    ctView.detachFromWindow().release()
    ntView.detachFromWindow().release()

    assertThat(ctEffects).containsExactly("detach 0")
    assertThat(ntEffects).containsExactly("detach 0")
  }

  @Test
  fun `useEffect re-fires on dep change for both CT and nested tree`() {
    val ctEffects = mutableListOf<String>()
    val ntEffects = mutableListOf<String>()

    val ctView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          EffectTrackingComponent(ctEffects, dep = 0)
        }
    val ntView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          NestedTreeHost { EffectTrackingComponent(ntEffects, dep = 0) }
        }

    ctEffects.clear()
    ntEffects.clear()

    ctView.setRoot(EffectTrackingComponent(ctEffects, dep = 1))
    ntView.setRoot(NestedTreeHost { EffectTrackingComponent(ntEffects, dep = 1) })

    assertThat(ctEffects).containsExactly("detach 0", "attach 1")
    assertThat(ntEffects).containsExactly("detach 0", "attach 1")
  }

  @Test
  fun `useEffect does not re-fire when dep is unchanged for both CT and nested tree`() {
    val ctEffects = mutableListOf<String>()
    val ntEffects = mutableListOf<String>()

    val ctView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          EffectTrackingComponent(ctEffects, dep = 0)
        }
    val ntView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          NestedTreeHost { EffectTrackingComponent(ntEffects, dep = 0) }
        }

    ctEffects.clear()
    ntEffects.clear()

    ctView.setRoot(EffectTrackingComponent(ctEffects, dep = 0))
    ntView.setRoot(NestedTreeHost { EffectTrackingComponent(ntEffects, dep = 0) })

    assertThat(ctEffects).isEmpty()
    assertThat(ntEffects).isEmpty()
  }

  // ---------------------------------------------------------------------------
  // Event handler parity
  // ---------------------------------------------------------------------------

  @Test
  fun `click handler fires for both CT and nested tree`() {
    val ctClicks = mutableListOf<String>()
    val ntClicks = mutableListOf<String>()

    val ctView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          ClickTrackingComponent(ctClicks, id = "btn")
        }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          NestedTreeHost { ClickTrackingComponent(ntClicks, id = "btn") }
        }

    lithoTestRule.act(ctView) { clickOnTag("btn") }
    lithoTestRule.act(ntView) { clickOnTag("btn") }

    assertThat(ctClicks).containsExactly("clicked btn")
    assertThat(ntClicks).containsExactly("clicked btn")
  }

  @Test
  fun `click handler captures updated props after re-render for both CT and nested tree`() {
    val ctClicks = mutableListOf<String>()
    val ntClicks = mutableListOf<String>()

    val ctView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          ClickTrackingComponent(ctClicks, id = "v1")
        }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          NestedTreeHost { ClickTrackingComponent(ntClicks, id = "v1") }
        }

    ctView.setRoot(ClickTrackingComponent(ctClicks, id = "v2"))
    ntView.setRoot(NestedTreeHost { ClickTrackingComponent(ntClicks, id = "v2") })

    lithoTestRule.act(ctView) { clickOnTag("v2") }
    lithoTestRule.act(ntView) { clickOnTag("v2") }

    assertThat(ctClicks).containsExactly("clicked v2")
    assertThat(ntClicks).containsExactly("clicked v2")
  }

  // ---------------------------------------------------------------------------
  // State parity
  // ---------------------------------------------------------------------------

  @Test
  fun `useState initial value renders the same for both CT and nested tree`() {
    val ctView = lithoTestRule.render(widthPx = 500, heightPx = 500) { CounterComponent() }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          NestedTreeHost { CounterComponent() }
        }

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Count: 0")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Count: 0")
  }

  @Test
  fun `state update via click produces same output for both CT and nested tree`() {
    val ctView = lithoTestRule.render(widthPx = 500, heightPx = 500) { CounterComponent() }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          NestedTreeHost { CounterComponent() }
        }

    lithoTestRule.act(ctView) { clickOnTag("increment") }
    lithoTestRule.act(ntView) { clickOnTag("increment") }

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Count: 1")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Count: 1")
  }

  @Test
  fun `multiple state updates produce same output for both CT and nested tree`() {
    val ctView = lithoTestRule.render(widthPx = 500, heightPx = 500) { CounterComponent() }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          NestedTreeHost { CounterComponent() }
        }

    repeat(3) {
      lithoTestRule.act(ctView) { clickOnTag("increment") }
      lithoTestRule.act(ntView) { clickOnTag("increment") }
    }

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Count: 3")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Count: 3")
  }

  @Test
  fun `state persists across re-renders with same props for both CT and nested tree`() {
    val ctView = lithoTestRule.render(widthPx = 500, heightPx = 500) { CounterComponent() }
    val ntView =
        lithoTestRule.render(widthPx = 500, heightPx = 500) {
          NestedTreeHost { CounterComponent() }
        }

    lithoTestRule.act(ctView) { clickOnTag("increment") }
    lithoTestRule.act(ntView) { clickOnTag("increment") }

    ctView.setRoot(CounterComponent())
    ntView.setRoot(NestedTreeHost { CounterComponent() })

    LithoViewAssert.assertThat(ctView.lithoView).hasVisibleText("Count: 1")
    LithoViewAssert.assertThat(ntView.lithoView).hasVisibleText("Count: 1")
  }

  // ---------------------------------------------------------------------------
  // Test components
  // ---------------------------------------------------------------------------

  private class FixedSizeComponent : KComponent() {
    override fun ComponentScope.render(): Component =
        Column(style = Style.width(120.dp).height(80.dp)) {
          child(Text(text = "Fixed", textSize = 14.sp))
        }
  }

  private class EffectTrackingComponent(
      private val effects: MutableList<String>,
      private val dep: Int,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      useEffect(dep) {
        effects.add("attach $dep")
        onCleanup { effects.add("detach $dep") }
      }
      return Row()
    }
  }

  private class ClickTrackingComponent(
      private val clicks: MutableList<String>,
      private val id: String,
  ) : KComponent() {
    override fun ComponentScope.render(): Component {
      return Row(style = Style.viewTag(id).onClick { _ -> clicks.add("clicked $id") }) {
        child(Text(text = id, textSize = 14.sp))
      }
    }
  }

  private class CounterComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val count = useState { 0 }
      return Column {
        child(Text(text = "Count: ${count.value}", textSize = 14.sp))
        child(
            Row(style = Style.viewTag("increment").onClick { _ -> count.update(count.value + 1) }) {
              child(Text(text = "+", textSize = 14.sp))
            },
        )
      }
    }
  }

  private class TreePropReadingComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Text(text = "Label: ${labelTreeProp.value}", textSize = 14.sp)
    }
  }

  private class CrashingComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      error("boom")
    }
  }

  private fun createErrorBoundaryComponentTree(errors: MutableList<String>): ComponentTree {
    return ComponentTree.create(lithoTestRule.context)
        .componentsConfiguration(
            lithoTestRule.context.lithoConfiguration.componentsConfig.copy(
                errorEventHandler =
                    object : ErrorEventHandler() {
                      override fun onError(cc: ComponentContext, e: Exception): Component {
                        errors.add(e.message ?: "missing message")
                        return com.facebook.litho.widget.Text.create(cc)
                            .text("Recovered from error")
                            .textSizeSp(14f)
                            .build()
                      }
                    },
            ),
        )
        .enableChildClipping(true)
        .build()
  }

  /**
   * PrimitiveComponent wrapper that renders its child via useNestedTree + NestedLithoTree, matching
   * the production pattern (e.g., VerticalScroll). Uses bindToRenderTreeView so that effects and
   * mount lifecycle are handled identically to production.
   */
  private class NestedTreeHost(
      private val child: ComponentScope.() -> Component,
  ) : PrimitiveComponent() {

    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      val (nestedTreeState, resolveResult) = useNestedTree(root = child())

      return LithoPrimitive(
          Primitive(
              layoutBehavior = NestedTreeLayoutBehavior(nestedTreeState, resolveResult),
              mountBehavior =
                  MountBehavior(
                      ViewAllocator { context -> LithoRenderTreeView(context) },
                  ) {
                    bindToRenderTreeView(state = nestedTreeState) { this }
                  },
          ),
          style = null,
      )
    }
  }

  private class NestedTreeLayoutBehavior(
      private val nestedTreeState: NestedLithoTreeState,
      private val resolveResult: ResolveResult,
  ) : LayoutBehavior {
    override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
      val layoutState =
          NestedLithoTree.layout(
              result = resolveResult,
              sizeConstraints = sizeConstraints,
              current = nestedTreeState.currentLayoutState,
              version = layoutVersion,
          )

      return PrimitiveLayoutResult(
          width = layoutState.width.coerceIn(sizeConstraints.minWidth, sizeConstraints.maxWidth),
          height =
              layoutState.height.coerceIn(sizeConstraints.minHeight, sizeConstraints.maxHeight),
          layoutData = layoutState,
      )
    }
  }

  companion object {
    private val labelTreeProp = treePropOf { "default label" }
  }
}
