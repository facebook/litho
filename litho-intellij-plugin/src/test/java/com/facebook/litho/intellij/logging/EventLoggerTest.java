/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.intellij.logging;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.intellij.openapi.extensions.Extensions;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Test;

public class EventLoggerTest extends LithoPluginIntellijTest {

  public EventLoggerTest() {
    super("testdata/logging");
  }

  @Test
  public void noTestLogging() {
    EventLogger[] extensions =
        Extensions.getExtensions(LithoLoggerProvider.LithoEventLogger.EP_NAME);
    Assert.assertEquals(0, extensions.length);
  }

  @Test
  public void nameTest() {
    Assert.assertEquals(
        "com.facebook.litho.intellij.eventLogger",
        LithoLoggerProvider.LithoEventLogger.EP_NAME.getName());
  }

  @Test
  public void logSyncTest() {
    TestLogger testLogger = new TestLogger();
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    new LithoLoggerProvider.LithoEventLogger(loggers, Runnable::run).log(event);
    Assert.assertEquals(event, testLogger.event);
  }

  @Test
  public void logAsyncTest() throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    TestLogger testLogger = new TestLogger(countDownLatch);
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    new LithoLoggerProvider.LithoEventLogger(loggers).log(event);
    countDownLatch.await();
    Assert.assertEquals(event, testLogger.event);
  }

  @Test
  public void nonBlockingTest() {
    TestLogger testLogger = new TestLogger();
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    new LithoLoggerProvider.LithoEventLogger(loggers).log(event);
    Assert.assertNotEquals(event, testLogger.event);
  }

  static class TestLogger implements EventLogger {
    private final CountDownLatch lock;
    String event;

    TestLogger(CountDownLatch lock) {
      this.lock = lock;
    }

    TestLogger() {
      this.lock = new CountDownLatch(1);
    }

    @Override
    public void log(String event) {
      this.event = event;
      lock.countDown();
    }

    @Override
    public void log(String event, Map<String, String> metadata) {
      log(event);
    }
  }
}
