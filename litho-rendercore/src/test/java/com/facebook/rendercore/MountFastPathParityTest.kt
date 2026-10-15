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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.collection.longObjectMapOf
import androidx.collection.scatterSetOf
import com.facebook.rendercore.Reducer.getReducedTree
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.RenderUnit.RenderUnitOperationException
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventBus
import com.facebook.rendercore.debug.DebugEventSubscriber
import com.facebook.rendercore.testing.LayoutResultWrappingNode
import com.facebook.rendercore.testing.SimpleLayoutResult
import com.facebook.rendercore.testing.TestHostRenderUnit
import com.facebook.rendercore.testing.TestHostView
import com.facebook.rendercore.testing.TestNode
import com.facebook.rendercore.testing.TestRenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Verifies that the mount fast path ([RenderTreeNode.isFastPathEligible]) produces identical
 * observable behavior to the slow path — no missed binder calls, no extra binder calls, matching
 * mount stats, and identical content state.
 *
 * Each test runs the same mount+update scenario twice — once with the fast path disabled, once
 * enabled — then asserts identical outcomes.
 */
@SuppressLint("MissingTestAnnotation")
@RunWith(RobolectricTestRunner::class)
class MountFastPathParityTest {

  @Before
  fun setUp() {
    RenderCoreConfig.isDebugModeEnabled = true
  }

  @After
  fun tearDown() {
    RenderCoreConfig.isDebugModeEnabled = false
  }

  // region Test 1: Stable item reused identically

