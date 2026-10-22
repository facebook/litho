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

import androidx.annotation.GuardedBy
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.facebook.litho.annotations.Hook

/**
 * Keeps the [useSaveableState] values of content that leaves the tree for a while, such as the
 * destinations on a navigation back stack.
 *
 * Each key, for example a back-stack entry ID, gets its own registry. While the content for a key
 * is rendered through [SaveableStateProvider], its values are live. [retain] keeps them after the
 * content leaves the tree, and the next [SaveableStateProvider] for the key restores them. They are
 * saved with the tree, so they also come back after recreation and process death. [removeState]
 * drops them. Plain `useState` values are not kept.
 *
 * ```
 * // Push: keep the covered destination's values, then show the new one.
 * holder.retain(current.id)
 * backStack.update { it + next }
 *
 * // Render: only the top destination is in the tree.
 * holder.SaveableStateProvider(top.id) { top.render() }
 *
 * // After a pop commits: the popped destination will not come back.
 * holder.removeState(popped.id)
 * ```
 *
 * Without a saveable-state registry above it, the holder keeps the values in memory for the
 * lifetime of its component.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LithoSaveableStateHolder
internal constructor(private val registry: LithoSaveableStateRegistry) {
  private val lock = Any()

  @GuardedBy("lock") private val children = mutableMapOf<String, LithoSaveableStateRegistry>()

  /** Renders [content] with the values stored under [key]. Rendering two keys at once is fine. */
  @Suppress("FunctionName")
  fun SaveableStateProvider(key: String, content: () -> Component): Component =
      key(key) {
        TreePropProvider(LithoSaveableStateRegistryTreeProp to registryFor(key)) { content() }
      }

  /**
   * Keeps the current values under [key] after its content leaves the tree. Call it on the main
   * thread before the update that removes the content, while its values are still registered.
   */
  @MainThread
  fun retain(key: String) {
    synchronized(lock) { children[key] }?.retainActiveValues()
  }

  /**
   * Drops the values stored under [key]. Call it after the update that removed the content commits,
   * so a render still in flight cannot register the values again.
   */
  @MainThread
  fun removeState(key: String) {
    synchronized(lock) { children.remove(key) }?.close()
    registry.discardChildState(key)
  }

  internal fun close() {
    val removed = synchronized(lock) { children.values.toList().also { children.clear() } }
    removed.forEach(LithoSaveableStateRegistry::close)
    registry.close()
  }

  internal fun registryFor(key: String): LithoSaveableStateRegistry =
      synchronized(lock) { children.getOrPut(key) { registry.createChildRegistry(key) } }
}

/**
 * Returns a [LithoSaveableStateHolder] saved with this component, like a [useSaveableState] value.
 * Removing the component drops every value the holder kept.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Hook
fun ComponentScope.useSaveableStateHolder(): LithoSaveableStateHolder {
  val parent = LithoSaveableStateRegistryTreeProp.value
  val storageKey = saveableStateKey(
      globalKey = context.globalKey,
      hookLocalKey = requireCurrentHookKey(),
      explicitKey = null,
      componentName = context.componentScope?.simpleName ?: "unknown component",
  )
  val holder =
      useStateWithDeps(parent, storageKey) {
            LithoSaveableStateHolder(
                parent?.createChildRegistry(storageKey) ?: LithoSaveableStateRegistry(),
            )
          }
          .value
  useEffect(holder) { onCleanup(holder::close) }
  return holder
}
