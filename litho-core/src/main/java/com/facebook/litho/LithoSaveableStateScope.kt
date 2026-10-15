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

@file:OptIn(com.facebook.litho.annotations.ExperimentalLithoApi::class)

package com.facebook.litho

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import com.facebook.litho.annotations.ExperimentalLithoApi

/**
 * A lifecycle-backed namespace for state declared with [useSaveableState].
 *
 * Obtain a scope with [lithoSaveableStateScope] and supply it to a root ComponentTree with
 * [ComponentTree.Builder.withSaveableStateScope]. Use a different key for each independently saved
 * tree owned by the same Fragment or Activity. A scope may be reused by a replacement tree only
 * after the previous tree is released; it must not back multiple live root trees concurrently.
 * Release every tree using this scope before calling [close]. Closing permanently discards the
 * scope's state, so creating another scope with the same key starts with fresh state.
 */
@ExperimentalLithoApi
class LithoSaveableStateScope
internal constructor(
    internal val registry: LithoSaveableStateRegistry,
    private var owner: SavedStateRegistryOwner?,
    private val providerKey: String,
    private val valueClassLoader: ClassLoader,
) : AutoCloseable {

  internal fun requireActiveRegistry(): LithoSaveableStateRegistry {
    val currentOwner = owner
    check(currentOwner != null) { "Cannot attach a closed LithoSaveableStateScope" }
    check(currentOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
      "Cannot attach a LithoSaveableStateScope after its Android owner was destroyed. Close the " +
          "scope after releasing its ComponentTree."
    }
    return registry
  }

  internal fun requireCompatibleClassLoader(classLoader: ClassLoader) {
    check(valueClassLoader === classLoader) {
      "A LithoSaveableStateScope must use the same host Context class loader for every access"
    }
  }

  /** Unregisters this scope and permanently clears its state after its tree has been released. */
  @MainThread
  override fun close() {
    ThreadUtils.assertMainThread()
    val currentOwner = owner ?: return
    currentOwner.savedStateRegistry.unregisterSavedStateProvider(providerKey)
    owner = null
    registry.close()
  }
}

/** Supplies the lifecycle-backed scope used by [useSaveableState] in this tree. */
@ExperimentalLithoApi
@MainThread
fun ComponentTree.Builder.withSaveableStateScope(
    scope: LithoSaveableStateScope,
): ComponentTree.Builder {
  ThreadUtils.assertMainThread()
  return withLithoSaveableStateRegistry(scope.requireActiveRegistry())
}

/**
 * Creates a lazily initialized saveable-state scope owned by this Fragment.
 *
 * Access the property after the Fragment's saved state has been restored, such as from
 * `onCreateView()` or later. Release its ComponentTree before closing the scope in `onDestroy()`;
 * do not close it in `onDestroyView()` when the Fragment will retain the logical state scope. If
 * tree creation is conditional, guard cleanup with the same condition so cleanup does not
 * initialize an unused scope.
 */
@ExperimentalLithoApi
fun Fragment.lithoSaveableStateScope(key: String): Lazy<LithoSaveableStateScope> =
    lazy(LazyThreadSafetyMode.NONE) {
      ThreadUtils.assertMainThread()
      val hostContext =
          checkNotNull(context) {
            "A Litho saveable-state scope must be accessed while its Fragment is attached"
          }
      getOrCreateLithoSaveableStateScope(
          this,
          key,
          hostContext.classLoader,
      )
    }

/**
 * Creates a lazily initialized saveable-state scope owned by this Activity.
 *
 * Access the property after `super.onCreate()` has restored the Activity's saved state. Release its
 * ComponentTree before closing the scope in `onDestroy()`. If tree creation is conditional, guard
 * cleanup with the same condition so cleanup does not initialize an unused scope.
 */
@ExperimentalLithoApi
fun ComponentActivity.lithoSaveableStateScope(key: String): Lazy<LithoSaveableStateScope> =
    lazy(LazyThreadSafetyMode.NONE) {
      ThreadUtils.assertMainThread()
      getOrCreateLithoSaveableStateScope(this, key, classLoader)
    }

@MainThread
internal fun getOrCreateLithoSaveableStateScope(
    owner: SavedStateRegistryOwner,
    key: String,
    valueClassLoader: ClassLoader,
): LithoSaveableStateScope {
  require(key.isNotEmpty()) { "A Litho saveable-state scope key cannot be empty" }
  validateOwner(owner, key)

  val providerKey = "$LITHO_SAVEABLE_STATE_SCOPE_KEY_PREFIX$key"
  val existingProvider = owner.savedStateRegistry.getSavedStateProvider(providerKey)
  if (existingProvider != null) {
    check(existingProvider is LithoSaveableStateProvider) {
      "[$key] The Android saved-state provider key is already used by a non-Litho provider"
    }
    existingProvider.scope.requireCompatibleClassLoader(valueClassLoader)
    return existingProvider.scope
  }

  val scope = LithoSaveableStateScope(
      registry =
          LithoSaveableStateRegistry(
              restoredState = owner.savedStateRegistry.consumeRestoredStateForKey(providerKey),
              valueClassLoader = valueClassLoader,
          ),
      owner = owner,
      providerKey = providerKey,
      valueClassLoader = valueClassLoader,
  )
  val provider = LithoSaveableStateProvider(scope)
  owner.savedStateRegistry.registerSavedStateProvider(providerKey, provider)
  return scope
}

private class LithoSaveableStateProvider(val scope: LithoSaveableStateScope) :
    SavedStateRegistry.SavedStateProvider {

  override fun saveState() = scope.registry.snapshot()
}

private fun validateOwner(owner: SavedStateRegistryOwner, key: String) {
  check(owner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
    "[$key] Cannot create a Litho saveable-state scope after its Android owner was destroyed"
  }
  check(owner.savedStateRegistry.isRestored) {
    "[$key] A Litho saveable-state scope must be accessed after the Android owner's saved state " +
        "is restored"
  }
}

private const val LITHO_SAVEABLE_STATE_SCOPE_KEY_PREFIX =
    "com.facebook.litho.LithoSaveableStateScope:"
