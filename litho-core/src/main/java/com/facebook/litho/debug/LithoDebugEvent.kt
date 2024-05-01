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

import android.os.Process
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventDispatcher

object LithoDebugEvent {

  @JvmField var enableStackTraces: Boolean = true

  @JvmStatic
  fun generateStateTrace(): String {
    return if (enableStackTraces) {
      buildString {
        Throwable().stackTrace.forEachIndexed { index, item ->
          if (index > 4) { // skip first 4 elements since they are internal
            if (index > 5) { // add the joiner after the first element
              append('\n')
            }
            append(if (index > 5) "|${" ".repeat(12)}$item" else item.toString())
          }
        }
      }
    } else {
      "<disabled>"
    }
  }

  val RenderCore: DebugEvent.Companion = DebugEvent
  const val RenderRequest = "Litho.RenderRequest"
  const val LayoutCommitted = "Litho.LayoutCommitted"
  const val StateUpdateEnqueued = "Litho.StateUpdateEnqueued"
  const val RenderOnMainThreadStarted = "RenderOnMainThreadStarted"
  const val ComponentResolved = "Litho.Resolve.ComponentResolved"
  const val ComponentResolveStart = "Litho.Resolve.ComponentResolved.Start"
  const val ComponentRendered = "Litho.Resolve.ComponentRendered"
  const val ComponentTreeResolve = "Litho.ComponentTree.Resolve"
  const val ComponentTreeLayout = "Litho.ComponentTree.Layout"
  const val ComponentTreeResume = "Litho.ComponentTree.Resume"
  const val TreeFutureRun = "Litho.TreeFuture.Run"
  const val TreeFutureGet = "Litho.TreeFuture.Get"
  const val TreeFutureWait = "Litho.TreeFuture.Wait"
  const val TreeFutureInterrupt = "Litho.TreeFuture.Interrupt"
  const val TreeFutureResume = "Litho.TreeFuture.Resume"
  const val ComponentTreeMountContentPreallocated = "Litho.ComponentTree.MountContent.Preallocated"
  const val DebugInfo = "Litho.DebugInfo" // used to report debug events upstream
}

object LithoDebugEventAttributes {
  const val Root = "root"
  const val Attribution = "attribution"
  const val RootId = "root_id"
  const val CurrentRootId = "current_root_id"
  const val CurrentWidthSpec = "current_width_spec"
  const val CurrentHeightSpec = "current_height_spec"
  const val CurrentSizeConstraint = "current_size_constraint"
  const val SizeConstraint = "size_constraint"
  const val SizeSpecsMatch = "size_specs_match"
  const val IdMatch = "id_match"
  const val HasMainThreadLayoutState = "has_main_thread_layout_state"
  const val Breadcrumb = "breadcrumb"
  const val RenderExecutionMode = "execution-mode"
  const val Forced = "forced"
  const val Component = "component"
  const val Stack = "stack"
  const val Cause = "cause"
}

object LithoDebugEvents {

  object TreeFuture {

    @JvmStatic
    fun run(treeId: Int, name: String) {
      dispatch(type = LithoDebugEvent.TreeFutureRun, treeId = treeId) { attrs ->
        attrs[DebugEventAttribute.Name] = name
        attrs[DebugEventAttribute.ThreadPriority] = Process.getThreadPriority(Process.myTid())
      }
    }

    @JvmStatic
    fun wait(treeId: Int, name: String) {
      dispatch(type = LithoDebugEvent.TreeFutureWait, treeId = treeId) { attrs ->
        attrs[DebugEventAttribute.Name] = name
        attrs[DebugEventAttribute.ThreadPriority] = Process.getThreadPriority(Process.myTid())
      }
    }

    @JvmStatic
    fun get(treeId: Int, name: String, wasInterrupted: Boolean) {
      dispatch(type = LithoDebugEvent.TreeFutureGet, treeId = treeId) { attrs ->
        attrs[DebugEventAttribute.Name] = name
        attrs[DebugEventAttribute.WasInterrupted] = wasInterrupted
        attrs[DebugEventAttribute.ThreadPriority] = Process.getThreadPriority(Process.myTid())
      }
    }

    @JvmStatic
    fun resume(treeId: Int, name: String) {
      dispatch(type = LithoDebugEvent.TreeFutureResume, treeId = treeId) { attrs ->
        attrs[DebugEventAttribute.Name] = name
        attrs[DebugEventAttribute.ThreadPriority] = Process.getThreadPriority(Process.myTid())
      }
    }

    @JvmStatic
    fun interrupt(treeId: Int, name: String) {
      dispatch(type = LithoDebugEvent.TreeFutureInterrupt, treeId = treeId) { attrs ->
        attrs[DebugEventAttribute.Name] = name
        attrs[DebugEventAttribute.ThreadPriority] = Process.getThreadPriority(Process.myTid())
      }
    }
  }

  private fun dispatch(
      type: String,
      treeId: Int,
      putAttrs: (MutableMap<String, Any?>) -> Unit = {},
  ) {
    DebugEventDispatcher.dispatch(
        type = type,
        renderStateId = { treeId.toString() },
    ) { attrs ->
      putAttrs(attrs)
    }
  }
}
