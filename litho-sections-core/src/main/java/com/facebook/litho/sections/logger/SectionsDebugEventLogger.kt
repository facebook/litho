// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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
