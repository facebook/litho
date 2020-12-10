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

package com.facebook.litho.testing.logging;

import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.litho.DefaultComponentsReporter;
import com.facebook.rendercore.LogLevel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestComponentsReporter extends DefaultComponentsReporter {

  private final List<Pair<LogLevel, String>> mLoggedMessages = new LinkedList<>();

  public List<Pair<LogLevel, String>> getLoggedMessages() {
    return mLoggedMessages;
  }

  @Override
  public void report(
      LogLevel level,
      String categoryKey,
      String message,
      @Nullable Throwable cause,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata) {
    super.report(level, categoryKey, message, cause, samplingFrequency, metadata);
    mLoggedMessages.add(new Pair<>(level, message));
  }

  public boolean hasMessageType(LogLevel logLevel) {
    for (Pair<LogLevel, String> loggedMessage : mLoggedMessages) {
      if (loggedMessage.first == logLevel) {
        return true;
      }
    }
    return false;
  }
}
