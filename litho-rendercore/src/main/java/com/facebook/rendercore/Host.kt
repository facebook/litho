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

abstract class Host(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

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

  /** @return number of [MountItem]s that are currently mounted in the host. */
  abstract val mountItemCount: Int

  /** @return the [MountItem] that was mounted with the given index. */
  abstract fun getMountItemAt(index: Int): MountItem

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
