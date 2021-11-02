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

public interface ErrorReporterDelegate {

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   */
  void report(LogLevel level, String categoryKey, String message);

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param cause Cause to log.
   */
  void report(LogLevel level, String categoryKey, String message, Throwable cause);

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param samplingFrequency Sampling frequency to override default one.
   */
  void report(LogLevel level, String categoryKey, String message, int samplingFrequency);

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param cause Cause to log.
   * @param samplingFrequency Sampling frequency to override default one.
   */
  void report(
      LogLevel level, String categoryKey, String message, Throwable cause, int samplingFrequency);

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level The log level.
   * @param categoryKey Unique key for aggregation.
   * @param message Message to log.
   * @param samplingFrequency sampling frequency to override default one.
   * @param metadata map of metadata associated with the message.
   */
  void report(
      LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata);

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
  void report(
      LogLevel level,
      String categoryKey,
      String message,
      @Nullable Throwable cause,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata);
}
