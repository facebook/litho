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

package com.facebook.litho.sections.logger

import com.facebook.litho.debug.LithoDebugEvent
import com.facebook.litho.debug.LithoDebugEvent.NoRenderStateId
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventDispatcher

object SectionsDebugEventLogger {

  @JvmStatic
  @JvmName("log")
  fun log(name: String, writeAttributes: (MutableMap<String, Any?>) -> Unit) {
    DebugEventDispatcher.dispatch(LithoDebugEvent.DebugInfo, NoRenderStateId) { attributes ->
      writeAttributes(attributes)
      attributes[DebugEventAttribute.Name] = name
    }
  }
}
