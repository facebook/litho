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

import androidx.annotation.UiThread
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.litho.annotations.Hook

/**
 * Registers a callback to perform side-effects when this Component is attached/detached from the
 * tree.
 *
 * The [onAttach] callback will be invoked when the owning Component first becomes part of a
 * committed layout on the ComponentTree on the main thread. It should return a [CleanupFunc]
 * callback to be invoked when the owning Component is detached, e.g. the ComponentTree is released
 * or the Component is no longer part of the tree.
 *
 * Additionally, any time the list of [deps] changes between layouts, the existing onAttach's
 * cleanup callback will be invoked, and the new onAttach callback will be invoked.
 *
 * [deps] should contain any props or state your attach/cleanup callbacks use. For example, if
 * you're using useEffect to subscribe to a data store based on an id you get as a prop, [deps]
 * should include that id so that if the id changes, we will unsubscribe from the old id and
 * subscribe with the new id.
 */
@Hook
fun ComponentScope.useEffect(vararg deps: Any?, onAttach: () -> CleanupFunc?) {
  val entries = useEffectEntries ?: ArrayList()
  useEffectEntries = entries
  val uniqueId = "${context.globalKey}:${entries.size}"
  entries.add(UseEffectAttachable(uniqueId, deps, onAttach))
}

/**
 * Registers a callback to perform side-effects when this Component is attached/detached from the
 * tree.
 *
 * It is an error to call [ComponentScope.useEffect] without deps parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version is not
// used without deps parameters.
@Deprecated(USE_EFFECT_NO_DEPS_ERROR, level = DeprecationLevel.ERROR)
@Suppress("unused", "UNUSED_PARAMETER")
@Hook
fun ComponentScope.useEffect(onAttach: () -> CleanupFunc?): Unit =
    throw IllegalStateException(USE_EFFECT_NO_DEPS_ERROR)

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

private class UseEffectAttachable(
    private val id: String,
    private val deps: Array<out Any?>?,
    private val attachCallback: () -> CleanupFunc?
) : Attachable {

  @ThreadConfined(ThreadConfined.UI) var detachHandler: CleanupFunc? = null
  @ThreadConfined(ThreadConfined.UI) var isAttached: Boolean = false

  override val uniqueId: String = id

  override fun useLegacyUpdateBehavior(): Boolean = false

  @UiThread
  override fun attach() {
    check(!isAttached) { "Attach should only be called when detached!" }
    detachHandler = attachCallback()
    isAttached = true
  }

  @UiThread
  override fun detach() {
    check(isAttached) { "Detach should only be called when attached!" }
    detachHandler?.onCleanup()
    isAttached = false
  }

  @UiThread
  override fun shouldUpdate(nextEntry: Attachable) =
      !deps.contentDeepEquals((nextEntry as UseEffectAttachable).deps)
}

private const val USE_EFFECT_NO_DEPS_ERROR =
    "useEffect must provide 'deps' parameter that determines whether the existing 'onAttach' cleanup callback will be invoked, and the new 'onAttach' callback will be invoked"
