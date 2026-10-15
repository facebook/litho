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

class RCRenderTraceTest {

  @Before
  fun setUp() {
    RenderTraceIds.resetForTesting()
  }

  @Test
  fun `forward path emits expected points and success`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markRunning()
    trace.markCommitted()
    trace.markPendingCommitToUI()
    trace.markCommittedToUI()
    trace.markAwaitingCompletion(RCRenderTrace.AWAITING_REASON_MOUNT_PENDING)
    trace.markComplete()

    assertThat(instrumenter.points.map { it.name })
        .containsExactly(
            RenderTraceManager.POINT_TRACE_RUNNING,
            RenderTraceManager.POINT_TRACE_COMMITTED,
            RenderTraceManager.POINT_TRACE_PENDING_COMMIT_TO_UI,
            RenderTraceManager.POINT_TRACE_COMMITTED_TO_UI,
            RenderTraceManager.POINT_TRACE_AWAITING_COMPLETION,
        )
    assertThat(instrumenter.points.last().annotations)
        .containsEntry(
            RenderTraceManager.KEY_AWAITING_COMPLETION_REASON,
            RCRenderTrace.AWAITING_REASON_MOUNT_PENDING,
        )
    assertThat(instrumenter.successfulTraceIds).containsExactly(trace.traceId)
    assertThat(trace.currentState).isEqualTo(RenderTraceState.COMPLETE)
    assertThat(trace.isTerminal).isTrue()
  }

  @Test
  fun `trace state only advances forward`() {
    val manager = RenderTraceManager(RecordingInstrumenter())
    val trace = manager.beginResolveForTest()

    trace.markPendingCommitToUI()
    trace.markRunning()
    trace.markCommitted()

    assertThat(trace.currentState).isEqualTo(RenderTraceState.PENDING_COMMIT_TO_UI)
  }

  @Test
  fun `markRunning twice only emits running point once`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markRunning()
    trace.markRunning()

    assertThat(instrumenter.points.map { it.name })
        .containsExactly(RenderTraceManager.POINT_TRACE_RUNNING)
  }

  @Test
  fun `markCommitted without markRunning advances directly to committed`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markCommitted()

    assertThat(trace.currentState).isEqualTo(RenderTraceState.COMMITTED)
    assertThat(instrumenter.points.map { it.name })
        .containsExactly(RenderTraceManager.POINT_TRACE_COMMITTED)
  }

  @Test
  fun `cancelled pending trace emits cancel without prior points`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markCancelled(RCRenderTrace.CANCEL_REASON_SUPERSEDED_BEFORE_RUN)

    assertThat(trace.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(instrumenter.points).isEmpty()
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_SUPERSEDED_BEFORE_RUN)
  }

  @Test
  fun `cancelled committed trace emits cancel and is terminal`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()
    trace.markRunning()
    trace.markCommitted()

    trace.markCancelled(RCRenderTrace.CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI)

    assertThat(trace.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(trace.isTerminal).isTrue()
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_COMMIT_SUPERSEDED_BEFORE_COMMIT_TO_UI)
  }

  @Test
  fun `point and annotation before terminal are forwarded`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.addPoint("custom_point", mapOf("source" to "test"))
    trace.addAnnotation("custom_key", "custom_value")

    assertThat(instrumenter.points)
        .containsExactly(Point("custom_point", mapOf("source" to "test")))
    assertThat(instrumenter.annotations).containsExactly(Annotation("custom_key", "custom_value"))
  }

  @Test
  fun `complete trace ignores cancellation`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markComplete()
    trace.markCancelled(RCRenderTrace.CANCEL_REASON_HOST_DESTROYED)

    assertThat(trace.currentState).isEqualTo(RenderTraceState.COMPLETE)
    assertThat(instrumenter.successfulTraceIds).containsExactly(trace.traceId)
    assertThat(instrumenter.cancelledReasons).isEmpty()
  }

  @Test
  fun `cancelled trace ignores completion`() {
    val instrumenter = RecordingInstrumenter()
    val manager = RenderTraceManager(instrumenter)
    val trace = manager.beginResolveForTest()

    trace.markCancelled(RCRenderTrace.CANCEL_REASON_HOST_DESTROYED)
    trace.markComplete()

    assertThat(trace.currentState).isEqualTo(RenderTraceState.CANCELLED)
    assertThat(instrumenter.cancelledReasons)
        .containsExactly(RCRenderTrace.CANCEL_REASON_HOST_DESTROYED)
    assertThat(instrumenter.successfulTraceIds).isEmpty()
  }

  private class RecordingInstrumenter : TraceInstrumenter {
    val cancelledReasons = mutableListOf<String>()
    val successfulTraceIds = mutableListOf<Int>()
    val points = mutableListOf<Point>()
    val annotations = mutableListOf<Annotation>()

    override fun beginTrace(traceId: Int, renderId: Int, operation: RenderTraceOperation) = Unit

    override fun cancelTrace(traceId: Int, reason: String) {
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
      points.add(Point(name, annotations))
    }

    override fun addAnnotation(traceId: Int, key: String, value: Any) {
      annotations.add(Annotation(key, value))
    }
  }

  private data class Point(val name: String, val annotations: Map<String, Any?>? = null)

  private data class Annotation(val key: String, val value: Any)
}

private fun RenderTraceManager.beginResolveForTest(): RCRenderTrace =
    createResolveTrace().also { begin(it) }
