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

import com.facebook.rendercore.ErrorReporter
import com.facebook.rendercore.ErrorReporterDelegate

/**
 * This is intended as a hook into `android.util.Log`, but allows you to provide your own
 * functionality. Use it as `ComponentsReporter.emitMessage(level, message);` As a default, it
 * simply calls `android.util.Log` (see [DefaultComponentsReporter]). You may supply your own with
 * [ComponentsReporter.provide].
 */
object ComponentsReporter {

  @JvmStatic
  fun provide(instance: ErrorReporterDelegate?) {
    ErrorReporter.provide(instance)
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level One of [LogLevel.WARNING], [LogLevel.ERROR], [LogLevel.FATAL].
   * @param categoryKey Unique key for aggregating all occurrences of given error in error
   *   aggregation systems
   * @param message Message to log
   * @param samplingFrequency sampling frequency to override default one
   * @param metadata map of metadata associated with the message
   */
  @JvmOverloads
  @JvmStatic
  fun emitMessage(
      level: LogLevel,
      categoryKey: String,
      message: String,
      samplingFrequency: Int = 0,
      metadata: Map<String?, Any?>? = null
  ) {
    ErrorReporter.report(map(level), categoryKey, message, null, samplingFrequency, metadata)
  }

  @JvmStatic
  fun map(level: LogLevel): com.facebook.rendercore.LogLevel {
    return when (level) {
      LogLevel.WARNING -> com.facebook.rendercore.LogLevel.WARNING
      LogLevel.ERROR -> com.facebook.rendercore.LogLevel.ERROR
      LogLevel.FATAL -> com.facebook.rendercore.LogLevel.FATAL
    }
  }

  enum class LogLevel {
    WARNING,
    ERROR,
    FATAL
  }
}
