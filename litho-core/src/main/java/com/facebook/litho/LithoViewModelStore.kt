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

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.facebook.litho.utils.LithoThreadLocal
import com.facebook.rendercore.thread.utils.ThreadUtils.runOnUiThread
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

/**
 * Owns AndroidX [ViewModel] instances used by Litho components.
 *
 * AndroidX normally exposes ViewModels through lifecycle owners whose stores are accessed from the
 * main thread. Litho components can resolve concurrently on background threads, including multiple
 * resolves that request the same ViewModel before either resolve completes. This store coordinates
 * those requests so that a key creates exactly one instance and every caller observes that same
 * instance.
 *
 * The store also coordinates creation with scope teardown, supports retry after a failed factory,
 * and delegates each retained instance to AndroidX [ViewModelStore] so closing this scope preserves
 * the standard [ViewModel.onCleared] contract. The owner of this object defines its lifetime and is
 * responsible for closing it when that scope ends.
 *
 * This type is thread-safe. The request that installs a new entry runs the factory on its resolve
 * thread; other resolve threads requesting that key wait for the result. Factories must be fast and
 * non-blocking. A factory must receive its dependencies directly rather than request another
 * ViewModel from a [LithoViewModelStore].
 */
class LithoViewModelStore @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor() {
  private val entries = ConcurrentHashMap<LithoViewModelKey, ViewModelEntry>()
  private val closed = AtomicBoolean(false)
  private val calculationLock = Any()
  private val calculationDepth = LithoThreadLocal<Int>()

  @GuardedBy("calculationLock") private var activeCalculationCount = 0
  @Volatile private var closeRequested = false

  /**
   * Keeps this store open while one Litho calculation may use its models, or returns `null` when
   * owner teardown has already requested closure. A caller may continue without a lease if its
   * calculation does not access ViewModels.
   */
  internal fun tryAcquireCalculationLease(): CalculationLease? {
    synchronized(calculationLock) {
      if (closeRequested) return null
      activeCalculationCount++
    }
    calculationDepth.set((calculationDepth.get() ?: 0) + 1)
    return CalculationLease(this, Thread.currentThread())
  }

  /**
   * Returns the [ViewModel] identified by [modelClass] and [key], creating it when absent.
   *
   * The store is the owner scope. Within one store, requests for the same class and key return the
   * same instance. Use distinct keys when the same owner needs multiple instances of one ViewModel
   * class. Concurrent requests for the same identity share the result from the request that
   * installed the entry. A factory cannot request another Litho ViewModel because nested factories
   * can form cross-thread dependency cycles.
   */
  internal fun <T : ViewModel> getOrCreate(
      modelClass: KClass<T>,
      key: String? = null,
      factory: () -> T,
  ): T {
    check(creatingViewModel.get() != true) {
      "A Litho ViewModel factory must not request another ViewModel"
    }
    check(!closeRequested || (calculationDepth.get() ?: 0) > 0) {
      "Cannot access a ViewModel after its Litho scope was cleared"
    }
    val identity = LithoViewModelKey(modelClass, key)

    entries[identity]?.let {
      return await(it)
    }

    val candidate = ViewModelEntry()
    val existing = entries.putIfAbsent(identity, candidate)
    if (existing != null) {
      return await(existing)
    }

    creatingViewModel.set(true)
    try {
      check(!closed.get()) { "Cannot create a ViewModel after its Litho scope was cleared" }
      val viewModel =
          ComponentsSystrace.trace({ "LithoViewModel.create" }) { candidate.create(factory) }
      return returnIfOpen(candidate, viewModel).cast()
    } catch (throwable: Throwable) {
      entries.remove(identity, candidate)
      candidate.fail(throwable)
      throw throwable
    } finally {
      creatingViewModel.remove()
    }
  }

  /** Waits for an in-progress factory and rejects its result if this store closed while waiting. */
  private fun <T : ViewModel> await(entry: ViewModelEntry): T {
    val viewModel = ComponentsSystrace.trace({ "LithoViewModel.await" }) { entry.await() }
    return returnIfOpen(entry, viewModel).cast()
  }

  /** Prevents a creation result from escaping after the owning scope has closed. */
  private fun returnIfOpen(entry: ViewModelEntry, viewModel: ViewModel): ViewModel {
    if (!closed.get()) return viewModel
    entry.close()
    error("Cannot access a ViewModel after its Litho scope was cleared")
  }

