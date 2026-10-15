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

package com.facebook.rendercore

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap

/**
 * Execution context for a mount [continuation][MountContinuation]. Each [MountState] owns one of
 * these and routes continuation reads through the [ContinuationValueReader] supplied at
 * construction time. Integrations install their own reader to deliver continuation values produced
 * by the layout phase to the mount phase.
 */
class MountContinuationContext internal constructor(private val reader: ContinuationValueReader) :
    ContinuationContext {

  /**
   * Set around a continuation invoke so [get] records which continuation values the running
   * continuation read (and the value it applied). `null` outside of an invoke.
   */
  internal var currentRecorder: ContinuationValueRecorder? = null

  @Suppress("UNCHECKED_CAST")
  operator fun <T> get(key: Any): T? {
    val value = reader.readValue(key)
    currentRecorder?.record(key, reader.readVersion(key))
    return value as? T
  }

  /** Reads the current version for [key] without recording it as a dependency. */
  internal fun peekVersion(key: Any): Long = reader.readVersion(key)
}

/**
 * Records the continuation values a binder's continuations read so their freshness can be checked
 * cheaply on a later restart pass.
 */
internal fun interface ContinuationValueRecorder {
  fun record(key: Any, version: Long)
}

/**
 * Carries continuation values from a layout root to its mount integration. The values themselves
 * are consumed by an integration-side [ContinuationValueReader] installed on the integration's
 * [MountState].
 */
class ContinuationValues(private val values: ScatterMap<Any, Any?>) {
  fun hasValues(): Boolean = values.isNotEmpty()

  /**
   * Returns the [ScatterMap] of continuation values carried by this object, or `null` when it does
   * not carry values.
   */
  fun valuesOrNull(): ScatterMap<Any, Any?>? = if (hasValues()) values else null

  companion object {
    @JvmField val EMPTY: ContinuationValues = ContinuationValues(emptyScatterMap())
  }
}

/**
 * A [continuation][Continuation] that can be executed within the context of a
 * [MountContinuationContext] in the mount phase
 */
fun interface MountContinuation<R> : Continuation<R, MountContinuationContext>

/** Controls whether a [MountState] should register itself for restart when it mounts its tree */
sealed interface MountStateRestartPolicy {
  /**
   * The default policy will cause a mount state to register itself for restart if it has any
   * continuations in its tree.
   *
   * Continuations may exist from render units in the mount state's render tree or from registered
   * nested mount states. [Always] nested mount states are registered even when they have no local
   * continuations, but a registered mount state is restarted only when it reports pending restart
   * work.
   *
   * This policy makes restart of the specifying mount state adaptive.
   */
  object Default : MountStateRestartPolicy

  /**
   * The specifying mount state will always register itself with the nearest host continuation
   * registry. Registration makes the mount state visible to its host's restart checks; the mount
   * state is restarted only when [MountState.hasPendingRestartWork] returns true.
   */
  object Always : MountStateRestartPolicy

  /**
   * The specifying mount state will not register itself for restart. It is recommended to always
   * use the [Default] policy. However, a host may choose to specify this policy if it has a custom
   * means of restarting its mount state. An example use case for this policy would be preventing
   * automatic restarts of [RenderTreeHost]s in a recycler view for which mount is ideally restarted
   * through the adapter notification/list diffing mechanism
   */
  object Never : MountStateRestartPolicy
}

/**
 * A [MountStateRestartRegistry] keeps track of any nested [MountState]s that a [MountItem] may own.
 * The registry is used to restart the nested [MountState]s when the [MountItem] is not updated
 */
internal class MountStateRestartRegistry {
  private var mountStates: MutableScatterSet<MountState>? = null

  fun interface AttachResult {
    fun onDetached()
  }

  fun register(mountState: MountState): AttachResult {
    val mountStates = mountStates ?: MutableScatterSet<MountState>(2).also { mountStates = it }
    if (!mountStates.contains(mountState)) {
      mountStates.add(mountState)
    }

    return AttachResult { mountStates.remove(mountState) }
  }

  fun restartMountStates() {
    val mountStates = mountStates ?: return
    if (mountStates.isEmpty()) {
      return
    }
    RenderCoreSystrace.beginSection("restartNestedHostContinuations")
    mountStates.forEach { mountState -> mountState.getRootHost().requestLayout() }
    RenderCoreSystrace.endSection()
  }

  fun hasAnyMountStates(): Boolean = mountStates?.isNotEmpty() == true

  fun anyMountStateHasPendingRestartWork(): Boolean =
      mountStates?.any { it.hasPendingRestartWork() } == true

  fun clear() {
    mountStates?.clear()
    mountStates = null
  }
}

/**
 * Currently active [MountStateRestartRegistry] for nested mount state registration. Set by
 * [withHostContinuationRegistry] around the call site that owns a registry (today: a parent mount
 * applying bounds to a child mount item that holds nested mount states). Read by
 * [MountState.attachToRestartRegistryIfNeeded] when a nested mount state mounts and needs to
 * register itself with the nearest host registry.
 */
internal var currentMountStateRestartRegistry: MountStateRestartRegistry? = null

internal inline fun <R> withHostContinuationRegistry(
    registry: MountStateRestartRegistry,
    block: () -> R,
): R {
  val previous = currentMountStateRestartRegistry
  try {
    currentMountStateRestartRegistry = registry
    return block()
  } finally {
    currentMountStateRestartRegistry = previous
  }
}
