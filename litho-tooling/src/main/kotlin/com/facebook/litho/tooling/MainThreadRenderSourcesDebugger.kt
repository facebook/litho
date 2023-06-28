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

package com.facebook.litho.tooling

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEventAttributes.Breadcrumb
import com.facebook.litho.debug.LithoDebugEventAttributes.HasMainThreadLayoutState
import com.facebook.litho.debug.LithoDebugEventAttributes.MainThreadLayoutStatePrettySizeSpecs
import com.facebook.litho.debug.LithoDebugEventAttributes.MainThreadLayoutStateRootId
import com.facebook.litho.debug.LithoDebugEventAttributes.MeasurePrettySizeSpecs
import com.facebook.litho.debug.LithoDebugEventAttributes.Root
import com.facebook.litho.debug.LithoDebugEventAttributes.RootId
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventSubscriber

/**
 * This subscriber listens for Litho framework events related to sources of renders forced to be
 * done in the main thread. This was created with the goal of flagging these inconsistencies in
 * different features that use Litho due to the need to specify the size specs in advance.
 */
class MainThreadRenderSourcesDebugger(
    private val onMainThreadRenderDetected: OnMainThreadRenderDetected
) : DebugEventSubscriber(LithoDebugEvent.RenderOnMainThreadStarted) {

  override fun onEvent(event: DebugEvent) {
    val eventBreadcrumb: String = event.attributeOrNull(Breadcrumb) ?: return
    if (event.type != LithoDebugEvent.RenderOnMainThreadStarted) return

    val hasMainThreadLayoutState = event.attribute(HasMainThreadLayoutState, false)
    if (hasMainThreadLayoutState) {
      onMainThreadLayoutStateMismatch(eventBreadcrumb, event)
    } else {
      onMainThreadLayoutStateNotPresent(eventBreadcrumb, event)
    }
  }

  private fun onMainThreadLayoutStateMismatch(breadcrumb: String, event: DebugEvent) {
    onMainThreadRenderDetected.onMainThreadRenderDetected(
        breadcrumb,
        MainThreadRenderSource.MainThreadLayoutStateMismatch(
            root = event.attribute(Root),
            mainThreadLayoutStateSizeSpecs = event.attribute(MainThreadLayoutStatePrettySizeSpecs),
            mainThreadLayoutRootId = event.attribute(MainThreadLayoutStateRootId),
            measureSizeSpecs = event.attribute(MeasurePrettySizeSpecs),
            rootId = event.attribute(RootId),
            breadcrumb = breadcrumb))
  }

  private fun onMainThreadLayoutStateNotPresent(breadcrumb: String, event: DebugEvent) {
    onMainThreadRenderDetected.onMainThreadRenderDetected(
        breadcrumb,
        MainThreadRenderSource.NoMainThreadLayoutState(
            root = event.attribute(Root), breadcrumb = breadcrumb))
  }

  fun interface OnMainThreadRenderDetected {

    fun onMainThreadRenderDetected(breadcrumb: String, source: MainThreadRenderSource)
  }

  sealed class MainThreadRenderSource {

    @DataClassGenerate(toString = Mode.KEEP, equalsHashCode = Mode.KEEP)
    data class NoMainThreadLayoutState(val root: String, val breadcrumb: String) :
        MainThreadRenderSource()

    @DataClassGenerate(toString = Mode.KEEP, equalsHashCode = Mode.KEEP)
    data class MainThreadLayoutStateMismatch(
        val root: String,
        val mainThreadLayoutStateSizeSpecs: String,
        val mainThreadLayoutRootId: Int,
        val measureSizeSpecs: String,
        val rootId: Int,
        val breadcrumb: String
    ) : MainThreadRenderSource()
  }
}
