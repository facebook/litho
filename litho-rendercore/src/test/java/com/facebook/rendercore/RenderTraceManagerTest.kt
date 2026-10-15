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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class RenderTraceManagerTest {

  @Before
  fun setUp() {
    RenderTraceIds.resetForTesting()
  }

  @Test
  fun `beginResolveTrace allocates new trace and render ids`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)

    manager.beginResolveForTest().markRunning()
    val second = manager.beginResolveForTest()

    assertThat(second.traceId).isEqualTo(1)
    assertThat(second.renderId).isEqualTo(1)
    assertThat(second.operation).isEqualTo(RenderTraceOperation.RESOLVE)
    assertThat(manager.currentPending).isSameAs(second)
    assertThat(manager.inFlightTraceCount()).isEqualTo(2)
    assertThat(instrumenter.begunTraces)
        .containsExactly(
            BegunTrace(0, 0, RenderTraceOperation.RESOLVE),
            BegunTrace(1, 1, RenderTraceOperation.RESOLVE),
        )
  }

  @Test
  fun `beginResolveTrace cancels previous pending trace`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)

    val first = manager.beginResolveForTest()
    val second = manager.beginResolveForTest()

    assertThat(first.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(second.currentState).isEqualTo(RenderTraceState.PENDING)
    assertThat(manager.currentPending).isSameAs(second)
    assertThat(manager.inFlightTraceCount()).isEqualTo(1)
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_SUPERSEDED_BEFORE_RUN)
  }

  @Test
  fun `beginResolveTrace does not cancel previous running trace`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)

    val first = manager.beginResolveForTest()
    first.markRunning()
    val second = manager.beginResolveForTest()

    assertThat(first.currentState).isEqualTo(RenderTraceState.RUNNING)
    assertThat(second.currentState).isEqualTo(RenderTraceState.PENDING)
    assertThat(manager.currentPending).isSameAs(second)
    assertThat(manager.inFlightTraceCount()).isEqualTo(2)
    assertThat(instrumenter.cancelledReasons).isEmpty()
  }

  @Test
  fun `beginMeasureTrace reuses caller supplied render id`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)

    val trace = manager.beginMeasureForTest(42)

    assertThat(trace.traceId).isEqualTo(0)
    assertThat(trace.renderId).isEqualTo(42)
    assertThat(trace.operation).isEqualTo(RenderTraceOperation.MEASURE)
    assertThat(instrumenter.begunTraces)
        .containsExactly(BegunTrace(0, 42, RenderTraceOperation.MEASURE))
  }

  @Test
  fun `stale onTraceCommitted arrival is no-op and leaves newer trace committed`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val older = manager.beginResolveForTest()
    older.markRunning()
    val newer = manager.beginResolveForTest()
    newer.markRunning()

    // Newer trace commits first.
    newer.markCommitted()
    // Older trace's markCommitted arrives late; should be rejected by lastCommittedTraceId
    // and become a no-op, leaving newer in COMMITTED.
    older.markCommitted()

    assertThat(newer.currentState).isEqualTo(RenderTraceState.COMMITTED)
    assertThat(older.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI)
  }

  @Test
  fun `markCommitted cancels previous committed trace that never reached UI`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markRunning()
    first.markCommitted()
    val second = manager.beginResolveForTest()
    second.markRunning()

    second.markCommitted()

    assertThat(first.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(second.currentState).isEqualTo(RenderTraceState.COMMITTED)
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI)
  }

  @Test
  fun `markCommitted does not cancel previous trace that already reached pending commit to UI`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markRunning()
    first.markCommitted()
    first.markPendingCommitToUI()
    val second = manager.beginResolveForTest()
    second.markRunning()

    second.markCommitted()

    assertThat(first.currentState).isEqualTo(RenderTraceState.PENDING_COMMIT_TO_UI)
    assertThat(instrumenter.cancelledReasons).isEmpty()
  }

  @Test
  fun `markPendingCommitToUI cancels previous pending commit that never reached UI`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markRunning()
    first.markCommitted()
    first.markPendingCommitToUI()
    val second = manager.beginResolveForTest()
    second.markRunning()
    second.markCommitted()

    second.markPendingCommitToUI()

    assertThat(first.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(second.currentState).isEqualTo(RenderTraceState.PENDING_COMMIT_TO_UI)
    assertThat(manager.currentPendingCommitToUI).isSameAs(second)
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_DEFERRED_COMMIT_TO_UI_SUPERSEDED)
  }

  @Test
  fun `markPendingCommitToUI does not cancel previous trace that already committed to UI`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markRunning()
    first.markCommitted()
    first.markPendingCommitToUI()
    first.markCommittedToUI()
    val second = manager.beginResolveForTest()
    second.markRunning()
    second.markCommitted()

    second.markPendingCommitToUI()

    assertThat(first.currentState).isEqualTo(RenderTraceState.COMMITTED_TO_UI)
    assertThat(instrumenter.cancelledReasons).isEmpty()
  }

  @Test
  fun `trace transitions update manager pointers`() {
    val manager = RenderTraceManager(RecordingInstrumenter())
    val trace = manager.beginResolveForTest()

    trace.markRunning()
    assertThat(manager.currentPending).isNull()

    trace.markCommitted()

    trace.markPendingCommitToUI()
    assertThat(manager.currentPendingCommitToUI).isSameAs(trace)

    trace.markCommittedToUI()
    assertThat(manager.currentPendingCommitToUI).isNull()
    assertThat(manager.currentCommittedToUI).isSameAs(trace)

    trace.markAwaitingCompletion(RCRenderTrace.AWAITING_REASON_MOUNT_PENDING)
    assertThat(trace.currentState).isEqualTo(RenderTraceState.AWAITING_COMPLETION)

    trace.markComplete()
    assertThat(manager.currentCompleted).isSameAs(trace)
    assertThat(manager.inFlightTraceCount()).isEqualTo(0)
  }

  @Test
  fun `cancelled trace clears active pointers and emits cancellation`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()
    trace.markRunning()
    trace.markCommitted()
    trace.markPendingCommitToUI()

    trace.markCancelled(RCRenderTrace.CANCEL_REASON_DEFERRED_COMMIT_TO_UI_SUPERSEDED)

    assertThat(trace.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(manager.currentPendingCommitToUI).isNull()
    assertThat(manager.inFlightTraceCount()).isEqualTo(0)
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_DEFERRED_COMMIT_TO_UI_SUPERSEDED)
  }

  @Test
  fun `terminal traces ignore later points and annotations`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markComplete()
    trace.addPoint("ignored")
    trace.addAnnotation("ignored", true)

    assertThat(instrumenter.successfulTraceIds).containsExactly(trace.traceId)
    assertThat(instrumenter.points.map { it.name }).doesNotContain("ignored")
    assertThat(instrumenter.annotations.map { it.key }).doesNotContain("ignored")
  }

  @Test
  fun `cancelAll cancels every in-flight trace with host destroyed reason`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markRunning()
    first.markCommitted()
    val second = manager.beginResolveForTest()
    second.markRunning()

    manager.cancelAll()

    assertThat(first.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(second.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(manager.inFlightTraceCount()).isEqualTo(0)
    assertThat(instrumenter.cancelled)
        .containsExactly(
            CancelledTrace(first.traceId, RCRenderTrace.CANCEL_REASON_HOST_DESTROYED),
            CancelledTrace(second.traceId, RCRenderTrace.CANCEL_REASON_HOST_DESTROYED),
        )
  }

  @Test
  fun `cancelAll skips terminal traces`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markComplete()
    val second = manager.beginResolveForTest()

    manager.cancelAll()

    assertThat(instrumenter.successfulTraceIds).containsExactly(first.traceId)
    assertThat(instrumenter.cancelled)
        .containsExactly(CancelledTrace(second.traceId, RCRenderTrace.CANCEL_REASON_HOST_DESTROYED))
  }

  @Test
  fun `concurrent traces advance independently when they do not conflict at commit`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val first = manager.beginResolveForTest()
    first.markRunning()
    val second = manager.beginResolveForTest()
    second.markRunning()

    // First trace runs its full lifecycle before second commits, so no supersede fires.
    first.markCommitted()
    first.markPendingCommitToUI()
    first.markCommittedToUI()
    first.markComplete()

    second.markCommitted()
    second.markPendingCommitToUI()
    second.markCommittedToUI()
    second.markComplete()

    assertThat(instrumenter.successfulTraceIds).containsExactly(first.traceId, second.traceId)
    assertThat(instrumenter.cancelled).isEmpty()
  }

  @Test
  fun `addPoint and addAnnotation forward to instrumenter while trace is in flight`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()
    val annotations = mapOf<String, Any?>("a" to 1, "b" to "x")

    trace.addPoint("checkpoint", annotations)
    trace.addAnnotation("k", "v")

    assertThat(instrumenter.points).containsExactly(Point(trace.traceId, "checkpoint", annotations))
    assertThat(instrumenter.annotations).containsExactly(Annotation(trace.traceId, "k", "v"))
  }

  private class RecordingInstrumenter : TraceInstrumenter {
    val begunTraces = mutableListOf<BegunTrace>()
    val cancelled = mutableListOf<CancelledTrace>()
    val cancelledReasons = mutableListOf<String>()
    val successfulTraceIds = mutableListOf<Int>()
    val points = mutableListOf<Point>()
    val annotations = mutableListOf<Annotation>()

    override fun beginTrace(traceId: Int, renderId: Int, operation: RenderTraceOperation) {
      begunTraces.add(BegunTrace(traceId, renderId, operation))
    }

    override fun cancelTrace(traceId: Int, reason: String) {
      cancelled.add(CancelledTrace(traceId, reason))
      cancelledReasons.add(reason)
    }

    override fun endTraceSuccess(traceId: Int) {
      successfulTraceIds.add(traceId)
    }

    override fun addPoint(
        traceId: Int,
        name: String,
        annotations: Map<String, Any?>?,
    ) {
      points.add(Point(traceId, name, annotations))
    }

    override fun addAnnotation(traceId: Int, key: String, value: Any) {
      annotations.add(Annotation(traceId, key, value))
    }
  }

  private data class BegunTrace(
      val traceId: Int,
      val renderId: Int,
      val operation: RenderTraceOperation,
  )

  private data class CancelledTrace(val traceId: Int, val reason: String)

  private data class Point(
      val traceId: Int,
      val name: String,
      val annotations: Map<String, Any?>? = null,
  )

  private data class Annotation(val traceId: Int, val key: String, val value: Any)
}

private fun RenderTraceManager.beginResolveForTest(): RCRenderTrace =
    createResolveTrace().also { begin(it) }

private fun RenderTraceManager.beginMeasureForTest(renderId: Int): RCRenderTrace =
    createMeasureTrace(renderId).also { begin(it) }
