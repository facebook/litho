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
import androidx.collection.ScatterSet
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.state.StateId
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.visibility.VisibilityOutput
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [LithoYogaLayoutFunction] layout caching behavior, specifically verifying that cached
 * layout results are not reused when a component is dirty.
 *
 * This is a regression test for D90580316. Before the fix, cached layout results were being reused
 * even when a component was dirty (needs update due to state read), which caused node props
 * (including transition values) to not be properly updated.
 */
@RunWith(LithoTestRunner::class)
class LithoYogaLayoutCacheTest {

  private val componentContext =
      ComponentContext(ApplicationProvider.getApplicationContext<Context>())

  /**
   * Tests that when a component is dirty, the cached layout result is NOT used for partial reuse.
   *
   * The test:
   * 1. Creates a node and calculates its layout (populating the cache)
   * 2. Creates a new LayoutCache with the previous layout's cache data
   * 3. Creates a new node with the SAME ID
   * 4. Marks the node as dirty by including its key in scopesToUpdate
   * 5. Calculates layout and verifies the cached layout result is NOT used
   *
   * Without the fix, the cached layout would be reused even though the node is dirty, resulting in
   * incorrect layout dimensions.
   */
  @Test
  fun `dirty component should not use cached layout result`() {
    val resolveContext = componentContext.setRenderStateContextForTests()

    // Create initial node
    val node1 = createNode(resolveContext)
    val componentKey = node1.tailComponentKey

    // Calculate first layout (this populates the cache)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayout(node1, layoutContext1)

    assertThat(layoutResult1).isNotNull

    // Get the cache data from the first layout
    val cacheData = layoutContext1.layoutCache.toCacheData()

    // Create a new node but we'll give it the same ID by cloning the original node and modifying it
    val node2 = node1.clone()
    // Note: clone() preserves the ID, so cache lookup by ID will find the old entry

    // Create layout context with:
    // 1. Cache from previous layout (so cache lookup will succeed)
    // 2. scopesToUpdate containing our component key (so it's marked dirty)
    val layoutCache2 = LayoutCache(cacheData)
    val layoutContext2 = createLayoutContext(
        resolveContext,
        layoutCache2,
        hashSetOf(componentKey), // Mark the component as dirty
    )

    // Calculate layout for the "dirty" node
    val layoutResult2 = calculateLayout(node2, layoutContext2)

    // With the fix: layout should be recalculated, not taken from cache
    // The key verification is that writeNodePropsToYogaNode was called
    assertThat(layoutResult2).isNotNull

    // Verify that the layout was not marked as cached (meaning fresh calculation happened)
    assertThat(requireNotNull(layoutResult2).layoutOutput.isCachedLayout)
        .describedAs("Layout should NOT be from cache when component is dirty")
        .isFalse()
  }

  /**
   * Tests that when a component is NOT dirty, the cached layout result CAN be reused for partial
   * reuse. This is the normal/expected caching behavior.
   */
  @Test
  fun `non-dirty component can use cached layout result`() {
    val resolveContext = componentContext.setRenderStateContextForTests()

    // Create initial node
    val node1 = createNode(resolveContext)

    // Calculate first layout (this populates the cache)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayout(node1, layoutContext1)

    assertThat(layoutResult1).isNotNull

    // Get the cache data from the first layout
    val cacheData = layoutContext1.layoutCache.toCacheData()

    // Clone the node (preserves ID)
    val node2 = node1.clone()

    // Create layout context with cache but WITHOUT marking as dirty
    val layoutCache2 = LayoutCache(cacheData)
    val layoutContext2 = createLayoutContext(
        resolveContext,
        layoutCache2,
        emptySet(), // Not dirty
    )

    // Calculate layout
    val layoutResult2 = calculateLayout(node2, layoutContext2)

    assertThat(layoutResult2).isNotNull

    // When not dirty, the cache CAN be used (partial reuse path)
    assertThat(requireNotNull(layoutResult2).layoutOutput.isCachedLayout)
        .describedAs("Layout can be from cache when component is not dirty")
        .isTrue()
  }

