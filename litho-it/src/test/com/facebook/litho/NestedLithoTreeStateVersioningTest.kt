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

import android.view.View
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.core.height
import com.facebook.litho.core.orDefault
import com.facebook.litho.core.width
import com.facebook.litho.effects.EffectHandler
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.CardClip
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.yoga.YogaDirection
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * Tests for NestedLithoTree versioning mechanism.
 *
 * These tests verify that nested tree resolve/layout results respect the parent's version, and that
 * the versioning mechanism prevents race conditions by rejecting stale layout states.
 */
@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NestedLithoTreeStateVersioningTest {

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()

  /**
   * Test that a component with nested tree gets layout state with version matching parent's
   * version. This verifies that the nested tree layout uses parentLayoutVersion correctly.
   */
  @Test
  fun nestedTreeInComponent_layoutStateVersionMatchesParentVersion() {
    val nestedStateRef = AtomicReference<NestedLithoTreeState>()
    val capturedParentVersionRef = AtomicInteger(-1)

    // Render a component that uses useNestedTree
    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      NestedTreeTestComponent(
          nestedStateRef = nestedStateRef,
          capturedParentVersionRef = capturedParentVersionRef,
      ) {
        Text(text = "Hello")
      }
    }

    // The nested tree state should have been set
    val nestedState = nestedStateRef.get()
    assertThat(nestedState).isNotNull()

    // The captured parent version should be valid
    val capturedParentVersion = capturedParentVersionRef.get()
    assertThat(capturedParentVersion).isGreaterThanOrEqualTo(0)

    // The nested tree's layout state version should match the captured parent version
    val layoutState = nestedState.currentLayoutState
    assertThat(layoutState).isNotNull()
    assertThat(layoutState!!.version)
        .describedAs("Nested tree layout version should match parent layout version")
        .isEqualTo(capturedParentVersion)
  }

  /**
   * Test that when the parent tree does multiple layouts, the nested tree's version matches each
   * parent's version.
   */
  @Test
  fun nestedTreeMultipleLayouts_versionMatchesParentVersion() {
    val nestedStateRef = AtomicReference<NestedLithoTreeState>()
    val capturedParentVersionRef = AtomicInteger(-1)

    val testView =
        lithoTestRule.render(widthPx = 100, heightPx = 100) {
          NestedTreeTestComponent(
              nestedStateRef = nestedStateRef,
              capturedParentVersionRef = capturedParentVersionRef,
          ) {
            Text(text = "Hello")
          }
        }

    // Get the first version
    val nestedState = nestedStateRef.get()
    assertThat(nestedState).isNotNull()
    val firstParentVersion = capturedParentVersionRef.get()
    val firstNestedLayoutVersion = nestedState.currentLayoutState!!.version

    // Verify first layout: nested version matches parent version
    assertThat(firstNestedLayoutVersion)
        .describedAs("First layout: nested tree version should match parent version")
        .isEqualTo(firstParentVersion)

    // Trigger a re-layout with different size
    testView.setSizeSpecs(makeSizeSpec(200, SizeSpec.EXACTLY), makeSizeSpec(200, SizeSpec.EXACTLY))

    // Get the second version
    val secondParentVersion = capturedParentVersionRef.get()
    val secondNestedLayoutVersion = nestedState.currentLayoutState!!.version

    // Verify second layout: nested version matches new parent version
    assertThat(secondNestedLayoutVersion)
        .describedAs("Second layout: nested tree version should match parent version")
        .isEqualTo(secondParentVersion)

    // Parent version should have increased (or stayed same if cached)
    assertThat(secondParentVersion).isGreaterThanOrEqualTo(firstParentVersion)
  }

  /**
   * Test the double measure scenario using a real wrapper component with nested tree. The component
   * measures its nested tree twice with different size constraints within the same layout pass.
   * Both measures should use the same parent version.
   */
  @Test
  fun nestedTreeDoubleMeasure_realComponentVerifiesBothMeasuresUseSameVersion() {
    val nestedStateRef = AtomicReference<NestedLithoTreeState>()
    val capturedVersionsRef = AtomicReference<MutableList<Int>>(mutableListOf())

    // Render a component that does double measure of its nested tree
    lithoTestRule.render(widthPx = 200, heightPx = 200) {
      DoubleMeasureNestedTreeComponent(
          nestedStateRef = nestedStateRef,
          capturedVersionsRef = capturedVersionsRef,
      ) {
        Text(text = "Double Measure Content")
      }
    }

    // Verify the nested tree state was set
    val nestedState = nestedStateRef.get()
    assertThat(nestedState).isNotNull()

    // Verify both measures captured the same parent version
    val capturedVersions = capturedVersionsRef.get()
    assertThat(capturedVersions)
        .describedAs("Should have captured versions from both measures")
        .hasSize(2)

    val firstMeasureVersion = capturedVersions[0]
    val secondMeasureVersion = capturedVersions[1]

    assertThat(firstMeasureVersion)
        .describedAs("First measure should have valid parent version")
        .isGreaterThanOrEqualTo(0)

    assertThat(secondMeasureVersion)
        .describedAs("Both measures should use the same parent version")
        .isEqualTo(firstMeasureVersion)

    // Verify the committed layout state has the same version
    assertThat(nestedState.currentLayoutState?.version)
        .describedAs("Committed layout should have the parent version")
        .isEqualTo(firstMeasureVersion)
  }

  /**
   * Test component that simulates double measure by measuring its nested tree twice with different
   * size constraints within the same layout pass.
   */
  private class DoubleMeasureNestedTreeComponent(
      private val nestedStateRef: AtomicReference<NestedLithoTreeState>,
      private val capturedVersionsRef: AtomicReference<MutableList<Int>>,
      private val child: ComponentScope.() -> Component,
  ) : PrimitiveComponent() {

    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      val (nestedTreeState, resolveResult) = useNestedTree(root = child())

      // Store the nested tree state for test verification
      nestedStateRef.set(nestedTreeState)

      return LithoPrimitive(
          Primitive(
              layoutBehavior =
                  DoubleMeasureLayoutBehavior(
                      nestedTreeState,
                      resolveResult,
                      capturedVersionsRef,
                  ),
              mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
          ),
          style = null,
      )
    }
  }

  /**
   * Layout behavior that measures the nested tree twice with different size constraints, simulating
   * the double measure scenario that can occur in real layouts.
   */
  private class DoubleMeasureLayoutBehavior(
      private val nestedTreeState: NestedLithoTreeState,
      private val resolveResult: ResolveResult,
      private val capturedVersionsRef: AtomicReference<MutableList<Int>>,
  ) : LayoutBehavior {
    override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
      val capturedVersions = capturedVersionsRef.get()

      // First measure with smaller constraints (simulating AT_MOST measure)
      val firstMeasureConstraints =
          SizeConstraints.fromMeasureSpecs(
              makeSizeSpec(sizeConstraints.maxWidth / 2, SizeSpec.AT_MOST),
              makeSizeSpec(sizeConstraints.maxHeight / 2, SizeSpec.AT_MOST),
          )
      val firstLayoutState =
          NestedLithoTree.layout(
              result = resolveResult,
              sizeConstraints = firstMeasureConstraints,
              current = nestedTreeState.currentLayoutState,
              version = layoutVersion,
          )
      capturedVersions.add(firstLayoutState.version)

      // Second measure with exact constraints (simulating EXACTLY measure after knowing size)
      val secondMeasureConstraints =
          SizeConstraints.fromMeasureSpecs(
              makeSizeSpec(sizeConstraints.maxWidth, SizeSpec.EXACTLY),
              makeSizeSpec(sizeConstraints.maxHeight, SizeSpec.EXACTLY),
          )
      val secondLayoutState =
          NestedLithoTree.layout(
              result = resolveResult,
              sizeConstraints = secondMeasureConstraints,
              current = firstLayoutState, // Pass first layout as current
              version = layoutVersion, // Same parent version
          )
      capturedVersions.add(secondLayoutState.version)

      // Commit the final (second) layout state
      nestedTreeState.commitAndRunEffects(secondLayoutState)

      return PrimitiveLayoutResult(
          width =
              secondLayoutState.width.coerceIn(sizeConstraints.minWidth, sizeConstraints.maxWidth),
          height =
              secondLayoutState.height.coerceIn(
                  sizeConstraints.minHeight,
                  sizeConstraints.maxHeight,
              ),
          layoutData = secondLayoutState,
      )
    }
  }

  /** Test that cleanup releases tree state properly */
  @Test
  fun cleanup_releasesTreeState() {
    val state = NestedLithoTreeState(treeState = TreeState())

    assertThat(state.treeState).isNotNull()

    state.cleanup()

    assertThat(state.treeState).isNull()
    assertThat(state.currentResolveResult).isNull()
    assertThat(state.currentLayoutState).isNull()
  }

  /** Test that commit rejects stale layout state with lower version */
  @Test
  fun commit_rejectsStaleLayoutState() {
    val nestedState = NestedLithoTreeState(treeState = TreeState())
    val context = ComponentTree.create(lithoTestRule.context).build().context
    val treeId = nestedState.id
    val sizeConstraints =
        SizeConstraints.fromMeasureSpecs(
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
        )

    // Create and commit layout state with version 2 first
    val result2 =
        NestedLithoTree.resolve(
            id = treeId,
            context = context,
            root = EmptyComponent(),
            treeProps = null,
            state = TreeState(),
            current = null,
            version = 2,
        )
    val layoutState2 =
        NestedLithoTree.layout(
            result = result2,
            sizeConstraints = sizeConstraints,
            current = null,
            version = 2,
        )

    nestedState.commitAndRunEffects(layoutState2)
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(2)
    assertThat(nestedState.currentResolveResult?.version).isEqualTo(2)

    // Create layout state with version 1 (stale)
    val result1 =
        NestedLithoTree.resolve(
            id = treeId,
            context = context,
            root = EmptyComponent(),
            treeProps = null,
            state = TreeState(),
            current = null,
            version = 1,
        )
    val layoutState1 =
        NestedLithoTree.layout(
            result = result1,
            sizeConstraints = sizeConstraints,
            current = null,
            version = 1,
        )

    // Attempt to commit version 1 - should be rejected
    nestedState.commitAndRunEffects(layoutState1)

    // Verify version 2 is still committed (version 1 was rejected)
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(2)
    assertThat(nestedState.currentResolveResult?.version).isEqualTo(2)
  }

  @Test
  fun commit_rejectsStaleLayoutWithoutMutatingEffects_andCleansUpBeforeCommit() {
    val nestedState = NestedLithoTreeState(treeState = TreeState(effectsHandler = EffectHandler()))
    val context = ComponentTree.create(lithoTestRule.context).build().context
    val treeId = nestedState.id
    val steps = mutableListOf<String>()
    val cleanupObservedLayoutVersion = AtomicInteger(-1)
    val sizeConstraints =
        SizeConstraints.fromMeasureSpecs(
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
        )

    fun createLayoutState(version: Int, root: Component): LayoutState {
      val result =
          NestedLithoTree.resolve(
              id = treeId,
              context = context,
              root = root,
              treeProps = null,
              state = nestedState.getUpdatedState(),
              current = nestedState.currentResolveResult,
              version = version,
          )
      return NestedLithoTree.layout(
          result = result,
          sizeConstraints = sizeConstraints,
          current = nestedState.currentLayoutState,
          version = version,
      )
    }

    val layoutState2 = createLayoutState(
        version = 2,
        root =
            NestedTreeEffectTestComponent(
                name = "v2",
                steps = steps,
                onEffectCleanup = {
                  cleanupObservedLayoutVersion.set(nestedState.currentLayoutState?.version ?: -1)
                },
            ),
    )
    assertThat(nestedState.commitAndRunEffects(layoutState2)).isTrue
    assertThat(steps).containsExactly("attach:v2")

    val staleLayoutState1 = createLayoutState(
        version = 1,
        root = NestedTreeEffectTestComponent(name = "stale-v1", steps = steps),
    )
    assertThat(nestedState.commitAndRunEffects(staleLayoutState1)).isFalse
    assertThat(steps).containsExactly("attach:v2")
    assertThat(cleanupObservedLayoutVersion.get()).isEqualTo(-1)

    val layoutState3 = createLayoutState(
        version = 3,
        root = NestedTreeEffectTestComponent(name = "v3", steps = steps),
    )
    assertThat(nestedState.commitAndRunEffects(layoutState3)).isTrue

    assertThat(steps).containsExactly("attach:v2", "detach:v2", "attach:v3")
    assertThat(cleanupObservedLayoutVersion.get()).isEqualTo(2)
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(3)
  }

  /** Test that commit accepts newer layout state with higher version */
  @Test
  fun commit_acceptsNewerLayoutState() {
    val nestedState = NestedLithoTreeState(treeState = TreeState())
    val context = ComponentTree.create(lithoTestRule.context).build().context
    val treeId = nestedState.id
    val sizeConstraints =
        SizeConstraints.fromMeasureSpecs(
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
        )

    // Create and commit layout state with version 1 first
    val result1 =
        NestedLithoTree.resolve(
            id = treeId,
            context = context,
            root = EmptyComponent(),
            treeProps = null,
            state = TreeState(),
            current = null,
            version = 1,
        )
    val layoutState1 =
        NestedLithoTree.layout(
            result = result1,
            sizeConstraints = sizeConstraints,
            current = null,
            version = 1,
        )

    nestedState.commitAndRunEffects(layoutState1)
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(1)
    assertThat(nestedState.currentResolveResult?.version).isEqualTo(1)

    // Create layout state with version 2 (newer)
    val result2 =
        NestedLithoTree.resolve(
            id = treeId,
            context = context,
            root = EmptyComponent(),
            treeProps = null,
            state = TreeState(),
            current = null,
            version = 2,
        )
    val layoutState2 =
        NestedLithoTree.layout(
            result = result2,
            sizeConstraints = sizeConstraints,
            current = null,
            version = 2,
        )

    // Commit version 2 - should be accepted
    nestedState.commitAndRunEffects(layoutState2)

    // Verify version 2 is now committed
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(2)
    assertThat(nestedState.currentResolveResult?.version).isEqualTo(2)
  }

  /** Test that commit accepts newer layout with same (cached) resolve version */
  @Test
  fun commit_acceptsNewerLayoutWithCachedResolve() {
    val nestedState = NestedLithoTreeState(treeState = TreeState())
    val context = ComponentTree.create(lithoTestRule.context).build().context
    val treeId = nestedState.id

    // Create and commit initial state with resolve v1, layout v1
    val result1 =
        NestedLithoTree.resolve(
            id = treeId,
            context = context,
            root = EmptyComponent(),
            treeProps = null,
            state = TreeState(),
            current = null,
            version = 1,
        )
    val layoutState1 =
        NestedLithoTree.layout(
            result = result1,
            sizeConstraints =
                SizeConstraints.fromMeasureSpecs(
                    makeSizeSpec(100, SizeSpec.EXACTLY),
                    makeSizeSpec(100, SizeSpec.EXACTLY),
                ),
            current = null,
            version = 1,
        )

    nestedState.commitAndRunEffects(layoutState1)
    assertThat(nestedState.currentResolveResult?.version).isEqualTo(1)
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(1)

    // Create new layout v2 with SAME resolve result (simulating cached resolve)
    val layoutState2 =
        NestedLithoTree.layout(
            result = result1, // Same resolve result (cached)
            sizeConstraints =
                SizeConstraints.fromMeasureSpecs(
                    makeSizeSpec(200, SizeSpec.EXACTLY),
                    makeSizeSpec(200, SizeSpec.EXACTLY),
                ),
            current = null,
            version = 2,
        )

    // Commit should succeed even though resolve version is same
    nestedState.commitAndRunEffects(layoutState2)

    // Verify layout v2 is committed (resolve stays at v1 since it's the same result)
    assertThat(nestedState.currentResolveResult?.version).isEqualTo(1)
    assertThat(nestedState.currentLayoutState?.version).isEqualTo(2)
  }
}

