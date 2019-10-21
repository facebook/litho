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

package com.facebook.litho.intellij.logging;

import com.facebook.litho.intellij.extensions.EventLogger;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class DebounceEventLoggerTest {

  @Test
  public void logNoDebounce() {
    TestLogger testLogger = new TestLogger();
    DebounceEventLogger debounceEventLogger = new DebounceEventLogger(-1, testLogger);

    int count = 100;
    for (int i = 0; i < count; i++) {
      debounceEventLogger.log(null);
    }

    Assert.assertEquals(count, testLogger.count);
  }

  @Test
  public void logDebounce() {
    TestLogger testLogger = new TestLogger();
    DebounceEventLogger debounceEventLogger = new DebounceEventLogger(10, testLogger);

    for (int i = 0; i < 100; i++) {
      debounceEventLogger.log(null);
    }

    Assert.assertEquals(1, testLogger.count);
  }

  @Test
  public void logDebouncePaused() throws InterruptedException {
    TestLogger testLogger = new TestLogger();
    DebounceEventLogger debounceEventLogger = new DebounceEventLogger(10, testLogger);

    int count = 5;
    for (int i = 0; i < count; i++) {
      debounceEventLogger.log(null);
      Thread.sleep(15);
    }

    Assert.assertEquals(count, testLogger.count);
  }

  static class TestLogger implements EventLogger {
    int count = 0;

    @Override
    public void log(String event, Map<String, String> metadata) {
      count++;
    }
  }
}
