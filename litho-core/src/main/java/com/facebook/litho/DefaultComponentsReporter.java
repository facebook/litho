/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import android.util.Log;

public class DefaultComponentsReporter implements ComponentsReporter.Reporter {
  private static final String CATEGORY = "Components";

  @Override
  public void emitMessage(ComponentsReporter.LogLevel level, String message) {
    emitMessage(level, message, /* take default*/ 0);
  }

  @Override
  public void emitMessage(
      ComponentsReporter.LogLevel level, String message, int samplingFrequency) {
    switch (level) {
      case WARNING:
        Log.w(CATEGORY, message);
        break;
      case ERROR:
        Log.e(CATEGORY, message);
        break;
      case FATAL:
        Log.e(CATEGORY, message);
        throw new RuntimeException(message);
    }
  }
}
