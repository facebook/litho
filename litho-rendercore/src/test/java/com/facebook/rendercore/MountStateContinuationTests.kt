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

package com.facebook.rendercore

import android.content.Context
import android.view.View
import androidx.collection.ScatterMap
import androidx.collection.mutableScatterMapOf
import com.facebook.rendercore.Reducer.getReducedTree
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.RenderCoreTestRule
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.TestHostView
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.utils.MeasureSpecUtils
import com.facebook.rendercore.utils.areObjectsEquivalent
import java.util.concurrent.atomic.AtomicLong
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MountStateContinuationTests {
  @Rule @JvmField val renderCoreTestRule: RenderCoreTestRule = RenderCoreTestRule()
  @Rule @JvmField val expectedException: ExpectedException = ExpectedException.none()

  @Test
  fun `mount continuation can access context values`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)

    val continuationValues = mutableScatterMapOf<Any, Any?>("key" to "value")

    var capturedValue: String? = null

    val renderUnit =
        createViewRenderUnit<View> { _ ->
          capturedValue = useContinuation { get<String>("key") }
          return@createViewRenderUnit UnbindFunction {}
        }

    leaf.setRenderUnit(renderUnit)

    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c, MapContinuationValueReader(continuationValues))

    mountState.mount(renderTree)

    assertThat(capturedValue).isEqualTo("value")
  }

  @Test
  fun `continuation context values are correctly scoped across trees`() {
    val c: Context = RuntimeEnvironment.application

    val parentValues: ScatterMap<Any, Any?> = mutableScatterMapOf("p_key" to "p_value")
    val childValues: ScatterMap<Any, Any?> = mutableScatterMapOf("c_key" to "c_value")

    val childReader = MapContinuationValueReader(childValues)
    val hostAllocator =
        object : ContentAllocator<RenderTreeHostView> {
          override fun createContent(context: Context): RenderTreeHostView =
              TestRenderTreeHostView(context, childReader)

          override val renderType: RenderType = RenderType.VIEW
        }

    var parentValue: String? = null
    var childValueReadInParent: String? = null
    val parentRenderUnit =
        createRenderUnitWithLayoutData<RenderTreeHostView>(contentAllocator = hostAllocator) {
            content,
            layoutData ->
          parentValue = useContinuation { get<String>("p_key") }
          childValueReadInParent = useContinuation { get<String>("c_key") }
          val nestedRenderTree = layoutData as? RenderTree
          if (nestedRenderTree != null) {
            content.setMountInput(MountInput(nestedRenderTree, ContinuationValues.EMPTY))
          }
          UnbindFunction { content.setMountInput(null) }
        }

    var childValue: String? = null
    var parentValReadInChild: String? = null
    val childRenderUnit =
        createViewRenderUnit<View> { content ->
          childValue = useContinuation { get<String>("c_key") }
          parentValReadInChild = useContinuation { get<String>("p_key") }
          return@createViewRenderUnit UnbindFunction {}
        }

    val childLayoutTree =
        SimpleLayoutResult.create().width(200).height(200).renderUnit(childRenderUnit).build()
    val childRenderTree = createRenderTree(c, LayoutResultWrappingNode(childLayoutTree))

    val parentLayoutTree =
        SimpleLayoutResult.create()
            .width(200)
            .renderUnit(parentRenderUnit)
            .layoutData(childRenderTree)
            .build()

    val renderTree = createRenderTree(c, LayoutResultWrappingNode(parentLayoutTree))
    val mountState = createMountState(c, MapContinuationValueReader(parentValues))

    mountState.mount(renderTree)

    // expect that the continuation values were read correctly even with nested trees
    assertThat(parentValue).isEqualTo("p_value")
    assertThat(childValue).isEqualTo("c_value")
    // expect that continuation values are scoped to the tree they were meant for
    assertThat(childValueReadInParent).isNull()
    assertThat(parentValReadInChild).isNull()
  }

  @Test
  fun `when multiple continuations are registered, they are executed in order`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)

    val continuationValues =
        mutableScatterMapOf<Any, Any?>("first_key" to 42, "second_key" to "value")

    // Track execution of continuations
    var firstContinuationResult: Int? = null
    var secondContinuationResult: String? = null
    var firstExecutionCount = 0
    var secondExecutionCount = 0

    val renderUnit =
        createRenderUnitWithLayoutData<View> { content, layoutData ->
          firstContinuationResult = useContinuation {
            firstExecutionCount++
            get<Int>("first_key")
          }
          secondContinuationResult = useContinuation {
            secondExecutionCount++
            get<String>("second_key")
          }
          UnbindFunction {}
        }

    leaf.setRenderUnit(renderUnit)

    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c, MapContinuationValueReader(continuationValues))

    mountState.mount(renderTree)

    assertThat(firstExecutionCount).describedAs("First continuation execution count").isEqualTo(1)
    assertThat(secondExecutionCount).describedAs("Second continuation execution count").isEqualTo(1)

    assertThat(firstContinuationResult).describedAs("First continuation result").isEqualTo(42)
    assertThat(secondContinuationResult)
        .describedAs("Second continuation result")
        .isEqualTo("value")
  }

  @Test
  fun `when a render unit is updated, its skipped binders re-run any of their continuations`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)

    val continuationValues =
        mutableScatterMapOf<Any, Any?>("first_key" to 42, "second_key" to "value")

    var executionCount = 0
    var bindExecution = 0
    val renderUnit =
        createViewRenderUnit<View> { content ->
          useContinuation { executionCount++ }
          bindExecution += 1
          return@createViewRenderUnit UnbindFunction {}
        }

    // create a simple tree with a render unit and layout data
    var tree = SimpleLayoutResult.create().width(200).renderUnit(renderUnit).layoutData(1).build()

    var renderTree = createRenderTree(c, LayoutResultWrappingNode(tree))
    val mountState = createMountState(c, MapContinuationValueReader(continuationValues))

    mountState.mount(renderTree)

    // expect that the continuation runs the first time and the RU is bound
    assertThat(executionCount).isEqualTo(1)
    assertThat(bindExecution).isEqualTo(1)

    // now update the layout data for the tree such that the RU will be updated but the binder will
    // not be rebound
    tree = SimpleLayoutResult.create().width(200).renderUnit(renderUnit).layoutData(2).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(tree))
    mountState.mount(renderTree)

    // expect that the continuation is executed again but the binder does not actually rebind
    assertThat(executionCount).isEqualTo(2)
    assertThat(bindExecution).isEqualTo(1)
  }

  @Test
  fun `when render unit rebinds, its old continuations are dropped and any of its new continuations are executed`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)

    data class Count(val value: Int)

    var bindCount = 0
    var unbindCount = 0

    val count1 = Count(1)
    var continuationValue = -1
    var renderUnit =
        createViewRenderUnit<View>(count1) { content ->
          useContinuation { continuationValue = count1.value }
          bindCount += 1
          return@createViewRenderUnit UnbindFunction { unbindCount += 1 }
        }

    // create a simple tree with the render unit and layout data
    var tree = SimpleLayoutResult.create().width(200).renderUnit(renderUnit).build()
    var renderTree = createRenderTree(c, LayoutResultWrappingNode(tree))
    val mountState = createMountState(c)

    mountState.mount(renderTree)

    // expect the continuation to be executed and the binder to have run
    assertThat(continuationValue).isEqualTo(1)
    assertThat(bindCount).isEqualTo(1)
    assertThat(unbindCount).isEqualTo(0)

    val count2 = Count(2)
    renderUnit =
        createViewRenderUnit<View>(count2) { content ->
          useContinuation { continuationValue = count2.value }
          bindCount += 1
          return@createViewRenderUnit UnbindFunction { unbindCount += 1 }
        }

    // now update the tree with a new render unit that binds to a different generation of the count
    // model
    tree = SimpleLayoutResult.create().width(200).renderUnit(renderUnit).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(tree))
    mountState.mount(renderTree)

    // expect the binder to rebind and the new continuation to run
    assertThat(unbindCount).isEqualTo(1)
    assertThat(bindCount).isEqualTo(2)
    assertThat(continuationValue).isEqualTo(2)
  }

  @Test
  fun `mount state correctly keeps track of render units that have continuations`() {
    val c: Context = RuntimeEnvironment.application

    var continuation1Executed = false
    var continuation2Executed = false
    val ru1 =
        createRenderUnitWithLayoutData<View> { content, layoutData ->
          val count = layoutData as Int
          if (count % 2 == 0) {
            useContinuation { continuation1Executed = true }
          }
          UnbindFunction {}
        }

    val ru2 =
        createRenderUnitWithLayoutData<View> { content, layoutData ->
          val count = layoutData as Int
          if (count % 2 == 1) {
            useContinuation { continuation2Executed = true }
          }
          UnbindFunction {}
        }

    var layoutData = 0
    var child1 = SimpleLayoutResult.create().renderUnit(ru1).layoutData(layoutData).build()
    var child2 = SimpleLayoutResult.create().renderUnit(ru2).layoutData(layoutData).build()
    var root = SimpleLayoutResult.create().width(200).child(child1).child(child2).build()

    var renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)
    mountState.mount(renderTree)

    // expect RU1's continuation to be executed and expect mount state to be tracking its render
    // unit for restart
    assertThat(continuation1Executed).isTrue
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue
    // expect RU2 to not be executed and mount state to not be tracking it for restarts
    assertThat(continuation2Executed).isFalse
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isFalse

    // Now update the layout data and remount the tree
    layoutData = 1
    continuation1Executed = false
    continuation2Executed = false

    child1 = SimpleLayoutResult.create().renderUnit(ru1).layoutData(layoutData).build()
    child2 = SimpleLayoutResult.create().renderUnit(ru2).layoutData(layoutData).build()
    root = SimpleLayoutResult.create().width(200).child(child1).child(child2).build()

    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)

    // expect RU1's continuation to not be executed and mount state to no longer be tracking it for
    // restart
    assertThat(continuation1Executed).isFalse
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isFalse
    // expect RU2's continuation to be executed and mount state to be tracking it for restart
    assertThat(continuation2Executed).isTrue
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isTrue

    // Remove child 2 and expect RU2 to no longer be tracked for restarts
    continuation2Executed = false
    // update layout data so that we're now tracking RU1
    layoutData = 2
    child1 = SimpleLayoutResult.create().renderUnit(ru1).layoutData(layoutData).build()
    root = SimpleLayoutResult.create().width(200).child(child1).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)

    assertThat(continuation2Executed).isFalse
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isFalse
    // expect it to be tracking RU1
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue

    // Add a new child with an RU that has continuations and expect mount state to be tracking it
    val ru3 =
        createViewRenderUnit<View> { content ->
          useContinuation { /* noop */ }
          UnbindFunction {}
        }
    assertThat(mountState.restartableMountItems.contains(ru3.id)).isFalse
    val child3 = SimpleLayoutResult.create().width(200).renderUnit(ru3).build()
    root = SimpleLayoutResult.create().width(200).child(child3).child(child1).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)

    // expect mount state to still be tracking RU1
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue
    // expect mount state to now be tracking RU3
    assertThat(mountState.restartableMountItems.contains(ru3.id)).isTrue
  }

  @Test
  fun `when render unit is not updated, continuations are re-executed`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)

    var continuationExecutionCount = 0
    var bindCount = 0
    var unbindCount = 0

    val ru1 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuationExecutionCount++ }
          UnbindFunction {}
        }

    val ru2 =
        createRenderUnitWithLayoutData<View> { content, layoutData ->
          bindCount++
          UnbindFunction { unbindCount++ }
        }

    val createTree: (layoutData: Int) -> RenderTree = { layoutData ->
      val root =
          SimpleLayoutResult.create()
              .width(200)
              .child(SimpleLayoutResult.create().renderUnit(ru1).build())
              .child(SimpleLayoutResult.create().renderUnit(ru2).layoutData(layoutData).build())
              .build()
      createRenderTree(c, LayoutResultWrappingNode(root))
    }

    // First mount with initial layout data
    var renderTree = createTree(0)
    val mountState = createMountState(c)

    mountState.mount(renderTree)

    // Verify initial bind and continuation execution
    assertThat(continuationExecutionCount).isEqualTo(1)
    assertThat(bindCount).isEqualTo(1)
    assertThat(unbindCount).isEqualTo(0)

    // Update with different layout data - RU1 should re-execute its continuations but RU2 should
    // rebind
    renderTree = createTree(1)
    mountState.mount(renderTree)

    assertThat(continuationExecutionCount).isEqualTo(2)
    assertThat(bindCount).isEqualTo(2)
    assertThat(unbindCount).isEqualTo(1)
  }

  @Test
  fun `when mount skips, all continuations in render tree are re-executed`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf1 = TestNode(0, 0, 10, 10)
    val leaf2 = TestNode(10, 0, 10, 10)
    val leaf3 = TestNode(20, 0, 10, 10)
    root.addChild(leaf1)
    root.addChild(leaf2)
    root.addChild(leaf3)

    var continuation1Count = 0
    var continuation2Count = 0
    var bind3Count = 0
    var unbind3Count = 0

    val renderUnit1 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuation1Count++ }
          UnbindFunction {}
        }

    val renderUnit2 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuation2Count++ }
          UnbindFunction {}
        }

    val renderUnit3 =
        createViewRenderUnit<View> { content ->
          bind3Count++
          UnbindFunction { unbind3Count++ }
        }

    // Set up tree with layout data
    val tree1 = SimpleLayoutResult.create().renderUnit(renderUnit1).build()
    val tree2 = SimpleLayoutResult.create().renderUnit(renderUnit2).build()
    val tree3 = SimpleLayoutResult.create().renderUnit(renderUnit3).build()

    val parentTree =
        SimpleLayoutResult.create()
            .width(600)
            .height(200)
            .child(tree1)
            .child(tree2)
            .child(tree3)
            .build()

    val renderTree = createRenderTree(c, LayoutResultWrappingNode(parentTree))
    val mountState = createMountState(c)

    // Initial mount
    mountState.mount(renderTree)

    assertThat(continuation1Count).isEqualTo(1)
    assertThat(continuation2Count).isEqualTo(1)
    assertThat(bind3Count).isEqualTo(1)
    assertThat(unbind3Count).isEqualTo(0)

    // mount the same render tree and expect mount state to restart continuations for RU1 and RU2
    // but RU3 should not rebind
    mountState.mount(renderTree)

    // All continuations should execute again even though binders skip
    assertThat(continuation1Count).isEqualTo(2)
    assertThat(continuation2Count).isEqualTo(2)
    assertThat(bind3Count).isEqualTo(1)
    assertThat(unbind3Count).isEqualTo(0)
  }

  @Test
  fun `when mount skips, continuations only restart for changed continuation value versions`() {
    val c: Context = RuntimeEnvironment.application
    val values = mutableScatterMapOf<Any, Any?>("items" to listOf("first"))
    val versions = mutableScatterMapOf<Any, Long>("items" to 0L)

    var continuationExecutionCount = 0
    var appliedItems: List<String>? = null
    val renderUnit =
        createViewRenderUnit<View> {
          useContinuation {
            continuationExecutionCount++
            appliedItems = get<List<String>>("items")
          }
          UnbindFunction {}
        }

    val root = TestNode()
    root.addChild(TestNode(0, 0, 10, 10).also { it.setRenderUnit(renderUnit) })
    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c, VersionedMapContinuationValueReader(values, versions))

    mountState.mount(renderTree)
    assertThat(continuationExecutionCount).isEqualTo(1)
    assertThat(appliedItems).isEqualTo(listOf("first"))

    mountState.mount(renderTree)
    assertThat(continuationExecutionCount).isEqualTo(1)

    values["items"] = listOf("second")
    versions["items"] = 1L
    mountState.mount(renderTree)

    assertThat(continuationExecutionCount).isEqualTo(2)
    assertThat(appliedItems).isEqualTo(listOf("second"))
  }

  @Test
  fun `continuation restart dirtiness ignores keys that are no longer read`() {
    val c: Context = RuntimeEnvironment.application
    val values = mutableScatterMapOf<Any, Any?>("first" to "one", "second" to "two")
    val versions = mutableScatterMapOf<Any, Long>("first" to 0L, "second" to 0L)

    var readFirst = true
    var continuationExecutionCount = 0
    val renderUnit =
        createViewRenderUnit<View> {
          useContinuation {
            continuationExecutionCount++
            if (readFirst) {
              get<String>("first")
            } else {
              get<String>("second")
            }
          }
          UnbindFunction {}
        }

    val root = TestNode()
    root.addChild(TestNode(0, 0, 10, 10).also { it.setRenderUnit(renderUnit) })
    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c, VersionedMapContinuationValueReader(values, versions))

    mountState.mount(renderTree)
    assertThat(continuationExecutionCount).isEqualTo(1)

    readFirst = false
    values["first"] = "one-updated"
    versions["first"] = 1L
    mountState.mount(renderTree)
    assertThat(continuationExecutionCount).isEqualTo(2)

    values["first"] = "one-updated-again"
    versions["first"] = 2L
    mountState.mount(renderTree)
    assertThat(continuationExecutionCount).isEqualTo(2)
  }

  @Test
  fun `mount item correctly reports if it has continuations or not`() {
    val c: Context = RuntimeEnvironment.application
    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)

    // Create a render unit with a continuation
    val ruWithBinderContinuation =
        createRenderUnitWithLayoutData<View> { content, layoutData ->
          if (layoutData == 1) {
            useContinuation { /* noop */ }
          }
          UnbindFunction {}
        }

    leaf.setRenderUnit(ruWithBinderContinuation)
    leaf.setLayoutData(1)

    val renderTree = createRenderTree(c, root)
    val mountState = createMountState(c)

    mountState.mount(renderTree)

    // Get the mount item for the render unit
    val mountItem = mountState.getMountItem(ruWithBinderContinuation.id)
    check(mountItem != null)

    // Verify mount item has continuations
    assertThat(mountItem.hasAnyContinuations()).isTrue
    // Also verify mount state has continuations
    assertThat(mountState.restartableMountItems.contains(ruWithBinderContinuation.id)).isTrue

    // change the leaf's layout data so that it no longer has a continuation
    leaf.setLayoutData(2)
    val renderTree2 = createRenderTree(c, root)
    mountState.mount(renderTree2)

    // Now the mount item should not have continuations
    assertThat(mountItem.hasAnyContinuations()).isFalse
    // Also verify mount state no longer has continuations
    assertThat(mountState.restartableMountItems.contains(ruWithBinderContinuation.id)).isFalse

    // Now let's test a mount item with host continuations
    // First, create a leaf with a render unit that mounts a render tree
    val ruWithRenderTree = createRenderUnitWithLayoutData<TestRenderTreeHostView>(
        contentAllocator = RenderTreeHostViewAllocator,
    ) { content, layoutData ->
      val renderTree = layoutData as RenderTree
      content.setRenderTree(renderTree)
      UnbindFunction {}
    }
    val leaf2 =
        TestNode(0, 0, 10, 10).apply {
          setRenderUnit(ruWithRenderTree)

          val nestedTree =
              TestNode().apply {
                val ru =
                    createViewRenderUnit<View> { content ->
                      useContinuation { /* noop */ }
                      UnbindFunction {}
                    }
                setRenderUnit(ru)
              }
          setLayoutData(createRenderTree(c, nestedTree))
        }

    root.addChild(leaf2)

    val renderTree3 = createRenderTree(c, root)
    mountState.mount(renderTree3)

    // Verify that the mount item has continuations
    val mountItem2 = mountState.getMountItem(ruWithRenderTree.id)
    checkNotNull(mountItem2)
    assertThat(mountItem2.hasAnyContinuations()).isTrue
    // Verify that the mount item's render unit has no continuations
    assertThat(mountItem2.renderUnit.hasAnyContinuations()).isFalse

    // Verify that the mount state is tracking the new mount item with continuations
    assertThat(mountState.restartableMountItems.contains(ruWithRenderTree.id)).isTrue
  }

  @Test
  fun `mount state respects host restart policy`() {
    val c: Context = RuntimeEnvironment.application

    class Counter {
      var count = 0

      fun reset() {
        count = 0
      }
    }

    val counter = Counter()
    fun createNodeWithNestedTree(
        restartPolicy: MountStateRestartPolicy,
        useContinuation: Boolean,
    ): Pair<TestNode, RenderUnit<*>> {
      val ruWithRenderTree = createRenderUnitWithLayoutData<TestRenderTreeHostView>(
          contentAllocator = RenderTreeHostViewAllocator,
      ) { content, layoutData ->
        content.mountStateRestartPolicyInternal = restartPolicy
        val renderTree = layoutData as RenderTree
        content.setRenderTree(renderTree)
        UnbindFunction {
          content.setRenderTree(null)
          content.mountStateRestartPolicyInternal = MountStateRestartPolicy.Default
        }
      }
      val node =
          TestNode(0, 0, 10, 10).apply {
            setRenderUnit(ruWithRenderTree)

            val nestedTree =
                TestNode().apply {
                  val ru =
                      createViewRenderUnit<View> { content ->
                        if (useContinuation) {
                          useContinuation { counter.count++ }
                        }
                        UnbindFunction {}
                      }
                  setRenderUnit(ru)
                }
            setLayoutData(createRenderTree(c, nestedTree))
          }
      return Pair(node, ruWithRenderTree)
    }
    val widthSpec = MeasureSpecUtils.exactly(100)
    val heightSpec = MeasureSpecUtils.exactly(100)
    val host = TestRenderTreeHostView(c)
    val mountState = host.mountState
    val mountStateRestartRegistry = MountStateRestartRegistry()

    // Test the default policy, and expect that no host continuations are registered since the
    // nested tree has no continuations nor hasn't specified a host restart policy of "Always"
    val (leaf, ru) =
        createNodeWithNestedTree(
            restartPolicy = MountStateRestartPolicy.Default,
            useContinuation = false,
        )
    var root = TestNode().apply { addChild(leaf) }

    val renderTree = createRenderTree(c, root)

    withHostContinuationRegistry(mountStateRestartRegistry) {
      host.setRenderTree(renderTree)
      host.measure(widthSpec, heightSpec)
      host.layout(0, 0, 100, 100)
    }
    // Verify that with the default policy:
    // - the leaf's mount item has no continuations
    // - and that the root does not register any host continuations
    val mountItem = mountState.getMountItem(ru.id)
    checkNotNull(mountItem)
    assertThat(mountItem.hasAnyContinuations()).isFalse
    assertThat(mountState.restartableMountItems.contains(ru.id)).isFalse
    assertThat(mountStateRestartRegistry.hasAnyMountStates()).isFalse

    // reset
    counter.reset()
    mountStateRestartRegistry.clear()
    host.setRenderTree(null)

    // Test the default policy but with a continuation in the nested tree
    val (leaf2, ru2) =
        createNodeWithNestedTree(
            restartPolicy = MountStateRestartPolicy.Default,
            useContinuation = true,
        )
    root = TestNode().apply { addChild(leaf2) }
    val renderTree2 = createRenderTree(c, root)
    withHostContinuationRegistry(mountStateRestartRegistry) {
      host.setRenderTree(renderTree2)
      host.measure(widthSpec, heightSpec)
      host.layout(0, 0, 100, 100)
    }
    // Verify that with the default policy + continuations:
    // - the leaf's mount item now has continuations
    // - and that the root registers a host continuation
    val mountItem2 = mountState.getMountItem(ru2.id)
    checkNotNull(mountItem2)
    // expect the continuation to have executed
    assertThat(counter.count).isEqualTo(1)
    assertThat(mountItem2.hasAnyContinuations()).isTrue
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isTrue
    assertThat(mountStateRestartRegistry.hasAnyMountStates()).isTrue
    // run the host continuation
    mountStateRestartRegistry.restartMountStates()
    // manually run layout since this is the root
    host.measure(widthSpec, heightSpec)
    host.layout(0, 0, 100, 100)
    // expect the continuation to have executed again
    assertThat(counter.count).isEqualTo(2)

    // reset
    counter.reset()
    mountStateRestartRegistry.clear()
    host.setRenderTree(null)

    // Test the Never policy with a continuation in the nested tree
    val (leaf3, ru3) =
        createNodeWithNestedTree(
            restartPolicy = MountStateRestartPolicy.Never,
            useContinuation = true,
        )
    root = TestNode().apply { addChild(leaf3) }
    val renderTree3 = createRenderTree(c, root)
    withHostContinuationRegistry(mountStateRestartRegistry) {
      host.setRenderTree(renderTree3)
      host.measure(widthSpec, heightSpec)
      host.layout(0, 0, 100, 100)
    }
    // Verify that with the never policy + continuations:
    // - the leaf's mount item has no continuations
    // - the root does not register a host continuation
    // - however, the nested mount state does have mount continuations
    val mountItem3 = mountState.getMountItem(ru3.id)
    checkNotNull(mountItem3)
    // expect the continuation to have executed
    assertThat(counter.count).isEqualTo(1)
    assertThat(mountItem3.hasAnyContinuations()).isFalse
    assertThat(mountState.restartableMountItems.contains(ru3.id)).isFalse
    assertThat(mountStateRestartRegistry.hasAnyMountStates()).isFalse
    // get the nested mount state
    val nestedRenderTreeHost = mountItem3.content as TestRenderTreeHostView
    assertThat(nestedRenderTreeHost.mountState.restartableMountItems.size > 0).isTrue

    // reset
    counter.reset()
    mountStateRestartRegistry.clear()
    host.setRenderTree(null)

    // Test the Always policy with no continuation in the nested tree
    val (leaf4, ru4) =
        createNodeWithNestedTree(
            restartPolicy = MountStateRestartPolicy.Always,
            useContinuation = false,
        )
    root = TestNode().apply { addChild(leaf4) }
    val renderTree4 = createRenderTree(c, root)
    withHostContinuationRegistry(mountStateRestartRegistry) {
      host.setRenderTree(renderTree4)
      host.measure(widthSpec, heightSpec)
      host.layout(0, 0, 100, 100)
    }
    // Verify that with the always policy:
    // - the leaf's mount item has continuations
    // - the root registers a host continuation
    val mountItem4 = mountState.getMountItem(ru4.id)
    checkNotNull(mountItem4)
    assertThat(mountItem4.hasAnyContinuations()).isTrue
    assertThat(mountState.restartableMountItems.contains(ru4.id)).isTrue
    assertThat(mountStateRestartRegistry.hasAnyMountStates()).isTrue
    // run the host continuation
    mountStateRestartRegistry.restartMountStates()
    // expect the root host to have requested layout
    assertThat(host.isLayoutRequested).isTrue
  }

  @Test
  fun `when mount state unmounts all its items, it unregisters its restart continuation`() {
    //        root (host)
    //         |
    //        child (host)
    //         |
    //        leaf [with continuation]

    val c: Context = RuntimeEnvironment.application

    // Create a parent host continuation registry
    val registry = MountStateRestartRegistry()

    // Create a nested render tree with a continuation
    var continuationExecuted = false

    // Create a render unit that hosts the nested tree
    val hostRu = createRenderUnitWithLayoutData<TestRenderTreeHostView>(
        contentAllocator = RenderTreeHostViewAllocator,
    ) { content, layoutData ->
      val renderTree = layoutData as? RenderTree
      content.setRenderTree(renderTree)
      UnbindFunction { content.setRenderTree(null) }
    }

    val hostNode =
        TestNode(0, 0, 100, 100).apply {
          setRenderUnit(hostRu)
          val leafNode =
              TestNode().apply {
                val ru =
                    createViewRenderUnit<View> { content ->
                      useContinuation { continuationExecuted = true }
                      UnbindFunction {}
                    }
                setRenderUnit(ru)
              }
          setLayoutData(createRenderTree(c, leafNode))
        }
    val root = TestNode().apply { addChild(hostNode) }

    var renderTree = createRenderTree(c, root)
    val widthSpec = MeasureSpecUtils.exactly(200)
    val heightSpec = MeasureSpecUtils.exactly(200)
    val parentHost = TestRenderTreeHostView(c)

    // Mount within the parent host continuation registry
    withHostContinuationRegistry(registry) {
      parentHost.setRenderTree(renderTree)
      parentHost.measure(widthSpec, heightSpec)
      parentHost.layout(0, 0, 200, 200)
    }

    // Verify the continuation was executed
    assertThat(continuationExecuted).isTrue

    // Verify the parent registry has host continuations registered
    assertThat(registry.hasAnyMountStates()).isTrue

    // remount the tree without the nested tree
    hostNode.setLayoutData(null)
    renderTree = createRenderTree(c, root)
    withHostContinuationRegistry(registry) {
      parentHost.setRenderTree(renderTree)
      parentHost.measure(widthSpec, heightSpec)
      parentHost.layout(0, 0, 200, 200)
    }

    // Verify the parent registry no longer has the nested mount state registered
    assertThat(registry.hasAnyMountStates()).isFalse
    // Check that the mount item of the nested render unit no longer has host continuations
    val nestedMountItem = parentHost.mountState.getMountItem(hostRu.id)
    checkNotNull(nestedMountItem)
    assertThat(nestedMountItem.hasAnyContinuations()).isFalse
  }

  @Test
  fun `when mount item is removed, mount state correctly reports if it has continuations`() {
    val c: Context = RuntimeEnvironment.application

    // Create two render units with continuations
    var continuation1Executed = false
    var continuation2Executed = false

    val ru1 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuation1Executed = true }
          UnbindFunction {}
        }

    val ru2 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuation2Executed = true }
          UnbindFunction {}
        }

    val child1 = SimpleLayoutResult.create().renderUnit(ru1).build()
    val child2 = SimpleLayoutResult.create().renderUnit(ru2).build()
    var root = SimpleLayoutResult.create().width(200).child(child1).child(child2).build()

    var renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)

    // Mount both render units
    mountState.mount(renderTree)

    // Verify both continuations were executed and both are tracked
    assertThat(continuation1Executed).isTrue
    assertThat(continuation2Executed).isTrue
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isTrue

    // Reset flags
    continuation1Executed = false
    continuation2Executed = false

    // Now unmount ru2 by creating a tree without it
    root = SimpleLayoutResult.create().width(200).child(child1).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)

    // Verify ru2 is no longer tracked for continuations
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isFalse
    // ru1 should still be tracked
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue

    // Verify the mount item for ru2 was cleared properly
    assertThat(mountState.getMountItem(ru2.id)).isNull()

    // Mount the same tree again to verify only ru1's continuation runs
    mountState.mount(renderTree)

    assertThat(continuation1Executed).isTrue
    assertThat(continuation2Executed).isFalse

    // Reset and unmount all
    continuation1Executed = false
    root = SimpleLayoutResult.create().width(200).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)

    // Verify no continuations are tracked
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isFalse
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isFalse

    // Mount again to verify no continuations execute
    mountState.mount(renderTree)
    assertThat(continuation1Executed).isFalse
    assertThat(continuation2Executed).isFalse
  }

  @Test
  fun `when mount items move, mount state correctly reports if it has continuations`() {
    val c: Context = RuntimeEnvironment.application

    // Create two render units with continuations
    var continuation1Executed = false
    var continuation2Executed = false

    val ru1 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuation1Executed = true }
          UnbindFunction {}
        }

    val ru2 =
        createViewRenderUnit<View> { content ->
          useContinuation { continuation2Executed = true }
          UnbindFunction {}
        }

    val hostViewAllocator = ViewAllocator { c -> TestHostView(c) }
    val parent1Ru =
        createViewRenderUnit<TestHostView>(contentAllocator = hostViewAllocator) { content ->
          UnbindFunction {}
        }
    val rootRu =
        createViewRenderUnit<TestHostView>(contentAllocator = hostViewAllocator) { content ->
          UnbindFunction {}
        }

    //       root [H]
    //         |
    //      parent1 [H]
    //      /     \
    //   child1  child2
    val child1 = SimpleLayoutResult.create().renderUnit(ru1).build()
    val child2 = SimpleLayoutResult.create().renderUnit(ru2).build()
    var parent1 =
        SimpleLayoutResult.create()
            .width(200)
            .renderUnit(parent1Ru)
            .child(child1)
            .child(child2)
            .build()

    var root = SimpleLayoutResult.create().width(200).child(parent1).renderUnit(rootRu).build()
    var renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    val mountState = createMountState(c)

    // Mount the root
    mountState.mount(renderTree)
    // Verify both continuations were executed and both render units are tracked
    assertThat(continuation1Executed).isTrue
    assertThat(continuation2Executed).isTrue
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isTrue

    // Reset flags
    continuation1Executed = false
    continuation2Executed = false

    // Now swap the children of parent1
    parent1 =
        SimpleLayoutResult.create()
            .width(200)
            .renderUnit(parent1Ru)
            .child(child2)
            .child(child1)
            .build()
    root = SimpleLayoutResult.create().width(200).child(parent1).renderUnit(rootRu).build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)
    // Verify both continuations were executed and both render units are still tracked
    assertThat(continuation1Executed).isTrue
    assertThat(continuation2Executed).isTrue
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isTrue

    // Reset flags
    continuation1Executed = false
    continuation2Executed = false

    // Move child1 to the root
    parent1 = SimpleLayoutResult.create().width(200).renderUnit(parent1Ru).child(child2).build()
    root =
        SimpleLayoutResult.create()
            .width(200)
            .child(parent1)
            .child(child1)
            .renderUnit(rootRu)
            .build()
    renderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(renderTree)
    // Verify that both continuations were executed and both render units are still tracked
    assertThat(continuation1Executed).isTrue
    assertThat(continuation2Executed).isTrue
    assertThat(mountState.restartableMountItems.contains(ru1.id)).isTrue
    assertThat(mountState.restartableMountItems.contains(ru2.id)).isTrue
  }

  @Test
  fun `deeply nested mount state with binder continuation restarts on ancestors skipping`() {
    val c: Context = RuntimeEnvironment.application

    class Stat(var bindCount: Int, var unbindCount: Int)
    val renderUnitStats = mutableMapOf<BinderId, Stat>()
    val createRenderUnitWithNestedTree: () -> RenderUnit<*> = {
      createRenderUnitWithLayoutData<TestRenderTreeHostView>(
          contentAllocator = RenderTreeHostViewAllocator,
      ) { content, layoutData ->
        val renderTree = layoutData as? RenderTree
        content.setRenderTree(renderTree)
        val stat = renderUnitStats.getOrPut(binderId) { Stat(0, 0) }
        stat.bindCount++
        UnbindFunction {
          stat.unbindCount++
          content.setRenderTree(null)
        }
      }
    }
    // Create a render unit with a continuation
    var execCount = 0

    val ru =
        createViewRenderUnit<View> { content ->
          useContinuation { execCount++ }
          UnbindFunction {}
        }

    //         root
    //          |
    //        grandparent
    //          |
    //       parent
    //          |
    //        child [with continuation]
    val child = SimpleLayoutResult.create().renderUnit(ru).build()
    val childRenderTree = createRenderTree(c, LayoutResultWrappingNode(child))

    val parent =
        SimpleLayoutResult.create()
            .renderUnit(createRenderUnitWithNestedTree())
            .layoutData(childRenderTree)
            .build()
    val parentRenderTree = createRenderTree(c, LayoutResultWrappingNode(parent))

    val grandParent =
        SimpleLayoutResult.create()
            .renderUnit(createRenderUnitWithNestedTree())
            .layoutData(parentRenderTree)
            .build()

    val grandParentRenderTree = createRenderTree(c, LayoutResultWrappingNode(grandParent))

    val root =
        SimpleLayoutResult.create()
            .width(200)
            .renderUnit(createRenderUnitWithNestedTree())
            .layoutData(grandParentRenderTree)
            .build()

    val mountState = createMountState(c)
    val rootRenderTree = createRenderTree(c, LayoutResultWrappingNode(root))
    mountState.mount(rootRenderTree)

    // Verify the continuation was executed
    assertThat(execCount).isEqualTo(1)
    // expect all render unit with nested trees to have been bound
    assertThat(renderUnitStats).hasSize(3)
    assertThat(renderUnitStats.values.all { it.bindCount == 1 && it.unbindCount == 0 }).isTrue
    // Restart mount at the root
    mountState.mount(rootRenderTree)
    // Verify the continuation was executed again
    assertThat(execCount).isEqualTo(2)
    // expect all render unit with nested trees to not be rebound
    assertThat(renderUnitStats.values.all { it.bindCount == 1 && it.unbindCount == 0 }).isTrue
  }
}

