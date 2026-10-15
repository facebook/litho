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

import androidx.annotation.AnyThread
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe

enum class RenderTraceOperation {
  RESOLVE,
  MEASURE,
}

enum class RenderTraceState {
  PENDING,
  RUNNING,
  COMMITTED,
  PENDING_COMMIT_TO_UI,
  COMMITTED_TO_UI,
  AWAITING_COMPLETION,
  COMPLETE,
  CANCELLED,
}

interface TraceInstrumenter {
  fun beginTrace(traceId: Int, renderId: Int, operation: RenderTraceOperation)

  fun cancelTrace(traceId: Int, reason: String)

  fun endTraceSuccess(traceId: Int)

  fun addPoint(traceId: Int, name: String, annotations: Map<String, Any?>?)

  fun addAnnotation(traceId: Int, key: String, value: Any)
}

@ThreadSafe
class RCRenderTrace
internal constructor(
    val traceId: Int,
    val renderId: Int,
    val operation: RenderTraceOperation,
    private val manager: RenderTraceManager,
) {
  private val state = AtomicReference(RenderTraceState.PENDING)

  val currentState: RenderTraceState
    get() = state.get()

  val isTerminal: Boolean
    get() {
      val current = state.get()
      return current == RenderTraceState.COMPLETE || current == RenderTraceState.CANCELLED
    }

  @AnyThread
  fun markAwaitingCompletion(reason: String? = null) {
    if (advanceTo(RenderTraceState.AWAITING_COMPLETION)) {
      manager.onTraceAwaitingCompletion(this, reason)
    }
  }

  @AnyThread
  fun markComplete() {
    if (advanceTo(RenderTraceState.COMPLETE)) {
      manager.onTraceComplete(this)
    }
  }

  @AnyThread
  fun addPoint(name: String, annotations: Map<String, Any?>? = null) {
    if (!isTerminal) {
      manager.addPoint(this, name, annotations)
    }
  }

  @AnyThread
  fun addAnnotation(key: String, value: Any) {
    if (!isTerminal) {
      manager.addAnnotation(this, key, value)
    }
  }

  @AnyThread
  internal fun markRunning() {
    if (advanceTo(RenderTraceState.RUNNING)) {
      manager.onTraceRunning(this)
    }
  }

  @AnyThread
  internal fun markCommitted() {
    if (advanceTo(RenderTraceState.COMMITTED)) {
      manager.onTraceCommitted(this)
    }
  }

  @AnyThread
  internal fun markPendingCommitToUI() {
    if (advanceTo(RenderTraceState.PENDING_COMMIT_TO_UI)) {
      manager.onTracePendingCommitToUI(this)
    }
  }

  @AnyThread
  internal fun markCommittedToUI() {
    if (advanceTo(RenderTraceState.COMMITTED_TO_UI)) {
      manager.onTraceCommittedToUI(this)
    }
  }

  @AnyThread
  internal fun markCancelled(reason: String) {
    if (transitionToCancelled()) {
      manager.onTraceCancelled(this, reason)
    }
  }

  private fun advanceTo(target: RenderTraceState): Boolean {
    while (true) {
      val current = state.get()
      if (current == RenderTraceState.COMPLETE || current == RenderTraceState.CANCELLED) {
        return false
      }
      if (current.ordinal >= target.ordinal) {
        return false
      }
      if (state.compareAndSet(current, target)) {
        return true
      }
    }
  }

  private fun transitionToCancelled(): Boolean {
    while (true) {
      val current = state.get()
      if (current == RenderTraceState.COMPLETE || current == RenderTraceState.CANCELLED) {
        return false
      }
      if (state.compareAndSet(current, RenderTraceState.CANCELLED)) {
        return true
      }
    }
  }

  companion object {
    const val CANCEL_REASON_SUPERSEDED_BEFORE_RUN: String = "superseded_before_run"
    const val CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI: String =
        "commit_superseded_before_commit_to_ui"
    const val CANCEL_REASON_DEFERRED_COMMIT_TO_UI_SUPERSEDED: String =
        "deferred_commit_to_ui_superseded"
    const val CANCEL_REASON_HOST_DESTROYED: String = "host_destroyed"
    const val CANCEL_REASON_NEWER_RESOLVE_COMMITTED: String = "newer_resolve_committed"
    const val CANCEL_REASON_NEWER_LAYOUT_COMMITTED: String = "newer_layout_committed"
    const val CANCEL_REASON_RESOLVE_FAILED: String = "resolve_failed"
    const val CANCEL_REASON_LAYOUT_FAILED: String = "layout_failed"
    const val AWAITING_REASON_MOUNT_PENDING: String = "mount_pending"
  }
}
