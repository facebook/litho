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

package com.facebook.litho;

import androidx.annotation.Nullable;
import java.util.Map;

/**
 * This is intended as a hook into {@code android.util.Log}, but allows you to provide your own
 * functionality. Use it as
 *
 * <p>{@code ComponentsReporter.emitMessage(level, message);} As a default, it simply calls {@code
 * android.util.Log} (see {@link DefaultComponentsReporter}). You may supply your own with {@link
 * ComponentsReporter#provide(ComponentsReporter.Reporter)}.
 */
public class ComponentsReporter {

  public enum LogLevel {
    WARNING,
    ERROR,
    FATAL
  }

  private static volatile Reporter sInstance = null;

  public interface Reporter {
    /**
     * Emit a message that can be logged or escalated by the logger implementation.
     *
     * @param level
     * @param categoryKey Unique key for aggregating all occurrences of given error in error
     *     aggregation systems
     * @param message Message to log
     */
    void emitMessage(LogLevel level, String categoryKey, String message);

    /**
     * Emit a message that can be logged or escalated by the logger implementation.
     *
     * @param level
     * @param categoryKey Unique key for aggregating all occurrences of given error in error
     *     aggregation systems
     * @param message Message to log
     * @param samplingFrequency sampling frequency to override default one
     */
    void emitMessage(LogLevel level, String categoryKey, String message, int samplingFrequency);

    /**
     * Emit a message that can be logged or escalated by the logger implementation.
     *
     * @param level
     * @param categoryKey Unique key for aggregating all occurrences of given error in error
     *     aggregation systems
     * @param message Message to log
     * @param samplingFrequency sampling frequency to override default one
     * @param metadata map of metadata associated with the message
     */
    void emitMessage(
        LogLevel level,
        String categoryKey,
        String message,
        int samplingFrequency,
        @Nullable Map<String, Object> metadata);
  }

  private ComponentsReporter() {}

  public static void provide(Reporter instance) {
    sInstance = instance;
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level
   * @param categoryKey Unique key for aggregating all occurrences of given error in error
   *     aggregation systems
   * @param message Message to log
   */
  public static void emitMessage(LogLevel level, String categoryKey, String message) {
    getInstance().emitMessage(level, categoryKey, message);
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level One of {@link LogLevel#WARNING}, {@link LogLevel#ERROR}, {@link LogLevel#FATAL}.
   * @param categoryKey Unique key for aggregating all occurrences of given error in error
   *     aggregation systems
   * @param message Message to log
   * @param samplingFrequency sampling frequency to override default one
   */
  public static void emitMessage(
      LogLevel level, String categoryKey, String message, int samplingFrequency) {
    getInstance().emitMessage(level, categoryKey, message, samplingFrequency);
  }

  /**
   * Emit a message that can be logged or escalated by the logger implementation.
   *
   * @param level One of {@link LogLevel#WARNING}, {@link LogLevel#ERROR}, {@link LogLevel#FATAL}.
   * @param categoryKey Unique key for aggregating all occurrences of given error in error
   *     aggregation systems
   * @param message Message to log
   * @param samplingFrequency sampling frequency to override default one
   * @param metadata map of metadata associated with the message
   */
  public static void emitMessage(
      LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata) {
    getInstance().emitMessage(level, categoryKey, message, samplingFrequency, metadata);
  }

  private static Reporter getInstance() {
    if (sInstance == null) {
      synchronized (ComponentsReporter.class) {
        if (sInstance == null) {
          sInstance = new DefaultComponentsReporter();
        }
      }
    }
    return sInstance;
  }
}
