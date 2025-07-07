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

@file:JvmName("ComponentRenderer")

package com.facebook.litho

import com.facebook.litho.state.StateReadRecorder
import com.facebook.litho.state.UiStateReadRecords

/**
 * Wrapper around the Component-specific render implementation.
 *
 * @param resolveContext The ResolveContext for the Component being rendered.
 * @param render The Component-specific render implementation.
 * @return The RenderResult from the Component-specific render implementation.
 * @see Component.resolve
 * @see KComponent.render
 * @see PrimitiveComponent.render
 * @see SpecGeneratedComponent.render
 */
internal inline fun <T> ScopedComponentInfo.runInRecorderScope(
    resolveContext: ResolveContext,
    crossinline render: () -> RenderResult<T>
): RenderResult<T> {
  return if (context.isReadTrackingEnabled) {
    var result: RenderResult<T>? = null
    val stateReads = StateReadRecorder.record(resolveContext.treeId) { result = render() }
    context.scopedComponentInfo.stateReads = stateReads
    checkNotNull(result)
  } else {
    render()
  }
}

internal val ComponentContext.isReadTrackingEnabled: Boolean
  // Workaround for package-private calls directly in an inline function that seem
  // to generate bytecode that's incompatible with Java call-sites
  get() = lithoTree?.isReadTrackingEnabled == true

internal val ComponentContext.uiStateReadRecords: UiStateReadRecords
  get() =
      checkNotNull(lithoTree?.uiStateReadRecordsProvider?.getUiStateReadRecords()) {
        "Could not retrieve the UI state read records. This is likely because the LithoTree is not initialized."
      }
