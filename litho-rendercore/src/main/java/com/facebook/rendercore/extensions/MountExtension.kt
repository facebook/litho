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

package com.facebook.rendercore.extensions

import android.graphics.Rect
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.MountDelegateTarget
import com.facebook.rendercore.MountState
import com.facebook.rendercore.RenderTreeNode

/**
 * Mount extension which can be registered by a MountState as an extension which can override
 * mounting behaviour. MountState will rely on the extensions registered on the MountDelegate to
 * decide what to mount or unmount. If no extensions are registered on the MountState's delegate, it
 * falls back to its default behaviour.
 *
 * Implementations of MountExtension may also implement extra extension callbacks, to get notified
 * when events for such callbacks are triggered.
 *
 * @see GapWorkerCallbacks
 * @see InformsMountCallback
 * @see OnItemCallbacks
 * @see VisibleBoundsCallbacks
 */
abstract class MountExtension<Input, State> {

  fun createExtensionState(mountDelegate: MountDelegate?): ExtensionState<State> =
      ExtensionState(this, mountDelegate, createState())

  protected abstract fun createState(): State

  /**
   * Called for setting up input on the extension before mounting.
   *
   * @param extensionState The inner state of this extension when beforeMount is called.
   * @param input The new input the extension should use.
   */
  open fun beforeMount(
      extensionState: ExtensionState<State>,
      input: Input?,
      localVisibleRect: Rect?
  ) = Unit

  /** Called immediately after mounting. */
  open fun afterMount(extensionState: ExtensionState<State>) = Unit

  /** Called after all the Host's children have been unmounted. */
  open fun onUnmount(extensionState: ExtensionState<State>) = Unit

  /** Called after all the Host's children have been unbound. */
  open fun onUnbind(extensionState: ExtensionState<State>) = Unit

  val name: String
    get() {
      // This API is primarily used for tracing, and the section names have a char limit of 127.
      // If the class name exceeds that it will be replace by the simple name.
      // In a release build the class name will be minified, so it is unlikely to hit the limit.
      val name = javaClass.name
      return if (name.length > 80) javaClass.simpleName else "<cls>$name</cls>"
    }

  companion object {
    @JvmStatic
    fun getMountTarget(extensionState: ExtensionState<*>): MountDelegateTarget =
        extensionState.mountDelegate.mountDelegateTarget

    @JvmStatic protected fun isRootItem(id: Long): Boolean = id == MountState.ROOT_HOST_ID

    @JvmStatic
    protected fun getContentAt(extensionState: ExtensionState<*>, position: Int): Any? =
        extensionState.mountDelegate.getContentAt(position)

    @JvmStatic
    protected fun getContentById(extensionState: ExtensionState<*>, id: Long): Any? =
        extensionState.mountDelegate.getContentById(id)

    @JvmStatic
    protected fun isLockedForMount(
        extensionState: ExtensionState<*>,
        renderTreeNode: RenderTreeNode
    ): Boolean = isLockedForMount(extensionState, renderTreeNode.renderUnit.id)

    @JvmStatic
    protected fun isLockedForMount(extensionState: ExtensionState<*>, id: Long): Boolean =
        extensionState.mountDelegate.isLockedForMount(id)
  }
}
