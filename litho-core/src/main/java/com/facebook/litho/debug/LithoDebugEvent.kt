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
  const val LayoutCommitted = "Litho.LayoutCommitted"
  const val LayoutCalculated = "Litho.LayoutCalculated"
  const val MeasureSizeSpecsMismatch = "SizeSpecsMismatch"
}

object LithoDebugEventAttributes {

  const val Root = "root"
  const val MainThreadLayoutStateWidthSpec = "main_thread_layout_state_width_spec"
  const val MainThreadLayoutStateHeightSpec = "main_thread_layout_state_height_spec"
  const val MeasureWidthSpec = "measure_width_spec"
  const val MeasureHeightSpec = "measure_height_spec"
  const val Breadcrumb = "breadcrumb"
}
