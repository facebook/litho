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

import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Map;

public class DefaultComponentsReporter implements ComponentsReporter.Reporter {
  private static final String CATEGORY_PREFIX = "Litho:";

  @Override
  public void emitMessage(ComponentsReporter.LogLevel level, String categoryKey, String message) {
    emitMessage(level, categoryKey, message, /* take default*/ 0);
  }

  @Override
  public void emitMessage(
      ComponentsReporter.LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency) {
    emitMessage(level, categoryKey, message, samplingFrequency, null);
  }

  @Override
  public void emitMessage(
      ComponentsReporter.LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata) {
    switch (level) {
      case WARNING:
        Log.w(CATEGORY_PREFIX + categoryKey, message);
        break;
      case ERROR:
        Log.e(CATEGORY_PREFIX + categoryKey, message);
        break;
      case FATAL:
        Log.e(CATEGORY_PREFIX + categoryKey, message);
        throw new RuntimeException(message);
    }
  }
}
