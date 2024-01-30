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
import com.facebook.rendercore.Host
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.Systracer
import java.util.HashSet

class ExtensionState<State>
internal constructor(
    private val _extension: MountExtension<Any?, State>,
    val mountDelegate: MountDelegate,
    val state: State
) {
  private val layoutOutputMountRefs: MutableSet<Long> = HashSet()

  val renderStateId: Int
    get() = mountDelegate.mountDelegateTarget.getRenderStateId()

  val rootHost: Host
    get() = mountDelegate.mountDelegateTarget.getRootHost()

  val extension: MountExtension<*, State>
    get() = _extension

  val tracer: Systracer
    get() = mountDelegate.tracer

  fun releaseAllAcquiredReferences() {
    for (id in layoutOutputMountRefs) {
      mountDelegate.releaseMountRef(id)
    }
    layoutOutputMountRefs.clear()
  }

  fun acquireMountReference(id: Long, isMounting: Boolean) {
    val alreadyOwnedRef = !layoutOutputMountRefs.add(id)
    check(!alreadyOwnedRef) { "Cannot acquire the same reference more than once." }
    if (isMounting) {
      mountDelegate.acquireAndMountRef(id)
    } else {
      mountDelegate.acquireMountRef(id)
    }
  }

  fun releaseMountReference(id: Long, isMounting: Boolean) {
    val ownedRef = layoutOutputMountRefs.remove(id)
    check(ownedRef) { "Trying to release a reference that wasn't acquired." }
    if (isMounting) {
      mountDelegate.releaseAndUnmountRef(id)
    } else {
      mountDelegate.releaseMountRef(id)
    }
  }

  fun ownsReference(id: Long): Boolean = layoutOutputMountRefs.contains(id)

  fun beforeMount(localVisibleRect: Rect?, input: Any?) {
    _extension.beforeMount(this, input, localVisibleRect)
  }

  fun afterMount() {
    _extension.afterMount(this)
  }

  fun onUnbind() {
    _extension.onUnbind(this)
  }

  fun onUnmount() {
    _extension.onUnmount(this)
  }
}
