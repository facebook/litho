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
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.rendercore.LogLevel
import java.lang.ref.WeakReference
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
 * registry lifetime and closes it when the corresponding tree or navigation entry is permanently
 * removed. A closed registry is inert: calculations that still hold it read no restored values,
 * register nothing, and snapshot nothing.
 *
 * A child registry created without a key cannot be told apart from its siblings, so it keeps its
 * values only for its own lifetime and never reads or writes its parent's saved state. The same
 * applies to two open children with the same key: from the moment the second one reads, the key is
 * neither restored nor saved for the rest of the parent's lifetime.
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
    private val claimRootState: (() -> Bundle?)? = null,
) : AutoCloseable {
  private val lock = Any()
  private val parentConnectionLock = Any()

  @GuardedBy("parentConnectionLock") private var parentStateRestored = false

  @GuardedBy("parentConnectionLock") private var rootStateClaimed = false

  @GuardedBy("parentConnectionLock") private var parentRegistration: Registration? = null

  @GuardedBy("parentConnectionLock") private var parentKeyAmbiguous = false

  @GuardedBy("lock")
  private val retainedValues =
      Bundle(restoredState ?: Bundle()).apply {
        classLoader = valueClassLoader
      }

  @GuardedBy("lock") private val providers = mutableMapOf<String, Provider>()

  /** The open child holding each child key. Weak, so a child dropped without closing frees it. */
  @GuardedBy("lock")
  private val childKeyHolders = mutableMapOf<String, WeakReference<LithoSaveableStateRegistry>>()

  @GuardedBy("lock") private val ambiguousChildKeys = mutableSetOf<String>()

  /** Keys two live providers registered at once; values under them are no longer saved. */
  @GuardedBy("lock") private val ambiguousKeys = mutableSetOf<String>()

  @GuardedBy("lock") private var closed = false

  constructor(
      restoredState: Bundle? = null,
      valueClassLoader: ClassLoader? = null,
  ) : this(restoredState, valueClassLoader, null, null, null)

  /**
   * Returns retained values and the latest value of every provider, for the owning scope's save.
   */
  internal fun snapshotForOwner(): Bundle = snapshotLocal()

  /** Returns restored state without consuming it during a potentially speculative resolve. */
  internal fun restoredValue(key: String): RestoredSaveableState? {
    restoreParentStateIfNeeded()
    return synchronized(lock) { restoredValueLocked(key) }
  }

  @GuardedBy("lock")
  private fun restoredValueLocked(key: String): RestoredSaveableState? {
    if (closed) return null
    return try {
      if (!retainedValues.containsKey(key)) return null
      val retainedValue = retainedValues.getBundle(key)
      if (retainedValue == null) {
        discardRetainedValue(key)
        return null
      }
      val valueBundle = Bundle(retainedValue)
      valueBundle.classLoader = valueClassLoader
      @Suppress("DEPRECATION") RestoredSaveableState(valueBundle.get(VALUE_KEY))
    } catch (_: RuntimeException) {
      discardRetainedValue(key)
      null
    }
  }

  /**
   * Registers the value provider for a committed hook and consumes any previously restored value.
   *
   * Two live providers with one key can't be told apart, as when separate trees share this
   * registry. Then neither is saved and the key's restored value is dropped, as with an ambiguous
   * child key: both hooks keep working like `useState`, and the first clash is reported.
   */
  internal fun registerProvider(key: String, provider: () -> Any?): Registration {
    restoreParentStateIfNeeded()
    val registeredProvider = Provider(provider)
    if (synchronized(lock) { closed }) return Registration(this, key, registeredProvider)
    // Validate the value now so unsupported types fail at registration instead of state saving.
    registeredProvider.savedValue(key)
    var previousRetainedValue: Bundle? = null
    var duplicate = false
    var firstDuplicate = false

    synchronized(lock) {
      // The registry may close or another provider may claim this key during validation.
      if (closed) return Registration(this, key, registeredProvider)
      if (key in ambiguousKeys || providers.containsKey(key)) {
        duplicate = true
        firstDuplicate = ambiguousKeys.add(key)
        providers.remove(key)
        discardRetainedValue(key)
      } else {
        providers[key] = registeredProvider
        previousRetainedValue =
            try {
              retainedValues.getBundle(key)?.let(::Bundle)
            } catch (_: RuntimeException) {
              null
            }
        discardRetainedValue(key)
      }
    }
    if (duplicate) {
      if (firstDuplicate) reportDuplicateKey(key)
      return Registration(this, key, registeredProvider)
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
          if (closed) {
            return Bundle().apply { classLoader = valueClassLoader }
          }
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
          if (closed) return
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

  /** Clears this registry; later registrations, restored-state reads, and snapshots are no-ops. */
  override fun close() {
    val registration =
        synchronized(parentConnectionLock) {
          synchronized(lock) {
            closed = true
            providers.clear()
            retainedValues.clear()
            childKeyHolders.clear()
          }
          parentRegistration.also { parentRegistration = null }
        }
    registration?.close()
    parentStorageKey?.let { key -> parentRegistry?.releaseChildKey(key, this) }
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
    val parent = parentRegistry
    if (parent == null) {
      claimRootStateIfNeeded()
      return
    }
    val storageKey = parentStorageKey ?: return
    synchronized(parentConnectionLock) {
      if (parentStateRestored || synchronized(lock) { closed }) return
      val restoredState = parent.claimChildKey(storageKey, this)
      if (restoredState != null) {
        synchronized(lock) {
          if (closed) return
          retainedValues.putAll(Bundle(restoredState).apply { classLoader = valueClassLoader })
        }
      }
      parentStateRestored = true
    }
  }

  /**
   * Claims this root's saved values from its owner on the first read. Values the tree already
   * established are kept, so a claim never overwrites live state. A closed registry never claims,
   * so a calculation finishing after release cannot take the values from the next tree.
   *
   * The claim runs under [parentConnectionLock] so a concurrent first read waits for the merge
   * instead of reading defaults. This is safe because the owner never calls into a registry while
   * holding its own lock.
   */
  private fun claimRootStateIfNeeded() {
    val claim = claimRootState ?: return
    val failure =
        synchronized(parentConnectionLock) {
          if (rootStateClaimed) return
          rootStateClaimed = true
          if (synchronized(lock) { closed }) return
          val claimedState = claim() ?: return
          synchronized(lock) {
            if (closed) return
            try {
              val restored = Bundle(claimedState).apply { classLoader = valueClassLoader }
              restored.keySet().toList().forEach { key ->
                if (!providers.containsKey(key) && !retainedValues.containsKey(key)) {
                  restored.getBundle(key)?.let { retainedValues.putBundle(key, Bundle(it)) }
                }
              }
              null
            } catch (exception: RuntimeException) {
              exception
            }
          }
        }
    failure?.let { reportSaveableStateRestoreFailure("root claim", it) }
  }

  private fun registerWithParentIfNeeded() {
    val parent = parentRegistry ?: return
    val storageKey = parentStorageKey ?: return
    restoreParentStateIfNeeded()
    synchronized(parentConnectionLock) {
      if (parentRegistration != null || parentKeyAmbiguous) return
      if (synchronized(lock) { closed }) return
      val registration = parent.registerChildProvider(storageKey, this, ::snapshotLocal)
      if (registration == null) {
        parentKeyAmbiguous = true
      } else {
        parentRegistration = registration
      }
    }
  }

  /**
   * Gives [child] the child key [key] and returns the values saved under it. If another open child
   * already holds the key, the key becomes ambiguous instead: the holder stops being saved, and
   * neither child gets any values.
   */
  private fun claimChildKey(key: String, child: LithoSaveableStateRegistry): Bundle? {
    restoreParentStateIfNeeded()
    synchronized(lock) {
      if (closed || key in ambiguousChildKeys) return null
      val holder = childKeyHolders[key]?.get()
      if (holder != null && holder !== child && !holder.isClosed()) {
        ambiguousChildKeys += key
        childKeyHolders.remove(key)
        providers.remove(key)
        discardRetainedValue(key)
        return null
      }
      childKeyHolders[key] = WeakReference(child)
      return restoredValueLocked(key)?.value as? Bundle
    }
  }

  private fun releaseChildKey(key: String, child: LithoSaveableStateRegistry) {
    synchronized(lock) {
      if (childKeyHolders[key]?.get() === child) {
        childKeyHolders.remove(key)
      }
    }
  }

  /** Registers [child]'s snapshot under [key], or returns `null` unless [child] holds the key. */
  private fun registerChildProvider(
      key: String,
      child: LithoSaveableStateRegistry,
      provider: () -> Any?,
  ): Registration? {
    restoreParentStateIfNeeded()
    val registeredProvider = Provider(provider)
    synchronized(lock) {
      if (closed || childKeyHolders[key]?.get() !== child || providers.containsKey(key)) {
        return null
      }
      providers[key] = registeredProvider
      discardRetainedValue(key)
    }
    registerWithParentIfNeeded()
    return Registration(this, key, registeredProvider)
  }

  private fun reportDuplicateKey(key: String) {
    DebugInfoReporter.report(
        category = DUPLICATE_KEY_CATEGORY,
        level = LogLevel.WARNING,
    ) {
      this["key"] = key
    }
  }

  private fun isClosed(): Boolean = synchronized(lock) { closed }

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

  internal companion object {
    private const val CHILD_REGISTRY_PREFIX: String = "litho-child-registry:"
    private const val VALUE_KEY: String = "value"
    private const val DUPLICATE_KEY_CATEGORY: String = "LithoSaveableState:DuplicateKey"

    /**
     * Creates a root ComponentTree's registry whose saved values come from [claimState] when a
     * component first reads saveable state.
     */
    fun ownerBacked(
        valueClassLoader: ClassLoader?,
        claimState: () -> Bundle?,
    ): LithoSaveableStateRegistry =
        LithoSaveableStateRegistry(null, valueClassLoader, null, null, claimState)
  }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RestoredSaveableState internal constructor(val value: Any?)

/**
 * Reports saved values that could not be read; the affected values start from their initializers.
 */
internal fun reportSaveableStateRestoreFailure(source: String, exception: RuntimeException) {
  DebugInfoReporter.report(category = "LithoSaveableState:RestoreFailed") {
    this["source"] = source
    this["exception"] = exception.javaClass.name
    this["message"] = exception.message
  }
}
