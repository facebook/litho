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

import androidx.annotation.RestrictTo
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import java.util.concurrent.atomic.AtomicReference

/** Converts a value to and from a representation supported by Android saved state. */
@ExperimentalLithoApi
interface Saver<T> {
  fun save(value: T): Any?

  fun restore(value: Any?): T
}

/** Creates a [Saver] from its save and restore operations. */
@ExperimentalLithoApi
fun <T> Saver(save: (T) -> Any?, restore: (Any?) -> T): Saver<T> =
    object : Saver<T> {
      override fun save(value: T): Any? = save.invoke(value)

      override fun restore(value: Any?): T = restore.invoke(value)
    }

/**
 * Declares component state that can be restored by a lifecycle-backed host.
 *
 * The owning registry, component identity, hook call site, and optional [key] identify the value.
 * The [initializer] runs when no value has been retained or restored for that identity. Values
 * captured by [initializer] are initial inputs rather than observable dependencies; use [key] to
 * represent a different logical state owner.
 *
 * When this component leaves the committed tree, its provider is removed and its value is not
 * retained by this hook. A lifecycle or navigation host that replaces the whole ComponentTree can
 * retain the tree's registry explicitly so active values survive into the replacement tree.
 *
 * The [saver] must encode the value into a type supported by Android [android.os.Bundle]. The
 * default saver stores Bundle-compatible values directly. The factory and saver may run on a Litho
 * resolve thread and must not access Views or other main-thread-only objects.
 */
@ExperimentalLithoApi
@Hook
fun <T> ComponentScope.useSaveableState(
    key: String? = null,
    saver: Saver<T> = autoSaver(),
    initializer: () -> T,
): State<T> {
  val registry =
      checkNotNull(LithoSaveableStateRegistryTreeProp.value) {
        "No LithoSaveableStateRegistry is available for the current ComponentTree. " +
            "Create this tree through a lifecycle-backed Litho host that supplies saveable-state " +
            "ownership."
      }
  val hookLocalKey = requireCurrentHookKey()
  val storageKey = saveableStateKey(
      globalKey = context.globalKey,
      hookLocalKey = hookLocalKey,
      explicitKey = key,
      componentName = context.componentScope?.simpleName ?: "unknown component",
  )
  val state =
      useStateWithDeps(storageKey) {
        val restored = registry.restoredValue(storageKey)
        if (restored == null) initializer() else saver.restore(restored.value)
      }
  val latestProvider = useState { AtomicReference<() -> Any?>({ saver.save(state.value) }) }.value

  useEffect(saver, state) {
    latestProvider.set { saver.save(state.value) }
    null
  }

  useEffect(registry, storageKey) {
    val registration = registry.registerProvider(storageKey) { latestProvider.get().invoke() }
    onCleanup(registration::close)
  }

  return state
}

@JvmField
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LithoSaveableStateRegistryTreeProp: TreeProp<LithoSaveableStateRegistry?> = treePropOf { null }

private object AutoSaver : Saver<Any?> {
  override fun save(value: Any?): Any? = value

  override fun restore(value: Any?): Any? = value
}

@Suppress("UNCHECKED_CAST") private fun <T> autoSaver(): Saver<T> = AutoSaver as Saver<T>
