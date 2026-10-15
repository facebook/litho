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

package com.facebook.rendercore.incrementalmount

import android.graphics.Rect

/** The output object for incremental mount extension */
class IncrementalMountOutput(
    @JvmField val id: Long,
    @JvmField val index: Int,
    bounds: Rect?,
    private var excludeFromIncrementalMount: Boolean,
    val debugKey: String?,
    val hostOutput: IncrementalMountOutput?,
    // Non-null: a null type would never be estimated nor recorded, so it could never learn a cost.
    @JvmField val contentType: Any,
    // Not derived from [hostOutput]: callers can have a host id without a host output.
    @JvmField val hostId: Long,
) {
  var originalBounds: Rect? = null
    private set

  @JvmField val bounds: Rect = Rect(bounds)

  init {
    // TODO: invert API to add child to a host rather than passing the host to the child.
    if (hostOutput != null) {
      ensureHostBounds(this, hostOutput)
    }
  }

  fun excludeFromIncrementalMount(): Boolean {
    return excludeFromIncrementalMount
  }

  companion object {
    const val NO_HOST_ID: Long = -1L

    /** Ensure the host IncrementalMountOuput's bounds are containing the bounds of this item. */
    private fun ensureHostBounds(child: IncrementalMountOutput?, host: IncrementalMountOutput?) {
      if (host == null) {
        return
      }

      val childBounds = requireNotNull(child).bounds
      val hostBounds = host.bounds
      var needsUpdate = false

      if (child.excludeFromIncrementalMount) {
        host.excludeFromIncrementalMount = true
        needsUpdate = true
      }

      if (childBounds.top < hostBounds.top) {
        maybeCacheOriginalBounds(host)
        hostBounds.top = childBounds.top
        needsUpdate = true
      }

      if (childBounds.bottom > hostBounds.bottom) {
        maybeCacheOriginalBounds(host)
        hostBounds.bottom = childBounds.bottom
        needsUpdate = true
      }

      if (childBounds.left < hostBounds.left) {
        maybeCacheOriginalBounds(host)
        hostBounds.left = childBounds.left
        needsUpdate = true
      }

      if (childBounds.right > hostBounds.right) {
        maybeCacheOriginalBounds(host)
        hostBounds.right = childBounds.right
        needsUpdate = true
      }

      if (needsUpdate) {
        ensureHostBounds(host, host.hostOutput)
      }
    }

    /**
     * Expands a host's bounds to contain a child's bounds. Unlike [ensureHostBounds], this does NOT
     * walk up the host chain — it only expands the immediate host. Designed for batch bottom-up
     * finalization where the caller guarantees correct traversal order (children processed before
     * their hosts), making recursive propagation unnecessary.
     */
    @JvmStatic
    fun expandHostToContainChild(
        child: IncrementalMountOutput,
        host: IncrementalMountOutput,
    ) {
      val childBounds = child.bounds
      val hostBounds = host.bounds

      if (child.excludeFromIncrementalMount) {
        host.excludeFromIncrementalMount = true
      }

      if (childBounds.top < hostBounds.top) {
        maybeCacheOriginalBounds(host)
        hostBounds.top = childBounds.top
      }

      if (childBounds.bottom > hostBounds.bottom) {
        maybeCacheOriginalBounds(host)
        hostBounds.bottom = childBounds.bottom
      }

      if (childBounds.left < hostBounds.left) {
        maybeCacheOriginalBounds(host)
        hostBounds.left = childBounds.left
      }

      if (childBounds.right > hostBounds.right) {
        maybeCacheOriginalBounds(host)
        hostBounds.right = childBounds.right
      }
    }

    private fun maybeCacheOriginalBounds(output: IncrementalMountOutput) {
      if (output.originalBounds == null) {
        output.originalBounds = Rect(output.bounds)
      }
    }
  }
}
