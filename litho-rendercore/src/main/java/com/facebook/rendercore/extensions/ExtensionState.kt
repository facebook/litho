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
import androidx.collection.MutableLongSet
import com.facebook.rendercore.Host
import com.facebook.rendercore.MountDelegate
import com.meta.systrace.Systracer

class ExtensionState<State>
internal constructor(
    private val _extension: MountExtension<Any?, State>,
    val mountDelegate: MountDelegate,
    val state: State,
) {
  private val layoutOutputMountRefs: MutableLongSet? = MutableLongSet()

  val renderStateId: Int
    get() = mountDelegate.mountDelegateTarget.getRenderStateId()

  val rootHost: Host
    get() = mountDelegate.mountDelegateTarget.getRootHost()

  val extension: MountExtension<*, State>
    get() = _extension

  val tracer: Systracer
    get() = mountDelegate.tracer

  fun releaseAllAcquiredReferences() {
    val refs = checkNotNull(layoutOutputMountRefs)
    try {
      refs.forEach { id -> mountDelegate.releaseMountRef(id) }
    } finally {
      // Always clear the ownership set, even if a releaseMountRef throws. We still fail fast (the
      // exception propagates), but without this a single failed release would leave the set
      // populated, so every subsequent beforeMount/onUnbind/onUnmount would re-iterate it and
      // re-throw on the same id — turning a one-shot failure into a permanently crash-looping host.
      refs.clear()
    }
  }

  fun acquireMountReference(id: Long, isMounting: Boolean) {
    val alreadyOwnedRef = !checkNotNull(layoutOutputMountRefs).add(id)
    check(!alreadyOwnedRef) { "Cannot acquire the same reference more than once." }
    if (isMounting) {
      mountDelegate.acquireAndMountRef(id)
    } else {
      mountDelegate.acquireMountRef(id)
    }
  }

  fun releaseMountReference(id: Long, isMounting: Boolean) {
    val ownedRef = checkNotNull(layoutOutputMountRefs).remove(id)
    check(ownedRef) { "Trying to release a reference that wasn't acquired." }
    if (isMounting) {
      mountDelegate.releaseAndUnmountRef(id)
    } else {
      mountDelegate.releaseMountRef(id)
    }
  }

  fun ownsReference(id: Long): Boolean = checkNotNull(layoutOutputMountRefs).contains(id)

  fun beforeMount(localVisibleRect: Rect?, input: Any?) {
    _extension.beforeMount(this, input, localVisibleRect)
  }

  fun afterMount() {
    _extension.afterMount(this)
  }

  fun afterMount(reason: VisibilityChangeReason) {
    _extension.afterMount(this, reason)
  }

  fun onUnbind() {
    _extension.onUnbind(this)
  }

  fun onBeforeUnmount() {
    _extension.onBeforeUnmount(this)
  }

  fun onUnmount() {
    _extension.onUnmount(this)
  }
}
