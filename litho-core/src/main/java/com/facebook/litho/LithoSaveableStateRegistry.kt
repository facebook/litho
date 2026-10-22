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
import com.facebook.litho.state.StateReadRecorder
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

  /** Whether [detachFromParent] stopped saving this child, until [attachToParent]. */
  @GuardedBy("parentConnectionLock") private var detachedFromParent = false

  /** Whether [detachFromParent] gave up this child's key, which it claims again when saved. */
  @GuardedBy("parentConnectionLock") private var parentKeyReleased = false

  /** Whether this child was saved in its parent when detached, so [attachToParent] saves it. */
  @GuardedBy("parentConnectionLock") private var savedWhenDetached = false

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

  /** Whether this child is in its parent's snapshot; see [setSavedInParent]. */
  @GuardedBy("lock") private var savedInParent = true

  /** Child keys [discardChildState] dropped before this registry restored its saved values. */
  @GuardedBy("lock") private val keysDiscardedBeforeRestore = mutableSetOf<String>()

  /**
   * Per key, bumped when a restored value for it arrives after components already read this
   * registry. Hooks key their state on it, so only those keys read the restored value again.
   */
  @GuardedBy("lock") private val restoreGenerations = mutableMapOf<String, Int>()

  /** Set once any late restored value arrives, so hooks skip [restoreGeneration] until then. */
  @Volatile private var lateRestored = false

  constructor(
      restoredState: Bundle? = null,
      valueClassLoader: ClassLoader? = null,
  ) : this(restoredState, valueClassLoader, null, null, null)

  /**
   * Returns retained values and the latest value of every provider, for the owning scope's save.
   */
  internal fun snapshotForOwner(): Bundle = snapshotLocal()

  /**
   * Adopts restored values that arrived after the tree's first render. Only called before the tree
   * mounts, when no provider is registered yet, so the restored values replace the initial ones.
   *
   * An open child that already read its key never reads it again, so it adopts the values saved
   * under that key the same way, and its own children after it. Ambiguous keys stay dropped.
   */
  internal fun adoptLateRestoredValues(values: Bundle) {
    val children = mutableListOf<Pair<LithoSaveableStateRegistry, Bundle>>()
    val failure =
        synchronized(lock) {
          if (closed) return
          try {
            val restored = Bundle(values).apply { classLoader = valueClassLoader }
            restored.keySet().toList().forEach { key ->
              if (key in ambiguousKeys || key in ambiguousChildKeys) return@forEach
              restored.getBundle(key)?.let { value ->
                retainedValues.putBundle(key, Bundle(value))
                restoreGenerations[key] = (restoreGenerations[key] ?: 0) + 1
                lateRestored = true
                childKeyHolders[key]
                    ?.get()
                    ?.takeIf { !it.isClosed() }
                    ?.let { child ->
                      (restoredValueLocked(key)?.value as? Bundle)?.let { children += child to it }
                    }
              }
            }
            null
          } catch (exception: RuntimeException) {
            exception
          }
        }
    failure?.let { reportSaveableStateRestoreFailure("late restore", it) }
    children.forEach { (child, childValues) -> child.adoptLateRestoredValues(childValues) }
  }

  /** How many late restored values arrived for [key]; see [adoptLateRestoredValues]. */
  internal fun restoreGeneration(key: String): Int =
      synchronized(lock) { restoreGenerations[key] ?: 0 }

  internal fun hasLateRestoredValues(): Boolean = lateRestored

  /**
   * How many late restored values arrived, for all keys. Content that rendered before the count
   * changed reads them only when it renders again, which a host reusing its layout has to force.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun lateRestoreCount(): Int = synchronized(lock) { restoreGenerations.values.sum() }

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
          val unsavedKeys = unsavedChildKeys()
          val retained = copyRetainedValues().apply { unsavedKeys.forEach(::remove) }
          retained to providers.filterKeys { it !in unsavedKeys }.toList()
        }
    activeProviders.forEach { (key, provider) ->
      snapshot.putBundle(key, provider.savedValue(key))
    }
    return snapshot
  }

  /**
   * Retains active values before an owner replaces its ComponentTree or unbinds a collection row.
   *
   * Ordinary provider removal discards the value so dynamic components cannot accumulate retained
   * state for the entire registry lifetime. Tree, navigation and collection-row owners call this
   * operation only when the same logical state scope will continue, in a replacement tree or when
   * the row is bound again. With no active provider there is nothing to retain, and nothing is
   * claimed from the parent or owner either. A child left out of this registry's snapshot
   * ([setSavedInParent]) keeps its values itself, so they are not retained here.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun retainActiveValues() {
    if (synchronized(lock) { providers.isEmpty() }) return
    restoreParentStateIfNeeded()
    val activeProviders =
        synchronized(lock) {
          if (closed) return
          val unsavedKeys = unsavedChildKeys()
          providers.filterKeys { it !in unsavedKeys }.toList()
        }
    val values = activeProviders.map { (key, provider) ->
      Triple(key, provider, provider.savedValue(key))
    }

    synchronized(lock) {
      if (closed) return
      val unsavedKeys = unsavedChildKeys()
      values.forEach { (key, provider, value) ->
        val currentProvider = providers[key]
        if (currentProvider === provider && key !in unsavedKeys) {
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

  /**
   * Stops saving this child in its parent and gives up its key, but keeps its values, while the
   * content using it is detached and may come back with its state. Until [attachToParent], this
   * child claims nothing from its parent and is not saved there, even when providers register, as
   * the rows of a removed list still may.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun detachFromParent() {
    val registration =
        synchronized(parentConnectionLock) {
          detachedFromParent = true
          parentKeyReleased = true
          savedWhenDetached = savedWhenDetached || parentRegistration != null
          parentRegistration.also { parentRegistration = null }
        }
    registration?.close()
    parentStorageKey?.let { key -> parentRegistry?.releaseChildKey(key, this) }
  }

  /**
   * Ends [detachFromParent]: this child takes its key back and is saved in its parent again, right
   * away if it was saved when detached or has providers now, otherwise on its next registration.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun attachToParent() {
    val register =
        synchronized(parentConnectionLock) {
          if (!detachedFromParent) return
          detachedFromParent = false
          savedWhenDetached.also { savedWhenDetached = false } ||
              synchronized(lock) { providers.isNotEmpty() }
        }
    if (register) registerWithParentIfNeeded()
  }

  /**
   * Sets whether this child is saved in its parent, which it is by default.
   *
   * While it isn't, the parent leaves it out of its snapshot, with no entry for its key, and keeps
   * no copy of the values this child read from it. This child still holds its key, keeps its values
   * and its own children, and can read the values restored for it. Saving it again saves its
   * current values. This claims nothing from the parent: values restored for this child stay in the
   * parent's snapshot until this child reads them.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun setSavedInParent(saved: Boolean) {
    synchronized(lock) {
      if (closed || savedInParent == saved) return
      savedInParent = saved
    }
    val parent = parentRegistry ?: return
    val storageKey = parentStorageKey ?: return
    if (!saved) {
      parent.dropValuesOfUnsavedChild(storageKey, this)
    } else if (synchronized(parentConnectionLock) { parentStateRestored }) {
      // The parent dropped its copy of the values this child read, so this child provides them.
      registerWithParentIfNeeded()
    }
  }

  /**
   * Whether this registry holds anything to save: a provider, a retained value, or an open child
   * holding a key. One that holds nothing can be closed without losing a value: values its parent
   * restored for it that it has not read stay in the parent, for the next child with its key.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun holdsState(): Boolean =
      synchronized(lock) {
        if (closed) return false
        if (providers.isNotEmpty()) return true
        val hasRetainedValues =
            try {
              !retainedValues.isEmpty
            } catch (_: RuntimeException) {
              true
            }
        hasRetainedValues || childKeyHolders.values.any { it.get()?.isClosed() == false }
      }

  /**
   * Drops the values saved for the child registry created with [key], including values restored for
   * a child that was never created again. A child that already read them keeps its copy. This
   * claims nothing from the parent or owner: values this registry has not restored yet are dropped
   * when it restores them.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun discardChildState(key: String) {
    val storageKey = "$CHILD_REGISTRY_PREFIX$key"
    synchronized(parentConnectionLock) {
      val restorePending =
          (parentStorageKey != null && !parentStateRestored) ||
              (claimRootState != null && !rootStateClaimed)
      synchronized(lock) {
        if (closed) return
        discardRetainedValue(storageKey)
        if (restorePending) keysDiscardedBeforeRestore += storageKey
      }
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
    val parent = parentRegistry
    if (parent == null) {
      claimRootStateIfNeeded()
      return
    }
    val storageKey = parentStorageKey ?: return
    synchronized(parentConnectionLock) {
      if (parentStateRestored || detachedFromParent || synchronized(lock) { closed }) return
      val restoredState = parent.claimChildKey(storageKey, this)
      synchronized(lock) {
        if (closed) return
        if (restoredState != null) {
          retainedValues.putAll(Bundle(restoredState).apply { classLoader = valueClassLoader })
        }
        keysDiscardedBeforeRestore.forEach(::discardRetainedValue)
        keysDiscardedBeforeRestore.clear()
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
   * instead of reading defaults. This can't deadlock with the owner: under its own lock, the owner
   * only takes registries' [lock]s, to hand over late restored values, never a
   * [parentConnectionLock]; and nothing holding a [lock] calls the owner or takes a
   * [parentConnectionLock].
   */
  private fun claimRootStateIfNeeded() {
    val claim = claimRootState ?: return
    val failure =
        synchronized(parentConnectionLock) {
          if (rootStateClaimed) return
          rootStateClaimed = true
          val discardedKeys =
              synchronized(lock) {
                if (closed) return
                keysDiscardedBeforeRestore.toSet().also { keysDiscardedBeforeRestore.clear() }
              }
          val claimedState = claim() ?: return
          synchronized(lock) {
            if (closed) return
            try {
              val restored = Bundle(claimedState).apply { classLoader = valueClassLoader }
              restored.keySet().toList().forEach { key ->
                if (
                    key !in discardedKeys &&
                        !providers.containsKey(key) &&
                        !retainedValues.containsKey(key)
                ) {
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
      if (detachedFromParent || parentRegistration != null || parentKeyAmbiguous) return
      if (synchronized(lock) { closed }) return
      if (parentKeyReleased) {
        // The values saved under the key are older than the ones this child kept, so they stay.
        parent.claimChildKey(storageKey, this)
        parentKeyReleased = false
      }
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
      val restoredState = restoredValueLocked(key)?.value as? Bundle
      if (!child.isSavedInParent()) discardRetainedValue(key)
      return restoredState
    }
  }

  private fun releaseChildKey(key: String, child: LithoSaveableStateRegistry) {
    synchronized(lock) {
      if (childKeyHolders[key]?.get() === child) {
        childKeyHolders.remove(key)
      }
    }
  }

  /** Drops this registry's copy of [child]'s values while [child] holds [key] unsaved. */
  private fun dropValuesOfUnsavedChild(key: String, child: LithoSaveableStateRegistry) {
    synchronized(lock) {
      if (childKeyHolders[key]?.get() === child && !child.isSavedInParent()) {
        discardRetainedValue(key)
      }
    }
  }

  /** The keys held by children that [setSavedInParent] left out of this registry's snapshot. */
  @GuardedBy("lock")
  private fun unsavedChildKeys(): Set<String> {
    if (childKeyHolders.isEmpty()) return emptySet()
    return childKeyHolders.entries.mapNotNullTo(HashSet()) { (key, holder) ->
      key.takeIf { holder.get()?.isSavedInParent() == false }
    }
  }

  private fun isSavedInParent(): Boolean = synchronized(lock) { savedInParent }

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

    /**
     * Reads the provider's value. It runs outside the render of the tree that registered it,
     * sometimes inside another tree's render, as when a list releases a row while it lays out,
     * where a tracked read would count as a cross-tree read that returns a stale value or crashes.
     */
    fun savedValue(key: String): Bundle =
        synchronized(lock) {
          valueBundle(key, StateReadRecorder.untracked { value() })
        }
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