  @Test
  fun stableItem_reusedIdentically() {
    data class RunResult(
        val bindCount: Int,
        val unbindCount: Int,
        val reusedCount: Int,
        val updatedCount: Int,
        val contentPreUpdate: Int,
        val contentPostUpdate: Int,
    )

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      val binder = TestBinder<Any>(bindOrder, unbindOrder)
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))

      val contentPreUpdate = System.identityHashCode(mountState.getContentAt(1))
      bindOrder.clear()
      unbindOrder.clear()

      // Update: same RU instance, same layoutData, same bounds
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      val contentPostUpdate = System.identityHashCode(mountState.getContentAt(1))
      val stats = mountState.mountStats
      return RunResult(
          bindCount = bindOrder.size,
          unbindCount = unbindOrder.size,
          reusedCount = stats?.reusedCount ?: -1,
          updatedCount = stats?.updatedCount ?: -1,
          contentPreUpdate = contentPreUpdate,
          contentPostUpdate = contentPostUpdate,
      )
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.reusedCount).describedAs("reusedCount").isEqualTo(slow.reusedCount)
    assertThat(fast.updatedCount).describedAs("updatedCount").isEqualTo(slow.updatedCount)
    assertThat(fast.bindCount).describedAs("no rebind on stable item").isEqualTo(0)
    assertThat(fast.unbindCount).describedAs("no unbind on stable item").isEqualTo(0)
    // Content View must be reused (same instance), not recreated
    assertThat(fast.contentPreUpdate)
        .describedAs("fast path reuses same View")
        .isEqualTo(fast.contentPostUpdate)
    assertThat(slow.contentPreUpdate)
        .describedAs("slow path reuses same View")
        .isEqualTo(slow.contentPostUpdate)
  }

  // endregion

  // region Test 2: Changed RenderUnit falls through to slow path

  @Test
  fun changedRenderUnit_fallsThroughToSlowPath() {
    data class RunResult(val bindCount: Int, val unbindCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val layoutData = Any()
      val renderUnit = TestRenderUnit()
      val id = renderUnit.id
      val binder = TestBinder<Any>(bindOrder, unbindOrder)
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setLayoutData(layoutData)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      // New RU instance (different ===) + different layoutData → shouldUpdate triggers
      val newRenderUnit = TestRenderUnit()
      newRenderUnit.setId(id)
      val newBinder = TestBinder<Any>(bindOrder, unbindOrder)
      newRenderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(newRenderUnit, newBinder),
      )
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setLayoutData(Any()) // different reference → shouldUpdate returns true
      leaf2.setRenderUnit(newRenderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      return RunResult(bindCount = bindOrder.size, unbindCount = unbindOrder.size)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.unbindCount).describedAs("unbind actually triggered").isGreaterThan(0)
    assertThat(fast.bindCount).describedAs("bind actually triggered").isGreaterThan(0)
  }

  // endregion

  // region Test 3: Changed layoutData falls through to slow path

  @Test
  fun changedLayoutData_fallsThroughToSlowPath() {
    data class RunResult(val bindCount: Int, val unbindCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      val binder = TestBinder<Any>(bindOrder, unbindOrder)
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      // Same RU instance but different layoutData
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setLayoutData(Any())
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      return RunResult(bindCount = bindOrder.size, unbindCount = unbindOrder.size)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.unbindCount).describedAs("unbind actually triggered").isGreaterThan(0)
    assertThat(fast.bindCount).describedAs("bind actually triggered").isGreaterThan(0)
  }

  // endregion

  // region Test 4: Changed bounds falls through to slow path

  @Test
  fun changedBounds_fallsThroughToSlowPath() {
    data class RunResult(
        val bindCount: Int,
        val unbindCount: Int,
        val contentWidth: Int,
        val contentHeight: Int,
    )

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      val binder = TestBinder<Any>(bindOrder, unbindOrder)
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      // Same RU, different bounds
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 20, 20)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      val content = checkNotNull(mountState.getContentAt(1) as? View)
      return RunResult(bindOrder.size, unbindOrder.size, content.width, content.height)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.contentWidth).describedAs("content width").isEqualTo(slow.contentWidth)
    assertThat(fast.contentHeight).describedAs("content height").isEqualTo(slow.contentHeight)
  }

  // endregion

  // region Test 5: Multiple items with mixed eligibility

  @Test
  fun multipleItems_mixedEligibility() {
    data class RunResult(
        val bindCount: Int,
        val unbindCount: Int,
        val reusedCount: Int,
        val updatedCount: Int,
        val mountedCount: Int,
        val totalMountItems: Int,
    )

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()

      val stableRU = TestRenderUnit()
      stableRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              stableRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )
      val changedRU = TestRenderUnit()
      changedRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              changedRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf1 = TestNode(0, 0, 10, 10)
      val leaf2 = TestNode(10, 0, 10, 10)
      root.addChild(leaf1)
      root.addChild(leaf2)
      leaf1.setRenderUnit(stableRU)
      leaf2.setRenderUnit(changedRU)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      val newRU = TestRenderUnit()
      newRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              newRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )
      val root2 = TestNode()
      val l1 = TestNode(0, 0, 10, 10)
      val l2 = TestNode(10, 0, 10, 10)
      val l3 = TestNode(20, 0, 10, 10)
      root2.addChild(l1)
      root2.addChild(l2)
      root2.addChild(l3)
      l1.setRenderUnit(stableRU)
      l2.setLayoutData(Any())
      l2.setRenderUnit(changedRU)
      l3.setRenderUnit(newRU)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      val stats = mountState.mountStats
      return RunResult(
          bindOrder.size,
          unbindOrder.size,
          stats?.reusedCount ?: -1,
          stats?.updatedCount ?: -1,
          stats?.mountedCount ?: -1,
          mountState.getMountItemCount(),
      )
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.reusedCount).describedAs("reusedCount").isEqualTo(slow.reusedCount)
    assertThat(fast.updatedCount).describedAs("updatedCount").isEqualTo(slow.updatedCount)
    assertThat(fast.mountedCount).describedAs("mountedCount").isEqualTo(slow.mountedCount)
    assertThat(fast.totalMountItems)
        .describedAs("total mount items")
        .isEqualTo(slow.totalMountItems)
  }

  // endregion

  // region Test 6: Stable host with child reused identically

  @Test
  fun stableHostWithChild_reusedIdentically() {
    data class RunResult(
        val bindCount: Int,
        val unbindCount: Int,
        val reusedCount: Int,
        val hostPre: Int,
        val hostPost: Int,
    )

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val hostRU = TestHostRenderUnit()
      hostRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              hostRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )
      val childRU = TestRenderUnit()
      childRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              childRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val hostNode = TestNode(0, 0, 50, 50)
      val childNode = TestNode(0, 0, 10, 10)
      root.addChild(hostNode)
      hostNode.addChild(childNode)
      hostNode.setRenderUnit(hostRU)
      childNode.setRenderUnit(childRU)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      val hostPre = System.identityHashCode(mountState.getContentAt(1))
      bindOrder.clear()
      unbindOrder.clear()

      val root2 = TestNode()
      val h2 = TestNode(0, 0, 50, 50)
      val c2 = TestNode(0, 0, 10, 10)
      root2.addChild(h2)
      h2.addChild(c2)
      h2.setRenderUnit(hostRU)
      c2.setRenderUnit(childRU)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))
      val hostPost = System.identityHashCode(mountState.getContentAt(1))

      val stats = mountState.mountStats
      return RunResult(
          bindOrder.size,
          unbindOrder.size,
          stats?.reusedCount ?: -1,
          hostPre,
          hostPost,
      )
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.reusedCount).describedAs("reusedCount").isEqualTo(slow.reusedCount)
    assertThat(fast.bindCount).describedAs("no rebind on stable items").isEqualTo(0)
    assertThat(fast.hostPre).describedAs("fast path reuses host View").isEqualTo(fast.hostPost)
  }

  // endregion

  // region Test 7: Unbound item falls through to slow path

  @Test
  fun unboundItem_fallsThroughToSlowPath() {
    data class RunResult(val bindCount: Int, val unbindCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      renderUnit.addAttachBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      mountState.detach()
      bindOrder.clear()
      unbindOrder.clear()

      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      return RunResult(bindOrder.size, unbindOrder.size)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
  }

  // endregion

  // region Test 8: Stable item with attach binder reused identically

  @Test
  fun stableItemWithAttachBinder_reusedIdentically() {
    data class RunResult(val bindCount: Int, val unbindCount: Int, val reusedCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )
      renderUnit.addAttachBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      val stats = mountState.mountStats
      return RunResult(bindOrder.size, unbindOrder.size, stats?.reusedCount ?: -1)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.reusedCount).describedAs("reusedCount").isEqualTo(slow.reusedCount)
    assertThat(fast.bindCount).describedAs("no rebind").isEqualTo(0)
    assertThat(fast.unbindCount).describedAs("no unbind").isEqualTo(0)
  }

  // endregion

  // region Test 9: Dirty binder via extraBindersToUpdate falls through

  @Test
  fun dirtyBinder_fallsThroughToSlowPath() {
    data class RunResult(val bindCount: Int, val unbindCount: Int, val updatedCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      val binder = TestBinder<Any>(bindOrder, unbindOrder)
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      // Same RU instance but mark binder dirty via extraBindersToUpdate
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      val dirtyBinderId =
          BinderId(renderUnit.id, BinderType.MOUNT, ClassBinderKey.of(binder.javaClass))
      mountState.mount(createRenderTree(c, root2, fastPathEligible), scatterSetOf(dirtyBinderId))

      val stats = mountState.mountStats
      return RunResult(bindOrder.size, unbindOrder.size, stats?.updatedCount ?: -1)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.updatedCount).describedAs("updatedCount").isEqualTo(slow.updatedCount)
  }

  @Test
  fun rebindBinders_reportsStateReadRebind() {
    // This path skips shouldUpdate, so without an explicit report the rebind is invisible.
    val c: Context = RuntimeEnvironment.application
    val bound: MutableList<BinderId> = ArrayList()
    val reported: MutableList<BinderId> = ArrayList()
    val observer =
        object : BinderObserver() {
          override fun onBind(binderId: BinderId, func: () -> Unit) {
            bound.add(binderId)
            func()
          }

          override fun onUnbind(binderId: BinderId, func: () -> Unit) = func()

          override fun onStateReadRebind(binderId: BinderId) {
            reported.add(binderId)
          }
        }

    val renderUnit = TestRenderUnit()
    val binder = TestBinder<Any>(ArrayList(), ArrayList())
    renderUnit.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
    )

    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)
    leaf.setRenderUnit(renderUnit)
    val mountState = createMountState(c, observer)
    mountState.mount(createRenderTree(c, root, false))

    // Take the id from the mount so the test does not have to reconstruct the binder key.
    val binderId = bound.first { it.renderUnitId == renderUnit.id }
    reported.clear()

    mountState.rebindBinders(longObjectMapOf(renderUnit.id, scatterSetOf(binderId)))

    assertThat(reported).containsExactly(binderId)
  }

  @Test
  fun rebindBinders_dispatchesRenderUnitUpdatedPerRenderUnit() {
    // The tracer drains on RenderUnitUpdated, so this path must dispatch one per render unit.
    // Two units, not one: with one, hoisting the dispatch out of the loop would still pass.
    val c: Context = RuntimeEnvironment.application
    val bound: MutableList<BinderId> = ArrayList()
    val observer =
        object : BinderObserver() {
          override fun onBind(binderId: BinderId, func: () -> Unit) {
            bound.add(binderId)
            func()
          }

          override fun onUnbind(binderId: BinderId, func: () -> Unit) = func()
        }

    fun renderUnitWithBinder(): TestRenderUnit =
        TestRenderUnit().apply {
          addOptionalMountBinder(
              DelegateBinder.createDelegateBinder<Any, View, Any>(
                  this,
                  TestBinder<Any>(ArrayList(), ArrayList()),
              ),
          )
        }

    val firstUnit = renderUnitWithBinder()
    val secondUnit = renderUnitWithBinder()

    val root = TestNode()
    val firstLeaf = TestNode(0, 0, 10, 10)
    val secondLeaf = TestNode(0, 10, 10, 20)
    root.addChild(firstLeaf)
    root.addChild(secondLeaf)
    firstLeaf.setRenderUnit(firstUnit)
    secondLeaf.setRenderUnit(secondUnit)
    val mountState = createMountState(c, observer)
    mountState.mount(createRenderTree(c, root, false))

    // Take the ids from the mount so the test does not have to reconstruct the binder keys.
    val firstBinderId = bound.first { it.renderUnitId == firstUnit.id }
    val secondBinderId = bound.first { it.renderUnitId == secondUnit.id }

    val events: MutableList<DebugEvent> = ArrayList()
    val subscriber =
        object : DebugEventSubscriber(DebugEvent.RenderUnitUpdated) {
          override fun onEvent(event: DebugEvent) {
            events.add(event)
          }
        }
    DebugEventBus.subscribe(subscriber)
    try {
      mountState.rebindBinders(
          longObjectMapOf(
              firstUnit.id,
              scatterSetOf(firstBinderId),
              secondUnit.id,
              scatterSetOf(secondBinderId),
          ),
      )
    } finally {
      DebugEventBus.unsubscribe(subscriber)
    }

    assertThat(events.map { it.attribute<Long>(DebugEventAttribute.RenderUnitId) })
        .containsExactlyInAnyOrder(firstUnit.id, secondUnit.id)

    // The payload is a contract, not decoration. MountUiDebugger.onEvent reads RootHostHashCode
    // into a non-null Int before it filters on the host, so a missing attribute throws in every
    // subscribed debugger rather than only the one that owns the event.
    for (event in events) {
      assertThat(event.attributeOrNull<Int>(DebugEventAttribute.RootHostHashCode))
          .describedAs("RootHostHashCode")
          .isNotNull()
      assertThat(event.attributeOrNull<Rect>(DebugEventAttribute.Bounds))
          .describedAs("Bounds")
          .isNotNull()
      assertThat(event.attributeOrNull<String>(DebugEventAttribute.Description))
          .describedAs("Description")
          .isNotNull()
    }
  }

  @Test
  fun rebindBinders_endsTraceWhenRebindThrows() {
    // endTrace drains what the rebind wrote, so a throwing binder must not strand it.
    val c: Context = RuntimeEnvironment.application
    val bound: MutableList<BinderId> = ArrayList()
    var shouldThrow = false
    val observer =
        object : BinderObserver() {
          override fun onBind(binderId: BinderId, func: () -> Unit) {
            bound.add(binderId)
            func()
            if (shouldThrow) {
              throw RuntimeException("bind blew up")
            }
          }

          override fun onUnbind(binderId: BinderId, func: () -> Unit) = func()
        }

    val renderUnit = TestRenderUnit()
    renderUnit.addOptionalMountBinder(
        DelegateBinder.createDelegateBinder<Any, View, Any>(
            renderUnit,
            TestBinder<Any>(ArrayList(), ArrayList()),
        ),
    )

    val root = TestNode()
    val leaf = TestNode(0, 0, 10, 10)
    root.addChild(leaf)
    leaf.setRenderUnit(renderUnit)
    val mountState = createMountState(c, observer)
    mountState.mount(createRenderTree(c, root, false))

    val binderId = bound.first { it.renderUnitId == renderUnit.id }

    val events: MutableList<DebugEvent> = ArrayList()
    val subscriber =
        object : DebugEventSubscriber(DebugEvent.RenderUnitUpdated) {
          override fun onEvent(event: DebugEvent) {
            events.add(event)
          }
        }
    DebugEventBus.subscribe(subscriber)
    shouldThrow = true
    try {
      assertThatThrownBy {
            mountState.rebindBinders(longObjectMapOf(renderUnit.id, scatterSetOf(binderId)))
          }
          .isInstanceOf(RenderUnitOperationException::class.java)
    } finally {
      shouldThrow = false
      DebugEventBus.unsubscribe(subscriber)
    }

    assertThat(events).hasSize(1)
    assertThat(events.single().attribute<Long>(DebugEventAttribute.RenderUnitId))
        .isEqualTo(renderUnit.id)
  }

  /**
   * A binder dirtied via extraBindersToUpdate short-circuits shouldUpdate, so it has to be reported
   * separately or the rebind is invisible to observers.
   */
  @Test
  fun dirtyBinder_reportsStateReadRebind() {
    fun run(fastPathEligible: Boolean, collectBindReasons: Boolean): List<BinderId> {
      val c: Context = RuntimeEnvironment.application
      val reported: MutableList<BinderId> = ArrayList()
      val observer =
          object : BinderObserver() {
            override fun onBind(binderId: BinderId, func: () -> Unit) = func()

            override fun onUnbind(binderId: BinderId, func: () -> Unit) = func()

            override fun onStateReadRebind(binderId: BinderId) {
              reported.add(binderId)
            }
          }

      val renderUnit = TestRenderUnit()
      val binder = TestBinder<Any>(ArrayList(), ArrayList())
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(renderUnit, binder),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c, observer)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      reported.clear()

      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      val dirtyBinderId =
          BinderId(renderUnit.id, BinderType.MOUNT, ClassBinderKey.of(binder.javaClass))

      val previous = RenderCoreConfig.collectBindReasons
      RenderCoreConfig.collectBindReasons = collectBindReasons
      try {
        mountState.mount(createRenderTree(c, root2, fastPathEligible), scatterSetOf(dirtyBinderId))
      } finally {
        RenderCoreConfig.collectBindReasons = previous
      }
      return reported
    }

    assertThat(run(fastPathEligible = false, collectBindReasons = true))
        .describedAs("slow path reports the state-read rebind")
        .hasSize(1)
    assertThat(run(fastPathEligible = true, collectBindReasons = true))
        .describedAs("fast path reports the state-read rebind")
        .hasSize(1)

    assertThat(run(fastPathEligible = false, collectBindReasons = false))
        .describedAs(
            "slow path still records the rebind: the tracer entry the bind duration " +
                "attaches to must not depend on collectBindReasons",
        )
        .hasSize(1)
    assertThat(run(fastPathEligible = true, collectBindReasons = false))
        .describedAs("fast path still records the rebind when collectBindReasons is off")
        .hasSize(1)
  }

  // endregion

  // region Test 10: View with requestLayout() falls through to slow path

  @Test
  fun layoutRequestedView_fallsThroughToSlowPath() {
    data class RunResult(val bindCount: Int, val unbindCount: Int, val reusedCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))

      // Force layout request on mounted View
      (checkNotNull(mountState.getContentAt(1) as? View)).requestLayout()
      bindOrder.clear()
      unbindOrder.clear()

      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      val stats = mountState.mountStats
      return RunResult(bindOrder.size, unbindOrder.size, stats?.reusedCount ?: -1)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.reusedCount).describedAs("reusedCount").isEqualTo(slow.reusedCount)
  }

  // endregion

  // region Test 11: Changed padding falls through to slow path

  @Test
  fun changedPadding_fallsThroughToSlowPath() {
    data class RunResult(val bindCount: Int, val unbindCount: Int, val reusedCount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val hostRU = TestHostRenderUnit()
      hostRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              hostRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val layout1 =
          SimpleLayoutResult.create()
              .width(200)
              .height(200)
              .child(
                  SimpleLayoutResult.create()
                      .renderUnit(hostRU)
                      .width(50)
                      .height(50)
                      .padding(5, 5, 5, 5),
              )
              .build()
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, LayoutResultWrappingNode(layout1), fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      // Same RU, same bounds, different padding
      val layout2 =
          SimpleLayoutResult.create()
              .width(200)
              .height(200)
              .child(
                  SimpleLayoutResult.create()
                      .renderUnit(hostRU)
                      .width(50)
                      .height(50)
                      .padding(10, 10, 10, 10),
              )
              .build()
      mountState.mount(createRenderTree(c, LayoutResultWrappingNode(layout2), fastPathEligible))

      val stats = mountState.mountStats
      return RunResult(bindOrder.size, unbindOrder.size, stats?.reusedCount ?: -1)
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.reusedCount).describedAs("reusedCount").isEqualTo(slow.reusedCount)
  }

  // endregion

  // region Test 12: Removed item during update

  @Test
  fun removedItem_unmountOrderMatchesSlowPath() {
    data class RunResult(
        val bindCount: Int,
        val unbindCount: Int,
        val unmountedCount: Int,
        val totalItems: Int,
    )

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val stableRU = TestRenderUnit()
      stableRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              stableRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )
      val removedRU = TestRenderUnit()
      removedRU.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              removedRU,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf1 = TestNode(0, 0, 10, 10)
      val leaf2 = TestNode(10, 0, 10, 10)
      root.addChild(leaf1)
      root.addChild(leaf2)
      leaf1.setRenderUnit(stableRU)
      leaf2.setRenderUnit(removedRU)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      bindOrder.clear()
      unbindOrder.clear()

      // Keep item 1, remove item 2
      val root2 = TestNode()
      val l1 = TestNode(0, 0, 10, 10)
      root2.addChild(l1)
      l1.setRenderUnit(stableRU)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      val stats = mountState.mountStats
      return RunResult(
          bindOrder.size,
          unbindOrder.size,
          stats?.unmountedCount ?: -1,
          mountState.getMountItemCount(),
      )
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindCount).describedAs("bind count").isEqualTo(slow.bindCount)
    assertThat(fast.unbindCount).describedAs("unbind count").isEqualTo(slow.unbindCount)
    assertThat(fast.unmountedCount).describedAs("unmountedCount").isEqualTo(slow.unmountedCount)
    assertThat(fast.totalItems).describedAs("total items after removal").isEqualTo(slow.totalItems)
    assertThat(fast.unmountedCount).describedAs("removed item was unmounted").isGreaterThan(0)
  }

  // endregion

  // region Test 13: unmountAllItems after fast-path reuse

  @Test
  fun unmountAllItems_afterFastPathReuse() {
    data class RunResult(val unbindCount: Int, val itemsAfterUnmount: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      renderUnit.addOptionalMountBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )
      renderUnit.addAttachBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))

      // Fast-path reuse
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))

      bindOrder.clear()
      unbindOrder.clear()
      mountState.unmountAllItems()

      return RunResult(unbindOrder.size, mountState.getMountItemCount())
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.unbindCount)
        .describedAs("unbind count during unmountAll")
        .isEqualTo(slow.unbindCount)
    assertThat(fast.itemsAfterUnmount).describedAs("no items remaining").isEqualTo(0)
    assertThat(slow.itemsAfterUnmount).describedAs("no items remaining (slow)").isEqualTo(0)
    assertThat(fast.unbindCount).describedAs("binders actually unbound").isGreaterThan(0)
  }

  // endregion

  // region Test 14: Detach, mount new tree, attach

  @Test
  fun detach_mountNewTree_attach_bindsCorrectTree() {
    data class RunResult(val bindsDuringMount: Int, val bindsDuringAttach: Int, val totalItems: Int)

    fun run(fastPathEligible: Boolean): RunResult {
      val c: Context = RuntimeEnvironment.application

      val bindOrder: MutableList<Any?> = ArrayList()
      val unbindOrder: MutableList<Any?> = ArrayList()
      val renderUnit = TestRenderUnit()
      renderUnit.addAttachBinder(
          DelegateBinder.createDelegateBinder<Any, View, Any>(
              renderUnit,
              TestBinder<Any>(bindOrder, unbindOrder),
          ),
      )

      val root = TestNode()
      val leaf = TestNode(0, 0, 10, 10)
      root.addChild(leaf)
      leaf.setRenderUnit(renderUnit)
      val mountState = createMountState(c)
      mountState.mount(createRenderTree(c, root, fastPathEligible))
      mountState.detach()
      bindOrder.clear()
      unbindOrder.clear()

      // Mount while detached
      val root2 = TestNode()
      val leaf2 = TestNode(0, 0, 10, 10)
      root2.addChild(leaf2)
      leaf2.setRenderUnit(renderUnit)
      mountState.mount(createRenderTree(c, root2, fastPathEligible))
      val bindsDuringMount = bindOrder.size
      bindOrder.clear()

      // Attach
      mountState.attach()

      return RunResult(bindsDuringMount, bindOrder.size, mountState.getMountItemCount())
    }

    val slow = run(fastPathEligible = false)
    val fast = run(fastPathEligible = true)

    assertThat(fast.bindsDuringMount)
        .describedAs("binds during mount")
        .isEqualTo(slow.bindsDuringMount)
    assertThat(fast.bindsDuringAttach)
        .describedAs("binds during attach")
        .isEqualTo(slow.bindsDuringAttach)
    assertThat(fast.totalItems).describedAs("total items").isEqualTo(slow.totalItems)
  }

  // endregion

  // region Helpers

  companion object {
    private fun createRenderTree(
        c: Context,
        root: Node<Any?>,
        fastPathEligible: Boolean = false,
    ): RenderTree {
      val sizeConstraints = SizeConstraints.exact(200, 200)
      val layoutContext = LayoutContext<Any?>(c, null, -1, LayoutCache(), null)
      val result = root.calculateLayout(layoutContext, sizeConstraints)
      val tree = getReducedTree(layoutContext, result, sizeConstraints, RenderState.NO_ID)
      if (!fastPathEligible) return tree
      // Rebuild non-root nodes with isFastPathEligible = true so the mount fast path engages.
      val count = tree.mountableOutputCount
      val rootNode = tree.getRenderTreeNodeAtIndex(0)
      val newNodes =
          Array<RenderTreeNode>(count) { i ->
            if (i == 0) rootNode
            else {
              val node = tree.getRenderTreeNodeAtIndex(i)
              RenderTreeNode(
                  node.parent,
                  node.renderUnit,
                  node.layoutData,
                  node.bounds,
                  node.resolvedPadding,
                  node.positionInParent,
                  node.poolScope,
                  isFastPathEligible = true,
              )
            }
          }
      return RenderTree(
          rootNode,
          newNodes,
          null,
          tree.sizeConstraints,
          tree.renderStateId,
          tree.extensionResults,
          tree.renderContext,
          tree.debugData,
      )
    }

    private fun createMountState(
        c: Context,
        binderObserver: BinderObserver? = null,
    ): MountState {
      return MountState(TestHostView(c), binderObserver = binderObserver)
    }
  }

  // endregion
}
