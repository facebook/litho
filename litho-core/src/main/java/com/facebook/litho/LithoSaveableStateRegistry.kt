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

import android.os.Bundle
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Registry of component state that can be snapshotted into Android saved state.
 *
 * Android's [androidx.savedstate.SavedStateRegistry] is owned and accessed by a lifecycle host on
 * the main thread. Litho may resolve components on multiple background threads, so a host restores
 * one [Bundle] into this registry and registers this registry's [snapshot] as one Android saved
 * state provider. Components then register value providers without directly accessing the Android
 * registry.
 *
 * This type is thread-safe. Provider code is evaluated outside the registry lock, while each
 * individual provider is serialized to prevent concurrent saver calls. The owner defines the
 * registry lifetime and must close it when the corresponding tree or navigation entry is
 * permanently removed.
 *
 * @param restoredState a snapshot produced by a previous registry instance and restored through
 *   Android's saved-state mechanism, or `null` for a new state scope.
 * @param valueClassLoader the host [android.content.Context] class loader used to deserialize
 *   custom values from the restored Bundle. Hosts should pass `Context.getClassLoader()`; `null`
 *   uses the Bundle's default class loader.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LithoSaveableStateRegistry
private constructor(
    restoredState: Bundle? = null,
    private val valueClassLoader: ClassLoader? = null,
    private val parentRegistry: LithoSaveableStateRegistry? = null,
    private val parentStorageKey: String? = null,
) : AutoCloseable {
  private val lock = Any()
  private val parentConnectionLock = Any()

  @GuardedBy("parentConnectionLock") private var parentStateRestored = false

  @GuardedBy("parentConnectionLock") private var parentRegistration: Registration? = null

  @GuardedBy("lock")
  private val retainedValues =
      Bundle(restoredState ?: Bundle()).apply {
        classLoader = valueClassLoader
      }

  @GuardedBy("lock") private val providers = mutableMapOf<String, Provider>()

  @GuardedBy("lock") private var closed = false

  constructor(
      restoredState: Bundle? = null,
      valueClassLoader: ClassLoader? = null,
  ) : this(restoredState, valueClassLoader, null, null)

  /** Returns restored state without consuming it during a potentially speculative resolve. */
  internal fun restoredValue(key: String): RestoredSaveableState? {
    restoreParentStateIfNeeded()
    return synchronized(lock) {
      checkOpen()
      try {
        if (!retainedValues.containsKey(key)) {
          return@synchronized null
        }
        val retainedValue = retainedValues.getBundle(key)
        if (retainedValue == null) {
          discardRetainedValue(key)
          return@synchronized null
        }
        val valueBundle = Bundle(retainedValue)
        valueBundle.classLoader = valueClassLoader
        @Suppress("DEPRECATION") RestoredSaveableState(valueBundle.get(VALUE_KEY))
      } catch (_: RuntimeException) {
        discardRetainedValue(key)
        null
      }
    }
  }

  /**
   * Registers the value provider for a committed hook and consumes any previously restored value.
   */
  internal fun registerProvider(key: String, provider: () -> Any?): Registration {
    restoreParentStateIfNeeded()
    val registeredProvider = Provider(provider)
    // Validate the value now so unsupported types fail at registration instead of state saving.
    registeredProvider.savedValue(key)
    val previousRetainedValue: Bundle?

    synchronized(lock) {
      // The registry may close or another provider may claim this key during validation.
      checkOpen()
      check(!providers.containsKey(key)) {
        "A saveable-state provider is already registered for key '$key'"
      }
      providers[key] = registeredProvider
      previousRetainedValue =
          try {
            retainedValues.getBundle(key)?.let(::Bundle)
          } catch (_: RuntimeException) {
            null
          }
      discardRetainedValue(key)
    }

    try {
      registerWithParentIfNeeded()
    } catch (exception: RuntimeException) {
      rollbackRegistration(key, registeredProvider, previousRetainedValue)
      throw exception
    }
    return Registration(this, key, registeredProvider)
  }

  /**
   * Returns a Bundle containing retained values and the latest value from every active provider.
   */
  fun snapshot(): Bundle {
    restoreParentStateIfNeeded()
    return snapshotLocal()
  }

  private fun snapshotLocal(): Bundle {
    val (snapshot, activeProviders) =
        synchronized(lock) {
          checkOpen()
          copyRetainedValues() to providers.toList()
        }
    activeProviders.forEach { (key, provider) ->
      snapshot.putBundle(key, provider.savedValue(key))
    }
    return snapshot
  }

  /**
   * Retains active values before an owner replaces its ComponentTree.
   *
   * Ordinary provider removal discards the value so dynamic components cannot accumulate retained
   * state for the entire registry lifetime. Tree and navigation owners call this operation only
   * when the same logical state scope will continue in a replacement tree.
   */
  internal fun retainActiveValues() {
    restoreParentStateIfNeeded()
    val activeProviders =
        synchronized(lock) {
          checkOpen()
          providers.toList()
        }
    val values = activeProviders.map { (key, provider) ->
      Triple(key, provider, provider.savedValue(key))
    }

    synchronized(lock) {
      if (closed) return
      values.forEach { (key, provider, value) ->
        val currentProvider = providers[key]
        if (currentProvider === provider) {
          retainedValues.putBundle(key, value)
        }
      }
    }
  }

  /** Clears this registry and rejects future registrations, restored-state reads, and snapshots. */
  override fun close() {
    val registration =
        synchronized(parentConnectionLock) {
          parentRegistration.also { parentRegistration = null }
        }
    registration?.close()

    synchronized(lock) {
      if (closed) return
      closed = true
      providers.clear()
      retainedValues.clear()
    }
  }

  /** Creates an independently keyed registry whose snapshot is owned by this registry. */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun createChildRegistry(
      key: String?,
  ): LithoSaveableStateRegistry = LithoSaveableStateRegistry(
      valueClassLoader = valueClassLoader,
      parentRegistry = this,
      parentStorageKey = key?.let { "$CHILD_REGISTRY_PREFIX$it" },
  )

  private fun restoreParentStateIfNeeded() {
    val parent = parentRegistry ?: return
    synchronized(parentConnectionLock) {
      if (parentStateRestored) return
      val storageKey = requireParentStorageKey()
      val restoredState = parent.restoredValue(storageKey)?.value as? Bundle
      if (restoredState != null) {
        synchronized(lock) {
          checkOpen()
          retainedValues.putAll(Bundle(restoredState).apply { classLoader = valueClassLoader })
        }
      }
      parentStateRestored = true
    }
  }

  private fun registerWithParentIfNeeded() {
    val parent = parentRegistry ?: return
    restoreParentStateIfNeeded()
    synchronized(parentConnectionLock) {
      if (parentRegistration != null) return
      val storageKey = requireParentStorageKey()
      parentRegistration = parent.registerProvider(storageKey, ::snapshotLocal)
    }
  }

  private fun requireParentStorageKey(): String =
      checkNotNull(parentStorageKey) {
        "A nested ComponentTree using useSaveableState needs a stable identity. Give the " +
            "nested tree a stable item key or configure its ComponentTree with " +
            "withNestedTreeSaveableStateKey()."
      }

  private fun unregister(key: String, provider: Provider) {
    synchronized(lock) {
      if (closed) return
      if (providers[key] === provider) {
        providers.remove(key)
      }
    }
  }

  private fun rollbackRegistration(
      key: String,
      provider: Provider,
      previousRetainedValue: Bundle?,
  ) {
    synchronized(lock) {
      if (closed || providers[key] !== provider) return
      providers.remove(key)
      if (previousRetainedValue != null) {
        retainedValues.putBundle(key, previousRetainedValue)
      }
    }
  }

  @GuardedBy("lock")
  private fun checkOpen() {
    check(!closed) { "Cannot access saveable state after its Litho scope was cleared" }
  }

  @GuardedBy("lock")
  private fun copyRetainedValues(): Bundle {
    val snapshot = Bundle()
    snapshot.classLoader = valueClassLoader
    val keys =
        try {
          retainedValues.keySet().toList()
        } catch (_: RuntimeException) {
          return snapshot
        }
    keys.forEach { key ->
      try {
        val retainedValue = retainedValues.getBundle(key)
        if (retainedValue == null) {
          discardRetainedValue(key)
          return@forEach
        }
        val copiedValue = Bundle(retainedValue)
        copiedValue.classLoader = valueClassLoader
        snapshot.putBundle(key, copiedValue)
      } catch (_: RuntimeException) {
        discardRetainedValue(key)
      }
    }
    return snapshot
  }

  @GuardedBy("lock")
  private fun discardRetainedValue(key: String) {
    try {
      retainedValues.remove(key)
    } catch (_: RuntimeException) {}
  }

  private fun valueBundle(key: String, value: Any?): Bundle {
    try {
      val bundle = bundleOf(VALUE_KEY to value)
      bundle.classLoader = valueClassLoader
      return bundle
    } catch (exception: IllegalArgumentException) {
      throw IllegalArgumentException(
          "Saveable-state provider for key '$key' returned unsupported value type " +
              "'${value?.javaClass?.name ?: "null"}'. Provide a Saver that produces a " +
              "Bundle-compatible value.",
          exception,
      )
    }
  }

  internal inner class Provider(private val value: () -> Any?) {
    private val lock = Any()

    fun savedValue(key: String): Bundle = synchronized(lock) { valueBundle(key, value()) }
  }

  internal class Registration(
      private val registry: LithoSaveableStateRegistry,
      private val key: String,
      private val provider: Provider,
  ) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
      if (closed.compareAndSet(false, true)) {
        registry.unregister(key, provider)
      }
    }
  }

  private companion object {
    const val CHILD_REGISTRY_PREFIX: String = "litho-child-registry:"
    const val VALUE_KEY: String = "value"
  }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RestoredSaveableState internal constructor(val value: Any?)
