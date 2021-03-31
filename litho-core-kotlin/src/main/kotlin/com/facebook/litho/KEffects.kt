/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import androidx.annotation.UiThread
import com.facebook.infer.annotation.ThreadConfined

/**
 * Registers a callback to perform side-effects when this Component is attached/detached from the
 * tree.
 *
 * The [onAttach] callback will be invoked when the owning Component first becomes part of a
 * committed layout on the ComponentTree on the main thread. It should return a []CleanupFunc]
 * callback to be invoked when the owning Component is detached, e.g. the ComponentTree is released
 * or the Component is no longer part of the tree.
 *
 * Additionally, any time the list of [deps] changes between layouts, the existing onAttach's
 * cleanup callback will be invoked, and the new onAttach callback will be invoked. **If no deps are
 * provided, this will happen on every layout**.
 *
 * [deps] should contain any props or state your attach/cleanup callbacks use. For example, if
 * you're using useEffect to subscribe to a data store based on an id you get as a prop, [deps]
 * should include that id so that if the id changes, we will unsubscribe from the old id and
 * subscribe with the new id.
 */
fun ComponentScope.useEffect(vararg deps: Any, onAttach: () -> CleanupFunc?) {
  val entries = useEffectEntries ?: ArrayList()
  useEffectEntries = entries
  val uniqueId = "${context.globalKey}:${entries.size}"
  val persistence =
      if (deps.isEmpty()) {
        EffectPersistence.ALWAYS_UPDATE
      } else {
        EffectPersistence.USE_DEPS
      }
  entries.add(UseEffectAttachable(uniqueId, persistence, deps, onAttach))
}

/**
 * Registers a callback to perform side-effects when this Component is attached/detached from the
 * tree. Unlike [useEffect], the onAttach callback will only be invoked once when the Component is
 * attached to the tree and the cleanup callback will only be invoked once when the Component is
 * detached from the tree. They will never be invoked on new layouts.
 *
 * **Note**: it is almost definitely an error to use this variant of useEffect if your callbacks
 * capture and use any props or state. If they do, you should use [useEffect] and pass those
 * captured objects as deps.
 */
fun ComponentScope.usePersistentEffect(onAttach: () -> CleanupFunc?) {
  val entries = useEffectEntries ?: ArrayList()
  useEffectEntries = entries
  val uniqueId = "${context.globalKey}:${entries.size}"
  entries.add(UseEffectAttachable(uniqueId, EffectPersistence.PERSISTENT, null, onAttach))
}

/**
 * Defines a cleanup function to be invoked when the owning Component is detached or the deps in
 * [useEffect] change.
 */
inline fun onCleanup(crossinline cleanupFunc: () -> Unit) = CleanupFunc { cleanupFunc() }

/**
 * Interface for the [onCleanup] function: use [onCleanup] to define the cleanup function for your
 * useEffect hook.
 */
fun interface CleanupFunc {
  fun onCleanup()
}

private enum class EffectPersistence {
  ALWAYS_UPDATE,
  PERSISTENT,
  USE_DEPS,
}

private class UseEffectAttachable(
    private val id: String,
    private val persistence: EffectPersistence,
    private val deps: Array<out Any>?,
    private val attachCallback: () -> CleanupFunc?
) : Attachable {

  @ThreadConfined(ThreadConfined.UI) var detachHandler: CleanupFunc? = null
  @ThreadConfined(ThreadConfined.UI) var isAttached = false

  override fun getUniqueId() = id

  override fun useLegacyUpdateBehavior() = false

  @UiThread
  override fun attach(layoutStateContext: LayoutStateContext) {
    check(!isAttached) { "Attach should only be called when detached!" }
    detachHandler = attachCallback()
    isAttached = true
  }

  @UiThread
  override fun detach(layoutStateContext: LayoutStateContext) {
    check(isAttached) { "Detach should only be called when attached!" }
    detachHandler?.onCleanup()
    isAttached = false
  }

  @UiThread
  override fun shouldUpdate(nextEntry: Attachable) =
      when (persistence) {
        EffectPersistence.ALWAYS_UPDATE -> true
        EffectPersistence.PERSISTENT -> false
        EffectPersistence.USE_DEPS ->
            !deps.contentDeepEquals((nextEntry as UseEffectAttachable).deps)
      }
}
