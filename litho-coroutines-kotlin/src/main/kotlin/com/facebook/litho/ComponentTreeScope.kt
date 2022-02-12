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

package com.facebook.litho

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lazily creates a [CoroutineScope] that will be active as long as the [ComponentTree] is not
 * released. This scope will be canceled when the ComponentTree is released.
 */
val ComponentTree.componentTreeScope: CoroutineScope
  get() {
    while (true) {
      val existing = mInternalScopeRef.get() as? CoroutineScope
      if (existing != null) {
        return existing
      }
      val job = SupervisorJob()
      val newScope = ComponentTreeScope(this, job + Dispatchers.Main.immediate)
      if (mInternalScopeRef.compareAndSet(null, newScope)) {
        newScope.register()
        job.invokeOnCompletion { mInternalScopeRef.compareAndSet(newScope, null) }
        return newScope
      }
    }
  }

/**
 * A [CoroutineScope] that is scoped to whether the [ComponentTree] is released.
 *
 * If the tree is already released, this scope will immediately be canceled.
 */
class ComponentTreeScope(
    private val componentTree: ComponentTree,
    override val coroutineContext: CoroutineContext,
) : CoroutineScope {

  init {
    if (componentTree.isReleased) {
      coroutineContext.cancel()
    }
  }

  private fun cancelScopeContext() {
    coroutineContext.cancel()
  }

  fun register() {
    launch(Dispatchers.Main.immediate) {
      if (componentTree.isReleased) {
        cancelScopeContext()
      } else {
        componentTree.addOnReleaseListener { cancelScopeContext() }
      }
    }
  }
}