private fun createRenderTree(c: Context, root: Node<Any?>): RenderTree {
  val sizeConstraints = SizeConstraints.exact(200, 200)
  val layoutContext = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
  val result = root.calculateLayout(layoutContext, sizeConstraints)
  return getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)
}

private fun createMountState(
    c: Context,
    reader: ContinuationValueReader = ContinuationValueReader { null },
): MountState {
  return MountState(TestRenderTreeHostView(c), continuationValueReader = reader)
}

/** Test [ContinuationValueReader] backed by a mutable [ScatterMap]. */
internal class MapContinuationValueReader(private val values: ScatterMap<Any, Any?>) :
    ContinuationValueReader {
  override fun readValue(key: Any): Any? = values[key]
}

/** Test [ContinuationValueReader] backed by mutable values and versions. */
internal class VersionedMapContinuationValueReader(
    private val values: ScatterMap<Any, Any?>,
    private val versions: ScatterMap<Any, Long>,
) : ContinuationValueReader {
  override fun readValue(key: Any): Any? = values[key]

  override fun readVersion(key: Any): Long = versions[key] ?: UNKNOWN_CONTINUATION_VALUE_VERSION
}

private val idGenerator = AtomicLong(1)

class ViewAllocator<V : View>(
    override val renderType: RenderType = RenderType.VIEW,
    private val allocator: (context: Context) -> V,
) : ContentAllocator<V> {
  override fun createContent(context: Context): V = allocator(context)
}

