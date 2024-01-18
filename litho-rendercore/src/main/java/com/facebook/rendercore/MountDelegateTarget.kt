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

import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.RenderCoreExtension
import java.util.ArrayList

/** Exposes Some RenderCore [MountState] API to [RenderCoreExtension] */
interface MountDelegateTarget {

  fun notifyMount(id: Long)

  fun notifyUnmount(id: Long)

  fun needsRemount(): Boolean

  fun mount(renderTree: RenderTree)

  fun attach()

  fun detach()

  fun unmountAllItems()

  fun unbindMountItem(mountItem: MountItem)

  fun isRootItem(position: Int): Boolean

  fun getRootHost(): Host

  fun getContentAt(position: Int): Any?

  fun getContentById(id: Long): Any?

  fun unregisterAllExtensions()

  fun getHosts(): ArrayList<Host>

  fun getMountItemAt(position: Int): MountItem?

  /** Returns the total number mount items currently mounted. */
  fun getMountItemCount(): Int

  /** Returns the total number render units in the MountState; mounted and unmounted. */
  fun getRenderUnitCount(): Int

  fun setUnmountDelegateExtension(unmountDelegateExtension: UnmountDelegateExtension<*>)

  fun removeUnmountDelegateExtension()

  fun getMountDelegate(): MountDelegate?

  @Deprecated("Only used for Litho's integration. Marked for removal.")
  fun <Input, State> registerMountExtension(
      mountExtension: MountExtension<Input, State>
  ): ExtensionState<State>

  /** Returns the id for the [RenderState] */
  fun getRenderStateId(): Int
}
