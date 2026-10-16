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

package com.facebook.litho.rangemanager

import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleOwner
import com.facebook.litho.AOSPLifecycleOwnerProvider
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.LithoVisibilityEventsListener
import com.facebook.rendercore.thread.utils.ThreadUtils

/**
 * The visibility events controller of one tree a [LithoCollectionItem] builds: it forwards
 * [parent]'s events until the item [destroy]s it along with the tree, the way a `RecyclerBinder`
 * gives each of its trees a controller of its own.
 *
 * Everything nested in the tree inherits its controller, and a nested collection keeps its item
 * trees until that controller is destroyed, so a tree subscribed to [parent] directly would keep
 * them for as long as [parent] lives.
 *
 * Its state is [parent]'s until destroyed, read live rather than copied, so a tree built off the
 * main thread while [parent] changes still starts in [parent]'s current state.
 */
internal class TreeVisibilityEventsController
private constructor(private val parent: LithoVisibilityEventsController) :
    LithoVisibilityEventsController, LithoVisibilityEventsListener, AOSPLifecycleOwnerProvider {

  /** Guarded by this. */
  private val listeners = HashSet<LithoVisibilityEventsListener>()

  @Volatile private var isDestroyed = false

  override val visibilityState: LithoVisibilityState
    get() = if (isDestroyed) LithoVisibilityState.DESTROYED else parent.visibilityState

  override val lifecycleOwner: LifecycleOwner?
    get() = (parent as? AOSPLifecycleOwnerProvider)?.lifecycleOwner

  override fun onMovedToState(state: LithoVisibilityState) {
    moveToVisibilityState(state)
  }

  @UiThread
  override fun moveToVisibilityState(newVisibilityState: LithoVisibilityState) {
    if (newVisibilityState == LithoVisibilityState.DESTROYED) {
      destroy()
      return
    }
    val toNotify = synchronized(this) { if (isDestroyed) return else listeners.toList() }
    toNotify.forEach { it.onMovedToState(newVisibilityState) }
  }

  /**
   * Ignored once destroyed: nothing would ever tell the listener, so holding it would only keep it
   * alive. A tree built while the parent is torn down subscribes after the destroy.
   */
  @AnyThread
  override fun addListener(listener: LithoVisibilityEventsListener) {
    synchronized(this) {
      if (!isDestroyed) {
        listeners.add(listener)
      }
    }
  }

  @AnyThread
  override fun removeListener(listener: LithoVisibilityEventsListener) {
    synchronized(this) { listeners.remove(listener) }
  }

  /** Releases everything subscribed to this controller and stops listening to [parent]. */
  @UiThread
  fun destroy() {
    ThreadUtils.assertMainThread()
    val toNotify =
        synchronized(this) {
          if (isDestroyed) {
            return
          }
          isDestroyed = true
          listeners.toList().also { listeners.clear() }
        }
    parent.removeListener(this)
    toNotify.forEach { it.onMovedToState(LithoVisibilityState.DESTROYED) }
  }

  /** Stops listening to [parent] without destroying anything, for a tree that was never built. */
  @AnyThread
  fun unsubscribe() {
    parent.removeListener(this)
  }

  companion object {
    /** A controller already listening to [parent]. Trees can be built on any thread. */
    @AnyThread
    fun childOf(parent: LithoVisibilityEventsController): TreeVisibilityEventsController =
        TreeVisibilityEventsController(parent).also { parent.addListener(it) }
  }
}