val RenderTreeHostViewAllocator =
    ViewAllocator<TestRenderTreeHostView> { c -> TestRenderTreeHostView(c) }

class TestRenderTreeHostView(
    c: Context,
    reader: ContinuationValueReader = ContinuationValueReader { null },
) : RenderTreeHostView(c) {
  var mountStateRestartPolicyInternal: MountStateRestartPolicy = MountStateRestartPolicy.Default
  public override val mountState: MountState = MountState(
      this,
      mountStateRestartPolicyProvider = { mountStateRestartPolicyInternal },
      continuationValueReader = reader,
  )

  fun setRenderTree(renderTree: RenderTree?) {
    val mountInput =
        if (renderTree == null) null else MountInput(renderTree, ContinuationValues.EMPTY)
    setMountInput(mountInput)
  }
}

fun interface UnbindFunction {
  fun onUnbind()
}

sealed interface ShouldUpdatePolicy {
  fun shouldUpdate(
      currentModel: Any?,
      newModel: Any?,
      currentLayoutData: Any?,
      nextLayoutData: Any?,
  ): Boolean

  object ModelOnly : ShouldUpdatePolicy {
    override fun shouldUpdate(
        currentModel: Any?,
        newModel: Any?,
        currentLayoutData: Any?,
        nextLayoutData: Any?,
    ): Boolean = !areObjectsEquivalent(currentModel, newModel)
  }

