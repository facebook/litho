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

package com.facebook.litho.rangemanager

import android.os.Looper
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.LayoutState
import com.facebook.litho.RenderSource
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEventAttributes.Attribution
import com.facebook.litho.debug.LithoDebugEventAttributes.Breadcrumb
import com.facebook.litho.debug.LithoDebugEventAttributes.CurrentSizeConstraint
import com.facebook.litho.debug.LithoDebugEventAttributes.HasMainThreadLayoutState
import com.facebook.litho.debug.LithoDebugEventAttributes.IdMatch
import com.facebook.litho.debug.LithoDebugEventAttributes.ResolveVersion
import com.facebook.litho.debug.LithoDebugEventAttributes.SizeConstraint
import com.facebook.litho.debug.LithoDebugEventAttributes.SizeSpecsMatch
import com.facebook.litho.rangemanager.LithoPreparationHealthEvent.MainThreadRender.Phase
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventBus
import com.facebook.rendercore.debug.DebugEventSubscriber
import com.meta.rangemanager.PreparationHealthObserver

/**
 * Bridges Litho's [DebugEventBus] to a [PreparationHealthObserver].
 *
 * Subscribes to [LithoDebugEvent.RenderOnMainThreadStarted] and classifies events as
 * [LithoPreparationHealthEvent.Late] or [LithoPreparationHealthEvent.Wrong], then reports them via
 * [PreparationHealthObserver.reportEvent].
 *
 * It also reports work the main thread did outside measure as
 * [LithoPreparationHealthEvent.MainThreadRender]: a whole render (e.g. a sync `setRoot` at bind),
 * only its layout (same root at a new size, so the resolve was skipped), or the rest of a
 * background resolve it interrupted. A render that resolves and lays out on main is reported once,
 * for its resolve. Not seen yet: main blocking on a resolve a background thread is still running,
 * which is emitted from that thread.
 *
 * The [keyExtractor] lambda allows each surface to parse its own key format from the debug event
 * breadcrumb (format: "binderId/cacheKey"). If the extractor returns null, the event is ignored.
 *
 * Lifecycle: call [start]/[stop] to subscribe/unsubscribe from [DebugEventBus]. While started,
 * every resolve and layout in the process builds its debug event, not only the trees this source
 * reports on, as with Litho's own `SyncRenderDetector` and `ResolveEfficiencyDetector`. Start it
 * only in debug or health-tracking modes.
 *
 * @param observer The observer to report events to.
 * @param keyExtractor Extracts a surface-specific key from the breadcrumb string. Return null to
 *   skip the event.
 */
class LithoHealthEventSource(
    private val observer: PreparationHealthObserver,
    private val keyExtractor: (breadcrumb: String) -> Any?,
) :
    DebugEventSubscriber(
        LithoDebugEvent.RenderOnMainThreadStarted,
        LithoDebugEvent.ComponentTreeResolve,
        LithoDebugEvent.ComponentTreeResume,
        LithoDebugEvent.ComponentTreeLayout,
    ) {

  private var started = false

  /** Per tree, the main-thread resolve just reported, so the layout that completes it is not. */
  private val reportedResolves = HashMap<String, ReportedResolve>()

  fun start() {
    if (started) {
      return
    }
    started = true
    DebugEventBus.subscribe(this)
  }

  fun stop() {
    if (!started) {
      return
    }
    started = false
    DebugEventBus.unsubscribe(this)
    reportedResolves.clear()
  }

  override fun onEvent(event: DebugEvent) {
    when (event.type) {
      LithoDebugEvent.RenderOnMainThreadStarted -> onRenderOnMainThreadStarted(event)
      LithoDebugEvent.ComponentTreeResolve -> onMainThreadWork(event, Phase.RESOLVE)
      LithoDebugEvent.ComponentTreeResume -> onMainThreadWork(event, Phase.RESUME)
      LithoDebugEvent.ComponentTreeLayout -> onMainThreadWork(event, Phase.LAYOUT)
    }
  }

  private fun onRenderOnMainThreadStarted(event: DebugEvent) {
    val breadcrumb: String = event.attributeOrNull(Breadcrumb) ?: return
    val key = keyExtractor(breadcrumb) ?: return

    val hasMainThreadLayoutState = event.attribute(HasMainThreadLayoutState, false)
    val sizeSpecsMatch = event.attribute(SizeSpecsMatch, true)
    val idMatch = event.attribute(IdMatch, true)

    val healthEvent =
        when {
          !hasMainThreadLayoutState -> LithoPreparationHealthEvent.Late
          !idMatch || !sizeSpecsMatch ->
              LithoPreparationHealthEvent.Wrong(
                  sizeSpecsMatch = sizeSpecsMatch,
                  idMatch = idMatch,
                  currentSizeConstraint = event.attributeOrNull(CurrentSizeConstraint),
                  incomingSizeConstraint = event.attributeOrNull(SizeConstraint),
              )
          else -> return // No actual mismatch detected — skip reporting.
        }

    observer.reportEvent(key, healthEvent)
  }

  private fun onMainThreadWork(event: DebugEvent, phase: Phase) {
    if (event.threadName != Looper.getMainLooper().thread.name) {
      return
    }
    val source = event.attributeOrNull<String>(DebugEventAttribute.Source) ?: "unknown"
    // Already reported through RenderOnMainThreadStarted.
    if (source == MEASURE_RENDER_SOURCE) {
      return
    }
    val breadcrumb: String = event.attributeOrNull(Breadcrumb) ?: return
    val key = keyExtractor(breadcrumb) ?: return
    val resolve = ReportedResolve(event.attributeOrNull(ResolveVersion), source)
    if (phase == Phase.LAYOUT) {
      if (reportedResolves.remove(event.renderStateId) == resolve) {
        return
      }
    } else {
      reportedResolves[event.renderStateId] = resolve
    }
    // A state update's resolve carries the component that requested it; layout events carry none.
    val requestedBy: String? =
        if (source == UPDATE_STATE_SYNC_SOURCE) event.attributeOrNull(Attribution) else null
    observer.reportEvent(
        key,
        LithoPreparationHealthEvent.MainThreadRender(source, phase, requestedBy),
    )
  }

  @DataClassGenerate private data class ReportedResolve(val version: Int?, val source: String)

  private companion object {
    val MEASURE_RENDER_SOURCE: String =
        LayoutState.layoutSourceToString(RenderSource.MEASURE_SET_SIZE_SPEC)
    val UPDATE_STATE_SYNC_SOURCE: String =
        LayoutState.layoutSourceToString(RenderSource.UPDATE_STATE_SYNC)
  }
}
