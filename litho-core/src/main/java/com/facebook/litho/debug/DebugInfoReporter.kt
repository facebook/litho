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

import com.facebook.litho.debug.LithoDebugEvent.NoRenderStateId
import com.facebook.rendercore.LogLevel
import com.facebook.rendercore.RenderState
import com.facebook.rendercore.debug.DebugEventAttribute.Name
import com.facebook.rendercore.debug.DebugEventDispatcher.dispatch

/**
 * The DebugInfoReporter is used to report debug information using the [DebugEventDispatcher]. These
 * events are recorded as LITHO_DEBUG_INFO QPL events. It is recommended to gate debug logging to
 * reduce the overhead of logging and control the sampling rate.
 */
object DebugInfoReporter {

  /** @param [category] the debug event category. It is saved as the 'name' attribute in QPL */
  @JvmStatic
  fun report(
      category: String,
      renderStateId: Int = RenderState.NO_ID,
      write: MutableMap<String, Any?>.() -> Unit,
  ) {
    dispatch(
        LithoDebugEvent.DebugInfo,
        if (renderStateId == RenderState.NO_ID) {
          NoRenderStateId
        } else {
          { renderStateId.toString() }
        },
        LogLevel.DEBUG,
    ) { attribute ->
      attribute.write()
      attribute[Name] = category
    }
  }
}
