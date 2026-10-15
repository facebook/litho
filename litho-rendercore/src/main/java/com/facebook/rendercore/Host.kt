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
import android.util.AttributeSet
import android.view.ViewGroup

fun interface MountItemVisitor {
  /** @return `true` to continue visiting, or `false` to stop. */
  fun visit(mountItem: MountItem): Boolean
}

abstract class Host(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

  private var mountItemTraversalDepth = 0

  init {
    clipChildren = true
  }

  /**
   * Mounts the given [MountItem] with unique index.
   *
   * @param index index of the [MountItem]. Guaranteed to be the same index as is passed for the
   *   corresponding `unmount(index, mountItem)` call.
   * @param mountItem item to be mounted into the host.
   */
  abstract fun mount(index: Int, mountItem: MountItem)

  /**
   * Unmounts the given [MountItem]
   *
   * @param mountItem item to be unmounted from the host.
   */
  abstract fun unmount(mountItem: MountItem)

  /**
   * Unmounts the given [MountItem] with unique index.
   *
   * @param index index of the [MountItem]. Guaranteed to be the same index as was passed for the
   *   corresponding `mount(index, mountItem)` call.
   * @param mountItem item to be unmounted from the host.
   */
  abstract fun unmount(index: Int, mountItem: MountItem)

  /**
   * @return number of [MountItem]s in active mount storage. Items temporarily displaced by
   *   [moveItem] are not included; use [visitMountItems] to visit active and displaced items.
   */
  abstract val mountItemCount: Int

  /**
   * @return the [MountItem] that was mounted with the given index.
   *
   * The meaning of [index] is implementation-defined and the implementations disagree: `HostView`
   * treats it as the mount index passed to [mount] and throws on an empty slot, while
   * `ComponentHost` treats it as an ordinal into its packed storage. **Do not use this to iterate**
   * — use [visitMountItems], which is well-defined on every implementation.
   */
  abstract fun getMountItemAt(index: Int): MountItem

  /**
   * Visits each [MountItem] in active or displaced mount storage exactly once. Active items are
   * visited in ascending mount-index order, followed by items displaced by [moveItem] in ascending
   * displaced-index order. Items that have already moved to separate disappearing-item storage are
   * not visited.
   *
   * This is a live traversal. Structurally mutating this host, either directly or indirectly, from
   * [MountItemVisitor.visit] throws [IllegalStateException]. A caller that needs to unmount items
   * must first collect its own snapshot. Implementations with sparse indices or separate
   * displaced-item storage override [visitMountItemsUnguarded] rather than this method.
   *
   * @return `true` if every item was visited, or `false` if [visitor] stopped traversal.
   * @throws IllegalStateException if the visitor structurally mutates this host.
   */
  fun visitMountItems(visitor: MountItemVisitor): Boolean {
    mountItemTraversalDepth++
    try {
      return visitMountItemsUnguarded(visitor)
    } finally {
      mountItemTraversalDepth--
    }
  }

  /**
   * The traversal body behind [visitMountItems], which is what holds the reentrancy guard open
   * around this call. Implementations with sparse indices or separate displaced-item storage must
   * override this; the default walks [mountItemCount] through [getMountItemAt]. This is the only
   * override point: [snapshotMountItems] and [isMountItemStorageEmpty] are derived from it.
   *
   * An override must stop and return `false` as soon as [visitor] returns `false`, otherwise
   * [isMountItemStorageEmpty] reports a non-empty host as empty.
   *
   * @return `true` if every item was visited, or `false` if [visitor] stopped traversal.
   */
  protected open fun visitMountItemsUnguarded(visitor: MountItemVisitor): Boolean {
    for (i in 0 until mountItemCount) {
      if (!visitor.visit(getMountItemAt(i))) return false
    }
    return true
  }

  /**
   * `true` when neither active nor displaced mount storage holds a [MountItem]. Unlike
   * [mountItemCount] this also accounts for items displaced by [moveItem].
   *
   * Allocation-free: a visitor that stops at the first item can only report a completed traversal
   * when there was nothing to visit.
   */
  val isMountItemStorageEmpty: Boolean
    get() = visitMountItems { false }

  /** Subclasses must call this before structurally modifying their mount-item storage. */
  protected fun checkNotVisitingMountItems() {
    check(mountItemTraversalDepth == 0) {
      "Cannot mutate Host mount items from a visitMountItems callback"
    }
  }

  /**
   * Returns a stable shallow snapshot of active and displaced mount items, in the order
   * [visitMountItems] would visit them: active items first, displaced items last. The host may be
   * structurally mutated after this method returns without affecting the snapshot.
   *
   * **Unmount a snapshot back-to-front.** Removing an item resolves a mount index that is occupied
   * in both stores to the displaced entry, so a displaced item has to be unmounted before the
   * active item that took its slot. Front-to-back teardown of the same snapshot unmounts the active
   * item's content but clears the displaced item's bookkeeping instead, silently leaving content
   * mounted.
   */
  fun snapshotMountItems(): List<MountItem> =
      ArrayList<MountItem>(mountItemCount).also { snapshot ->
        visitMountItems { item ->
          snapshot.add(item)
          true
        }
      }

  /**
   * Moves the MountItem associated to oldIndex in the newIndex position. This happens when a
   * RootHostView needs to re-arrange the internal order of its items. If an item is already present
   * in newIndex the item is guaranteed to be either unmounted or moved to a different index by
   * subsequent calls to either [.unmount] or [.moveItem].
   *
   * @param item The item that has been moved.
   * @param oldIndex The current index of the MountItem.
   * @param newIndex The new index of the MountItem.
   */
  abstract fun moveItem(item: MountItem?, oldIndex: Int, newIndex: Int)

  open val descriptionOfMountedItems: String
    get() = ""

  /**
   * Returns a [String] identifier that should be used to help to understand which kind of hierarchy
   * is being backed in the [MountState]. This information will be used for traces and debugging
   * purposes.
   */
  open val hostHierarchyMountStateIdentifier: String?
    get() = null

  /** Use this API to remove all mounted items from the Host to recover from errors. */
  open fun safelyUnmountAll() {}

  open fun setInLayout() {}

  open fun unsetInLayout() {}

  open fun removeViewListeners() {}
}
