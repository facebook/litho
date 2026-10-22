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
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.rendercore.LogLevel
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Saves the values of root ComponentTrees created with one [SavedStateRegistry], keyed by each
 * tree's root identity.
 *
 * Each identity is saved from one tree:
 * - The tree that claims it holds it while it is on screen. Once that tree is gone (released,
 *   swapped off its view, or never shown), the next tree of the identity takes it and starts from
 *   initial values.
 * - Two trees of one identity on screen at the same time make it ambiguous: neither is saved.
 * - A tree torn down with its host, as on back navigation, parks its values for the next tree.
 * - Restored values wait until a tree claims them, so a tree created after its data loads still
 *   restores.
 *
 * A tree with a saveable-state key is never ambiguous. The newest tree with the key owns it: values
 * from a released keyed tree go to the next tree with the key, even while the host is active, and a
 * new tree takes the key over from a live one, which keeps working but is no longer saved. Taking
 * the key from a tree that is still attached is reported, since it usually means two visible trees
 * share a key by mistake.
 *
 * Only the [MAX_PARKED_KEYS] most recently parked keys keep their values. Without a limit, rows
 * built as keyed root trees would save every row ever released, until the saved state is too large
 * for Android. A dropped key starts from initial values when it comes back, and the first drop is
 * reported.
 *
 * Claims may happen on any thread. Android saved-state access happens on the main thread, so a tree
 * that renders before the owner takes its restored values from Android claims nothing at first.
 * When the values arrive, that tree gets them if it hasn't been attached yet, and renders again
 * before anything is shown.
 */