private class NestedTreeEffectTestComponent(
    private val name: String,
    private val steps: MutableList<String>,
    private val onEffectCleanup: () -> Unit = {},
) : KComponent() {
  override fun ComponentScope.render(): Component {
    useEffect(name) {
      steps.add("attach:$name")
      onCleanup {
        steps.add("detach:$name")
        onEffectCleanup()
      }
    }
    return EmptyComponent()
  }
}

/** Test component that uses useNestedTree and exposes the NestedLithoTreeState for verification. */
private class NestedTreeTestComponent(
    private val nestedStateRef: AtomicReference<NestedLithoTreeState>,
    private val capturedParentVersionRef: AtomicInteger,
    private val child: ComponentScope.() -> Component,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val (nestedTreeState, resolveResult) = useNestedTree(root = child())

    // Store the nested tree state for test verification
    nestedStateRef.set(nestedTreeState)

    return LithoPrimitive(
        Primitive(
            layoutBehavior =
                NestedTreeTestLayoutBehavior(
                    nestedTreeState,
                    resolveResult,
                    capturedParentVersionRef,
                ),
            mountBehavior = MountBehavior(ViewAllocator { context -> View(context) }) {},
        ),
        style = null,
    )
  }

  private class NestedTreeTestLayoutBehavior(
      private val nestedTreeState: NestedLithoTreeState,
      private val resolveResult: ResolveResult,
      private val capturedParentVersionRef: AtomicInteger,
  ) : LayoutBehavior {
    override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
      // Capture the parent layout version for test verification
      capturedParentVersionRef.set(layoutVersion)

      val layoutState =
          NestedLithoTree.layout(
              result = resolveResult,
              sizeConstraints = sizeConstraints,
              current = nestedTreeState.currentLayoutState,
              version = layoutVersion,
          )

      nestedTreeState.commitAndRunEffects(layoutState)

      return PrimitiveLayoutResult(
          width = layoutState.width.coerceIn(sizeConstraints.minWidth, sizeConstraints.maxWidth),
          height =
              layoutState.height.coerceIn(sizeConstraints.minHeight, sizeConstraints.maxHeight),
          layoutData = layoutState,
      )
    }
  }

  private class RootComponentWithTreeProps {
    class A
  }

  private class NestedTreeParentComponent {
    class B
  }

  private class NestedTreeChildComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val a = getTreeProp<RootComponentWithTreeProps.A>()
      val b = getTreeProp<NestedTreeParentComponent.B>()
      val c = getTreeProp<NestedTreeComponent.C>()
      checkNotNull(a) { "A is null" }
      checkNotNull(b) { "B is null" }
      checkNotNull(c) { "C is null" }
      return key("Column") {
        Column { child(key("CardClip") { CardClip(style = Style.width(10.dp).height(10.dp)) }) }
      }
    }
  }

  private class NestedTreeComponent(private val style: Style = Style) : KComponent() {
    override fun ComponentScope.render(): Component {
      return RenderWithConstraints(style) { constraints ->
        val props = getTreeProp<ExtraProps>()
        props?.steps?.add(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
        val style = props?.direction?.let { Style.layoutDirection(it) }.orDefault()
        val row =
            key("Row") {
              Row(style = style) {
                child(key("NestedTreeChildComponent") { NestedTreeChildComponent() })
              }
            }
        TreePropProvider(legacyTreePropOf<C>() to C()) { row }
      }
    }

    class C

    class ExtraProps {
      @JvmField var steps: MutableList<LifecycleStep>? = null
      @JvmField var direction: YogaDirection? = null
    }
  }
}