  /**
   * Requests closure of this scope once. New calculations cannot acquire a usage, while models used
   * by existing calculations are cleared after the final usage ends. ViewModel access without an
   * existing usage is rejected immediately.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  fun close() {
    val shouldClose =
        synchronized(calculationLock) {
          if (closeRequested) {
            false
          } else {
            closeRequested = true
            activeCalculationCount == 0
          }
        }
    if (shouldClose) {
      closeEntries()
    }
  }

  private fun releaseCalculationLease(acquiredOnThread: Thread) {
    check(Thread.currentThread() === acquiredOnThread) {
      "A Litho ViewModel store usage must be released on the thread that acquired it"
    }
    val currentCalculationDepth = calculationDepth.get() ?: 0
    check(currentCalculationDepth > 0) {
      "No active Litho ViewModel store calculation exists on the current thread"
    }
    if (currentCalculationDepth == 1) {
      calculationDepth.set(null)
    } else {
      calculationDepth.set(currentCalculationDepth - 1)
    }

    val shouldClose =
        synchronized(calculationLock) {
          check(activeCalculationCount > 0) {
            "Litho ViewModel store active calculation count is already zero"
          }
          activeCalculationCount--
          closeRequested && activeCalculationCount == 0
        }
    if (shouldClose) {
      closeEntries()
    }
  }

  private fun closeEntries() {
    if (!closed.compareAndSet(false, true)) return
    entries.values.forEach(ViewModelEntry::close)
    entries.clear()
  }

  internal class CalculationLease(
      private val store: LithoViewModelStore,
      private val acquiredOnThread: Thread,
  ) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    override fun close() {
      if (closed.compareAndSet(false, true)) {
        store.releaseCalculationLease(acquiredOnThread)
      }
    }
  }

  private companion object {
    val creatingViewModel = ThreadLocal<Boolean>()
  }
}

/**
 * Identifies a ViewModel within one [LithoViewModelStore].
 *
 * This follows AndroidX owner-scoped semantics: the model class provides the default identity, and
 * an optional key distinguishes multiple instances of the same class in one owner.
 */
internal data class LithoViewModelKey(
    val modelClass: KClass<out ViewModel>,
    val key: String?,
)

private class ViewModelEntry {
  // State owns the atomic lifecycle transition; completion parks duplicate requests without
  // spinning until creation, failure, or closure publishes a terminal state.
  private val state = AtomicReference<EntryState>(EntryState.Creating)
  private val completion = CountDownLatch(1)

  /**
   * Runs the factory and publishes its result while the entry remains open. If the entry closes
   * before publication, the newly created ViewModel is cleared instead.
   */
  fun create(factory: () -> ViewModel): ViewModel {
    val storeHandle = ViewModelStoreHandle()
    val viewModel = storeHandle.create(factory)
    if (state.compareAndSet(EntryState.Creating, EntryState.Ready(viewModel, storeHandle))) {
      completion.countDown()
      return viewModel
    }

    storeHandle.clear()
    check(state.get() == EntryState.Closed) {
      "ViewModel entry changed unexpectedly while publishing its value"
    }
    error("Cannot publish a ViewModel after its Litho scope was cleared")
  }

  /** Publishes a factory failure and releases callers waiting for this entry. */
  fun fail(throwable: Throwable) {
    if (state.compareAndSet(EntryState.Creating, EntryState.Failed(throwable))) {
      completion.countDown()
    }
  }

  /** Waits until creation succeeds, fails, or is cancelled by scope closure. */
  fun await(): ViewModel {
    try {
      completion.await()
    } catch (interrupted: InterruptedException) {
      Thread.currentThread().interrupt()
      throw IllegalStateException("Interrupted while waiting for a ViewModel", interrupted)
    }
    return when (val current = state.get()) {
      is EntryState.Ready -> current.viewModel
      is EntryState.Failed ->
          throw IllegalStateException(
              "ViewModel creation failed on another resolve thread",
              current.throwable,
          )
      EntryState.Closed -> error("Cannot access a ViewModel after its Litho scope was cleared")
      EntryState.Creating -> error("ViewModel entry signaled completion without a terminal state")
    }
  }

  /**
   * Atomically closes this entry. Published models are cleared immediately; an in-flight creator
   * observes [EntryState.Closed] and clears the model before it can publish it.
   */
  fun close() {
    when (val previous = state.getAndSet(EntryState.Closed)) {
      EntryState.Creating -> completion.countDown()
      is EntryState.Ready -> previous.storeHandle.clear()
      is EntryState.Failed,
      EntryState.Closed -> Unit
    }
  }
}

private sealed interface EntryState {
  data object Creating : EntryState

  data class Ready(val viewModel: ViewModel, val storeHandle: ViewModelStoreHandle) : EntryState

  data class Failed(val throwable: Throwable) : EntryState

  data object Closed : EntryState
}

private class ViewModelStoreHandle {
  private val cleared = AtomicBoolean(false)
  private val store = ViewModelStore()

  /** Uses AndroidX's provider path so the created model retains standard clearing semantics. */
  @SuppressLint("ViewModelProviderInNonFragmentOrActivityClass", "WrongThread")
  fun create(factory: () -> ViewModel): ViewModel =
      ViewModelProvider.create(store, ViewModelFactory(factory))[VIEW_MODEL_KEY, ViewModel::class]

  /**
   * Clears the private AndroidX store at most once and performs AndroidX cleanup on the UI thread.
   */
  fun clear() {
    if (!cleared.compareAndSet(false, true)) return
    runOnUiThread { store.clear() }
  }

  companion object {
    private const val VIEW_MODEL_KEY = "viewModel"
  }
}

private class ViewModelFactory(private val factory: () -> ViewModel) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return factory() as T
  }
}

@Suppress("UNCHECKED_CAST") private fun <T : ViewModel> ViewModel.cast(): T = this as T
