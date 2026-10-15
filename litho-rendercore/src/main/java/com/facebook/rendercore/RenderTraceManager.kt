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
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class RenderTraceManager(private val instrumenter: TraceInstrumenter) {
  private val lock = Any()

  @GuardedBy("lock") private val inFlightTraces = MutableIntObjectMap<RCRenderTrace>()

  @Volatile
  var currentPending: RCRenderTrace? = null
    private set

  @Volatile private var currentCommitted: RCRenderTrace? = null

  @GuardedBy("lock") private var lastCommittedTraceId: Int = -1

  @Volatile
  var currentPendingCommitToUI: RCRenderTrace? = null
    private set

  @Volatile
  var currentCommittedToUI: RCRenderTrace? = null
    private set

  @Volatile
  var currentCompleted: RCRenderTrace? = null
    private set

  /**
   * Pure factory — allocates ids and constructs an [RCRenderTrace] for a resolve operation. No
   * registry update, no supersede, no instrumenter callback; safe to call while holding any
   * caller-side lock. Caller MUST call [begin] before the trace is observable to anything that
   * checks the in-flight registry (e.g. before firing `markRunning`).
   */
  @AnyThread
  fun createResolveTrace(): RCRenderTrace = RCRenderTrace(
      RenderTraceIds.nextTraceId(),
      RenderTraceIds.nextRenderId(),
      RenderTraceOperation.RESOLVE,
      this,
  )

  /**
   * [createResolveTrace] variant that reuses the caller-supplied [renderId] for a measure-only
   * operation.
   */
  @AnyThread
  fun createMeasureTrace(renderId: Int): RCRenderTrace = RCRenderTrace(
      RenderTraceIds.nextTraceId(),
      renderId,
      RenderTraceOperation.MEASURE,
      this,
  )

  /**
   * Registers [trace] as the current pending render, supersedes any prior pending trace that hasn't
   * started running yet, and fires `instrumenter.beginTrace`. Callers should invoke this after
   * releasing any of their own internal locks so the instrumenter callback never runs under
   * caller-held monitors.
   */
  @AnyThread
  fun begin(trace: RCRenderTrace) {
    var prevToSupersede: RCRenderTrace? = null

    synchronized(lock) {
      val pending = currentPending
      prevToSupersede = pending?.takeIf { it.currentState == RenderTraceState.PENDING }
      inFlightTraces.put(trace.traceId, trace)
      currentPending = trace
    }

    prevToSupersede?.markCancelled(RCRenderTrace.CANCEL_REASON_SUPERSEDED_BEFORE_RUN)
    instrumenter.beginTrace(trace.traceId, trace.renderId, trace.operation)
  }

  @AnyThread
  internal fun cancelAll(reason: String = RCRenderTrace.CANCEL_REASON_HOST_DESTROYED) {
    val traces =
        synchronized(lock) {
          val snapshot = ArrayList<RCRenderTrace>(inFlightTraces.size)
          inFlightTraces.forEachValue { snapshot.add(it) }
          snapshot
        }
    for (trace in traces) {
      trace.markCancelled(reason)
    }
  }

  @AnyThread
  internal fun onTraceRunning(trace: RCRenderTrace) {
    synchronized(lock) {
      if (currentPending === trace) {
        currentPending = null
      }
    }
    addPoint(trace, POINT_TRACE_RUNNING, null)
  }

  @AnyThread
  internal fun onTraceCommitted(trace: RCRenderTrace) {
    // Reject stale arrivals — an older commit racing behind a newer one would otherwise
    // overwrite currentCommitted and cancel the newer trace.
    var prevToCancel: RCRenderTrace? = null
    var shouldCancelIncomingTrace = false
    synchronized(lock) {
      shouldCancelIncomingTrace = trace.traceId < lastCommittedTraceId
      if (!shouldCancelIncomingTrace) {
        lastCommittedTraceId = trace.traceId
        prevToCancel = currentCommitted?.takeIf { it.currentState == RenderTraceState.COMMITTED }
        currentCommitted = trace
      }
    }

    if (shouldCancelIncomingTrace) {
      trace.markCancelled(RCRenderTrace.CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI)
    } else {
      prevToCancel?.markCancelled(RCRenderTrace.CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI)
      addPoint(trace, POINT_TRACE_COMMITTED, null)
    }
  }

  @UiThread
  internal fun onTracePendingCommitToUI(trace: RCRenderTrace) {
    val prev = currentPendingCommitToUI
    if (
        prev != null && prev !== trace && prev.currentState == RenderTraceState.PENDING_COMMIT_TO_UI
    ) {
      prev.markCancelled(RCRenderTrace.CANCEL_REASON_DEFERRED_COMMIT_TO_UI_SUPERSEDED)
    }
    synchronized(lock) {
      if (currentCommitted === trace) {
        currentCommitted = null
      }
      currentPendingCommitToUI = trace
    }
    addPoint(trace, POINT_TRACE_PENDING_COMMIT_TO_UI, null)
  }

  @AnyThread
  internal fun onTraceCommittedToUI(trace: RCRenderTrace) {
    synchronized(lock) {
      if (currentPendingCommitToUI === trace) {
        currentPendingCommitToUI = null
      }
      currentCommittedToUI = trace
    }
    addPoint(trace, POINT_TRACE_COMMITTED_TO_UI, null)
  }

  @AnyThread
  internal fun onTraceAwaitingCompletion(trace: RCRenderTrace, reason: String?) {
    addPoint(
        trace,
        POINT_TRACE_AWAITING_COMPLETION,
        if (reason != null) mapOf(KEY_AWAITING_COMPLETION_REASON to reason) else null,
    )
  }

  @AnyThread
  internal fun onTraceComplete(trace: RCRenderTrace) {
    synchronized(lock) {
      currentCompleted = trace
      unregisterLocked(trace)
    }
    instrumenter.endTraceSuccess(trace.traceId)
  }

  @AnyThread
  internal fun onTraceCancelled(trace: RCRenderTrace, reason: String) {
    synchronized(lock) { unregisterLocked(trace) }
    instrumenter.cancelTrace(trace.traceId, reason)
  }

  @AnyThread
  internal fun addPoint(
      trace: RCRenderTrace,
      name: String,
      annotations: Map<String, Any?>?,
  ) {
    if (isInFlight(trace)) {
      instrumenter.addPoint(trace.traceId, name, annotations)
    }
  }

  @AnyThread
  internal fun addAnnotation(trace: RCRenderTrace, key: String, value: Any) {
    if (isInFlight(trace)) {
      instrumenter.addAnnotation(trace.traceId, key, value)
    }
  }

  @VisibleForTesting
  internal fun inFlightTraceCount(): Int = synchronized(lock) { inFlightTraces.size }

  private fun isInFlight(trace: RCRenderTrace): Boolean =
      synchronized(lock) { inFlightTraces.containsKey(trace.traceId) }

  @GuardedBy("lock")
  private fun unregisterLocked(trace: RCRenderTrace) {
    inFlightTraces.remove(trace.traceId)
    if (currentPending === trace) {
      currentPending = null
    }
    if (currentCommitted === trace) {
      currentCommitted = null
    }
    if (currentPendingCommitToUI === trace) {
      currentPendingCommitToUI = null
    }
    if (currentCommittedToUI === trace) {
      currentCommittedToUI = null
    }
  }

  companion object {
    const val POINT_TRACE_RUNNING: String = "trace_running"
    const val POINT_TRACE_COMMITTED: String = "render_committed"
    const val POINT_TRACE_PENDING_COMMIT_TO_UI: String = "pending_commit_to_ui"
    const val POINT_TRACE_COMMITTED_TO_UI: String = "committed_to_ui"
    const val POINT_TRACE_AWAITING_COMPLETION: String = "awaiting_completion"
    const val POINT_RESOLVE_START: String = "resolve_start"
    const val POINT_RESOLVE_END: String = "resolve_end"
    const val POINT_LAYOUT_START: String = "layout_start"
    const val POINT_LAYOUT_END: String = "layout_end"
    const val POINT_WAIT_FOR_RESOLVE_END: String = "wait_for_resolve_end"
    const val KEY_AWAITING_COMPLETION_REASON: String = "awaiting_completion_reason"
  }
}

internal object RenderTraceIds {
  private val nextTraceId = AtomicInteger(0)
  private val nextRenderId = AtomicInteger(0)

  fun nextTraceId(): Int = nextTraceId.getAndIncrement()

  fun nextRenderId(): Int = nextRenderId.getAndIncrement()

  @VisibleForTesting
  fun resetForTesting() {
    nextTraceId.set(0)
    nextRenderId.set(0)
  }
}
