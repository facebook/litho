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

import com.facebook.rendercore.debug.DebugEvent

object LithoDebugEvent {
  val RenderCore: DebugEvent.Companion = DebugEvent
  const val RenderRequest = "Litho.RenderRequest"
  const val LayoutCommitted = "Litho.LayoutCommitted"
  const val StateUpdateEnqueued = "Litho.StateUpdateEnqueued"
  const val RenderOnMainThreadStarted = "RenderOnMainThreadStarted"
  const val ComponentPrepared = "Litho.Resolve.ComponentPrepared"
  const val ComponentResolved = "Litho.Resolve.ComponentResolved"
  const val ComponentTreeResolve = "Litho.ComponentTree.Resolve"
  const val Layout = "Litho.ComponentTree.Layout"
  const val ComponentTreeResolveResumed = "Litho.ComponentTree.Resolve.Resumed"
  const val ComponentTreeMountContentPreallocated = "Litho.ComponentTree.MountContent.Preallocated"
}

object LithoDebugEventAttributes {
  const val Root = "root"
  const val Attribution = "attribution"
  const val StateUpdateType = "state_update_type"
  const val RootId = "root_id"
  const val MainThreadLayoutStateRootId = "main_thread_layout_state_root_id"
  const val MainThreadLayoutStateWidthSpec = "main_thread_layout_state_width_spec"
  const val MainThreadLayoutStateHeightSpec = "main_thread_layout_state_height_spec"
  const val MainThreadLayoutStatePrettySizeSpecs = "main_thread_layout_state_size_specs_pretty"
  const val MeasureWidthSpec = "measure_width_spec"
  const val MeasureHeightSpec = "measure_height_spec"
  const val MeasurePrettySizeSpecs = "measure_size_specs_pretty"
  const val SizeSpecsMatch = "size_specs_match"
  const val IdMatch = "id_match"
  const val HasMainThreadLayoutState = "has_main_thread_layout_state"
  const val Breadcrumb = "breadcrumb"
  const val RenderSource = "source"
  const val RenderExecutionMode = "execution-mode"
  const val Forced = "forced"
  const val RunsOnMainThread = "runs_on_main_thread"
  const val Component = "component"
  const val ResolveVersion = "resolve_version"
  const val ResolveSource = "resolve_source"
}
