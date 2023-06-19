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

import android.annotation.SuppressLint

/** Exception class used to add additional Litho metadata to a crash. */
class LithoMetadataExceptionWrapper
private constructor(
    private val componentContext: ComponentContext?,
    private val componentTree: ComponentTree?,
    cause: Throwable
) : RuntimeException() {
  @JvmField var lastHandler: EventHandler<ErrorEvent>? = null
  private val componentNameLayoutStack = ArrayList<String>()
  private val customMetadata = HashMap<String, String>()

  init {
    initCause(cause)
    stackTrace = emptyArray()
  }

  internal constructor(cause: Throwable) : this(null, null, cause)

  internal constructor(
      componentContext: ComponentContext?,
      cause: Throwable
  ) : this(componentContext, null, cause)

  constructor(componentTree: ComponentTree?, cause: Throwable) : this(null, componentTree, cause)

  fun addComponentNameForLayoutStack(componentName: String) {
    componentNameLayoutStack.add(componentName)
  }

  // the crashing componentName is always the first in the mComponentNameLayoutStack, if it exists
  val crashingComponentName: String?
    get() = componentNameLayoutStack.firstOrNull()

  fun addCustomMetadata(key: String, value: String) {
    customMetadata[key] = value
  }

  @get:SuppressLint("BadMethodUse-java.lang.Class.getName", "ReflectionMethodUse")
  override val message: String
    get() {
      val msg = StringBuilder("Real Cause")
      val cause = deepestCause
      if (componentContext != null && componentContext.componentScope != null) {
        msg.append(" at <cls>")
            .append(componentContext.componentScope.javaClass.name)
            .append("</cls>")
      }
      msg.append(" => ")
          .append(cause.javaClass.canonicalName)
          .append(": ")
          .appendLine(cause.message)
          .appendLine("Litho Context:")
      if (componentNameLayoutStack.isNotEmpty()) {
        msg.append("  layout_stack: ")
        for (i in componentNameLayoutStack.indices.reversed()) {
          msg.append(componentNameLayoutStack[i])
          if (i != 0) {
            msg.append(" -> ")
          }
        }
        msg.appendLine()
      }
      if (componentContext?.logTag != null) {
        msg.append("  log_tag: ").appendLine(componentContext.logTag)
      } else if (componentTree?.logTag != null) {
        msg.append("  log_tag: ").appendLine(componentTree.logTag)
      }
      componentTree?.root?.let { root ->
        msg.append("  tree_root: <cls>").append(root.javaClass.name).appendLine("</cls>")
      }
      msg.append("  thread_name: ").appendLine(Thread.currentThread().name)
      msg.appendMap(customMetadata)
      return msg.toString().trim { it <= ' ' }
    }

  private val deepestCause: Throwable
    get() {
      var cause = checkNotNull(cause)
      while (true) {
        cause = cause.cause ?: break
      }
      return cause
    }

  companion object {
    const val LITHO_CONTEXT = "Litho Context:"

    private fun StringBuilder.appendMap(map: Map<String, String>) {
      for ((key, value) in map) {
        append("  ").append(key).append(": ").appendLine(value)
      }
    }
  }
}
