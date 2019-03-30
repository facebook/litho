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
package com.facebook.litho.testing.logging;

import com.facebook.litho.ComponentsReporter;
import com.facebook.litho.DefaultComponentsReporter;

public class TestComponentsReporter extends DefaultComponentsReporter {
  private ComponentsReporter.LogLevel mLevel;
  private String mMessage;

  @Override
  public void emitMessage(ComponentsReporter.LogLevel level, String message) {
    emitMessage(level, message, 0);
  }

  @Override
  public void emitMessage(
      ComponentsReporter.LogLevel level, String message, int samplingFrequency) {
    super.emitMessage(level, message, samplingFrequency);

    mLevel = level;
    mMessage = message;
  }

  public ComponentsReporter.LogLevel getLevel() {
    return mLevel;
  }

  public String getMessage() {
    return mMessage;
  }
}
