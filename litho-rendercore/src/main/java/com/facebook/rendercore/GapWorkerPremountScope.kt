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

import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import com.facebook.rendercore.thread.utils.ThreadUtils.assertMainThread

/**
 * Tracks whether the current main-thread call stack is inside an incremental-mount gap-worker
 * premount.
 *
 * The scope is held for the thread rather than per [MountDelegate] because one premount can mount
 * into more than one [MountState]. A unit that hosts nested render trees inflates them
 * synchronously, on their own `MountState` with their own [MountDelegate]. A per-delegate marker
 * would leave that nested work — typically the expensive part of the premount — unattributed.
 */
@UiThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GapWorkerPremountScope {

  var isActive: Boolean = false
    private set

  /**
   * Runs [block] with the scope active, restoring the previous value afterwards so nested scopes
   * behave correctly.
   *
   * Entered once per delegate per frame, so the main-thread assertion costs nothing measurable and
   * keeps this process-global flag from being written off the main thread.
   */
  fun <T> with(block: () -> T): T {
    assertMainThread()
    val wasActive = isActive
    isActive = true
    return try {
      block()
    } finally {
      isActive = wasActive
    }
  }
}
