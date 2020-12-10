/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.rendercore;

import androidx.annotation.Nullable;
import java.util.Map;

/**
 * This is intended as a hook into {@code android.util.Log}, but allows you to provide your own
 * functionality. Use it as
 *
 * <p>{@code ErrorReporter.emitMessage(level, message);} As a default, it simply calls {@code
 * android.util.Log} (see {@link DefaultErrorReporter}). You may supply your own with {@link
 * ErrorReporter#provide(ErrorReporterDelegate)}.
 */
public class ErrorReporter {

  private static volatile ErrorReporterDelegate sInstance = null;

  private ErrorReporter() {}

  public static void provide(ErrorReporterDelegate instance) {
    sInstance = instance;
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   */
  public static void report(final LogLevel level, final String categoryKey, final String message) {
    getInstance().report(level, categoryKey, message);
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param cause Cause to log.
   */
  public static void report(
      final LogLevel level, final String categoryKey, final String message, final Throwable cause) {
    getInstance().report(level, categoryKey, message, cause);
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param samplingFrequency Sampling frequency to override default one.
   */
  public static void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final int samplingFrequency) {
    getInstance().report(level, categoryKey, message, samplingFrequency);
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param cause Cause to log.
   * @param samplingFrequency Sampling frequency to override default one.
   */
  public static void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final Throwable cause,
      final int samplingFrequency) {
    getInstance().report(level, categoryKey, message, cause, samplingFrequency);
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log
   * @param samplingFrequency sampling frequency to override default one
   * @param metadata map of metadata associated with the message
   */
  public static void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final int samplingFrequency,
      final @Nullable Map<String, Object> metadata) {
    getInstance().report(level, categoryKey, message, samplingFrequency, metadata);
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
  public static void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final Throwable cause,
      final int samplingFrequency,
      final @Nullable Map<String, Object> metadata) {
    getInstance().report(level, categoryKey, message, cause, samplingFrequency, metadata);
  }

  private static ErrorReporterDelegate getInstance() {
    if (sInstance == null) {
      synchronized (ErrorReporter.class) {
        if (sInstance == null) {
          sInstance = new DefaultErrorReporter();
        }
      }
    }
    return sInstance;
  }
}