  object ModelAndLayoutData : ShouldUpdatePolicy {
    override fun shouldUpdate(
        currentModel: Any?,
        newModel: Any?,
        currentLayoutData: Any?,
        nextLayoutData: Any?,
    ): Boolean =
        !areObjectsEquivalent(currentModel, newModel) ||
            !areObjectsEquivalent(currentLayoutData, nextLayoutData)
  }
}

private val DefaultContentAllocator: ContentAllocator<View> = ViewAllocator { context ->
  View(context)
}

private fun <V : View> createRenderUnitWithLayoutData(
    model: Any? = Unit,
    contentAllocator: ContentAllocator<V>? = null,
    bind: BinderScope.(content: V, layoutData: Any?) -> UnbindFunction,
): RenderUnit<V> {
  val allocator = contentAllocator ?: DefaultContentAllocator
  @Suppress("UNCHECKED_CAST")
  return createRenderUnit<V, Any?>(
      model,
      contentAllocator = allocator as ContentAllocator<V>,
      bind = { model, content, layoutData -> this.bind(content, layoutData) },
      shouldUpdatePolicy = ShouldUpdatePolicy.ModelAndLayoutData,
  )
}

private fun <V : View> createViewRenderUnit(
    model: Any? = Unit,
    contentAllocator: ContentAllocator<V>? = null,
    bind: BinderScope.(content: View) -> UnbindFunction,
): RenderUnit<V> {
  val allocator = contentAllocator ?: DefaultContentAllocator
  @Suppress("UNCHECKED_CAST")
  return createRenderUnit<V, Any?>(
      model,
      contentAllocator = allocator as ContentAllocator<V>,
      bind = { model, content, layoutData -> this.bind(content) },
      shouldUpdatePolicy = ShouldUpdatePolicy.ModelOnly,
  )
}

