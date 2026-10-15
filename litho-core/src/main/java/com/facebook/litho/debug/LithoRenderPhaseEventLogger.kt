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

package com.facebook.litho.debug

import android.util.Log
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventSubscriber
import com.facebook.rendercore.debug.DebugProcessEvent
import com.facebook.rendercore.debug.TraceListener

/**
 * A focused debug event logger that only logs Litho phase events we care about. Enabled by default
 * in debug builds for easier debugging without drowning out other logcat messages.
 *
 * Filter in logcat with: adb logcat -s LithoRenderPhase
 */
class LithoRenderPhaseEventLogger :
    DebugEventSubscriber(
        // Only subscribe to events we care about for phase logging
        LithoDebugEvent.ComponentTreeResolve,
        LithoDebugEvent.ComponentTreeLayout,
        DebugEvent.RenderTreeMountStart,
        DebugEvent.RenderTreeMountEnd,
        LithoDebugEvent.AsyncLayoutDropped,
        LithoDebugEvent.MountEnd,
    ),
    TraceListener<Unit> {
  companion object {
    private const val TAG = "LithoRenderPhase"
  }

  override fun onTraceStart(
      type: String,
      timestamp: Long,
      renderStateId: String,
      attributes: Map<String, Any?>,
  ) {
    when (type) {
      LithoDebugEvent.ComponentTreeResolve -> {
        val root = attributes[LithoDebugEventAttributes.Root] ?: "?"
        val reason = attributes[LithoDebugEventAttributes.Reason] ?: "?"
        val resolveVersion = attributes[LithoDebugEventAttributes.ResolveVersion] ?: "?"
        Log.d(
            TAG,
            "RESOLVE_START|resolveVersion=$resolveVersion|reason=$reason|root=$root|state=$renderStateId",
        )
      }
      LithoDebugEvent.ComponentTreeLayout -> {
        val root = attributes[LithoDebugEventAttributes.Root] ?: "?"
        val layoutVersion = attributes[LithoDebugEventAttributes.LayoutVersion] ?: "?"
        val source = attributes["source"] ?: "?"
        val sizeConstraints = attributes["sizeConstraints"] ?: "?"
        Log.d(
            TAG,
            "LAYOUT_START|layoutVersion=$layoutVersion|root=$root|source=$source|sizeConstraints=$sizeConstraints|state=$renderStateId",
        )
      }
    }
  }

  override fun onTraceEnd(payload: Unit, event: DebugProcessEvent) {
    when (event.type) {
      LithoDebugEvent.ComponentTreeResolve -> {
        val attrs = event.attributes
        val resolveVersion = attrs[LithoDebugEventAttributes.ResolveVersion] ?: "?"
        val globalKeysChanged = attrs[LithoDebugEventAttributes.GlobalKeysChanged] ?: "?"
        val componentCount = attrs[LithoDebugEventAttributes.ComponentCount] ?: "?"
        val components = attrs[LithoDebugEventAttributes.Components] ?: "?"
        Log.d(
            TAG,
            "RESOLVE_END|resolveVersion=$resolveVersion|globalKeysChanged=$globalKeysChanged|componentCount=$componentCount|components=$components|state=${event.renderStateId}",
        )
      }
      LithoDebugEvent.ComponentTreeLayout -> {
        val attrs = event.attributes
        val layoutVersion = attrs[LithoDebugEventAttributes.LayoutVersion] ?: "?"
        val reused = attrs[LithoDebugEventAttributes.Reused] ?: "?"
        val boundsChanged = attrs[LithoDebugEventAttributes.BoundsChanged] ?: "?"
        Log.d(
            TAG,
            "LAYOUT_END|layoutVersion=$layoutVersion|reused=$reused|boundsChanged=$boundsChanged|state=${event.renderStateId}",
        )
      }
    }
  }

  override fun onEvent(event: DebugEvent) {
    when (event.type) {
      // ComponentTreeResolve and ComponentTreeLayout are handled by TraceListener
      DebugEvent.RenderTreeMountStart -> logMountStart(event)
      DebugEvent.RenderTreeMountEnd -> logMountEnd(event)
      LithoDebugEvent.AsyncLayoutDropped -> logAsyncLayoutDropped(event)
      LithoDebugEvent.MountEnd -> logLithoMountEnd(event)
    }
  }

  private fun logMountStart(event: DebugEvent) {
    val attrs = event.attributes
    val numOutputs = attrs["numMountableOutputs"] ?: "?"

    Log.d(TAG, "MOUNT_START|output-count=$numOutputs|state=${event.renderStateId}")
  }

  private fun logMountEnd(event: DebugEvent) {
    val attrs = event.attributes
    val mountedCount = attrs[LithoDebugEventAttributes.MountedCount] ?: "?"
    val unmountedCount = attrs[LithoDebugEventAttributes.UnmountedCount] ?: "?"
    val updatedCount = attrs[LithoDebugEventAttributes.UpdatedCount] ?: "?"
    val reusedCount = attrs[LithoDebugEventAttributes.ReusedCount] ?: "?"
    val remountedCount = attrs[LithoDebugEventAttributes.RemountedCount] ?: "?"
    val summaryByComponent = attrs[LithoDebugEventAttributes.SummaryByComponent] ?: "{}"

    Log.d(
        TAG,
        "MOUNT_END|mounted=$mountedCount|unmounted=$unmountedCount|updated=$updatedCount|reused=$reusedCount|remounted=$remountedCount|summary=$summaryByComponent|state=${event.renderStateId}",
    )
  }

  private fun logAsyncLayoutDropped(event: DebugEvent) {
    val attrs = event.attributes
    val root = attrs[LithoDebugEventAttributes.Root] ?: "?"
    val version = attrs["version"] ?: "?"
    val committedVersion = attrs["committed_version"] ?: "?"
    val layoutVersion = attrs[LithoDebugEventAttributes.LayoutVersion] ?: "?"
    val componentCount = attrs[LithoDebugEventAttributes.ComponentCount] ?: "?"

    Log.d(
        TAG,
        "ASYNC_LAYOUT_DROPPED|root=$root|version=$version|committedVersion=$committedVersion|layoutVersion=$layoutVersion|componentCount=$componentCount|state=${event.renderStateId}",
    )
  }

  private fun logLithoMountEnd(event: DebugEvent) {
    val attrs = event.attributes
    val layoutVersion = attrs[LithoDebugEventAttributes.LayoutVersion] ?: "?"
    Log.d(TAG, "MOUNT_INFO|layoutVersion=$layoutVersion|state=${event.renderStateId}")
  }
}
