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

package com.facebook.litho;

import static com.facebook.litho.ComponentsReporter.map;

import android.util.Log;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.ErrorReporterDelegate;
import com.facebook.rendercore.LogLevel;
import java.util.Map;

public class DefaultComponentsReporter implements ErrorReporterDelegate {

  private static final String CATEGORY_PREFIX = "Litho:";

  @Override
  public void report(
      LogLevel level,
      String categoryKey,
      String message,
      @Nullable Throwable cause,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata) {
    switch (level) {
      case WARNING:
        if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
          Log.w(CATEGORY_PREFIX + categoryKey, message, cause);
        }
        break;
      case ERROR:
        if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
          Log.e(CATEGORY_PREFIX + categoryKey, message, cause);
        }
        break;
      case FATAL:
        if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
          Log.e(CATEGORY_PREFIX + categoryKey, message, cause);
        }
        throw new RuntimeException(message);
    }
  }

  public void emitMessage(ComponentsReporter.LogLevel level, String categoryKey, String message) {
    emitMessage(level, categoryKey, message, /* take default*/ 0);
  }

  public void emitMessage(
      ComponentsReporter.LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency) {
    emitMessage(level, categoryKey, message, samplingFrequency, null);
  }

  public void emitMessage(
      ComponentsReporter.LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata) {
    report(map(level), categoryKey, message, null, samplingFrequency, metadata);
  }
}