private fun <Content : View, Model : Any?> createRenderUnit(
    model: Model,
    contentAllocator: ContentAllocator<Content>,
    bind: BinderScope.(model: Model, content: Content, layoutData: Any?) -> UnbindFunction,
    shouldUpdatePolicy: ShouldUpdatePolicy,
): RenderUnit<Content> {
  val renderUnit =
      object : RenderUnit<Content>() {
        override val id: Long = idGenerator.andIncrement
        override val contentAllocator: ContentAllocator<Content> = contentAllocator
      }

  val binder =
      object : RenderUnit.Binder<Model, Content, Any> {
        override fun shouldUpdate(
            currentModel: Model,
            newModel: Model,
            currentLayoutData: Any?,
            nextLayoutData: Any?,
        ): Boolean =
            shouldUpdatePolicy.shouldUpdate(
                currentModel,
                newModel,
                currentLayoutData,
                nextLayoutData,
            )

        override fun BinderScope.bind(
            context: Context,
            content: Content,
            model: Model,
            layoutData: Any?,
        ): Any? {
          return bind(model, content, layoutData)
        }

        override fun BinderScope.unbind(
            context: Context,
            content: Content,
            model: Model,
            layoutData: Any?,
            bindData: Any?,
        ) {
          val unbindFunction = bindData as? UnbindFunction
          unbindFunction?.onUnbind()
        }
      }
  renderUnit.addOptionalMountBinder(
      DelegateBinder.createDelegateBinder<Model, Content, Any>(model, binder),
  )
  return renderUnit
}