internal class ComponentTreeSaveableStateOwner
private constructor(
    private val valueClassLoader: ClassLoader,
) : SavedStateRegistry.SavedStateProvider, LifecycleEventObserver {
  private val lock = Any()

  /** One entry per identity: a root component's class and manual key, or a saveable-state key. */
  @GuardedBy("lock") private val entries = mutableMapOf<String, Entry>()

  /** The Android registry this owner is connected to, set on the first main-thread setup. */
  @GuardedBy("lock") private var registry: SavedStateRegistry? = null

  /**
   * Whether this owner's Bundle was taken from Android and its values applied; it is taken only
   * once. A tree that claims before then can still get its values late.
   */
  @GuardedBy("lock") private var restoredConsumed = false

  /** Set once the store is destroyed or another provider owns our key; nothing is saved after. */
  @GuardedBy("lock") private var disabled = false

  /** Keyed identities with parked values, oldest first. Bounded by [MAX_PARKED_KEYS]. */
  @GuardedBy("lock") private val parkedKeys = LinkedHashSet<String>()

  /** A dropped parked key is reported once per owner. */
  private val parkedKeyDropReported = AtomicBoolean(false)

  /** Runs between taking Android's restored values and delivering late ones, for tests. */
  @VisibleForTesting @Volatile internal var beforeLateRestoreDelivery: (() -> Unit)? = null

  /**
   * Called once per tree at its first saveable read, on any thread. Returns the values to restore,
   * or null when there are none or the identity is ambiguous.
   */
  fun claim(binding: ComponentTreeSaveableStateBinding, identity: String): Bundle? =
      if (binding.keyed) claimKey(binding, identity) else claimRootIdentity(binding, identity)

  /**
   * Claims a keyed identity. The newest tree with a key owns it:
   * - if no live tree holds the key, this tree takes its parked or restored values;
   * - if a live tree holds it, this tree takes over that tree's current values, and the old tree is
   *   no longer saved.
   *
   * For example, a screen that refreshes by building a new tree instead of calling `setRoot`:
   * ```
   * val oldTree = ComponentTree.create(context, this, Feed())
   *     .withSaveableStateKey("feed")
   *     .build()
   * lithoView.componentTree = oldTree   // oldTree claims "feed"; the user scrolls and taps
   *
   * val newTree = ComponentTree.create(context, this, Feed())
   *     .withSaveableStateKey("feed")
   *     .build()
   * lithoView.componentTree = newTree   // newTree takes oldTree's current values;
   *                                     // oldTree keeps working but is no longer saved
   * ```
   *
   * If `oldTree` is still attached when `newTree` claims, as in a crossfade where both are on
   * screen, the takeover is reported.
   *
   * Taking over reads the old tree's values outside the lock, so the tree holding the key can
   * change, or take late restored values, while they are read. Then the claim is tried once more;
   * if that happens again, the tree starts from initial values, as with `useState`.
   */
  private fun claimKey(binding: ComponentTreeSaveableStateBinding, identity: String): Bundle? {
    // At most two attempts. A second one only happens if a different tree came to hold the key, or
    // the holder took late restored values, while its values were being read.
    repeat(2) {
      val (holder, lateRestoreCount) =
          synchronized(lock) {
            if (disabled || binding.released) return null
            val entry = entries.getOrPut(identity) { Entry() }
            val live = entry.liveClaimant()
            // This tree already holds the key, so its values are already in its registry.
            if (live === binding) return null
            if (live == null) {
              // Nobody holds the key: take what a torn-down tree parked, else what Android
              // restored.
              entry.claimant = WeakReference(binding)
              entry.everClaimed = true
              val parked = entry.parked.remove(binding.lineage)
              binding.claimedBeforeRestore = !restoredConsumed && parked?.startingValues != false
              parkedKeys.remove(identity)
              return parked?.values ?: entry.restored.also { entry.restored = null }
            }
            live to entry.lateRestoreCount
          }
      // A live tree holds the key. If it is still attached, two visible trees may share a key.
      if (holder.attached) reportKeyTakenFromAttachedTree(identity)
      // Read its values outside the lock: this runs its Savers and nested trees' providers.
      val values = holder.snapshotValues()
      synchronized(lock) {
        if (disabled || binding.released) return null
        val entry = entries[identity] ?: return null
        // Take over only if the same tree still holds the key and took no late restored values
        // since they were read; otherwise try again.
        if (entry.liveClaimant() === holder && entry.lateRestoreCount == lateRestoreCount) {
          entry.claimant = WeakReference(binding)
          // Before the restore, the new tree only copied starting values unless the old one was
          // shown, so it can take the restored values in its place.
          binding.claimedBeforeRestore =
              !restoredConsumed && holder.claimedBeforeRestore && !holder.mayHaveShownValues()
          entry.parked.clear()
          parkedKeys.remove(identity)
          return values
        }
      }
    }
    // A different tree came to hold the key during both attempts: start from initial values.
    return null
  }

  private fun reportKeyTakenFromAttachedTree(identity: String) {
    DebugInfoReporter.report(
        category = "LithoSaveableState:KeyTakenFromAttachedTree",
        level = LogLevel.WARNING,
    ) {
      this["identity"] = identity
    }
  }

  /**
   * Takes the identity and returns its parked or restored values, unless the tree holding it is on
   * screen. Then this tree starts from initial values, and [attach] decides again once it is shown.
   *
   * For example, a screen that builds a new tree when its data changes:
   * ```
   * fun onData(data: Data) {
   *   lithoView.componentTree =
   *       ComponentTree.create(context, this, Feed(data)).build()
   * }
   * ```
   *
   * Setting the new tree detaches the old one, so the new tree takes "Feed" and is saved from then
   * on. Two trees of one class on screen together, such as a top and a bottom `FeedHeader`, are not
   * saved; giving them different keys, `key("top") { FeedHeader() }`, saves both.
   */
  private fun claimRootIdentity(
      binding: ComponentTreeSaveableStateBinding,
      identity: String,
  ): Bundle? =
      synchronized(lock) {
        if (disabled || binding.released) return null
        val entry = entries.getOrPut(identity) { Entry() }
        if (entry.ambiguous) return null
        val live = entry.liveClaimant()
        // This tree already holds the identity, so its values are already in its registry.
        if (live === binding) return null
        if (live?.attached == true) {
          // Both on screen: they can't be told apart.
          if (binding.attached) entry.markAmbiguous()
          return null
        }
        entry.claimant = WeakReference(binding)
        entry.everClaimed = true
        val parked = entry.parked.remove(binding.lineage)
        binding.claimedBeforeRestore = !restoredConsumed && parked?.startingValues != false
        parked?.values ?: entry.restored.also { entry.restored = null }
      }

  /**
   * Called when a tree is attached. The tree takes back values it parked when its view went away.
   * An unkeyed tree then holds its identity, unless another tree of it is already on screen, which
   * makes the identity ambiguous. A keyed tree only gets its key back if no newer tree took it.
   */
  @MainThread
  fun attach(binding: ComponentTreeSaveableStateBinding) {
    val identity = binding.identity ?: return
    synchronized(lock) {
      val entry = entries[identity] ?: return
      val parked = entry.parked[binding.lineage]
      val parkedByThis = parked != null && parked.parkedBy.get() === binding
      if (parkedByThis) {
        entry.parked.remove(binding.lineage)
        if (binding.keyed) parkedKeys.remove(identity)
      }
      if (entry.ambiguous) return
      val live = entry.liveClaimant()
      when {
        live === binding -> Unit
        binding.keyed -> if (parkedByThis) entry.claimant = WeakReference(binding)
        // Both on screen: they can't be told apart.
        live?.attached == true -> entry.markAmbiguous()
        else -> entry.claimant = WeakReference(binding)
      }
    }
  }

  /**
   * Keeps this tree's values for the next tree of its identity and lineage. For example, a
   * Fragment's tree going onto the back stack:
   * ```
   * override fun onCreateView(...): View {
   *   tree = ComponentTree.create(requireContext(), this, Feed()).build()
   *   return LithoView(requireContext()).apply { componentTree = tree }
   * }
   *
   * override fun onDestroyView() {
   *   tree?.release()   // the view goes away with the host stopped: Feed's values are parked
   *   super.onDestroyView()
   * }
   * ```
   *
   * On Back, `onCreateView` builds a new tree, and its first read takes the parked values.
   *
   * A keyed tree's values count toward [MAX_PARKED_KEYS].
   */
  @MainThread
  fun park(binding: ComponentTreeSaveableStateBinding) {
    val identity = binding.identity ?: return
    // Snapshots run providers, including Savers and nested registries, so take them outside lock.
    val values = binding.snapshotValues()
    val startingValues = binding.claimedBeforeRestore && !binding.mayHaveShownValues()
    val dropped =
        synchronized(lock) {
          val entry = entries[identity] ?: return
          if (entry.liveClaimant() !== binding) return
          entry.claimant = null
          entry.parked[binding.lineage] = Parked(values, WeakReference(binding), startingValues)
          if (binding.keyed) addParkedKey(identity) else null
        }
    if (dropped != null && parkedKeyDropReported.compareAndSet(false, true)) {
      reportParkedKeyDropped(dropped)
    }
  }

  /**
   * Makes [identity] the newest parked key. If that goes over [MAX_PARKED_KEYS], drops the oldest
   * key's parked values and returns that key.
   */
  @GuardedBy("lock")
  private fun addParkedKey(identity: String): String? {
    parkedKeys.remove(identity)
    parkedKeys.add(identity)
    if (parkedKeys.size <= MAX_PARKED_KEYS) return null
    val oldest = parkedKeys.first()
    parkedKeys.remove(oldest)
    entries[oldest]?.let { entry ->
      entry.parked.clear()
      if (entry.liveClaimant() == null && entry.restored == null) entries.remove(oldest)
    }
    return oldest
  }

  private fun reportParkedKeyDropped(identity: String) {
    DebugInfoReporter.report(
        category = "LithoSaveableState:ParkedKeyDropped",
        level = LogLevel.WARNING,
    ) {
      this["identity"] = identity
      this["maxParkedKeys"] = MAX_PARKED_KEYS
    }
  }

  /** Stops saving this tree's values; they are not handed to another tree. */
  @MainThread
  fun end(binding: ComponentTreeSaveableStateBinding) {
    val identity = binding.identity ?: return
    synchronized(lock) {
      val entry = entries[identity] ?: return
      if (entry.liveClaimant() === binding) entry.claimant = null
    }
  }

  /**
   * Registers this owner with Android and observes the store's lifecycle the first time, then takes
   * the restored values if Android has restored them. Safe to call repeatedly.
   */
  @MainThread
  fun initializeOnMain(store: SavedStateRegistryOwner) {
    ThreadUtils.assertMainThread()
    val savedStateRegistry = store.savedStateRegistry
    val shouldConnect =
        synchronized(lock) {
          if (disabled || registry != null) {
            false
          } else {
            registry = savedStateRegistry
            true
          }
        }
    if (shouldConnect) {
      if (store.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        destroy(store)
        return
      }
      val existingProvider = savedStateRegistry.getSavedStateProvider(PROVIDER_KEY)
      if (existingProvider != null && existingProvider !== this) {
        synchronized(lock) { disabled = true }
        return
      }
      if (existingProvider == null) {
        savedStateRegistry.registerSavedStateProvider(PROVIDER_KEY, this)
      }
      store.lifecycle.addObserver(this)
    }
    consumeRestoredStateIfReady(savedStateRegistry)
  }

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    synchronized(lock) { registry }?.let(::consumeRestoredStateIfReady)
    if (event == Lifecycle.Event.ON_DESTROY) {
      destroy(source)
    }
  }

  /**
   * Writes one entry per identity: the live tree's values, else its parked values, else restored
   * values nobody claimed, carried once. Ambiguous identities are not written. For example, a
   * screen with a live header, a parked list and an unclaimed restored panel saves all three.
   *
   * A tree that parked when its view went away but wasn't released, such as a tree kept by its
   * Fragment while the Fragment's view is on the back stack, can still change, so its current
   * values are saved rather than the ones it parked.
   */
  override fun saveState(): Bundle {
    val roots = Bundle()
    val liveTrees = mutableMapOf<String, ComponentTreeSaveableStateBinding>()
    synchronized(lock) {
      if (disabled) return Bundle()
      entries.forEach { (identity, entry) ->
        if (entry.ambiguous) return@forEach
        val claimant = entry.liveClaimant()
        val saved =
            when {
              claimant != null -> {
                liveTrees[identity] = claimant
                null
              }
              entry.parked.size == 1 -> {
                val parked = entry.parked.values.single()
                val parkedBy = parked.parkedBy.get()
                if (parkedBy != null && !parkedBy.released) {
                  liveTrees[identity] = parkedBy
                  null
                } else {
                  savedEntry(parked.values, carry = 0)
                }
              }
              entry.parked.size > 1 -> null
              entry.restored != null && entry.restoredCarry < MAX_CARRY ->
                  savedEntry(checkNotNull(entry.restored), carry = entry.restoredCarry + 1)
              else -> null
            }
        saved?.let { roots.putBundle(identity, it) }
      }
    }
    liveTrees.forEach { (identity, tree) ->
      roots.putBundle(identity, savedEntry(tree.snapshotValues(), carry = 0))
    }
    return bundleOf(ROOTS_KEY to roots)
  }

  /**
   * Takes this owner's restored Bundle from Android, once, after Android has restored it. Runs on
   * the main thread, because `SavedStateRegistry` is not thread-safe, from the first main-thread
   * setup and from each lifecycle event, so a restore that lands after setup is still picked up.
   *
   * Each saved identity becomes an [Entry.restored] value that waits for a tree to claim it, unless
   * a tree already claimed that identity in this session or it is ambiguous. If trees claimed it
   * before this Bundle was taken, by being built and rendered off the main thread, and none of them
   * has been shown, they only have starting values, so the restored values still win: the tree
   * holding the identity gets them and renders again, or, if a tree parked its starting values and
   * nobody holds it, the next tree to claim it gets them. Otherwise they are dropped. An identity
   * that can't be read is reported and skipped, and the others are still restored.
   *
   * For example, after process death the Fragment's first tree is built in `onCreateView`; its
   * `initializeOnMain` call takes the Bundle here, before the tree's first render claims its
   * values.
   */
  private fun consumeRestoredStateIfReady(savedStateRegistry: SavedStateRegistry) {
    if (!savedStateRegistry.isRestored) return
    if (synchronized(lock) { restoredConsumed || disabled }) return
    val (roots, identities) = readRestoredRoots(savedStateRegistry) ?: (Bundle() to emptyList())
    val failures = mutableListOf<RuntimeException>()
    val lateIdentities = mutableListOf<String>()
    synchronized(lock) {
      // Set together with the entries, so a claim on another thread sees both or neither.
      restoredConsumed = true
      identities.forEach { identity ->
        try {
          val saved = roots.getBundle(identity) ?: return@forEach
          saved.classLoader = valueClassLoader
          val values = saved.getBundle(VALUES_KEY) ?: return@forEach
          val entry = entries.getOrPut(identity) { Entry() }
          if (entry.ambiguous) return@forEach
          if (entry.everClaimed) {
            val carry = saved.getInt(CARRY_KEY)
            val live = entry.liveClaimant()
            if (live?.claimedBeforeRestore == true) {
              // Kept with the identity rather than this tree: a newer tree may take it over first.
              entry.lateRestored = values
              entry.lateRestoredCarry = carry
              lateIdentities += identity
            } else if (live == null) {
              restoreOverStartingValues(identity, entry, values, carry)
            }
            return@forEach
          }
          entry.restored = values
          entry.restoredCarry = saved.getInt(CARRY_KEY)
        } catch (exception: RuntimeException) {
          failures += exception
        }
      }
    }
    failures.forEach { reportSaveableStateRestoreFailure("owner restore", it) }
    beforeLateRestoreDelivery?.invoke()
    lateIdentities.forEach(::deliverLateRestore)
  }

  /**
   * Gives an identity's late restored values to the tree holding it now. The tree takes them under
   * the lock, so a tree replaced before delivery never gets them, and renders outside it. A
   * takeover that read the holder's values before they were taken reads them again, because
   * [Entry.lateRestoreCount] changed, so the new holder ends up with the restored values too.
   */
  @MainThread
  private fun deliverLateRestore(identity: String) {
    val holder =
        synchronized(lock) {
          val entry = entries[identity] ?: return
          val values = entry.lateRestored ?: return
          entry.lateRestored = null
          val holder = entry.liveClaimant()
          if (holder == null) {
            restoreOverStartingValues(identity, entry, values, entry.lateRestoredCarry)
            return
          }
          if (!holder.adoptLateRestore(values)) return
          entry.lateRestoreCount++
          holder
        }
    holder.renderLateRestore()
  }

  /**
   * Keeps restored values for the next tree of an identity nobody holds, in place of parked values
   * that are only starting values: left by trees that claimed before the restore and were never
   * shown. Parked values from a tree that was shown win, since the user may have changed them.
   */
  @GuardedBy("lock")
  private fun restoreOverStartingValues(
      identity: String,
      entry: Entry,
      values: Bundle,
      carry: Int,
  ) {
    if (entry.parked.isEmpty() || entry.parked.values.any { !it.startingValues }) return
    entry.parked.clear()
    parkedKeys.remove(identity)
    entry.restored = values
    entry.restoredCarry = carry
  }

  /** Takes the restored roots from Android, or null when there are none or they can't be read. */
  private fun readRestoredRoots(
      savedStateRegistry: SavedStateRegistry,
  ): Pair<Bundle, List<String>>? {
    val restored = savedStateRegistry.consumeRestoredStateForKey(PROVIDER_KEY) ?: return null
    // Reading the keys unparcels the roots Bundle, so it can throw on unreadable saved state too.
    return try {
      restored.classLoader = valueClassLoader
      val roots =
          restored.getBundle(ROOTS_KEY)?.apply { classLoader = valueClassLoader } ?: return null
      roots to roots.keySet().toList()
    } catch (exception: RuntimeException) {
      reportSaveableStateRestoreFailure("owner restore", exception)
      null
    }
  }

  /** Unregisters from Android and stops saving; later claims get nothing. */
  private fun destroy(store: LifecycleOwner) {
    val savedStateRegistry =
        synchronized(lock) {
          disabled = true
          entries.clear()
          parkedKeys.clear()
          registry
        }
    if (savedStateRegistry?.getSavedStateProvider(PROVIDER_KEY) === this) {
      savedStateRegistry.unregisterSavedStateProvider(PROVIDER_KEY)
    }
    store.lifecycle.removeObserver(this)
    synchronized(OWNERS_LOCK) {
      if (savedStateRegistry != null && OWNERS[savedStateRegistry]?.get() === this) {
        OWNERS.remove(savedStateRegistry)
      }
    }
  }

  private fun savedEntry(values: Bundle, carry: Int): Bundle =
      bundleOf(VALUES_KEY to values, CARRY_KEY to carry)

  /**
   * Values a torn-down tree left for its replacement, and the tree that left them. [startingValues]
   * is set when that tree claimed before the restore and was never shown, so restored values that
   * arrive later replace these.
   */
  private class Parked(
      val values: Bundle,
      val parkedBy: WeakReference<ComponentTreeSaveableStateBinding>,
      val startingValues: Boolean,
  )

  /**
   * What the owner knows about one identity. Its values are in one of three places: live in the
   * claimant tree, parked after that tree was torn down, or restored from Android and not yet
   * claimed. A claim takes parked values before restored ones.
   */
  private class Entry {
    /** Two trees of this identity were on screen together, so it is neither saved nor restored. */
    var ambiguous: Boolean = false

    /** The tree currently using this identity's values. An identity has at most one. */
    var claimant: WeakReference<ComponentTreeSaveableStateBinding>? = null

    /**
     * Values kept in memory after a tree was torn down with its host, until the next tree of this
     * identity claims them. For example, a Fragment's tree parks here when the Fragment goes onto
     * the back stack, and the tree rebuilt on Back takes the values. Keyed by lineage.
     */
    val parked: MutableMap<Any, Parked> = mutableMapOf()

    /**
     * Values Android restored after recreation or process death, waiting for a claim. For example,
     * a tree created only once its data loads finds its values here; if the screen is recreated
     * again before that, they are saved once more and then dropped.
     */
    var restored: Bundle? = null

    /** How many saves [restored] has been carried through unclaimed; dropped after [MAX_CARRY]. */
    var restoredCarry: Int = 0

    /** A tree claimed this identity in this session, so values restored later are dropped. */
    var everClaimed: Boolean = false

    /**
     * Restored values that arrived after a tree claimed this identity, waiting to be delivered to
     * whichever tree holds it then.
     */
    var lateRestored: Bundle? = null

    /** The saved carry count of [lateRestored]. */
    var lateRestoredCarry: Int = 0

    /** How many times the holder took late restored values; a takeover checks it hasn't changed. */
    var lateRestoreCount: Int = 0

    fun liveClaimant(): ComponentTreeSaveableStateBinding? =
        claimant?.get().also { if (it == null) claimant = null }

    fun markAmbiguous() {
      ambiguous = true
      claimant = null
      restored = null
      lateRestored = null
      parked.clear()
    }
  }

  internal companion object {
    /** Keeps explicit keys apart from class identities, whose class name never contains a colon. */
    const val KEYED_IDENTITY_PREFIX: String = "key:"

    private const val PROVIDER_KEY: String = "com.facebook.litho.ComponentTreeSaveableStateOwner"
    private const val ROOTS_KEY: String = "roots"
    private const val VALUES_KEY: String = "values"
    private const val CARRY_KEY: String = "carry"

    /** Restored values nobody claims are carried through one more save, then dropped. */
    private const val MAX_CARRY: Int = 1

    /** The most keyed trees whose parked values are kept and saved per owner. */
    @VisibleForTesting internal const val MAX_PARKED_KEYS: Int = 32

    private val OWNERS_LOCK = Any()

    /**
     * Finds the owner for a registry without keeping it alive, so nothing here can keep a host
     * alive. Once connected, an owner is kept by the registry it is registered with; before that,
     * as when every tree is built and laid out off the main thread, only by its trees.
     */
    @GuardedBy("OWNERS_LOCK")
    private val OWNERS =
        WeakHashMap<SavedStateRegistry, WeakReference<ComponentTreeSaveableStateOwner>>()

    /** Returns the owner for [store]'s SavedStateRegistry, creating it on first use. */
    fun of(
        store: SavedStateRegistryOwner,
        valueClassLoader: ClassLoader,
    ): ComponentTreeSaveableStateOwner =
        synchronized(OWNERS_LOCK) {
          val registry = store.savedStateRegistry
          OWNERS[registry]?.get()
              ?: ComponentTreeSaveableStateOwner(valueClassLoader).also {
                OWNERS[registry] = WeakReference(it)
              }
        }
  }
}
