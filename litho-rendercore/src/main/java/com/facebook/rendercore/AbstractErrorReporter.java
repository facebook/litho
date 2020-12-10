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
import com.facebook.infer.annotation.OkToExtend;
import java.util.Map;

@OkToExtend
public abstract class AbstractErrorReporter implements ErrorReporterDelegate {

  @Override
  public void report(final LogLevel level, final String categoryKey, final String message) {
    report(level, categoryKey, message, null, 0, null);
  }

  @Override
  public void report(
      final LogLevel level, final String categoryKey, final String message, final Throwable cause) {
    report(level, categoryKey, message, cause, 0, null);
  }

  @Override
  public void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final int samplingFrequency) {
    report(level, categoryKey, message, null, samplingFrequency, null);
  }

  @Override
  public void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final Throwable cause,
      final int samplingFrequency) {
    report(level, categoryKey, message, cause, samplingFrequency, null);
  }

  @Override
  public void report(
      final LogLevel level,
      final String categoryKey,
      final String message,
      final int samplingFrequency,
      final @Nullable Map<String, Object> metadata) {
    report(level, categoryKey, message, null, samplingFrequency, metadata);
  }
}
