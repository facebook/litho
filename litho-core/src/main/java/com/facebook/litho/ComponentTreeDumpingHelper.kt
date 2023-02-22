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

package com.facebook.litho

import android.view.View
import java.lang.StringBuilder

/**
 * Helper class in charge of dumping the component hierarchy related to a provided
 * [ComponentContext]
 *
 * This class is intended to be used only in debug environments due to limitations with the
 * preservation of the view hierarchy
 *
 * This will not provide a reliable representation of the hierarchy on non debug build of the app
 */
object ComponentTreeDumpingHelper {

  /** Dumps the tree related to the provided component context */
  @JvmStatic
  fun dumpContextTree(componentTree: ComponentTree?): String? {
    // Getting the base of the tree
    val rootComponent = DebugComponent.getRootInstance(componentTree) ?: return null
    val sb = StringBuilder()
    logComponent(rootComponent, 0, sb)
    return sb.toString()
  }

  /** Logs the content of a single debug component instance */
  private fun logComponent(debugComponent: DebugComponent, depth: Int, sb: StringBuilder) {
    // Logging the component name
    sb.append(debugComponent.component.simpleName)

    // Description of the component status (Visible, Has Click Handler)
    sb.append('{')
    val lithoView = debugComponent.lithoView
    val layout = debugComponent.layoutNode
    sb.append(if (lithoView != null && lithoView.visibility == View.VISIBLE) "V" else "H")
    if (layout != null && layout.clickHandler != null) {
      sb.append(" [clickable]")
    }
    if (layout != null) {
      sb.append(" ")
      sb.append(layout.layoutHeight)
      sb.append("x")
      sb.append(layout.layoutWidth)
    }
    sb.append('}')
    for (child in debugComponent.childComponents) {
      sb.append("\n")
      for (i in 0..depth) {
        sb.append("  ")
      }
      logComponent(child, depth + 1, sb)
    }
  }
}
