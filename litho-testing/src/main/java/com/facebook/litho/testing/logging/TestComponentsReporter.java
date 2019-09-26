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

import android.util.Pair;
import com.facebook.litho.ComponentsReporter;
import com.facebook.litho.DefaultComponentsReporter;
import java.util.LinkedList;
import java.util.List;

public class TestComponentsReporter extends DefaultComponentsReporter {
  private final List<Pair<ComponentsReporter.LogLevel, String>> mLoggedMessages =
      new LinkedList<>();

  @Override
  public void emitMessage(ComponentsReporter.LogLevel level, String categoryKey, String message) {
    emitMessage(level, categoryKey, message, 0);
  }

  @Override
  public void emitMessage(
      ComponentsReporter.LogLevel level,
      String categoryKey,
      String message,
      int samplingFrequency) {
    super.emitMessage(level, categoryKey, message, samplingFrequency);

    mLoggedMessages.add(new Pair<>(level, message));
  }

  public List<Pair<ComponentsReporter.LogLevel, String>> getLoggedMessages() {
    return mLoggedMessages;
  }

  public boolean hasMessageType(ComponentsReporter.LogLevel logLevel) {
    for (Pair<ComponentsReporter.LogLevel, String> loggedMessage : mLoggedMessages) {
      if (loggedMessage.first == logLevel) {
        return true;
      }
    }
    return false;
  }
}
