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
import android.graphics.Rect
import android.view.View
import com.facebook.rendercore.testing.TestRenderUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * [Host.visitMountItems] — the iteration entry point that [Host.getMountItemAt] cannot provide,
 * because its index means different things in different implementations and it does not expose
 * scrap.
 */
@RunWith(RobolectricTestRunner::class)
class HostVisitMountItemsTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.application
  }

  private fun itemsIn(host: Host): List<MountItem> =
      ArrayList<MountItem>().also { items ->
        val completed = host.visitMountItems { item ->
          items.add(item)
          true
        }
        assertThat(completed).isTrue()
      }

  /** All content in these tests is a [View], so the unit's default VIEW render type is correct. */
  private fun createMountItem(content: View): MountItem =
      MountItem(RenderTreeNode(null, TestRenderUnit(), null, Rect(), Rect(), 0), content)

  @Test
  fun visitMountItems_onEmptyHost_visitsNothing() {
    assertThat(itemsIn(HostView(context))).isEmpty()
  }

  @Test
  fun visitMountItems_visitsEveryMountedItem() {
    val hostView = HostView(context)
    val first = createMountItem(View(context))
    val second = createMountItem(View(context))
    hostView.mount(0, first)
    hostView.mount(1, second)

    assertThat(itemsIn(hostView)).containsExactly(first, second)
  }

  @Test
  fun visitMountItems_skipsGapsInSparseStorage() {
    // The backing array is sparse — items sit at their mount index, with nulls between. A plain
    // `for (i in 0 until mountItemCount) getMountItemAt(i)` loop throws on the first gap.
    val hostView = HostView(context)
    val first = createMountItem(View(context))
    val distant = createMountItem(View(context))
    hostView.mount(0, first)
    hostView.mount(37, distant)

    assertThat(hostView.mountItemCount).isEqualTo(2)
    assertThat(itemsIn(hostView)).containsExactly(first, distant)
  }

  @Test
  fun visitMountItems_usesAscendingStorageOrder() {
    val hostView = HostView(context)
    val atFive = createMountItem(View(context))
    val atOne = createMountItem(View(context))
    hostView.mount(5, atFive)
    hostView.mount(1, atOne)

    assertThat(itemsIn(hostView)).containsExactly(atOne, atFive)
  }

  @Test
  fun visitMountItems_includesItemsParkedInScrap() {
    // moveItem onto an occupied slot parks the displaced item in the scrap array, which
    // getMountItemAt does not expose at all.
    val hostView = HostView(context)
    val moved = createMountItem(View(context))
    val displaced = createMountItem(View(context))
    hostView.mount(0, moved)
    hostView.mount(1, displaced)
    hostView.moveItem(moved, 0, 1)

    assertThat(itemsIn(hostView))
        .describedAs("live items first, followed by displaced items")
        .containsExactly(moved, displaced)
  }

  @Test
  fun isMountItemStorageEmpty_tracksActiveAndDisplacedStorage() {
    val hostView = HostView(context)
    assertThat(hostView.isMountItemStorageEmpty).isTrue()

    val moved = createMountItem(View(context))
    val displaced = createMountItem(View(context))
    hostView.mount(0, moved)
    hostView.mount(1, displaced)
    assertThat(hostView.isMountItemStorageEmpty).isFalse()

    // Parks `displaced` in scrap, so both stores are now occupied.
    hostView.moveItem(moved, 0, 1)
    assertThat(hostView.isMountItemStorageEmpty).isFalse()

    for (item in hostView.snapshotMountItems().asReversed()) {
      hostView.unmount(item)
    }
    assertThat(hostView.isMountItemStorageEmpty).isTrue()
  }

  @Test
  fun visitMountItems_canStopWhileVisitingScrap() {
    val hostView = HostView(context)
    val moved = createMountItem(View(context))
    val displaced = createMountItem(View(context))
    hostView.mount(0, moved)
    hostView.mount(1, displaced)
    hostView.moveItem(moved, 0, 1)

    val visited = ArrayList<MountItem>()
    val completed = hostView.visitMountItems { item ->
      visited.add(item)
      item !== displaced
    }

    assertThat(completed).isFalse()
    assertThat(visited).containsExactly(moved, displaced)
  }

  @Test
  fun defaultVisitMountItems_supportsPackedHostSubclasses() {
    val host = PackedHost(context)
    val first = createMountItem(View(context))
    val second = createMountItem(View(context))
    host.mount(0, first)
    host.mount(1, second)

    assertThat(itemsIn(host)).containsExactly(first, second)
    assertThat(host.snapshotMountItems()).containsExactly(first, second)
  }

  @Test
  fun callerSnapshot_survivesUnmountingAfterTraversal() {
    val hostView = HostView(context)
    val first = createMountItem(View(context))
    val second = createMountItem(View(context))
    hostView.mount(0, first)
    hostView.mount(1, second)

    val snapshot = hostView.snapshotMountItems()
    for (item in snapshot) {
      hostView.unmount(item)
    }

    assertThat(snapshot).containsExactly(first, second)
    assertThat(hostView.mountItemCount).isEqualTo(0)
  }

  @Test
  fun snapshotMountItems_includesItemsParkedInScrap() {
    val hostView = HostView(context)
    val moved = createMountItem(View(context))
    val displaced = createMountItem(View(context))
    hostView.mount(0, moved)
    hostView.mount(1, displaced)
    hostView.moveItem(moved, 0, 1)

    assertThat(hostView.snapshotMountItems()).containsExactly(moved, displaced)
  }

  @Test
  fun visitMountItems_stopsWhenVisitorReturnsFalse() {
    val hostView = HostView(context)
    val first = createMountItem(View(context))
    val second = createMountItem(View(context))
    hostView.mount(0, first)
    hostView.mount(1, second)

    val visited = ArrayList<MountItem>()
    val completed = hostView.visitMountItems { item ->
      visited.add(item)
      false
    }

    assertThat(completed).isFalse()
    assertThat(visited).containsExactly(first)
  }

  @Test
  fun visitMountItems_rejectsReentrantStructuralMutations() {
    val hostView = HostView(context)
    val first = createMountItem(View(context))
    val second = createMountItem(View(context))
    hostView.mount(0, first)
    hostView.mount(1, second)

    assertThatThrownBy {
          hostView.visitMountItems {
            hostView.moveItem(first, 0, 2)
            true
          }
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Cannot mutate Host mount items")

    assertThat(itemsIn(hostView)).containsExactly(first, second)

    hostView.moveItem(first, 0, 2)
    assertThat(itemsIn(hostView)).containsExactly(second, first)
  }

  @Test
  fun snapshotTeardownInReverseOrder_clearsBothActiveAndScrapStorage() {
    // TransitionsExtension unmounts a snapshot back-to-front, which only works because a snapshot
    // lists active items before displaced ones. A slot that is occupied in both stores resolves to
    // the displaced entry on removal, so displaced storage has to drain first — front-to-back
    // teardown of this same snapshot leaves the active item mounted.
    val hostView = HostView(context)
    val moved = createMountItem(View(context))
    val displaced = createMountItem(View(context))
    hostView.mount(0, moved)
    hostView.mount(1, displaced)
    hostView.moveItem(moved, 0, 1)

    val snapshot = hostView.snapshotMountItems()
    assertThat(snapshot).containsExactly(moved, displaced)

    for (item in snapshot.asReversed()) {
      hostView.unmount(item)
    }

    assertThat(hostView.snapshotMountItems()).isEmpty()
    assertThat(hostView.mountItemCount).isEqualTo(0)
  }

  @Test
  fun defaultVisitMountItems_rejectsReentrantStructuralMutations() {
    // Subclasses override only the traversal body, so a host on the default body is guarded by
    // the same bracketing in Host.visitMountItems that HostView gets.
    val host = PackedHost(context)
    val first = createMountItem(View(context))
    host.mount(0, first)
    assertThatThrownBy {
          host.visitMountItems {
            host.unmount(first)
            true
          }
        }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("Cannot mutate Host mount items")
  }

  private class PackedHost(context: Context) : Host(context, null) {
    private val items = ArrayList<MountItem>()

    override fun mount(index: Int, mountItem: MountItem) {
      checkNotVisitingMountItems()
      items.add(index, mountItem)
    }

    override fun unmount(mountItem: MountItem) {
      checkNotVisitingMountItems()
      items.remove(mountItem)
    }

    override fun unmount(index: Int, mountItem: MountItem) {
      checkNotVisitingMountItems()
      items.removeAt(index)
    }

    override val mountItemCount: Int
      get() = items.size

    override fun getMountItemAt(index: Int): MountItem = items[index]

    override fun moveItem(item: MountItem?, oldIndex: Int, newIndex: Int) {
      checkNotVisitingMountItems()
      items.add(newIndex, items.removeAt(oldIndex))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) = Unit
  }
}