  /**
   * Tests that when a DiffNode tree is provided, cached layout results get their DiffNode set in
   * cloneLayoutResultsRecursively.
   *
   * This verifies the fix where DiffNode was previously not propagated to cached layout results.
   * Cached subtrees (via buildYogaTreeFromCache) now walk the DiffNode tree in parallel and set
   * _diffNode on each cloned result, so measureLithoNode can use the DiffNode's cached specs/sizes.
   */
  @Test
  fun `cached layout result gets DiffNode applied from diff tree`() {
    val resolveContext = componentContext.setRenderStateContextForTests()

    // Pass 1: initial layout — populates the layout cache
    val node = createNode(resolveContext)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayout(node, layoutContext1)
    assertThat(layoutResult1).isNotNull

    // Build a DiffNode tree that mirrors the layout result from pass 1
    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))

    // Pass 2: reuse the same node with cache AND a diff tree
    // The same node identity causes a full cache hit (buildYogaTreeFromCache).
    // With the fix, the DiffNode is set during cloneLayoutResultsRecursively,
    // which should set _diffNode on the cloned result from the diff tree.
    val cacheData = layoutContext1.layoutCache.toCacheData()
    val layoutCache2 = LayoutCache(cacheData)
    val layoutContext2 = createLayoutContext(resolveContext, layoutCache2, emptySet(), diffTree)
    val layoutResult2 = calculateLayout(node, layoutContext2)
    assertThat(layoutResult2).isNotNull

    val result2 = requireNotNull(layoutResult2)

    // Verify the layout was taken from the full cache path
    assertThat(result2.layoutOutput.isCachedLayout)
        .describedAs("Layout should be from cache")
        .isTrue()

    // Verify that DiffNode was applied from the diff tree.
    // Before the fix, this was null because applyDiffNode was not called for cached paths.
    assertThat(result2.diffNode)
        .describedAs("DiffNode should be applied to cached layout result")
        .isNotNull()
  }

  /**
   * Tests that when a parent is dirty but a child is not, the child hits the full cache path
   * (buildYogaTreeFromCache) and still gets its DiffNode applied from the parent's DiffNode.
   *
   * This is the main animation scenario: a parent's state changes every frame, its Yoga tree is
   * rebuilt, but children that haven't changed hit the full cache. The child's DiffNode must be
   * resolved from the parent's already-applied DiffNode via findDiffNode's child lookup path.
   */
  @Test
  fun `child gets DiffNode when parent is dirty but child is cached`() {
    val resolveContext = componentContext.setRenderStateContextForTests()

    // Create a parent with a child
    val node = createNodeWithChild(resolveContext)
    val parentKey = node.tailComponentKey

    // Pass 1: initial layout — populates the cache for both parent and child
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayout(node, layoutContext1)
    assertThat(layoutResult1).isNotNull

    // Build a DiffNode tree from pass 1's result
    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))

    // Pass 2: same node, with cache, diff tree, and PARENT marked dirty.
    // The parent is dirty (in scopesToUpdate) → it goes through the non-cached path.
    // The child is NOT dirty → it should hit the full cache path.
    // The child's DiffNode should be resolved from the parent's DiffNode.
    val cacheData = layoutContext1.layoutCache.toCacheData()
    val layoutCache2 = LayoutCache(cacheData)
    val layoutContext2 = createLayoutContext(
        resolveContext,
        layoutCache2,
        hashSetOf(parentKey),
        diffTree,
    )
    val layoutResult2 = calculateLayout(node, layoutContext2)
    assertThat(layoutResult2).isNotNull

    val result2 = requireNotNull(layoutResult2)

    // The parent should NOT be from cache (it's dirty)
    assertThat(result2.layoutOutput.isCachedLayout)
        .describedAs("Parent should not be from cache when dirty")
        .isFalse()

    // The parent should have its DiffNode applied (via applyDiffNode in non-cached path)
    assertThat(result2.diffNode)
        .describedAs("Parent should have DiffNode from applyDiffNode")
        .isNotNull()

    // The child should be from cache AND have its DiffNode applied
    assertThat(result2.childCount)
        .describedAs("Parent should have at least one child")
        .isGreaterThan(0)

    val childResult = result2.getChildAt(0)
    assertThat(childResult.layoutOutput.isCachedLayout)
        .describedAs("Child should be from cache when not dirty")
        .isTrue()
    assertThat(childResult.diffNode)
        .describedAs("Child should have DiffNode resolved from parent's DiffNode")
        .isNotNull()
  }

  /**
   * Tests end-to-end that a measurable component is NOT re-measured when the DiffNode cache has
   * matching specs. This verifies the full chain: DiffNode is applied to the cached layout result,
   * and measureLithoNode finds the DiffNode and skips onMeasure.
   */
  @Test
  fun `measurable component skips onMeasure when DiffNode specs match`() {
    val resolveContext = componentContext.setRenderStateContextForTests()

    // Create a measurable component (canMeasure = true) nested inside Rows.
    // TestDrawableComponent.create() defaults to canMeasure = true.
    val component =
        Row.create(componentContext)
            .child(
                Row.create(componentContext)
                    .child(
                        TestDrawableComponent.create(componentContext)
                            .measuredWidth(100)
                            .measuredHeight(50),
                    ),
            )
            .build()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))

    // Find the TestDrawableComponent instance from the resolved tree to track onMeasure calls
    val testComponent = findComponent(node, TestDrawableComponent::class.java)

    // Pass 1: initial layout — measures the component
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayout(node, layoutContext1)
    assertThat(layoutResult1).isNotNull

    // Verify that onMeasure WAS called in pass 1
    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs("onMeasure should be called on first layout")
        .isTrue()

    // Build a DiffNode tree from pass 1, populating it with the actual measurement specs/sizes
    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))
    populateDiffTreeMeasurements(diffTree, layoutResult1)

    // Reset the measure flag for pass 2
    testComponent.resetInteractions()

    // Pass 2: same node + cache + diff tree with matching specs
    val cacheData = layoutContext1.layoutCache.toCacheData()
    val layoutCache2 = LayoutCache(cacheData)
    val parentKey = node.tailComponentKey
    val layoutContext2 = createLayoutContext(
        resolveContext,
        layoutCache2,
        hashSetOf(parentKey),
        diffTree,
    )
    val layoutResult2 = calculateLayout(node, layoutContext2)
    assertThat(layoutResult2).isNotNull

    // Verify that onMeasure was NOT called in pass 2 — the DiffNode cache should have matched
    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs("onMeasure should NOT be called when DiffNode specs match")
        .isFalse()
  }

  // -- Spec-compatible measure cache tests --

  /**
   * AT_MOST(500) -> AT_MOST(400) with intrinsic size 100x50.
   *
   * The measured size (100) is strictly less than both old (500) and new (400) constraints, so the
   * component was NOT clamped. With the flag enabled, the cached result should be reused.
   */
  @Test
  fun `spec compatible AT_MOST to AT_MOST with measured size below both constraints skips remeasure`() {
    val resolveContext = componentContext.setRenderStateContextForTests()
    val component =
        Row.create(componentContext)
            .child(
                TestDrawableComponent.create(componentContext)
                    .measuredWidth(100)
                    .measuredHeight(50),
            )
            .build()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
    val testComponent = findComponent(node, TestDrawableComponent::class.java)

    // Pass 1: AT_MOST(500) x AT_MOST(500)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayoutWithSpecs(node, layoutContext1, 500, SizeSpec.AT_MOST)
    assertThat(layoutResult1).isNotNull
    assertThat(testComponent.wasMeasureCalled() as Boolean).isTrue()

    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))
    populateDiffTreeMeasurements(diffTree, layoutResult1)
    testComponent.resetInteractions()

    // Pass 2: AT_MOST(400) — empty cache isolates DiffNode as only cache source
    val layoutContext2 = createLayoutContext(resolveContext, LayoutCache(), emptySet(), diffTree)
    val layoutResult2 = calculateLayoutWithSpecs(node, layoutContext2, 400, SizeSpec.AT_MOST)

    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs("onMeasure should NOT be called when measured size fits both constraints")
        .isFalse()

    // Verify cached dimensions are correct
    val childResult =
        findLayoutResult(requireNotNull(layoutResult2), TestDrawableComponent::class.java)
    assertThat(childResult.contentWidth)
        .describedAs("Cached width should match original measurement")
        .isEqualTo(100)
    assertThat(childResult.contentHeight)
        .describedAs("Cached height should match original measurement")
        .isEqualTo(50)
  }

  /**
   * AT_MOST(100) -> AT_MOST(80) with intrinsic size 100x50.
   *
   * The measured width (100) equals the old constraint (100), so strict < fails. Must remeasure.
   */
  @Test
  fun `spec compatible AT_MOST to AT_MOST with measured size equal to constraint remeasures`() {
    val resolveContext = componentContext.setRenderStateContextForTests()
    val component =
        Row.create(componentContext)
            .child(
                TestDrawableComponent.create(componentContext)
                    .measuredWidth(100)
                    .measuredHeight(50),
            )
            .build()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
    val testComponent = findComponent(node, TestDrawableComponent::class.java)

    // Pass 1: AT_MOST(100) — measured width == constraint
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayoutWithSpecs(node, layoutContext1, 100, SizeSpec.AT_MOST)
    assertThat(layoutResult1).isNotNull
    assertThat(testComponent.wasMeasureCalled() as Boolean).isTrue()

    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))
    populateDiffTreeMeasurements(diffTree, layoutResult1)
    testComponent.resetInteractions()

    // Pass 2: AT_MOST(80)
    val layoutContext2 = createLayoutContext(resolveContext, LayoutCache(), emptySet(), diffTree)
    calculateLayoutWithSpecs(node, layoutContext2, 80, SizeSpec.AT_MOST)

    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs("onMeasure SHOULD be called when measured size equals old constraint")
        .isTrue()
  }

  /**
   * AT_MOST(500) -> EXACTLY(400) with intrinsic size 100x50.
   *
   * Mode change — isSpecCompatible requires same mode. Must remeasure.
   */
  @Test
  fun `spec compatible mode change AT_MOST to EXACTLY always remeasures`() {
    val resolveContext = componentContext.setRenderStateContextForTests()
    val component =
        Row.create(componentContext)
            .child(
                TestDrawableComponent.create(componentContext)
                    .measuredWidth(100)
                    .measuredHeight(50),
            )
            .build()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
    val testComponent = findComponent(node, TestDrawableComponent::class.java)

    // Pass 1: AT_MOST(500)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayoutWithSpecs(node, layoutContext1, 500, SizeSpec.AT_MOST)
    assertThat(layoutResult1).isNotNull
    assertThat(testComponent.wasMeasureCalled() as Boolean).isTrue()

    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))
    populateDiffTreeMeasurements(diffTree, layoutResult1)
    testComponent.resetInteractions()

    // Pass 2: EXACTLY(400) — mode change
    val layoutContext2 = createLayoutContext(resolveContext, LayoutCache(), emptySet(), diffTree)
    calculateLayoutWithSpecs(node, layoutContext2, 400, SizeSpec.EXACTLY)

    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs("onMeasure SHOULD be called when mode changes")
        .isTrue()
  }

  /**
   * AT_MOST(500)xAT_MOST(100) -> AT_MOST(400)xAT_MOST(40) with intrinsic size 100x50.
   *
   * Width: 100 < 500 AND 100 < 400 -> compatible Height: 50 < 100 BUT 50 >= 40 -> NOT compatible
   * (50 is NOT < 40) Since both dimensions must be compatible, this must remeasure.
   */
  @Test
  fun `spec compatible mixed dimensions width compatible height incompatible remeasures`() {
    val resolveContext = componentContext.setRenderStateContextForTests()
    val component =
        Row.create(componentContext)
            .child(
                TestDrawableComponent.create(componentContext)
                    .measuredWidth(100)
                    .measuredHeight(50),
            )
            .build()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
    val testComponent = findComponent(node, TestDrawableComponent::class.java)

    // Pass 1: AT_MOST(500) x AT_MOST(100)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 =
        calculateLayoutWithSpecs(node, layoutContext1, 500, SizeSpec.AT_MOST, 100, SizeSpec.AT_MOST)
    assertThat(layoutResult1).isNotNull
    assertThat(testComponent.wasMeasureCalled() as Boolean).isTrue()

    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))
    populateDiffTreeMeasurements(diffTree, layoutResult1)
    testComponent.resetInteractions()

    // Pass 2: AT_MOST(400) x AT_MOST(40) — width OK, height NOT (50 >= 40)
    val layoutContext2 = createLayoutContext(resolveContext, LayoutCache(), emptySet(), diffTree)
    calculateLayoutWithSpecs(node, layoutContext2, 400, SizeSpec.AT_MOST, 40, SizeSpec.AT_MOST)

    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs(
            "onMeasure SHOULD be called when height is incompatible even if width is compatible",
        )
        .isTrue()
  }

  // NOTE: UNSPECIFIED -> UNSPECIFIED test was intentionally omitted. When both old and new specs
  // are UNSPECIFIED, they are identical, so specsMatchExactly returns true before isSpecCompatible
  // is ever checked. The UNSPECIFIED branch of isSpecCompatible is unreachable in practice.

  /**
   * AT_MOST(400) -> AT_MOST(500) with intrinsic size 100x50.
   *
   * Expanding constraint — measured size (100) is still < both old (400) and new (500). Should skip
   * remeasure.
   */
  @Test
  fun `spec compatible AT_MOST expanding constraint skips remeasure`() {
    val resolveContext = componentContext.setRenderStateContextForTests()
    val component =
        Row.create(componentContext)
            .child(
                TestDrawableComponent.create(componentContext)
                    .measuredWidth(100)
                    .measuredHeight(50),
            )
            .build()
    val node = requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
    val testComponent = findComponent(node, TestDrawableComponent::class.java)

    // Pass 1: AT_MOST(400) x AT_MOST(400)
    val layoutContext1 = createLayoutContext(resolveContext, LayoutCache(), emptySet())
    val layoutResult1 = calculateLayoutWithSpecs(node, layoutContext1, 400, SizeSpec.AT_MOST)
    assertThat(layoutResult1).isNotNull
    assertThat(testComponent.wasMeasureCalled() as Boolean).isTrue()

    val diffTree = buildTestDiffTree(requireNotNull(layoutResult1))
    populateDiffTreeMeasurements(diffTree, layoutResult1)
    testComponent.resetInteractions()

    // Pass 2: AT_MOST(500) — expanding, still compatible
    val layoutContext2 = createLayoutContext(resolveContext, LayoutCache(), emptySet(), diffTree)
    calculateLayoutWithSpecs(node, layoutContext2, 500, SizeSpec.AT_MOST)

    assertThat(testComponent.wasMeasureCalled() as Boolean)
        .describedAs(
            "onMeasure should NOT be called when expanding AT_MOST constraint and measured size still fits",
        )
        .isFalse()
  }

  /** Populates a TestDiffNode tree with measurement specs and sizes from a layout result. */
  private fun populateDiffTreeMeasurements(diffNode: DiffNode, result: LithoLayoutResult) {
    diffNode.lastWidthSpec = result.widthSpec
    diffNode.lastHeightSpec = result.heightSpec
    diffNode.lastMeasuredWidth = result.contentWidth
    diffNode.lastMeasuredHeight = result.contentHeight
    for (i in 0 until minOf(diffNode.childCount, result.childCount)) {
      populateDiffTreeMeasurements(diffNode.getChildAt(i)!!, result.getChildAt(i))
    }
  }

  /** Builds a test DiffNode tree that mirrors the structure of a layout result. */
  private fun buildTestDiffTree(result: LithoLayoutResult): DiffNode {
    val node = result.node
    val scopedInfo = node.tailScopedComponentInfo
    val diffNode = TestDiffNode(scopedInfo.component, scopedInfo.context.globalKey, scopedInfo)
    for (i in 0 until result.childCount) {
      diffNode.addChild(buildTestDiffTree(result.getChildAt(i)))
    }
    return diffNode
  }

  private fun createNode(resolveContext: ResolveContext): LithoNode {
    val component = Row.create(componentContext).build()
    return requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
  }

  private fun createNodeWithChild(resolveContext: ResolveContext): LithoNode {
    val component =
        Row.create(componentContext)
            .child(Row.create(componentContext).child(Row.create(componentContext)))
            .build()
    return requireNotNull(Resolver.resolveTree(resolveContext, componentContext, component))
  }

  /** Finds a component of the given type in the LithoNode tree. */
  private fun <T : Component> findComponent(node: LithoNode, clazz: Class<T>): T {
    if (clazz.isInstance(node.tailComponent)) {
      @Suppress("UNCHECKED_CAST")
      return node.tailComponent as T
    }
    for (i in 0 until node.childCount) {
      try {
        return findComponent(node.getChildAt(i), clazz)
      } catch (_: NoSuchElementException) {
        // continue searching
      }
    }
    throw NoSuchElementException("Component ${clazz.simpleName} not found in tree")
  }

  /** Finds a layout result for a component of the given type in the layout result tree. */
  private fun findLayoutResult(
      result: LithoLayoutResult,
      componentClass: Class<*>,
  ): LithoLayoutResult {
    if (componentClass.isInstance(result.node.tailComponent)) return result
    for (i in 0 until result.childCount) {
      try {
        return findLayoutResult(result.getChildAt(i), componentClass)
      } catch (_: NoSuchElementException) {}
    }
    throw NoSuchElementException("Layout result for ${componentClass.simpleName} not found")
  }

  private fun createLayoutContext(
      resolveContext: ResolveContext,
      layoutCache: LayoutCache,
      scopesToUpdate: Set<GlobalKey>,
      currentDiffTree: DiffNode? = null,
  ): LayoutContext<LithoLayoutContext> {
    val lithoLayoutContext = LithoLayoutContext(
        resolveContext.treeId,
        resolveContext.cache,
        componentContext,
        resolveContext.treeState,
        resolveContext.layoutVersion,
        resolveContext.rootComponentId,
        resolveContext.isAccessibilityEnabled,
        layoutCache,
        currentDiffTree,
        null, // layoutStateFuture
        com.facebook.litho.state.DirtyScopeIndex.of(scopesToUpdate),
    )
    return LayoutContext(componentContext.androidContext, lithoLayoutContext, 0, layoutCache, null)
  }

  private fun calculateLayout(
      node: LithoNode,
      layoutContext: LayoutContext<LithoLayoutContext>,
  ): LithoLayoutResult? {
    return node.calculateLayout(
        layoutContext,
        SizeConstraints.fromMeasureSpecs(widthSpec = unspecified(), heightSpec = unspecified()),
    )
  }

  private fun calculateLayoutWithSpecs(
      node: LithoNode,
      layoutContext: LayoutContext<LithoLayoutContext>,
      size: Int,
      mode: Int,
  ): LithoLayoutResult? {
    return node.calculateLayout(
        layoutContext,
        SizeConstraints.fromMeasureSpecs(
            widthSpec = SizeSpec.makeSizeSpec(size, mode),
            heightSpec = SizeSpec.makeSizeSpec(size, mode),
        ),
    )
  }

  private fun calculateLayoutWithSpecs(
      node: LithoNode,
      layoutContext: LayoutContext<LithoLayoutContext>,
      widthSize: Int,
      widthMode: Int,
      heightSize: Int,
      heightMode: Int,
  ): LithoLayoutResult? {
    return node.calculateLayout(
        layoutContext,
        SizeConstraints.fromMeasureSpecs(
            widthSpec = SizeSpec.makeSizeSpec(widthSize, widthMode),
            heightSpec = SizeSpec.makeSizeSpec(heightSize, heightMode),
        ),
    )
  }
}

/** Minimal [DiffNode] implementation for testing. */
private class TestDiffNode(
    override val component: Component,
    override val componentGlobalKey: GlobalKey,
    override val scopedComponentInfo: ScopedComponentInfo,
) : DiffNode {
  private val _children = mutableListOf<DiffNode>()
  override val childCount: Int
    get() = _children.size

  override val children: List<DiffNode>
    get() = _children

  override fun getChildAt(i: Int): DiffNode = _children[i]

  override fun addChild(node: DiffNode) {
    _children.add(node)
  }

  override var lastMeasuredWidth = 0
  override var lastMeasuredHeight = 0
  override var lastWidthSpec = 0
  override var lastHeightSpec = 0
  override var layoutData: Any? = null
  override var effects: List<Attachable>? = null
  override var stateReads: ScatterSet<StateId>? = null
  override var contentOutput: LithoRenderUnit? = null
  override var backgroundOutput: LithoRenderUnit? = null
  override var foregroundOutput: LithoRenderUnit? = null
  override var borderOutput: LithoRenderUnit? = null
  override var hostOutput: LithoRenderUnit? = null
  override var visibilityOutput: VisibilityOutput? = null
  override var delegate: LayoutResult? = null
  override var primitive: Primitive? = null
}
