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
import androidx.lifecycle.ViewModel
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Declares a [ViewModel] scoped to the store supplied by the tree's host. Root trees supply a
 * [LithoViewModelStore] through [ComponentTree.Builder.withLithoViewModelStore], while nested trees
 * receive their current inherited store through TreeProps.
 *
 * Without a key, one instance of [T] is retained for the owning store:
 * ```
 * val model by useViewModel {
 *   ProfileViewModel(repository)
 * }
 * ```
 *
 * Use an explicit key when the same owner needs multiple instances of one ViewModel class:
 * ```
 * val model by useViewModel(key = userId) {
 *   ProfileViewModel(userId, repository)
 * }
 * ```
 *
 * The same class and key resolve the same ViewModel across components that share the store, and a
 * different key creates a different instance. Component identity and props are intentionally
 * excluded. Values captured by [factory] are creation arguments rather than observable inputs;
 * include changing identity in [key] or update the retained ViewModel explicitly. The factory may
 * run on a Litho resolve thread and must not access Views or other main-thread-only objects.
 * Reusable components that need independent instances of the same ViewModel class must receive a
 * stable key from their owner.
 *
 * Releasing the ComponentTree does not clear an owner-backed store. The Android owner clears its
 * models when its lifecycle scope ends.
 */
@ExperimentalLithoApi
@Hook
inline fun <reified T : ViewModel> ComponentScope.useViewModel(
    key: String? = null,
    noinline factory: () -> T,
): ReadOnlyProperty<Any?, T> = useViewModelWithClass(T::class, key, factory)

@PublishedApi
internal fun <T : ViewModel> ComponentScope.useViewModelWithClass(
    modelClass: KClass<T>,
    key: String? = null,
    factory: () -> T,
): ReadOnlyProperty<Any?, T> {
  val store =
      checkNotNull(LithoViewModelStoreTreeProp.value) {
        "No ViewModelStore is available for useViewModel in this ComponentTree. " +
            "Configure the root ComponentTree with " +
            "ComponentTree.Builder.withLithoViewModelStore(owner.getLithoViewModelStore()). " +
            "Nested ComponentTrees inherit their parent's store automatically."
      }
  return LithoViewModelDelegate(store.getOrCreate(modelClass, key, factory))
}

@JvmField
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LithoViewModelStoreTreeProp: TreeProp<LithoViewModelStore?> = treePropOf { null }

private class LithoViewModelDelegate<T : ViewModel>(
    private val viewModel: T,
) : ReadOnlyProperty<Any?, T> {
  override fun getValue(thisRef: Any?, property: KProperty<*>): T = viewModel
}
