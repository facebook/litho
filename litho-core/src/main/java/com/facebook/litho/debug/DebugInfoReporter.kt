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

import android.os.Build
import android.util.Log
import com.facebook.litho.config.LithoDebugConfigurations
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

  /** Detects if running in a test environment (Robolectric/JUnit) vs actual debug build */
  @JvmStatic
  fun isTestEnvironment(): Boolean {
    // Check for Robolectric
    if (Build.FINGERPRINT == "robolectric") {
      return true
    }
    // Check for JUnit
    try {
      Class.forName("org.junit.runner.JUnitCore")
      return true
    } catch (e: ClassNotFoundException) {
      return false
    }
  }

  /**
   * @param [category] the debug event category. It is saved as the 'name' attribute in QPL
   * @param [level] the log level; default is [LogLevel.DEBUG]
   */
  @JvmStatic
  @JvmOverloads
  fun report(
      category: String,
      level: LogLevel = LogLevel.DEBUG,
      renderStateId: Int = RenderState.NO_ID,
      write: MutableMap<String, Any?>.() -> Unit,
  ) {
    when (level) {
      LogLevel.VERBOSE,
      LogLevel.WARNING -> {
        val attributes = mutableMapOf<String, Any?>()
        attributes.write()
        Log.w("LithoWarning:", "$category\n$attributes")
      }
      LogLevel.DEBUG -> {
        dispatch(
            LithoDebugEvent.DebugInfo,
            if (renderStateId == RenderState.NO_ID) {
              NoRenderStateId
            } else {
              { renderStateId.toString() }
            },
            level,
        ) { attribute ->
          attribute.write()
          attribute[Name] = category
        }
      }
      LogLevel.ERROR -> {
        if (LithoDebugConfigurations.isDebugModeEnabled && !isTestEnvironment()) {
          val attributes = mutableMapOf<String, Any?>()
          attributes.write()
          throw RuntimeException("LithoError:$category\n$attributes")
        } else {
          dispatch(
              LithoDebugEvent.DebugInfo,
              if (renderStateId == RenderState.NO_ID) {
                NoRenderStateId
              } else {
                { renderStateId.toString() }
              },
              level,
          ) { attribute ->
            attribute.write()
            attribute[Name] = category
          }
        }
      }
      LogLevel.FATAL -> {
        val attributes = mutableMapOf<String, Any?>()
        attributes.write()
        throw RuntimeException("LithoError:$category\n$attributes")
      }
    }
  }

  /**
   * @param [category] the debug event category. It is saved as the 'name' attribute in QPL
   * @param [level] the log level; default is [LogLevel.DEBUG]
   */
  @JvmStatic
  @JvmOverloads
  inline fun report(
      category: () -> String,
      level: LogLevel = LogLevel.DEBUG,
      renderStateId: Int = RenderState.NO_ID,
      write: MutableMap<String, Any?>.() -> Unit,
  ) {
    when (level) {
      LogLevel.VERBOSE,
      LogLevel.WARNING -> {
        val attributes = mutableMapOf<String, Any?>()
        attributes.write()
        Log.w("LithoWarning:", "${category.invoke()}\n$attributes")
      }
      LogLevel.DEBUG -> {
        dispatch(
            LithoDebugEvent.DebugInfo,
            if (renderStateId == RenderState.NO_ID) {
              NoRenderStateId
            } else {
              { renderStateId.toString() }
            },
            level,
        ) { attribute ->
          attribute.write()
          attribute[Name] = category()
        }
      }
      LogLevel.ERROR -> {
        if (LithoDebugConfigurations.isDebugModeEnabled && !isTestEnvironment()) {
          val attributes = mutableMapOf<String, Any?>()
          attributes.write()
          throw RuntimeException("LithoError:${category.invoke()}\n$attributes")
        } else {
          dispatch(
              LithoDebugEvent.DebugInfo,
              if (renderStateId == RenderState.NO_ID) {
                NoRenderStateId
              } else {
                { renderStateId.toString() }
              },
              level,
          ) { attribute ->
            attribute.write()
            attribute[Name] = category()
          }
        }
      }
      LogLevel.FATAL -> {
        val attributes = mutableMapOf<String, Any?>()
        attributes.write()
        throw RuntimeException("LithoError:${category.invoke()}\n$attributes")
      }
    }
  }
}
