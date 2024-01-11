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

package com.facebook.rendercore

/**
 * This is intended as a hook into `android.util.Log`, but allows you to provide your own
 * functionality. Use it as
 *
 * `ErrorReporter.emitMessage(level, message);` As a default, it simply calls `android.util.Log`
 * (see [DefaultErrorReporter]). You may supply your own with [ErrorReporter.provide].
 */
object ErrorReporter {
  @Volatile private var INSTANCE: ErrorReporterDelegate? = null

  @get:JvmStatic
  val instance: ErrorReporterDelegate
    get() {
      if (INSTANCE == null) {
        synchronized(ErrorReporter::class.java) {
          if (INSTANCE == null) {
            INSTANCE = DefaultErrorReporter()
          }
        }
      }
      return checkNotNull(INSTANCE)
    }

  @JvmStatic
  fun provide(instance: ErrorReporterDelegate) {
    INSTANCE = instance
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log. * @param cause Cause to log.
   * @param cause Cause to log.
   * @param samplingFrequency sampling frequency to override default one.
   * @param metadata map of metadata associated with the message.
   */
  @JvmOverloads
  @JvmStatic
  fun report(
      level: LogLevel,
      categoryKey: String,
      message: String,
      cause: Throwable? = null,
      samplingFrequency: Int = 0,
      metadata: Map<String?, Any?>? = null
  ) {
    instance.report(level, categoryKey, message, cause, samplingFrequency, metadata)
  }
}
